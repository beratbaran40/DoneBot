// Detekt without type resolution mis-flags the private `.toUiItem` extension fns at the
// bottom of this file as unused, even though they're called from `loadGroupDetail`.
// It also flags trailing code after `?: return@mapNotNull null` guards as unreachable.
@file:Suppress("UnusedPrivateMember", "UnreachableCode")

package com.todoapp.mobile.ui.groups.groupdetail

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.GroupActivity
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.LanguageRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupActivityUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupMemberUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupTaskUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiAction
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiEffect
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiState
import com.todoapp.mobile.ui.home.ExistingPhoto
import com.todoapp.mobile.ui.home.PendingPhoto
import com.todoapp.mobile.ui.home.TaskFormState
import com.todoapp.mobile.ui.home.TaskFormUiAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass", "TooManyFunctions")
class GroupDetailViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val languageRepository: LanguageRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Screen.GroupDetail>()
    private val groupId = route.groupId

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var currentUserId: Long = -1L
    private var currentUserName: String = ""
    private var currentUserInitials: String = ""
    private var pendingDeleteJob: Job? = null
    private var appLocale: Locale = Locale.getDefault()

    init {
        viewModelScope.launch { appLocale = languageRepository.getCurrentLanguage().toLocale() }
        loadGroupData()
    }

    override fun onCleared() {
        super.onCleared()
        val previousId = (_uiState.value as? UiState.Success)?.undoDeleteTaskId ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        CoroutineScope(SupervisorJob()).launch {
            groupRepository
                .deleteGroupTask(groupId, previousId)
                .onFailure {
                    android.util.Log.e("GroupDetailViewModel", "Failed to flush pending task delete", it)
                }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnTabSelected -> updateSuccessState { it.copy(selectedTab = action.index) }
            is UiAction.OnTaskFilterSelected -> updateSuccessState { it.copy(taskFilter = action.filter) }
            is UiAction.OnStatusFilterSelected -> updateSuccessState { it.copy(statusFilter = action.status) }
            is UiAction.OnTimeFilterSelected -> updateSuccessState { it.copy(timeFilter = action.filter) }
            is UiAction.OnTaskChecked -> handleTaskChecked(action.taskId, action.isChecked)
            UiAction.OnNewTaskTap -> updateSuccessState { it.copy(isTaskSheetOpen = true) }
            UiAction.OnDismissGroupTaskSheet ->
                updateSuccessState {
                    it.copy(
                        isTaskSheetOpen = false,
                        taskFormState = TaskFormState(),
                        editingTaskId = null,
                    )
                }

            UiAction.OnGroupTaskCreate -> createGroupTask()
            is UiAction.OnGroupTaskFormAction -> handleTaskFormAction(action.action)
            UiAction.OnInviteTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.InviteMember(groupId)))
            is UiAction.OnRemoveMemberTap -> removeMember(action.userId)
            is UiAction.OnTaskTapped ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(
                        Screen.GroupTaskDetail(
                            groupId,
                            action.taskId,
                        ),
                    ),
                )

            is UiAction.OnTaskLongPress -> openEditSheet(action.taskId)
            is UiAction.OnDeleteTask -> updateSuccessState { it.copy(pendingDeleteTaskId = action.taskId) }
            is UiAction.OnAssignToMe -> onAssignToMe(action.taskId)
            UiAction.OnUndoDeleteTask -> undoDeleteTask()
            UiAction.OnScreenResumed -> loadGroupData()
            UiAction.OnAssignToMeConfirm -> confirmAssignToMe()
            UiAction.OnAssignToMeDismiss -> updateSuccessState { it.copy(pendingAssignTaskId = null) }
            UiAction.OnDeleteTaskConfirm -> confirmDeleteTask()
            UiAction.OnDeleteTaskDismiss -> updateSuccessState { it.copy(pendingDeleteTaskId = null) }
            UiAction.OnPullToRefresh -> {
                updateSuccessState { it.copy(isRefreshing = true) }
                loadGroupData(force = true)
            }
        }
    }

    private fun loadGroupData(force: Boolean = false) {
        viewModelScope.launch {
            val detailDeferred = async { groupRepository.getGroupDetail(groupId, force = force) }
            val tasksDeferred = async { groupRepository.getGroupTasks(groupId, force = force) }
            val activityDeferred = async { groupRepository.getGroupActivity(groupId, force = force) }

            val userResult = userRepository.getUserInfo()
            userResult.getOrNull()?.let { user ->
                currentUserId = user.id
                currentUserName = user.displayName
                currentUserInitials =
                    user.displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
            }

            val detailResult = detailDeferred.await()
            val tasksResult = tasksDeferred.await()
            val activityResult = activityDeferred.await()

            val detail = detailResult.getOrNull()
            if (detail == null) {
                _uiState.value = UiState.Error(detailResult.exceptionOrNull()?.message ?: "Failed to load group")
                return@launch
            }

            val tasks = tasksResult.getOrNull() ?: emptyList()
            val activities = activityResult.getOrNull() ?: emptyList()
            val members =
                detail.members.map { member ->
                    GroupMember(
                        userId = member.userId,
                        displayName = member.displayName,
                        email = member.email,
                        avatarUrl = member.avatarUrl,
                        role = member.role,
                        joinedAt = member.joinedAt,
                    )
                }

            val currentUserRole = members.find { it.userId == currentUserId }?.role?.uppercase().orEmpty()
            val membersById = members.associateBy { it.userId }
            val previousState = _uiState.value as? UiState.Success

            _uiState.value =
                UiState.Success(
                    groupId = groupId,
                    groupName = detail.name,
                    description = detail.description,
                    memberCount = members.size,
                    completedCount = tasks.count { it.isCompleted },
                    pendingCount = tasks.count { !it.isCompleted },
                    tasks = tasks.map { it.toUiItem(currentUserRole, membersById) },
                    members = members.map { it.toUiItem(currentUserId) },
                    activities = activities.map { it.toUiItem() },
                    currentUserRole = currentUserRole,
                    selectedTab = previousState?.selectedTab ?: 0,
                    taskFilter = previousState?.taskFilter ?: GroupDetailContract.TaskFilter.ALL,
                    statusFilter = previousState?.statusFilter ?: GroupDetailContract.GroupTaskStatusFilter.ALL,
                    timeFilter = previousState?.timeFilter ?: GroupDetailContract.GroupTaskTimeFilter.ALL,
                    isTaskSheetOpen = previousState?.isTaskSheetOpen ?: false,
                    taskFormState = previousState?.taskFormState ?: TaskFormState(),
                    editingTaskId = previousState?.editingTaskId,
                    pendingDeleteTaskId = previousState?.pendingDeleteTaskId,
                    pendingAssignTaskId = previousState?.pendingAssignTaskId,
                    isRefreshing = false,
                )
        }
    }

    private fun handleTaskChecked(
        taskId: Long,
        isChecked: Boolean,
    ) {
        val state = _uiState.value as? UiState.Success ?: return
        val task = state.tasks.find { it.id == taskId } ?: return
        val isAdmin = state.currentUserRole.uppercase() == "ADMIN"
        if (!isAdmin && !task.isAssignedToMe) {
            _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.only_assignee_can_complete)))
            return
        }
        updateSuccessState { s ->
            s.copy(
                tasks =
                s.tasks.map { t ->
                    if (t.id == taskId) t.copy(isCompleted = isChecked) else t
                },
                completedCount = if (isChecked) s.completedCount + 1 else s.completedCount - 1,
                pendingCount = if (isChecked) s.pendingCount - 1 else s.pendingCount + 1,
            )
        }
        val groupTask =
            GroupTask(
                id = task.id,
                title = task.title,
                description = task.description,
                isCompleted = task.isCompleted,
                priority = task.priority,
                dueDate = task.rawDueDate,
                assignee = null,
            )
        viewModelScope.launch {
            groupRepository
                .updateGroupTaskStatus(groupId, taskId, groupTask, isChecked)
                .onFailure {
                    updateSuccessState { s ->
                        s.copy(
                            tasks =
                            s.tasks.map { t ->
                                if (t.id == taskId) t.copy(isCompleted = !isChecked) else t
                            },
                            completedCount = if (isChecked) s.completedCount - 1 else s.completedCount + 1,
                            pendingCount = if (isChecked) s.pendingCount + 1 else s.pendingCount - 1,
                        )
                    }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                }
        }
    }

    private fun removeMember(userId: Long) {
        viewModelScope.launch {
            groupRepository
                .removeMember(groupId, userId)
                .onSuccess {
                    updateSuccessState { state ->
                        state.copy(
                            members = state.members.filter { it.userId != userId },
                            memberCount = state.memberCount - 1,
                        )
                    }
                    _uiEffect.trySend(UiEffect.ShowToast("Member removed"))
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast("Failed to remove member"))
                }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun handleTaskFormAction(action: TaskFormUiAction) {
        updateSuccessState { s ->
            val f = s.taskFormState
            val updated =
                when (action) {
                    is TaskFormUiAction.TitleChange -> f.copy(taskTitle = action.title)
                    is TaskFormUiAction.DateSelect -> f.copy(dialogSelectedDate = action.date)
                    TaskFormUiAction.DateDeselect -> f.copy(dialogSelectedDate = null)
                    is TaskFormUiAction.TimeStartChange -> f.copy(taskTimeStart = action.time)
                    is TaskFormUiAction.TimeEndChange -> f.copy(taskTimeEnd = action.time)
                    is TaskFormUiAction.DescriptionChange -> f.copy(taskDescription = action.description)
                    TaskFormUiAction.ToggleAdvancedSettings ->
                        f.copy(
                            isAdvancedSettingsExpanded = !f.isAdvancedSettingsExpanded,
                        )
                    is TaskFormUiAction.SecretChange -> f.copy(isTaskSecret = action.isSecret)
                    is TaskFormUiAction.ReminderOffsetChange -> f.copy(reminderOffsetMinutes = action.minutes)
                    is TaskFormUiAction.PriorityChange -> f.copy(selectedPriority = action.priority)
                    is TaskFormUiAction.AssigneeChange -> f.copy(selectedAssigneeId = action.userId)
                    TaskFormUiAction.Dismiss -> return@updateSuccessState s.copy(
                        isTaskSheetOpen = false,
                        taskFormState = TaskFormState(),
                    )

                    TaskFormUiAction.Create -> return@updateSuccessState s
                    is TaskFormUiAction.GroupSelectionChanged -> return@updateSuccessState s
                    is TaskFormUiAction.PhotoPicked ->
                        f.copy(
                            pendingPhotos =
                            f.pendingPhotos +
                                PendingPhoto(action.bytes, action.mimeType),
                        )
                    is TaskFormUiAction.PhotoRemoveAt ->
                        f.copy(
                            pendingPhotos = f.pendingPhotos.filterIndexed { i, _ -> i != action.index },
                        )
                    is TaskFormUiAction.ExistingPhotoToggleDelete ->
                        f.copy(
                            photoIdsToDelete =
                            if (action.photoId in f.photoIdsToDelete) {
                                f.photoIdsToDelete - action.photoId
                            } else {
                                f.photoIdsToDelete + action.photoId
                            },
                        )
                    is TaskFormUiAction.CategoryChange -> f.copy(selectedCategory = action.category)
                    is TaskFormUiAction.CustomCategoryNameChange -> f.copy(customCategoryName = action.name)
                    is TaskFormUiAction.RecurrenceChange -> f.copy(selectedRecurrence = action.recurrence)
                    is TaskFormUiAction.AllDayChange ->
                        f.copy(
                            isAllDay = action.isAllDay,
                            taskTimeStart = if (action.isAllDay) null else f.taskTimeStart,
                            taskTimeEnd = if (action.isAllDay) null else f.taskTimeEnd,
                            timeErrorRes = if (action.isAllDay) null else f.timeErrorRes,
                        )
                    is TaskFormUiAction.LocationPicked ->
                        f.copy(
                            locationName = action.name.takeIf { it.isNotBlank() },
                            locationAddress = action.address.takeIf { it.isNotBlank() },
                            locationLat = action.lat,
                            locationLng = action.lng,
                        )
                    TaskFormUiAction.LocationCleared ->
                        f.copy(
                            locationName = null,
                            locationAddress = null,
                            locationLat = null,
                            locationLng = null,
                        )
                }
            s.copy(taskFormState = updated)
        }
    }

    private fun openEditSheet(taskId: Long) {
        val state = _uiState.value as? UiState.Success ?: return
        if (state.currentUserRole.uppercase() != "ADMIN") {
            _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.only_admin_can_edit_task)))
            return
        }
        val task = state.tasks.find { it.id == taskId } ?: return
        val date =
            task.rawDueDate?.let {
                Instant
                    .ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        val time =
            task.rawDueDate?.let {
                Instant
                    .ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
            }
        updateSuccessState {
            it.copy(
                isTaskSheetOpen = true,
                editingTaskId = taskId,
                taskFormState =
                TaskFormState(
                    taskTitle = task.title,
                    taskDescription = task.description ?: "",
                    dialogSelectedDate = date,
                    taskTimeStart = time,
                    taskTimeEnd = time,
                    selectedPriority = task.priority,
                    selectedAssigneeId = task.assigneeId,
                    existingPhotos =
                    task.photoUrls.mapNotNull { url ->
                        val id = url.substringAfterLast('/').toLongOrNull() ?: return@mapNotNull null
                        ExistingPhoto(id = id, url = url)
                    },
                ),
            )
        }
    }

    private fun createGroupTask() {
        val state = _uiState.value as? UiState.Success ?: return
        val form = state.taskFormState
        if (form.taskTitle.isBlank() || form.dialogSelectedDate == null) {
            _uiEffect.trySend(UiEffect.ShowToast("Please fill in all required fields"))
            return
        }
        val timeStart = form.taskTimeStart ?: LocalTime.MIDNIGHT
        val dueDate =
            form.dialogSelectedDate
                .atTime(timeStart)
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()

        val editingId = state.editingTaskId
        if (editingId != null) {
            viewModelScope.launch {
                groupRepository
                    .updateGroupTask(
                        groupId = groupId,
                        taskId = editingId,
                        title = form.taskTitle,
                        description = form.taskDescription.ifBlank { null },
                        dueDate = dueDate,
                        priority = form.selectedPriority,
                        assignedToUserId = form.selectedAssigneeId,
                    ).onSuccess {
                        form.photoIdsToDelete.forEach { photoId ->
                            groupRepository.deleteTaskPhoto(editingId, photoId)
                        }
                        form.pendingPhotos.forEach { photo ->
                            groupRepository.uploadTaskPhoto(editingId, photo.bytes, photo.mimeType)
                        }
                        updateSuccessState {
                            it.copy(
                                isTaskSheetOpen = false,
                                taskFormState = TaskFormState(),
                                editingTaskId = null,
                            )
                        }
                        _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.task_updated)))
                        loadGroupData()
                    }.onFailure {
                        _uiEffect.trySend(
                            UiEffect.ShowToast(
                                it.message ?: context.getString(R.string.failed_to_update_task),
                            ),
                        )
                    }
            }
            return
        }

        val task =
            Task(
                title = form.taskTitle,
                description = form.taskDescription.ifBlank { null },
                date = form.dialogSelectedDate,
                timeStart = timeStart,
                timeEnd = form.taskTimeEnd ?: timeStart,
                isCompleted = false,
                isSecret = form.isTaskSecret,
            )
        val pendingPhotos = form.pendingPhotos
        viewModelScope.launch {
            groupRepository
                .createGroupTask(
                    groupId,
                    task,
                    priority = form.selectedPriority,
                    assignedToUserId = form.selectedAssigneeId,
                ).onSuccess { newTaskId ->
                    pendingPhotos.forEach { photo ->
                        groupRepository.uploadTaskPhoto(newTaskId, photo.bytes, photo.mimeType)
                    }
                    updateSuccessState { it.copy(isTaskSheetOpen = false, taskFormState = TaskFormState()) }
                    _uiEffect.trySend(UiEffect.ShowToast("Task added to group"))
                    loadGroupData()
                }.onFailure {
                    android.util.Log.e("GroupTaskCreate", "create failed", it)
                    _uiEffect.trySend(UiEffect.ShowToast(it.message ?: "Failed to create task"))
                }
        }
    }

    private fun confirmDeleteTask() {
        val state = _uiState.value as? UiState.Success ?: return
        val taskId = state.pendingDeleteTaskId ?: return
        val task = state.tasks.find { it.id == taskId } ?: return
        val wasCompleted = task.isCompleted

        flushPendingDelete()

        updateSuccessState { s ->
            s.copy(
                pendingDeleteTaskId = null,
                undoDeleteTaskId = taskId,
                completedCount = if (wasCompleted) s.completedCount - 1 else s.completedCount,
                pendingCount = if (!wasCompleted) s.pendingCount - 1 else s.pendingCount,
            )
        }

        pendingDeleteJob =
            viewModelScope.launch {
                delay(UNDO_DELAY_MS)
                updateSuccessState { it.copy(undoDeleteTaskId = null) }
                groupRepository
                    .deleteGroupTask(groupId, taskId)
                    .onFailure {
                        _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_delete_task)))
                        loadGroupData()
                    }
            }
    }

    private fun flushPendingDelete() {
        val previousId = (_uiState.value as? UiState.Success)?.undoDeleteTaskId ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccessState { it.copy(undoDeleteTaskId = null) }
        viewModelScope.launch {
            groupRepository
                .deleteGroupTask(groupId, previousId)
                .onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_delete_task)))
                    loadGroupData()
                }
        }
    }

    private fun undoDeleteTask() {
        val state = _uiState.value as? UiState.Success ?: return
        val taskId = state.undoDeleteTaskId ?: return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        val originalTask = state.tasks.find { it.id == taskId }
        val wasCompleted = originalTask?.isCompleted ?: false
        updateSuccessState { s ->
            s.copy(
                undoDeleteTaskId = null,
                completedCount = if (wasCompleted) s.completedCount + 1 else s.completedCount,
                pendingCount = if (!wasCompleted) s.pendingCount + 1 else s.pendingCount,
            )
        }
        loadGroupData()
    }

    private fun onAssignToMe(taskId: Long) {
        val state = _uiState.value as? UiState.Success ?: return
        val uiTask = state.tasks.find { it.id == taskId } ?: return
        when {
            uiTask.isAssignedToMe -> applyAssignToggle(taskId, isCurrentlyAssignedToMe = true)
            uiTask.assigneeId != null -> {
                if (state.currentUserRole.uppercase() == "ADMIN") applyUnassignOther(taskId)
            }
            else -> updateSuccessState { it.copy(pendingAssignTaskId = taskId) }
        }
    }

    private fun applyUnassignOther(taskId: Long) {
        updateSuccessState { s ->
            s.copy(
                tasks =
                s.tasks.map { t ->
                    if (t.id == taskId) {
                        t.copy(
                            isAssignedToMe = false,
                            assigneeName = null,
                            assigneeInitials = null,
                            assigneeId = null,
                            assigneeAvatarUrl = null,
                        )
                    } else {
                        t
                    }
                },
            )
        }
        viewModelScope.launch {
            groupRepository.unassignGroupTask(groupId, taskId).onFailure {
                _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                loadGroupData()
            }
        }
    }

    private fun confirmAssignToMe() {
        val state = _uiState.value as? UiState.Success ?: return
        val taskId = state.pendingAssignTaskId ?: return
        updateSuccessState { it.copy(pendingAssignTaskId = null) }
        applyAssignToggle(taskId, isCurrentlyAssignedToMe = false)
    }

    private fun applyAssignToggle(
        taskId: Long,
        isCurrentlyAssignedToMe: Boolean,
    ) {
        updateSuccessState { s ->
            s.copy(
                tasks =
                s.tasks.map { t ->
                    if (t.id == taskId) {
                        t.copy(
                            isAssignedToMe = !isCurrentlyAssignedToMe,
                            assigneeName = if (isCurrentlyAssignedToMe) null else currentUserName,
                            assigneeInitials = if (isCurrentlyAssignedToMe) null else currentUserInitials,
                            assigneeId = if (isCurrentlyAssignedToMe) null else currentUserId,
                            assigneeAvatarUrl =
                            if (isCurrentlyAssignedToMe) {
                                null
                            } else {
                                s.members.find { m -> m.userId == currentUserId }?.avatarUrl
                            },
                        )
                    } else {
                        t
                    }
                },
            )
        }
        viewModelScope.launch {
            val result =
                if (isCurrentlyAssignedToMe) {
                    groupRepository.unassignGroupTask(groupId, taskId)
                } else {
                    groupRepository.assignGroupTask(groupId, taskId, currentUserId)
                }
            result.onFailure {
                _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                loadGroupData()
            }
        }
    }

    private fun updateSuccessState(transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { current -> (current as? UiState.Success)?.let(transform) ?: current }
    }

    private fun GroupTask.toUiItem(
        currentUserRole: String = "",
        membersById: Map<Long, GroupMember> = emptyMap(),
    ): GroupTaskUiItem {
        val assigneeInitials =
            assignee
                ?.displayName
                ?.split(" ")
                ?.mapNotNull { it.firstOrNull()?.toString() }
                ?.take(2)
                ?.joinToString("")
        val isAssignedToMe = assignee?.userId == currentUserId
        // The /tasks list's assignedTo carries no avatar; resolve it from the loaded member list.
        val assigneeAvatar = assignee?.userId?.let { membersById[it]?.avatarUrl } ?: assignee?.avatarUrl
        return GroupTaskUiItem(
            id = id,
            title = title,
            description = description,
            assigneeId = assignee?.userId,
            assigneeAvatarUrl = assigneeAvatar,
            assigneeName = assignee?.displayName,
            assigneeInitials = assigneeInitials,
            dueTime = dueDate?.let { formatDueDate(it) },
            rawDueDate = dueDate,
            priority = priority,
            isCompleted = isCompleted,
            isAssignedToMe = isAssignedToMe,
            canDelete = currentUserRole.uppercase() == "ADMIN",
            photoUrls = photoUrls,
            locationName = locationName,
            locationAddress = locationAddress,
            locationLat = locationLat,
            locationLng = locationLng,
        )
    }

    private fun GroupMember.toUiItem(currentUserId: Long): GroupMemberUiItem {
        val initials =
            displayName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
        return GroupMemberUiItem(
            userId = userId,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            initials = initials.uppercase(),
            role = role,
            joinedAt = formatTimestamp(joinedAt),
            pendingTaskCount = pendingTaskCount,
            isCurrentUser = userId == currentUserId,
        )
    }

    private fun GroupActivity.toUiItem(): GroupActivityUiItem {
        val initials =
            actorName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
        return GroupActivityUiItem(
            id = id,
            type = type,
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl,
            actorInitials = initials.uppercase(),
            description = description,
            relativeTime = formatRelativeTime(timestamp),
            taskTitle = taskTitle,
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return context.getString(R.string.joined) + " " + sdf.format(Date(timestamp))
    }

    private fun localizedContext(): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(appLocale)
        return context.createConfigurationContext(config)
    }

    private fun formatDueDate(timestamp: Long): String {
        val ctx = localizedContext()
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        val date = Date(timestamp)
        return when {
            diff < TimeUnit.HOURS.toMillis(24) && diff > 0 -> {
                val sdf = SimpleDateFormat("HH:mm", appLocale)
                ctx.getString(R.string.due_prefix) + " " + sdf.format(date)
            }

            diff <= 0 && diff > -TimeUnit.HOURS.toMillis(24) -> {
                val sdf = SimpleDateFormat("HH:mm", appLocale)
                ctx.getString(R.string.due_today) + ", " + sdf.format(date)
            }

            else -> {
                val sdf = SimpleDateFormat("d MMM, HH:mm", appLocale)
                ctx.getString(R.string.due_prefix) + " " + sdf.format(date)
            }
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(60) ->
                context.getString(
                    R.string.minutes_ago,
                    TimeUnit.MILLISECONDS.toMinutes(diff).toInt(),
                )

            diff < TimeUnit.HOURS.toMillis(24) ->
                context.getString(
                    R.string.hours_ago,
                    TimeUnit.MILLISECONDS.toHours(diff).toInt(),
                )

            diff < TimeUnit.HOURS.toMillis(48) -> context.getString(R.string.yesterday)
            else -> context.getString(R.string.days_ago, TimeUnit.MILLISECONDS.toDays(diff).toInt())
        }
    }

    private companion object {
        const val UNDO_DELAY_MS = 5000L
    }
}
