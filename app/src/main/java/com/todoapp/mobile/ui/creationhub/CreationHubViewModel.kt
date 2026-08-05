package com.todoapp.mobile.ui.creationhub

import androidx.lifecycle.SavedStateHandle
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
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _effect = Channel<UiEffect>()
    val effect = _effect.receiveAsFlow()

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var membersJob: Job? = null

    // Lenient so a draft written by a previous app version (extra/renamed keys) still restores.
    private val draftJson = Json { ignoreUnknownKeys = true }

    init {
        // Restore a half-filled form after process death (SavedStateHandle survives the kill) before
        // anything paints, so the user sees their typed content instead of a blank form.
        savedStateHandle.get<String>(DRAFT_KEY)?.let { stored ->
            runCatching { draftJson.decodeFromString<CreationHubDraft>(stored) }
                .getOrNull()
                ?.let { draft -> _state.update { draft.toState(it) } }
        }
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
        // Persist the durable slice on every real change so the form survives process death.
        viewModelScope.launch {
            _state
                .map { it.toDraft() }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = draftJson.encodeToString(it) }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnCreateTaskCardTap -> _state.update { it.copy(step = Step.TASK_SCOPE) }
            is UiAction.OnScopeSelect -> selectScope(action.scope)
            is UiAction.OnJournalCardTap -> navigateOut(Screen.Journal)
            is UiAction.OnPomodoroCardTap ->
                navigateOut(if (pomodoroEngine.state.value.isRunning) Screen.Pomodoro else Screen.PomodoroLaunch)

            is UiAction.OnGroupCardTap -> navigateOut(Screen.CreateNewGroup)
            is UiAction.OnTypeSelect -> selectType(action.type)

            is UiAction.OnBack -> goBack()
            is UiAction.OnTitleChange -> _state.update { it.copy(title = action.title, titleError = false) }
            is UiAction.OnDateSelect -> _state.update { state ->
                state.copy(
                    date = action.date,
                    // An end before the start can never fire: firesOn rejects every day after the end
                    // and every day before the anchor, and past each other those two leave nothing —
                    // the task saves, then shows up on no day at all. The calendar's own gesture
                    // cannot produce it, but mixing sources can (a start dragged past an end that came
                    // from the chips), so the guard belongs here rather than in the picker.
                    recurrenceUntil = state.recurrenceUntil?.takeUnless { it.isBefore(action.date) },
                )
            }
            is UiAction.OnReminderSelect -> _state.update { it.copy(reminderOffsetMinutes = action.minutes) }
            is UiAction.OnFrequencySelect -> selectFrequency(action.recurrence)
            is UiAction.OnRecurrenceUntilSelect -> _state.update { it.copy(recurrenceUntil = action.until) }
            is UiAction.OnReminderTimeAdd -> _state.update {
                it.copy(reminderTimes = (it.reminderTimes + action.time).distinct().sorted())
            }
            is UiAction.OnReminderTimeRemove -> _state.update {
                it.copy(reminderTimes = it.reminderTimes - action.time)
            }
            is UiAction.OnIntervalChange -> _state.update { it.copy(recurrenceInterval = action.interval) }
            is UiAction.OnWeekdayToggle -> _state.update {
                val next = if (action.day in it.recurrenceByDay) {
                    it.recurrenceByDay - action.day
                } else {
                    it.recurrenceByDay + action.day
                }
                it.copy(recurrenceByDay = next)
            }
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
            Step.TASK_TYPE -> _state.update { it.copy(step = Step.TASK_SCOPE, taskType = null) }
            Step.TASK_SCOPE -> _state.update { it.copy(step = Step.HUB_ROOT, scope = null) }
            Step.HUB_ROOT -> _navEffect.trySend(NavigationEffect.Back)
        }
    }

    private fun selectScope(scope: TaskScope) {
        _state.update { it.copy(step = Step.TASK_TYPE, scope = scope) }
        // With a single admin group there is nothing to pick — auto-select it and load its members,
        // so the form can show the assignee picker straight away.
        if (scope == TaskScope.GROUP) {
            _state.value.adminGroups.singleOrNull()?.let { selectGroup(it.localId, it.remoteId) }
        }
    }

    /**
     * Picking a type starts that shape over — the date reset has always said so, and the rest of the
     * recurrence rule now follows it.
     *
     * Leaving the rule behind meant the custom form could furnish a routine with fields the routine
     * form never shows: an end date, an interval, a weekday set, absolute reminder times. The end was
     * the dangerous one, because [buildTask] writes it for anything that repeats — a routine could
     * save a bound it had no way to display, and with the date reset to today an end chosen earlier
     * could already be in the past. Mirrors what selectFrequency does when the repeat is switched off.
     */
    private fun selectType(type: TaskType) {
        _state.update { state ->
            state.copy(
                step = Step.TASK_CORE,
                taskType = type,
                date = LocalDate.now(),
                recurrence = when (type) {
                    // A custom task starts with nothing switched on: the frequency chips include a
                    // "don't repeat" option, so DAILY (the routine default) would silently make every
                    // custom task recur before the user chose anything.
                    TaskType.CUSTOM -> Recurrence.NONE
                    // A routine repeats by definition and its chips offer no NONE, so arriving from
                    // the custom form's NONE left every chip dark and saved a "routine" that was
                    // really a one-off.
                    TaskType.ROUTINE -> state.recurrence.takeIf { it != Recurrence.NONE } ?: Recurrence.DAILY
                    else -> state.recurrence
                },
                recurrenceUntil = null,
                recurrenceInterval = 1,
                recurrenceByDay = emptySet(),
                reminderTimes = emptyList(),
                placeholderIndex = Random.nextInt(CreationHubPlaceholders.count),
            )
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

    /**
     * Dropping the repeat also drops everything that only existed because of it. The rule fields are
     * hidden from the form when the frequency is NONE, so keeping them would leave the calendar
     * still drawing a span the user can no longer see or edit.
     */
    private fun selectFrequency(recurrence: Recurrence) {
        _state.update {
            if (recurrence == Recurrence.NONE) {
                it.copy(
                    recurrence = recurrence,
                    recurrenceUntil = null,
                    recurrenceInterval = 1,
                    recurrenceByDay = emptySet(),
                    reminderTimes = emptyList(),
                )
            } else {
                it.copy(recurrence = recurrence)
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
        val subtaskTitles = s.subtaskDrafts.map { it.trim() }.filter { it.isNotBlank() }

        // What gets written comes from the DATA, not the label. For a custom task every section is on
        // screen at once, so "does it repeat" is simply whether a frequency was picked and "is it
        // staged" is simply whether a step was typed — the classic three are fixed shapes of the same
        // questions. Nothing here asks the user to declare an intent twice.
        val recurs = type == TaskType.ROUTINE || (type == TaskType.CUSTOM && s.recurrence != Recurrence.NONE)
        val hasSteps = type == TaskType.STAGED || (type == TaskType.CUSTOM && subtaskTitles.isNotEmpty())

        if (!validateCapabilities(type, hasSteps, subtaskTitles)) return

        // ONE builder for both scopes. The group path used to assemble its own, much poorer Task —
        // which is exactly why a group task could never repeat or carry steps. Parity is now a
        // property of the code shape rather than something two call sites have to remember.
        val task = buildTask(s, type, recurs, hasSteps, subtaskTitles)
        if (s.scope == TaskScope.GROUP) {
            createGroupTask(s, task)
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
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
     * Only the CLASSIC staged type demands a step: picking it declared an intent the form must honour.
     * A custom task declares nothing up front, so leaving the step editor empty just means "no steps"
     * — refusing to save there would be scolding the user for not filling in an optional section.
     */
    private fun validateCapabilities(
        type: TaskType,
        hasSteps: Boolean,
        subtaskTitles: List<String>,
    ): Boolean {
        if (type == TaskType.STAGED && (!hasSteps || subtaskTitles.isEmpty())) {
            _effect.trySend(UiEffect.ShowToast(R.string.creation_need_one_step))
            return false
        }
        return true
    }

    private fun buildTask(
        state: UiState,
        type: TaskType,
        recurs: Boolean,
        hasSteps: Boolean,
        subtaskTitles: List<String>,
    ): Task {
        val allDay = state.isAllDay
        val start = if (allDay) LocalTime.MIDNIGHT else (state.timeStart ?: LocalTime.of(DEFAULT_START_HOUR, 0))
        val end = if (allDay) LocalTime.of(END_OF_DAY_HOUR, END_OF_DAY_MINUTE) else state.timeEnd ?: start.plusHours(1)
        return Task(
            title = state.title.trim(),
            description = state.description.trim().ifBlank { null },
            date = state.date,
            timeStart = start,
            timeEnd = end,
            isCompleted = false,
            isSecret = state.isSecret,
            // A repeating task reminds at absolute times, so the "N minutes before" offset never
            // applies to one — nor does it when explicit reminder times were given.
            reminderOffsetMinutes = if (recurs || state.reminderTimes.isNotEmpty()) null else state.reminderOffsetMinutes,
            // Only the CLASSIC staged type forces PERSONAL: a custom medicine course keeps MEDICINE
            // so its list chip still renders the pill icon.
            category = if (type == TaskType.STAGED) TaskCategory.PERSONAL else state.category,
            customCategoryName = if (type == TaskType.STAGED) {
                null
            } else {
                state.customCategoryName.takeIf { state.category == TaskCategory.OTHER && it.isNotBlank() }
            },
            recurrence = if (recurs) state.recurrence else Recurrence.NONE,
            recurrenceInterval = if (recurs) state.recurrenceInterval.coerceAtLeast(1) else 1,
            // A weekday set only means anything for WEEKLY; carrying it on other frequencies would be
            // dead data that firesOn ignores but contentEquals would still diff on.
            recurrenceByDay = if (recurs && state.recurrence == Recurrence.WEEKLY) state.recurrenceByDay else emptySet(),
            // A scheduled end only means something for something that repeats.
            recurrenceUntil = if (recurs) state.recurrenceUntil else null,
            reminderTimes = state.reminderTimes,
            isAllDay = allDay,
            locationName = state.locationName,
            locationAddress = state.locationAddress,
            locationLat = state.locationLat,
            locationLng = state.locationLng,
            subtasks = if (hasSteps) {
                subtaskTitles.mapIndexed { index, title -> Subtask(title = title, orderIndex = index) }
            } else {
                emptyList()
            },
        )
    }

    /**
     * Creates a task in the selected admin group. Unassigned (assignedToUserId = null) is valid and is
     * the default. Photos are uploaded after create using the returned remote task id (mirrors
     * GroupDetailViewModel).
     *
     * [built] arrives from the shared [buildTask], so a group task now carries the same recurrence
     * rule, steps, category and reminder times a personal one does — the create endpoint has always
     * accepted them (it is `POST /tasks` with `familyGroupId` set, and `toCreateTaskRequestDto`
     * sends the whole shape); only this function's own stripped-down `Task` stood in the way.
     */
    private fun createGroupTask(s: UiState, built: Task) {
        val remoteId = s.selectedGroupRemoteId
        if (remoteId == null) {
            _effect.trySend(UiEffect.ShowToast(R.string.creation_group_required))
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Secret mode is a personal-vault concept; a task the whole group can read is never secret.
            val task = built.copy(isSecret = false, clientTaskId = s.clientTaskId)
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
        private const val DRAFT_KEY = "creation_hub_draft"
        private const val END_OF_DAY_HOUR = 23
        private const val END_OF_DAY_MINUTE = 59
        private const val DEFAULT_START_HOUR = 9
    }
}
