package com.todoapp.mobile.ui.groups.grouptaskdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.common.deviceTimePattern
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.data.model.network.request.ReportTargetType
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.occurrenceIndex
import com.todoapp.mobile.domain.model.occurrenceTotal
import com.todoapp.mobile.domain.model.recurrenceRule
import com.todoapp.mobile.domain.model.startDate
import com.todoapp.mobile.domain.model.toStorageCsv
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.common.taskform.capabilities
import com.todoapp.mobile.ui.common.taskform.derivedTaskType
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.TaskUiModel
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiAction
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiEffect
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class GroupTaskDetailViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Screen.GroupTaskDetail>()
    private val groupId = route.groupId
    private val taskId = route.taskId

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var currentUserId: Long = -1L
    private var currentUserRole: String = ""

    init {
        loadTask()
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnBackTap -> _navEffect.trySend(NavigationEffect.Back)
            UiAction.OnToggleComplete -> toggleComplete()
            is UiAction.OnSubtaskToggle -> toggleSubtask(action.subtaskId, action.isCompleted)
            UiAction.OnEditTap -> openEditSheet()
            UiAction.OnEditDismiss -> updateSuccess { it.copy(isEditSheetOpen = false) }
            UiAction.OnEditSave -> saveEdit()
            is UiAction.OnEditTitleChange -> updateSuccess { it.copy(editTitle = action.title) }
            is UiAction.OnEditDescriptionChange -> updateSuccess { it.copy(editDescription = action.description) }
            is UiAction.OnEditDateSelect -> updateSuccess { it.copy(editDate = action.date) }
            UiAction.OnEditDateDeselect -> updateSuccess { it.copy(editDate = null) }
            is UiAction.OnEditAllDayChange -> changeEditAllDay(action.isAllDay)
            is UiAction.OnEditTimeStartChange -> updateSuccess { it.copy(editTimeStart = action.time) }
            is UiAction.OnEditTimeEndChange -> updateSuccess { it.copy(editTimeEnd = action.time) }
            is UiAction.OnEditAssigneeChange -> updateSuccess { it.copy(editAssigneeId = action.userId) }
            is UiAction.OnPhotoPicked -> uploadPhoto(action.bytes, action.mimeType)
            is UiAction.OnPhotoDelete -> deletePhoto(action.photoId)
            is UiAction.OnPhotoReport -> reportPhoto(action.relativeUrl)
            is UiAction.OnEditLocationPicked ->
                updateSuccess {
                    it.copy(
                        editLocationName = action.name.takeIf { v -> v.isNotBlank() },
                        editLocationAddress = action.address.takeIf { v -> v.isNotBlank() },
                        editLocationLat = action.lat,
                        editLocationLng = action.lng,
                    )
                }
            is UiAction.OnEditRecurrenceChange -> changeEditRecurrence(action.recurrence)
            is UiAction.OnEditIntervalChange -> updateSuccess { it.copy(editRecurrenceInterval = action.interval) }
            is UiAction.OnEditWeekdayToggle -> updateSuccess { s ->
                val next = if (action.day in s.editRecurrenceByDay) {
                    s.editRecurrenceByDay - action.day
                } else {
                    s.editRecurrenceByDay + action.day
                }
                s.copy(editRecurrenceByDay = next)
            }
            is UiAction.OnEditRecurrenceUntilChange -> updateSuccess { it.copy(editRecurrenceUntil = action.until) }
            is UiAction.OnEditReminderTimeAdd -> updateSuccess {
                it.copy(editReminderTimes = (it.editReminderTimes + action.time).distinct().sorted())
            }
            is UiAction.OnEditReminderTimeRemove -> updateSuccess {
                it.copy(editReminderTimes = it.editReminderTimes - action.time)
            }
            UiAction.OnEditLocationCleared ->
                updateSuccess {
                    it.copy(
                        editLocationName = null,
                        editLocationAddress = null,
                        editLocationLat = null,
                        editLocationLng = null,
                    )
                }
        }
    }

    private fun uploadPhoto(
        bytes: ByteArray,
        mimeType: String,
    ) {
        viewModelScope.launch {
            groupRepository
                .uploadTaskPhoto(taskId, bytes, mimeType)
                .onSuccess { loadTask(force = true) }
                .onFailure { _uiEffect.trySend(UiEffect.ShowToast(it.toUserMessage(context))) }
        }
    }

    private fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            groupRepository
                .deleteTaskPhoto(taskId, photoId)
                .onSuccess { loadTask(force = true) }
                .onFailure { _uiEffect.trySend(UiEffect.ShowToast(it.toUserMessage(context))) }
        }
    }

    private fun reportPhoto(relativeUrl: String) {
        viewModelScope.launch {
            groupRepository
                .reportContent(groupId, ReportTargetType.PHOTO, targetRef = relativeUrl)
                .onSuccess {
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.report_success_toast)))
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.report_failed_toast)))
                }
        }
    }

    private fun loadTask(force: Boolean = false) {
        viewModelScope.launch {
            currentUserId = userRepository.getUserInfo().getOrNull()?.id ?: -1L

            val detailResult = groupRepository.getGroupDetail(groupId)
            val detail = detailResult.getOrNull()
            if (detail != null) {
                currentUserRole = detail.members
                    .find { it.userId == currentUserId }
                    ?.role
                    ?.uppercase()
                    .orEmpty()
            }

            val members = groupRepository.getGroupMembers(groupId).getOrNull() ?: emptyList()
            val memberUiItems =
                members.map { member ->
                    val initials =
                        member.displayName
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase()
                    GroupDetailContract.GroupMemberUiItem(
                        userId = member.userId,
                        displayName = member.displayName,
                        email = member.email,
                        avatarUrl = member.avatarUrl,
                        initials = initials,
                        role = member.role,
                        joinedAt = "",
                        pendingTaskCount = 0,
                        isCurrentUser = member.userId == currentUserId,
                    )
                }

            // Shared per-day completion: any member's tick marks today's occurrence for everyone.
            val doneToday = groupRepository.observeGroupTasksDoneOn(LocalDate.now()).first()
            groupRepository
                .getGroupTasks(groupId, force = force)
                .onSuccess { tasks ->
                    val task = tasks.find { it.id == taskId }
                    if (task == null) {
                        _uiState.value = UiState.Error(context.getString(R.string.error_generic))
                    } else {
                        _uiState.value =
                            UiState.Success(
                                task = task.toUiModel(
                                    membersById = members.associateBy { m -> m.userId },
                                    doneToday = doneToday,
                                ),
                                groupName = detail?.name.orEmpty(),
                                members = memberUiItems,
                            )
                    }
                }.onFailure {
                    _uiState.value = UiState.Error(it.toUserMessage(context))
                }
        }
    }

    private fun toggleComplete() {
        val current = _uiState.value as? UiState.Success ?: return
        val previousTask = current.task
        // Same rule as the group overview list: only the assignee or an admin may complete.
        if (!previousTask.canComplete) {
            _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.only_assignee_can_complete)))
            return
        }
        val newCompleted = !previousTask.isCompleted
        // Optimistic flip; same pattern as GroupDetailViewModel.handleTaskChecked.
        _uiState.value = current.copy(task = previousTask.copy(isCompleted = newCompleted))
        viewModelScope.launch {
            val groupTask = GroupTask(
                id = previousTask.id,
                title = previousTask.title,
                description = previousTask.description,
                isCompleted = newCompleted,
                priority = previousTask.priority,
                dueDate = previousTask.rawDueDate,
                assignee = null,
            )
            // A routine completes ONE occurrence — today's. The flat flag would retire the whole
            // task, so a daily chore would never come back.
            val result = if (previousTask.recurrence != Recurrence.NONE) {
                groupRepository.setGroupTaskDayCompletion(groupId, taskId, LocalDate.now(), newCompleted)
            } else {
                groupRepository.updateGroupTaskStatus(groupId, taskId, groupTask, newCompleted)
            }
            result
                .onFailure {
                    _uiState.update { s -> (s as? UiState.Success)?.copy(task = previousTask) ?: s }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                }
        }
    }

    private fun toggleSubtask(subtaskId: Long, isCompleted: Boolean) {
        val current = _uiState.value as? UiState.Success ?: return
        val previousTask = current.task
        val next = previousTask.subtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = isCompleted) else it
        }
        // Optimistic, same as the completion toggle: the checkbox answers immediately and a failed
        // push restores the whole step list rather than leaving one step out of step.
        _uiState.value = current.copy(task = previousTask.copy(subtasks = next))
        viewModelScope.launch {
            groupRepository
                .setGroupSubtaskCompletion(
                    groupId = groupId,
                    taskId = taskId,
                    steps = previousTask.subtasks,
                    subtaskId = subtaskId,
                    isCompleted = isCompleted,
                ).onFailure {
                    _uiState.update { s -> (s as? UiState.Success)?.copy(task = previousTask) ?: s }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                }
        }
    }

    private fun openEditSheet() {
        val state = _uiState.value as? UiState.Success ?: return
        val task = state.task
        val dueMillis = task.rawDueDate
        val zdt = dueMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
        updateSuccess {
            it.copy(
                isEditSheetOpen = true,
                editTitle = task.title,
                editDescription = task.description.orEmpty(),
                editDate = zdt?.toLocalDate(),
                editIsAllDay = task.isAllDay,
                editTimeStart = task.timeStart ?: zdt?.toLocalTime(),
                editTimeEnd = task.timeEnd,
                editAssigneeId = task.assigneeUserId,
                editLocationName = task.locationName,
                editLocationAddress = task.locationAddress,
                editLocationLat = task.locationLat,
                editLocationLng = task.locationLng,
                editRecurrence = task.recurrence,
                editRecurrenceInterval = task.recurrenceInterval,
                editRecurrenceByDay = task.recurrenceByDay,
                editRecurrenceUntil = task.recurrenceUntil,
                editReminderTimes = task.reminderTimes,
            )
        }
    }

    /**
     * Dropping the repeat drops everything that only existed because of it — the same rule the two
     * personal forms apply. The rule fields disappear from the sheet at NONE, so anything left behind
     * would be saved without ever having been shown.
     */
    private fun changeEditRecurrence(recurrence: Recurrence) {
        updateSuccess {
            if (recurrence == Recurrence.NONE) {
                it.copy(
                    editRecurrence = recurrence,
                    editRecurrenceInterval = 1,
                    editRecurrenceByDay = emptySet(),
                    editReminderTimes = emptyList(),
                    // Not the end date: this endpoint reads null as "no change", so there is no way
                    // to clear it from here. It stops mattering the moment the task stops repeating
                    // (firesOn ignores UNTIL for NONE) and the backend's clearRecurrenceUntil flag
                    // will make the removal real.
                )
            } else {
                it.copy(editRecurrence = recurrence)
            }
        }
    }

    private fun saveEdit() {
        val state = _uiState.value as? UiState.Success ?: return
        if (state.editTitle.isBlank()) {
            _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.task_title_empty)))
            return
        }
        val allDay = state.editIsAllDay
        val startTime = if (allDay) LocalTime.MIDNIGHT else (state.editTimeStart ?: LocalTime.MIDNIGHT)
        val endTime = if (allDay) END_OF_DAY else (state.editTimeEnd ?: startTime)
        val dueDate: Long? =
            state.editDate?.let { date ->
                date
                    .atTime(startTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        val endToSend = state.endToSend()
        updateSuccess { it.copy(isSaving = true) }
        viewModelScope.launch {
            val locationCleared = state.editLocationName.isNullOrBlank() && state.task.locationName != null
            groupRepository
                .updateGroupTask(
                    groupId = groupId,
                    taskId = taskId,
                    title = state.editTitle,
                    description = state.editDescription.ifBlank { null },
                    dueDate = dueDate,
                    priority = state.task.priority,
                    assignedToUserId = state.editAssigneeId,
                    isAllDay = allDay,
                    timeStart = startTime.toSecondOfDay().toLong(),
                    timeEnd = endTime.toSecondOfDay().toLong(),
                    locationName = state.editLocationName?.takeIf { it.isNotBlank() },
                    locationAddress = state.editLocationAddress?.takeIf { it.isNotBlank() },
                    locationLat = state.editLocationLat,
                    locationLng = state.editLocationLng,
                    clearLocation = locationCleared,
                    recurrence = state.editRecurrence.name,
                    recurrenceInterval = state.editRecurrenceInterval.coerceAtLeast(1),
                    // Dead data on any other frequency, exactly as the personal edit form treats it.
                    recurrenceByDay = if (state.editRecurrence == Recurrence.WEEKLY) {
                        state.editRecurrenceByDay.toStorageCsv()
                    } else {
                        null
                    },
                    recurrenceUntil = endToSend?.toEpochDay(),
                    // Seconds on the wire, like every other reminder-time field.
                    reminderTimes = state.editReminderTimes.map { t -> t.toSecondOfDay() },
                ).onSuccess {
                    val newAssigneeName = state.members.find { it.userId == state.editAssigneeId }?.displayName
                    val newAssigneeInitials =
                        newAssigneeName
                            ?.split(" ")
                            ?.mapNotNull { it.firstOrNull()?.toString() }
                            ?.take(2)
                            ?.joinToString("")
                    updateSuccess { s ->
                        s.copy(
                            task =
                            s.task.copy(
                                title = s.editTitle,
                                description = s.editDescription.ifBlank { null },
                                dueTime = dueDate?.let { formatDueDate(it) },
                                rawDueDate = dueDate,
                                isAllDay = allDay,
                                timeStart = startTime,
                                timeEnd = endTime,
                                assigneeName = newAssigneeName,
                                assigneeInitials = newAssigneeInitials,
                                assigneeUserId = s.editAssigneeId,
                                isAssignedToMe = s.editAssigneeId == currentUserId,
                                locationName = s.editLocationName?.takeIf { it.isNotBlank() },
                                locationAddress = s.editLocationAddress?.takeIf { it.isNotBlank() },
                                locationLat = s.editLocationLat,
                                locationLng = s.editLocationLng,
                                recurrence = s.editRecurrence,
                                recurrenceInterval = s.editRecurrenceInterval,
                                recurrenceByDay = s.editRecurrenceByDay,
                                // Only when it actually moved — null here means "unchanged", not
                                // "cleared", and patching it to null would blank the progress bar.
                                recurrenceUntil = endToSend ?: s.task.recurrenceUntil,
                                reminderTimes = s.editReminderTimes,
                            ),
                            isEditSheetOpen = false,
                            isSaving = false,
                        )
                    }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.task_updated)))
                }.onFailure {
                    updateSuccess { s -> s.copy(isSaving = false) }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_update_task)))
                }
        }
    }

    private fun changeEditAllDay(isAllDay: Boolean) {
        updateSuccess { s ->
            if (!isAllDay && s.editTimeStart == null) {
                // Turning off all-day with no prior time: default to the next full hour + 1h end.
                val start = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                s.copy(editIsAllDay = false, editTimeStart = start, editTimeEnd = start.plusHours(1))
            } else {
                s.copy(editIsAllDay = isAllDay)
            }
        }
    }

    private fun updateSuccess(transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { current -> (current as? UiState.Success)?.let(transform) ?: current }
    }

    private fun GroupTask.toUiModel(
        membersById: Map<Long, GroupMember> = emptyMap(),
        doneToday: Set<Long> = emptySet(),
    ): TaskUiModel {
        val isAssignedToMe = assignee?.userId == currentUserId
        val assigneeInitials =
            assignee
                ?.displayName
                ?.split(" ")
                ?.mapNotNull { it.firstOrNull()?.toString() }
                ?.take(2)
                ?.joinToString("")
        // The /tasks list's assignedTo carries no avatar; resolve it from the loaded member list.
        val assigneeAvatar = assignee?.userId?.let { membersById[it]?.avatarUrl } ?: assignee?.avatarUrl
        return TaskUiModel(
            id = id,
            title = title,
            description = description,
            priority = priority,
            dueTime = dueDate?.let { formatDueDate(it) },
            rawDueDate = dueDate,
            isAllDay = isAllDay,
            timeStart = timeStart,
            timeEnd = timeEnd,
            // A routine's completion is per-occurrence; the flat flag can't express "today".
            isCompleted = if (recurrence != Recurrence.NONE) id in doneToday else isCompleted,
            assigneeName = assignee?.displayName,
            assigneeInitials = assigneeInitials,
            assigneeAvatarUrl = assigneeAvatar,
            assigneeUserId = assignee?.userId,
            isAssignedToMe = isAssignedToMe,
            canDelete = isAssignedToMe || currentUserRole == "ADMIN",
            canComplete = isAssignedToMe || currentUserRole == "ADMIN",
            photoUrls = photoUrls,
            locationName = locationName,
            locationAddress = locationAddress,
            locationLat = locationLat,
            locationLng = locationLng,
            category = category,
            customCategoryName = customCategoryName,
            taskType = derivedTaskType(capabilities()),
            recurrence = recurrence,
            recurrenceInterval = recurrenceInterval,
            recurrenceByDay = recurrenceByDay,
            recurrenceUntil = recurrenceUntil,
            reminderTimes = reminderTimes,
            subtasks = subtasks,
            routineDayIndex = routineProgress()?.first,
            routineDayTotal = routineProgress()?.second,
        )
    }

    /**
     * The scheduled end to send: what the sheet holds, pushed forward if the start has overtaken it.
     *
     * `firesOn` rejects every day before the anchor and every day after the end, so a crossed pair
     * leaves nothing at all — the task saves and then belongs to no day, taking its progress bar and
     * its alarms with it. The sheet has always been able to produce exactly that, because it sends
     * the start and never sent the end.
     *
     * The span is carried forward rather than dropped. The personal side drops it (see
     * `DetailsViewModel.endAfter`), but it is allowed to: here a null field means "no change", so an
     * end cannot be cleared from this endpoint at all. Keeping the length the user chose — "a
     * month-long course, starting later" — is the only reading that is both expressible and true to
     * what they set up. Removing an end properly needs a `clearRecurrenceUntil` flag on the backend,
     * the same shape as `clearLocation`.
     */
    private fun UiState.Success.endToSend(): LocalDate? {
        val end = editRecurrenceUntil ?: return null
        val start = editDate ?: return end
        if (!start.isAfter(end)) return end
        val originalStart = task.rawDueDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val span = if (originalStart != null && task.recurrenceUntil != null) {
            ChronoUnit.DAYS.between(originalStart, task.recurrenceUntil)
        } else {
            0L
        }
        return start.plusDays(span.coerceAtLeast(0L))
    }

    /**
     * "Day 12 of 30" for a bounded routine. Reuses the personal side's occurrence helpers, so a
     * group routine counts its days exactly the way a personal one does — including the by-weekday
     * case, where the count is a walk over real firing days rather than a subtraction.
     */
    private fun GroupTask.routineProgress(): Pair<Int, Int>? {
        if (recurrence == Recurrence.NONE || recurrenceUntil == null) return null
        val anchor = startDate ?: return null
        val rule = recurrenceRule
        val total = rule.occurrenceTotal(anchor) ?: return null
        val index = rule.occurrenceIndex(anchor, LocalDate.now()) ?: return null
        return index to total
    }

    private fun formatDueDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        return when {
            diff < TimeUnit.HOURS.toMillis(24) && diff > 0 -> {
                val sdf = SimpleDateFormat(deviceTimePattern(context), Locale.getDefault())
                context.getString(R.string.due_prefix) + " " + sdf.format(Date(timestamp))
            }
            diff <= 0 && diff > -TimeUnit.HOURS.toMillis(24) -> context.getString(R.string.due_today)
            else -> {
                val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                context.getString(R.string.due_prefix) + " " + sdf.format(Date(timestamp))
            }
        }
    }

    private companion object {
        private val END_OF_DAY: LocalTime = LocalTime.of(23, 59)
    }
}
