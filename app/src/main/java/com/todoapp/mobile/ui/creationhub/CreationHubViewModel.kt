package com.todoapp.mobile.ui.creationhub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.creationhub.CreationHubContract.AssigneeOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.GroupOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiEffect
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.mobile.ui.home.PendingPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CreationHubViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val groupRepository: GroupRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dailyPlanPreferences: DailyPlanPreferences,
    private val pomodoroEngine: PomodoroEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _effect = Channel<UiEffect>()
    val effect = _effect.receiveAsFlow()

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var membersJob: Job? = null

    init {
        // Group-task creation is offered only for groups the user administers.
        viewModelScope.launch {
            groupRepository.observeAllGroups()
                .map { groups ->
                    groups
                        .filter { it.role.uppercase() == "ADMIN" && it.remoteId != null }
                        .map { GroupOption(localId = it.id, remoteId = it.remoteId!!, name = it.name) }
                }
                .collect { admin -> _state.update { it.copy(adminGroups = admin) } }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnCreateTaskCardTap -> _state.update { it.copy(step = Step.TASK_TYPE) }
            is UiAction.OnJournalCardTap -> navigateOut(Screen.Journal)
            is UiAction.OnPomodoroCardTap ->
                navigateOut(if (pomodoroEngine.state.value.isRunning) Screen.Pomodoro else Screen.PomodoroLaunch)

            is UiAction.OnGroupCardTap -> navigateOut(Screen.CreateNewGroup)
            is UiAction.OnTypeSelect -> selectType(action.type)

            is UiAction.OnBack -> goBack()
            is UiAction.OnTitleChange -> _state.update { it.copy(title = action.title, titleError = false) }
            is UiAction.OnDateSelect -> _state.update { it.copy(date = action.date) }
            is UiAction.OnReminderSelect -> _state.update { it.copy(reminderOffsetMinutes = action.minutes) }
            is UiAction.OnFrequencySelect -> _state.update { it.copy(recurrence = action.recurrence) }
            is UiAction.OnSubtaskChange -> changeSubtask(action.index, action.text)
            is UiAction.OnSubtaskRemove -> removeSubtask(action.index)
            is UiAction.OnToggleDetails -> _state.update { it.copy(detailsExpanded = !it.detailsExpanded) }
            is UiAction.OnDescriptionChange -> _state.update { it.copy(description = action.text) }
            is UiAction.OnAllDayChange -> changeAllDay(action.isAllDay)
            is UiAction.OnTimeStartChange -> _state.update { it.copy(timeStart = action.time) }
            is UiAction.OnTimeEndChange -> _state.update { it.copy(timeEnd = action.time) }
            is UiAction.OnCategoryChange -> _state.update { it.copy(category = action.category) }
            is UiAction.OnCustomCategoryNameChange -> _state.update { it.copy(customCategoryName = action.name) }
            is UiAction.OnSecretChange -> _state.update { it.copy(isSecret = action.isSecret) }
            is UiAction.OnPhotoPick ->
                _state.update { it.copy(pendingPhotos = it.pendingPhotos + PendingPhoto(action.bytes, action.mimeType)) }

            is UiAction.OnPhotoRemove ->
                _state.update { it.copy(pendingPhotos = it.pendingPhotos.filterIndexed { i, _ -> i != action.index }) }

            is UiAction.OnLocationPicked ->
                _state.update {
                    it.copy(
                        locationName = action.name.ifBlank { null },
                        locationAddress = action.address.ifBlank { null },
                        locationLat = action.lat,
                        locationLng = action.lng,
                    )
                }

            is UiAction.OnLocationCleared ->
                _state.update {
                    it.copy(locationName = null, locationAddress = null, locationLat = null, locationLng = null)
                }

            is UiAction.OnGroupSelect -> selectGroup(action.localId, action.remoteId)
            is UiAction.OnAssigneeSelect -> _state.update { it.copy(selectedAssigneeId = action.userId) }
            is UiAction.OnPrioritySelect -> _state.update { it.copy(priority = action.priority) }

            is UiAction.OnCreate -> create()
        }
    }

    private fun navigateOut(route: Screen) {
        // Hub is a launcher, not a place to dwell: pop it so BACK from the destination returns Home.
        _navEffect.trySend(
            NavigationEffect.Navigate(route = route, popUpTo = Screen.CreationHub, isInclusive = true),
        )
    }

    private fun goBack() {
        when (_state.value.step) {
            Step.TASK_CORE -> _state.update { it.copy(step = Step.TASK_TYPE) }
            Step.TASK_TYPE -> _state.update { it.copy(step = Step.HUB_ROOT, taskType = null) }
            Step.HUB_ROOT -> _navEffect.trySend(NavigationEffect.Back)
        }
    }

    private fun selectType(type: TaskType) {
        _state.update {
            it.copy(
                step = Step.TASK_CORE,
                taskType = type,
                date = LocalDate.now(),
                placeholderIndex = Random.nextInt(CreationHubPlaceholders.count),
            )
        }
        // With a single admin group there's nothing to pick — auto-select it and load its members.
        if (type == TaskType.GROUP) {
            _state.value.adminGroups.singleOrNull()?.let { selectGroup(it.localId, it.remoteId) }
        }
    }

    private fun selectGroup(localId: Long, remoteId: Long) {
        _state.update {
            it.copy(
                selectedGroupLocalId = localId,
                selectedGroupRemoteId = remoteId,
                selectedAssigneeId = null,
                groupMembers = emptyList(),
            )
        }
        membersJob?.cancel()
        membersJob = viewModelScope.launch {
            // The user may never have opened this group's detail, so the local member table can be
            // empty/stale — warm it once from the remote, then render whatever the Flow emits.
            runCatching { groupRepository.getGroupMembers(remoteId) }
                .onSuccess { result ->
                    val members = result.getOrNull()
                    if (members != null) {
                        _state.update { it.copy(groupMembers = members.map(::toAssigneeOption)) }
                    }
                }
            groupRepository.observeGroupMembers(localId).collect { members ->
                _state.update { it.copy(groupMembers = members.map(::toAssigneeOption)) }
            }
        }
    }

    private fun toAssigneeOption(member: GroupMember): AssigneeOption = AssigneeOption(
        userId = member.userId,
        displayName = member.displayName,
        avatarUrl = member.avatarUrl,
        initials = member.displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() },
    )

    private fun changeAllDay(isAllDay: Boolean) {
        _state.update { s ->
            if (!isAllDay && s.timeStart == null) {
                val start = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                s.copy(isAllDay = false, timeStart = start, timeEnd = start.plusHours(1))
            } else {
                s.copy(isAllDay = isAllDay)
            }
        }
    }

    private fun changeSubtask(index: Int, text: String) {
        _state.update { s ->
            if (index !in s.subtaskDrafts.indices) return@update s
            val drafts = s.subtaskDrafts.toMutableList()
            drafts[index] = text
            // Keep one trailing empty row so the next step can be typed inline.
            if (index == drafts.lastIndex && text.isNotBlank()) drafts.add("")
            s.copy(subtaskDrafts = drafts)
        }
    }

    private fun removeSubtask(index: Int) {
        _state.update { s ->
            if (index !in s.subtaskDrafts.indices) return@update s
            val drafts = s.subtaskDrafts.toMutableList().apply { removeAt(index) }
            val withTrailing = if (drafts.isEmpty() || drafts.last().isNotBlank()) drafts + "" else drafts
            s.copy(subtaskDrafts = withTrailing)
        }
    }

    private fun create() {
        val s = _state.value
        if (s.isSaving) return
        if (s.title.isBlank()) {
            _state.update { it.copy(titleError = true) }
            return
        }
        val type = s.taskType ?: return
        if (type == TaskType.GROUP) {
            createGroupTask(s)
            return
        }
        val subtaskTitles = s.subtaskDrafts.map { it.trim() }.filter { it.isNotBlank() }
        if (type == TaskType.STAGED && subtaskTitles.isEmpty()) {
            _effect.trySend(UiEffect.ShowToast(R.string.creation_need_one_step))
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val allDay = s.isAllDay
            val start = if (allDay) LocalTime.MIDNIGHT else (s.timeStart ?: LocalTime.of(DEFAULT_START_HOUR, 0))
            val end = if (allDay) {
                LocalTime.of(END_OF_DAY_HOUR, END_OF_DAY_MINUTE)
            } else {
                s.timeEnd ?: start.plusHours(1)
            }
            val task = Task(
                title = s.title.trim(),
                description = s.description.trim().ifBlank { null },
                date = s.date,
                timeStart = start,
                timeEnd = end,
                isCompleted = false,
                isSecret = s.isSecret,
                reminderOffsetMinutes = if (type == TaskType.ROUTINE) null else s.reminderOffsetMinutes,
                category = if (type == TaskType.STAGED) TaskCategory.PERSONAL else s.category,
                customCategoryName = if (type == TaskType.STAGED) {
                    null
                } else {
                    s.customCategoryName.takeIf { s.category == TaskCategory.OTHER && it.isNotBlank() }
                },
                recurrence = if (type == TaskType.ROUTINE) s.recurrence else Recurrence.NONE,
                isAllDay = allDay,
                locationName = s.locationName,
                locationAddress = s.locationAddress,
                locationLat = s.locationLat,
                locationLng = s.locationLng,
                subtasks = if (type == TaskType.STAGED) {
                    subtaskTitles.mapIndexed { index, title -> Subtask(title = title, orderIndex = index) }
                } else {
                    emptyList()
                },
            )
            if (s.pendingPhotos.isNotEmpty()) {
                taskRepository.insertWithPhotos(task, s.pendingPhotos.map { it.bytes to it.mimeType })
            } else {
                taskRepository.insert(task)
            }
            scheduleOneShotReminder(task)
            _effect.trySend(UiEffect.ShowToast(R.string.creation_task_created))
            _navEffect.trySend(NavigationEffect.Back)
        }
    }

    /**
     * Creates a task in the selected admin group. Unassigned (assignedToUserId = null) is valid and is
     * the default. Photos are uploaded after create using the returned remote task id (mirrors
     * GroupDetailViewModel). No local AlarmScheduler call — group reminders are server-side.
     */
    private fun createGroupTask(s: UiState) {
        val remoteId = s.selectedGroupRemoteId
        if (remoteId == null) {
            _effect.trySend(UiEffect.ShowToast(R.string.creation_group_required))
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val allDay = s.isAllDay
            val start = if (allDay) LocalTime.MIDNIGHT else (s.timeStart ?: LocalTime.of(DEFAULT_START_HOUR, 0))
            val end = if (allDay) {
                LocalTime.of(END_OF_DAY_HOUR, END_OF_DAY_MINUTE)
            } else {
                s.timeEnd ?: start.plusHours(1)
            }
            val task = Task(
                title = s.title.trim(),
                description = s.description.trim().ifBlank { null },
                date = s.date,
                timeStart = start,
                timeEnd = end,
                isCompleted = false,
                isSecret = s.isSecret,
                isAllDay = allDay,
                locationName = s.locationName,
                locationAddress = s.locationAddress,
                locationLat = s.locationLat,
                locationLng = s.locationLng,
                // No recurrence / category / subtasks — group tasks are a flat, assignable task.
            )
            groupRepository.createGroupTask(
                groupId = remoteId,
                task = task,
                priority = s.priority,
                assignedToUserId = s.selectedAssigneeId,
            ).onSuccess { newTaskId ->
                s.pendingPhotos.forEach {
                    runCatching { groupRepository.uploadTaskPhoto(newTaskId, it.bytes, it.mimeType) }
                }
                _effect.trySend(UiEffect.ShowToast(R.string.creation_task_created))
                _navEffect.trySend(NavigationEffect.Back)
            }.onFailure {
                _state.update { it.copy(isSaving = false) }
                _effect.trySend(UiEffect.ShowToast(R.string.failed_to_create_task))
            }
        }
    }

    /**
     * Mirrors TaskRepositoryImpl.rescheduleOneShotAlarm for the create path: all-day tasks fire at the
     * user's daily-plan hour, and past triggers are skipped (AlarmManager fires those immediately).
     * Recurring tasks are scheduled inside the repository, so this is a no-op for them.
     */
    private suspend fun scheduleOneShotReminder(task: Task) {
        if (task.recurrence != Recurrence.NONE) return
        val offset = task.reminderOffsetMinutes ?: return
        val effectiveTime = if (task.isAllDay) {
            dailyPlanPreferences.observePlanTime().first() ?: DailyPlanDefaults.DEFAULT_PLAN_TIME
        } else {
            task.timeStart
        }
        val item = task.toAlarmItem(
            remindBeforeMinutes = offset,
            overrideStartTime = effectiveTime.takeIf { task.isAllDay },
        )
        val triggerMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return
        withContext(ioDispatcher) {
            runCatching { alarmScheduler.schedule(item, AlarmType.TASK) }
        }
    }

    private companion object {
        private const val END_OF_DAY_HOUR = 23
        private const val END_OF_DAY_MINUTE = 59
        private const val DEFAULT_START_HOUR = 9
    }
}
