package com.todoapp.mobile.ui.home

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.move
import com.todoapp.mobile.common.needsOverlayPermission
import com.todoapp.mobile.common.needsPostNotificationsPermission
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.security.SecretModeConditionFactory
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.domain.usecase.SetTaskCompletionUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.home.HomeContract.UiAction
import com.todoapp.mobile.ui.home.HomeContract.UiEffect
import com.todoapp.mobile.ui.home.HomeContract.UiState
import com.todoapp.mobile.ui.settings.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@Suppress("LargeClass")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val taskSyncRepository: TaskSyncRepository,
    private val setTaskCompletion: SetTaskCompletionUseCase,
    private val secretModePreferences: SecretPreferences,
    private val alarmScheduler: AlarmScheduler,
    private val pomodoroEngine: PomodoroEngine,
    private val groupRepository: GroupRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val clock: java.time.Clock,
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private data class DailyData(
        val tasks: List<Task>,
        val pendingTaskCountThisWeek: Int,
        val completedTaskCountThisWeek: Int,
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect: Flow<UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private lateinit var selectedTask: Task
    private var fetchJob: Job? = null
    private var pendingDeleteJob: Job? = null
    private var subtaskJob: Job? = null

    private val displayedMonthFlow = MutableStateFlow(YearMonth.now(clock))

    init {
        taskSyncRepository.fetchTasks()
        loadInitialData()
        setupAuxiliaryFlows()
        setupTaskDatesFlow()
    }

    override fun onCleared() {
        super.onCleared()
        val previous = (_uiState.value as? UiState.Success)?.pendingDeleteTask ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        CoroutineScope(SupervisorJob()).launch { taskRepository.delete(previous) }
    }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            is UiAction.OnRetry -> retry()
            is UiAction.OnDateSelect -> changeSelectedDate(uiAction)
            is UiAction.OnTaskCreate -> createTask()
            is UiAction.OnTaskCheck -> checkTask(uiAction)
            is UiAction.OnTaskLongPress -> onTaskLongPressed(uiAction)
            is UiAction.OnDeleteDialogConfirm -> onDeleteDialogConfirmed()
            is UiAction.OnDeleteDialogDismiss -> closeDeleteDialog()
            is UiAction.OnMoveTask -> updateTaskIndices(uiAction)
            is UiAction.OnTaskTitleChange -> changeTaskTitle(uiAction)
            is UiAction.OnTaskDescriptionChange -> changeTaskDescription(uiAction)
            is UiAction.OnTaskDateChange -> changeTaskDate(uiAction)
            is UiAction.OnTaskTimeStartChange -> changeTaskTimeStart(uiAction)
            is UiAction.OnTaskTimeEndChange -> changeTaskTimeEnd(uiAction)
            is UiAction.OnTaskSecretChange -> toggleTaskSecret(uiAction)
            is UiAction.OnReminderOffsetChange -> changeReminderOffset(uiAction.minutes)
            is UiAction.OnCategoryChange -> changeCategory(uiAction.category)
            is UiAction.OnCustomCategoryNameChange -> changeCustomCategoryName(uiAction.name)
            is UiAction.OnRecurrenceChange -> changeRecurrence(uiAction.recurrence)
            is UiAction.OnAllDayChange -> changeAllDay(uiAction.isAllDay)
            is UiAction.OnLocationPicked -> setFormLocation(uiAction.name, uiAction.address, uiAction.lat, uiAction.lng)
            is UiAction.OnLocationCleared -> setFormLocation(null, null, null, null)
            is UiAction.OnFilterChange -> changeFilter(uiAction.filter)
            is UiAction.OnSuggestCardPrimaryAction -> handleSuggestCardPrimary()
            is UiAction.OnSuggestCardSecondaryAction -> handleSuggestCardSecondary()
            is UiAction.OnSuggestCardDismiss -> dismissSuggestCard()
            is UiAction.OnTimeTick -> Unit
            is UiAction.OnDialogDateSelect -> updateDialogDate(uiAction)
            is UiAction.OnDialogDateDeselect -> deselectDialogDate()
            is UiAction.OnShowBottomSheet -> showBottomSheet()
            is UiAction.OnDismissBottomSheet -> dismissBottomSheet()
            is UiAction.OnToggleAdvancedSettings -> toggleAdvancedSettings()
            is UiAction.OnTaskClick -> openTaskDetail(uiAction.task)
            is UiAction.OnPomodoroTap -> navigateToPomodoro()
            is UiAction.OnJournalTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Journal))
            is UiAction.OnCreateHubTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.CreationHub))
            is UiAction.OnStagedExpandToggle -> toggleStagedExpand(uiAction.taskId)
            is UiAction.OnSubtaskToggle ->
                viewModelScope.launch(ioDispatcher) {
                    taskRepository.toggleSubtask(uiAction.subtaskId, uiAction.isCompleted)
                }
            is UiAction.OnCompletedStatCardTap -> navigateToFilteredTasks(isCompleted = true)
            is UiAction.OnPendingStatCardTap -> navigateToFilteredTasks(isCompleted = false)
            is UiAction.OnSuccessfulBiometricAuthenticationHandle -> handleSuccessfulBiometricAuthentication()
            is UiAction.OnToggleTaskSecret -> toggleExistingTaskSecret(uiAction)
            is UiAction.OnBiometricSuccessForSecretToggle -> performSecretToggle(uiAction)
            is UiAction.OnUndoDelete -> undoDelete()
            is UiAction.OnPreviousMonth -> navigateToPreviousMonth()
            is UiAction.OnNextMonth -> navigateToNextMonth()
            is UiAction.OnJumpToEarliestOverdue -> jumpToEarliestOverdue()
            is UiAction.OnGroupSelectionChanged ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(selectedGroupId = uiAction.groupId))
                }
            is UiAction.OnPendingPhotoAdd ->
                updateSuccessState { s ->
                    s.copy(
                        taskFormState =
                        s.taskFormState.copy(
                            pendingPhotos = s.taskFormState.pendingPhotos + PendingPhoto(uiAction.bytes, uiAction.mimeType),
                        ),
                    )
                }
            is UiAction.OnPendingPhotoRemove ->
                updateSuccessState { s ->
                    s.copy(
                        taskFormState =
                        s.taskFormState.copy(
                            pendingPhotos = s.taskFormState.pendingPhotos.filterIndexed { i, _ -> i != uiAction.index },
                        ),
                    )
                }
            is UiAction.DismissPermission -> dismissPermission(uiAction.type)
            is UiAction.PermissionGranted -> dismissPermission(uiAction.type)
            is UiAction.RefreshPermissions -> refreshPermissions()
        }
    }

    private fun refreshPermissions() {
        // Lifecycle ON_RESUME also triggers a remote task sync so off-device mutations
        // (DoneBot on web, another phone, direct backend writes) flow into Room and the
        // reactive Flows powering Home re-emit. The 60s cooldown in TaskSyncRepository
        // keeps quick foreground/background flips from hammering the worker.
        taskSyncRepository.fetchTasks()
        viewModelScope.launch {
            val pending = dataStoreHelper.observeFirstLoginPermissionPromptPending().first()
            val list =
                if (!pending) {
                    emptyList()
                } else {
                    buildList {
                        if (appContext.needsOverlayPermission()) add(PermissionType.OVERLAY)
                        if (appContext.needsPostNotificationsPermission()) add(PermissionType.NOTIFICATION)
                    }
                }
            updateSuccessState { it.copy(pendingPermissions = list) }
            if (pending && list.isEmpty()) {
                dataStoreHelper.setFirstLoginPermissionPromptPending(false)
            }
        }
    }

    private fun dismissPermission(type: PermissionType) {
        viewModelScope.launch {
            updateSuccessState { state ->
                state.copy(pendingPermissions = state.pendingPermissions - type)
            }
            val remaining = (_uiState.value as? UiState.Success)?.pendingPermissions.orEmpty()
            if (remaining.isEmpty()) {
                dataStoreHelper.setFirstLoginPermissionPromptPending(false)
            }
        }
    }

    private inline fun updateSuccessState(crossinline transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { currentState ->
            when (currentState) {
                is UiState.Success -> transform(currentState)
                else -> currentState
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                fetchDailyTask(today)
            } catch (e: IOException) {
                _uiState.value =
                    UiState.Error(
                        message = e.message ?: "Unknown error",
                        throwable = e,
                    )
            }
        }
    }

    private fun retry() {
        _uiState.value = UiState.Loading
        loadInitialData()
    }

    private fun navigateToFilteredTasks(isCompleted: Boolean) {
        val date = (uiState.value as? UiState.Success)?.selectedDate ?: LocalDate.now()
        _navEffect.trySend(NavigationEffect.Navigate(Screen.FilteredTasks(isCompleted, date.toEpochDay())))
    }

    private fun navigateToPomodoro() {
        if (pomodoroEngine.state.value.isRunning) {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.Pomodoro))
        } else {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.PomodoroLaunch))
        }
    }

    private fun toggleStagedExpand(taskId: Long) {
        val current = (_uiState.value as? UiState.Success)?.expandedStagedTaskId
        subtaskJob?.cancel()
        if (current == taskId) {
            updateSuccessState { it.copy(expandedStagedTaskId = null, expandedSubtasks = emptyList()) }
            return
        }
        updateSuccessState { it.copy(expandedStagedTaskId = taskId, expandedSubtasks = emptyList()) }
        subtaskJob = viewModelScope.launch {
            taskRepository.observeSubtasks(taskId).collect { subs ->
                updateSuccessState { it.copy(expandedSubtasks = subs) }
            }
        }
    }

    private fun fetchDailyTask(date: LocalDate) {
        fetchJob?.cancel()
        fetchJob =
            viewModelScope.launch {
                delay(LOADING_DELAY)
                combine(
                    taskRepository.observeTasksByDate(date, includeRecurringInstances = true),
                    taskRepository.observePendingTasksInAWeek(date),
                    taskRepository.countCompletedTasksInAWeek(date),
                    taskRepository.observeTaskPhotoUrls(),
                ) { tasks, pendingTaskCount, completedTaskCount, photoUrls ->
                    val withPhotos =
                        tasks.map { t ->
                            val urls = t.remoteId?.let { photoUrls[it] } ?: emptyList()
                            if (urls.isNotEmpty()) t.copy(photoUrls = urls) else t
                        }
                    timber.log.Timber.tag("TaskFetch").d(
                        "Home tasks=${withPhotos.size}, with photos=${withPhotos.count { it.photoUrls.isNotEmpty() }}, " +
                            "in-mem map size=${photoUrls.size}",
                    )
                    DailyData(withPhotos, pendingTaskCount, completedTaskCount)
                }.collect { data ->
                    var becameSuccess = false
                    val isFirstSuccess = _uiState.value !is UiState.Success
                    val initialDisplayName =
                        if (isFirstSuccess) {
                            dataStoreHelper.observeUser().first()?.displayName.orEmpty()
                        } else {
                            ""
                        }
                    val initialOverdue =
                        if (isFirstSuccess) {
                            taskRepository.observeOverdueTasks(LocalDate.now(clock)).first()
                        } else {
                            emptyList()
                        }
                    val initialTaskDates =
                        if (isFirstSuccess) {
                            val ym = YearMonth.from(date)
                            taskRepository.observeRange(ym.atDay(1), ym.atEndOfMonth()).first()
                                .map { it.date }.toSet()
                        } else {
                            emptySet()
                        }
                    _uiState.update { current ->
                        when (current) {
                            is UiState.Success ->
                                current.copy(
                                    tasks = data.tasks,
                                    pendingTaskCountThisWeek = data.pendingTaskCountThisWeek,
                                    completedTaskCountThisWeek = data.completedTaskCountThisWeek,
                                )

                            else -> {
                                becameSuccess = true
                                val dates = initialOverdue.map { it.date }.toSet()
                                val firstOfDisplayed = YearMonth.from(date).atDay(1)
                                createInitialState(date, data, initialDisplayName).copy(
                                    overdueDates = dates,
                                    hasOverdueBeforeDisplayedMonth =
                                    dates.any { d -> d.isBefore(firstOfDisplayed) },
                                    overdueCount = initialOverdue.size,
                                    taskDatesInMonth = initialTaskDates,
                                )
                            }
                        }
                    }
                    if (becameSuccess) refreshPermissions()
                }
            }
    }

    private fun createInitialState(
        date: LocalDate,
        data: DailyData,
        displayName: String = "",
    ) = UiState.Success(
        selectedDate = date,
        displayedMonth = YearMonth.from(date),
        tasks = data.tasks,
        completedTaskCountThisWeek = data.completedTaskCountThisWeek,
        pendingTaskCountThisWeek = data.pendingTaskCountThisWeek,
        isSheetOpen = false,
        isDeleteDialogOpen = false,
        isSecretModeEnabled = false,
        taskFormState = TaskFormState(),
        displayName = displayName,
        dayMode = com.todoapp.mobile.common.computeDayMode(java.time.LocalTime.now(clock)),
        currentTimeFormatted = java.time.LocalTime.now(clock).format(TIME_FORMATTER),
    )

    private fun changeSelectedDate(uiAction: UiAction.OnDateSelect) {
        val newMonth = YearMonth.from(uiAction.date)
        val firstOfNew = newMonth.atDay(1)
        updateSuccessState {
            it.copy(
                selectedDate = uiAction.date,
                displayedMonth = newMonth,
                hasOverdueBeforeDisplayedMonth = it.overdueDates.any { d -> d.isBefore(firstOfNew) },
            )
        }
        displayedMonthFlow.value = newMonth
        fetchDailyTask(uiAction.date)
    }

    private fun navigateToPreviousMonth() {
        val current = _uiState.value as? UiState.Success ?: return
        val newMonth = current.displayedMonth.minusMonths(1)
        val newDate = newMonth.atDay(1)
        val firstOfNew = newMonth.atDay(1)
        updateSuccessState {
            it.copy(
                displayedMonth = newMonth,
                selectedDate = newDate,
                hasOverdueBeforeDisplayedMonth = it.overdueDates.any { d -> d.isBefore(firstOfNew) },
            )
        }
        displayedMonthFlow.value = newMonth
        fetchDailyTask(newDate)
    }

    private fun navigateToNextMonth() {
        val current = _uiState.value as? UiState.Success ?: return
        val newMonth = current.displayedMonth.plusMonths(1)
        val newDate = if (newMonth == YearMonth.now()) LocalDate.now() else newMonth.atDay(1)
        val firstOfNew = newMonth.atDay(1)
        updateSuccessState {
            it.copy(
                displayedMonth = newMonth,
                selectedDate = newDate,
                hasOverdueBeforeDisplayedMonth = it.overdueDates.any { d -> d.isBefore(firstOfNew) },
            )
        }
        displayedMonthFlow.value = newMonth
        fetchDailyTask(newDate)
    }

    private fun jumpToEarliestOverdue() {
        val state = _uiState.value as? UiState.Success ?: return
        val earliest = state.overdueDates.minOrNull() ?: return
        val newMonth = YearMonth.from(earliest)
        val firstOfNew = newMonth.atDay(1)
        updateSuccessState {
            it.copy(
                displayedMonth = newMonth,
                selectedDate = earliest,
                hasOverdueBeforeDisplayedMonth = it.overdueDates.any { d -> d.isBefore(firstOfNew) },
            )
        }
        displayedMonthFlow.value = newMonth
        fetchDailyTask(earliest)
    }

    private fun setupTaskDatesFlow() {
        viewModelScope.launch {
            displayedMonthFlow
                .flatMapLatest { month ->
                    taskRepository.observeRange(month.atDay(1), month.atEndOfMonth())
                }
                .collect { tasks ->
                    val dates = tasks.map { it.date }.toSet()
                    updateSuccessState { it.copy(taskDatesInMonth = dates) }
                }
        }
    }

    private fun createTask() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return
        if (!validateTaskForm(currentState)) return

        viewModelScope.launch {
            val form = currentState.taskFormState
            val selectedDate = requireNotNull(form.dialogSelectedDate) {
                "validateTaskForm should have ensured dialogSelectedDate is non-null"
            }
            val task =
                Task(
                    title = form.taskTitle,
                    description = form.taskDescription.ifBlank { null },
                    date = selectedDate,
                    timeStart = if (form.isAllDay) {
                        java.time.LocalTime.MIDNIGHT
                    } else {
                        requireNotNull(form.taskTimeStart) { "validateTaskForm should have ensured taskTimeStart is non-null" }
                    },
                    timeEnd = if (form.isAllDay) {
                        java.time.LocalTime.of(23, 59)
                    } else {
                        requireNotNull(form.taskTimeEnd) { "validateTaskForm should have ensured taskTimeEnd is non-null" }
                    },
                    isCompleted = false,
                    isSecret = form.isTaskSecret,
                    reminderOffsetMinutes = form.reminderOffsetMinutes,
                    category = form.selectedCategory,
                    customCategoryName = form.customCategoryName.takeIf {
                        form.selectedCategory == com.todoapp.mobile.domain.model.TaskCategory.OTHER &&
                            it.isNotBlank()
                    },
                    recurrence = form.selectedRecurrence,
                    isAllDay = form.isAllDay,
                    locationName = form.locationName,
                    locationAddress = form.locationAddress,
                    locationLat = form.locationLat,
                    locationLng = form.locationLng,
                )
            if (form.selectedGroupId != null) {
                groupRepository.createGroupTask(form.selectedGroupId, task)
            } else if (form.pendingPhotos.isNotEmpty()) {
                taskRepository.insertWithPhotos(
                    task,
                    form.pendingPhotos.map { it.bytes to it.mimeType },
                )
                scheduleTaskReminders(task)
            } else {
                taskRepository.insert(task)
                scheduleTaskReminders(task)
            }
            dataStoreHelper.setLastUsedReminderOffset(form.reminderOffsetMinutes)
            clearTaskForm()
            dismissBottomSheet()
        }
    }

    private fun checkTask(uiAction: UiAction.OnTaskCheck) {
        viewModelScope.launch(ioDispatcher) {
            val task = uiAction.task
            setTaskCompletion(task, completed = !task.isCompleted)
        }
    }

    private fun deleteTask(task: Task) {
        flushPendingDelete()
        updateSuccessState { it.copy(pendingDeleteTask = task) }
        pendingDeleteJob =
            viewModelScope.launch {
                delay(UNDO_DELAY_MS)
                taskRepository.delete(task)
                updateSuccessState { it.copy(pendingDeleteTask = null) }
            }
    }

    private fun flushPendingDelete() {
        val previous = (_uiState.value as? UiState.Success)?.pendingDeleteTask ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccessState { it.copy(pendingDeleteTask = null) }
        viewModelScope.launch { taskRepository.delete(previous) }
    }

    private fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccessState { it.copy(pendingDeleteTask = null) }
    }

    private fun updateTaskIndices(uiAction: UiAction.OnMoveTask) {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        updateSuccessState { state ->
            val list = state.tasks.toMutableList()
            list.move(uiAction.from, uiAction.to)
            state.copy(tasks = list)
        }

        viewModelScope.launch(ioDispatcher) {
            taskRepository
                .reorderTasksForDate(
                    date = currentState.selectedDate,
                    fromIndex = uiAction.from,
                    toIndex = uiAction.to,
                ).onFailure { t ->
                    Log.e("HomeViewModel", "Failed to persist task reorder", t)
                }
        }
    }

    private fun onTaskLongPressed(uiAction: UiAction.OnTaskLongPress) {
        selectedTask = uiAction.task
        openDeleteDialog()
    }

    private fun changeTaskTitle(uiAction: UiAction.OnTaskTitleChange) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(taskTitle = uiAction.title)) }
    }

    private fun changeTaskDescription(uiAction: UiAction.OnTaskDescriptionChange) {
        updateSuccessState {
            it.copy(
                taskFormState = it.taskFormState.copy(taskDescription = uiAction.description),
            )
        }
    }

    private fun changeTaskDate(uiAction: UiAction.OnTaskDateChange) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(dialogSelectedDate = uiAction.date)) }
    }

    private fun changeTaskTimeStart(uiAction: UiAction.OnTaskTimeStartChange) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(taskTimeStart = uiAction.time)) }
    }

    private fun onDeleteDialogConfirmed() {
        deleteTask(selectedTask)
        closeDeleteDialog()
    }

    private fun changeTaskTimeEnd(uiAction: UiAction.OnTaskTimeEndChange) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(taskTimeEnd = uiAction.time)) }
    }

    private fun toggleTaskSecret(uiAction: UiAction.OnTaskSecretChange) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(isTaskSecret = uiAction.isSecret)) }
    }

    private fun changeReminderOffset(minutes: Long?) {
        updateSuccessState {
            it.copy(taskFormState = it.taskFormState.copy(reminderOffsetMinutes = minutes))
        }
    }

    private fun changeCategory(category: com.todoapp.mobile.domain.model.TaskCategory) {
        updateSuccessState { state ->
            val form = state.taskFormState
            // BIRTHDAY auto-defaults to YEARLY when the user hasn't picked one;
            // moving off BIRTHDAY reverts that auto-set so the explainer doesn't linger.
            val nextRecurrence = when {
                category == com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY &&
                    form.selectedRecurrence == com.todoapp.mobile.domain.model.Recurrence.NONE ->
                    com.todoapp.mobile.domain.model.Recurrence.YEARLY

                form.selectedCategory == com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY &&
                    category != com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY &&
                    form.selectedRecurrence == com.todoapp.mobile.domain.model.Recurrence.YEARLY ->
                    com.todoapp.mobile.domain.model.Recurrence.NONE

                else -> form.selectedRecurrence
            }
            state.copy(
                taskFormState = form.copy(
                    selectedCategory = category,
                    selectedRecurrence = nextRecurrence,
                    customCategoryName = if (category == com.todoapp.mobile.domain.model.TaskCategory.OTHER) {
                        form.customCategoryName
                    } else {
                        ""
                    },
                ),
            )
        }
    }

    private fun changeCustomCategoryName(name: String) {
        updateSuccessState {
            it.copy(taskFormState = it.taskFormState.copy(customCategoryName = name))
        }
    }

    private fun changeRecurrence(recurrence: com.todoapp.mobile.domain.model.Recurrence) {
        updateSuccessState { state ->
            val form = state.taskFormState
            // Recurring tasks anchor on a date (start date). Default to today if none set.
            val anchor = form.dialogSelectedDate
                ?: if (recurrence != com.todoapp.mobile.domain.model.Recurrence.NONE) LocalDate.now() else null
            state.copy(
                taskFormState = form.copy(
                    selectedRecurrence = recurrence,
                    dialogSelectedDate = anchor,
                ),
            )
        }
    }

    private fun changeAllDay(isAllDay: Boolean) {
        updateSuccessState { state ->
            val form = state.taskFormState
            state.copy(
                taskFormState = form.copy(
                    isAllDay = isAllDay,
                    // Hiding time pickers; clear any prior selection + lingering time error.
                    taskTimeStart = if (isAllDay) null else form.taskTimeStart,
                    taskTimeEnd = if (isAllDay) null else form.taskTimeEnd,
                    timeErrorRes = if (isAllDay) null else form.timeErrorRes,
                ),
            )
        }
    }

    private fun setFormLocation(name: String?, address: String?, lat: Double?, lng: Double?) {
        updateSuccessState { state ->
            state.copy(
                taskFormState = state.taskFormState.copy(
                    locationName = name?.takeIf { it.isNotBlank() },
                    locationAddress = address?.takeIf { it.isNotBlank() },
                    locationLat = lat,
                    locationLng = lng,
                ),
            )
        }
    }

    private fun changeFilter(filter: HomeContract.HomeFilter) {
        val state = _uiState.value as? UiState.Success ?: return
        if (state.selectedFilter == filter) return
        updateSuccessState {
            if (filter == HomeContract.HomeFilter.TODAY) {
                it.copy(selectedFilter = filter)
            } else {
                it.copy(selectedFilter = filter, lastRecurringFilter = filter)
            }
        }
        // Restart the data flow with the new filter source.
        when (filter) {
            HomeContract.HomeFilter.TODAY -> fetchDailyTask(state.selectedDate)
            else -> fetchByRecurrence(filterToRecurrence(filter))
        }
    }

    private fun setupAuxiliaryFlows() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(java.time.LocalDateTime.now(clock))
                    kotlinx.coroutines.delay(AUX_TICK_MILLIS)
                }
            }.map { now ->
                val mode = com.todoapp.mobile.common.computeDayMode(now.toLocalTime())
                // End-of-day suggestion (e.g. "move to tomorrow") is only meaningful at the
                // tail of the calendar day. NIGHT also covers 00:00-05:59, where the user
                // has just entered today — gate that out via the explicit hour check.
                val isEndOfDay = mode == com.todoapp.mobile.domain.model.DayMode.EVENING ||
                    (mode == com.todoapp.mobile.domain.model.DayMode.NIGHT && now.hour >= END_OF_DAY_HOUR)
                AuxTick(
                    today = now.toLocalDate(),
                    mode = mode,
                    isEndOfDay = isEndOfDay,
                    todayEpoch = now.toLocalDate().toEpochDay(),
                )
            }.distinctUntilChanged()
                .flatMapLatest { tick ->
                    kotlinx.coroutines.flow.combine(
                        dataStoreHelper.observeUser(),
                        taskRepository.observeTasksByDate(tick.today.minusDays(1), includeRecurringInstances = false),
                        dataStoreHelper.observeSuggestCardDismissedDay(),
                    ) { user, yesterdayTasks, dismissedDay ->
                        AuxState(
                            displayName = user?.displayName.orEmpty(),
                            yesterdayCompleted = yesterdayTasks.count { it.isCompleted },
                            isSuggestDismissedToday = dismissedDay == tick.todayEpoch,
                            dayMode = tick.mode,
                            isEndOfDayMoment = tick.isEndOfDay,
                        )
                    }
                }
                .collect { aux ->
                    updateSuccessState { state ->
                        state.copy(
                            displayName = aux.displayName,
                            yesterdayCompletedCount = aux.yesterdayCompleted,
                            isSuggestCardDismissedToday = aux.isSuggestDismissedToday,
                            dayMode = aux.dayMode,
                            isEndOfDayMoment = aux.isEndOfDayMoment,
                        )
                    }
                }
        }
        viewModelScope.launch {
            while (true) {
                val now = java.time.LocalTime.now(clock).format(TIME_FORMATTER)
                updateSuccessState { state ->
                    if (state.currentTimeFormatted == now) state else state.copy(currentTimeFormatted = now)
                }
                kotlinx.coroutines.delay(AUX_TICK_MILLIS)
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(LocalDate.now(clock))
                    kotlinx.coroutines.delay(AUX_TICK_MILLIS)
                }
            }
                .distinctUntilChanged()
                .flatMapLatest { today -> taskRepository.observeOverdueTasks(today) }
                .collect { overdue ->
                    val dates = overdue.map { it.date }.toSet()
                    updateSuccessState { state ->
                        val firstOfDisplayed = state.displayedMonth.atDay(1)
                        state.copy(
                            overdueDates = dates,
                            hasOverdueBeforeDisplayedMonth = dates.any { d -> d.isBefore(firstOfDisplayed) },
                            overdueCount = overdue.size,
                        )
                    }
                }
        }
    }

    private fun handleSuggestCardPrimary() {
        val state = _uiState.value as? UiState.Success ?: return
        when (state.dayMode) {
            com.todoapp.mobile.domain.model.DayMode.MORNING -> {
                _navEffect.trySend(
                    NavigationEffect.Navigate(
                        route = Screen.Chat,
                        popUpTo = Screen.Home,
                        saveState = true,
                        launchSingleTop = true,
                        restoreState = true,
                    ),
                )
            }
            com.todoapp.mobile.domain.model.DayMode.EVENING,
            com.todoapp.mobile.domain.model.DayMode.NIGHT -> {
                if (!state.isEndOfDayMoment) return
                val pendingIds = state.tasks.filter { !it.isCompleted }.map { it.id }
                viewModelScope.launch {
                    taskRepository.deferTasksToTomorrow(pendingIds)
                    dismissSuggestCard()
                }
            }
            com.todoapp.mobile.domain.model.DayMode.MIDDAY -> Unit
        }
    }

    private fun handleSuggestCardSecondary() {
        // Evening "Olduğu gibi bırak" — just dismisses for today
        dismissSuggestCard()
    }

    private fun dismissSuggestCard() {
        updateSuccessState { it.copy(isSuggestCardDismissedToday = true) }
        viewModelScope.launch {
            dataStoreHelper.setSuggestCardDismissedDay(LocalDate.now(clock).toEpochDay())
        }
    }

    private data class AuxState(
        val displayName: String,
        val yesterdayCompleted: Int,
        val isSuggestDismissedToday: Boolean,
        val dayMode: com.todoapp.mobile.domain.model.DayMode,
        val isEndOfDayMoment: Boolean,
    )

    private data class AuxTick(
        val today: java.time.LocalDate,
        val mode: com.todoapp.mobile.domain.model.DayMode,
        val isEndOfDay: Boolean,
        val todayEpoch: Long,
    )

    private fun filterToRecurrence(filter: HomeContract.HomeFilter): com.todoapp.mobile.domain.model.Recurrence = when (filter) {
        HomeContract.HomeFilter.TODAY -> com.todoapp.mobile.domain.model.Recurrence.NONE
        HomeContract.HomeFilter.DAILY -> com.todoapp.mobile.domain.model.Recurrence.DAILY
        HomeContract.HomeFilter.WEEKLY -> com.todoapp.mobile.domain.model.Recurrence.WEEKLY
        HomeContract.HomeFilter.MONTHLY -> com.todoapp.mobile.domain.model.Recurrence.MONTHLY
        HomeContract.HomeFilter.YEARLY -> com.todoapp.mobile.domain.model.Recurrence.YEARLY
    }

    private fun fetchByRecurrence(recurrence: com.todoapp.mobile.domain.model.Recurrence) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            taskRepository.observeRecurringByType(recurrence).collect { tasks ->
                updateSuccessState { it.copy(tasks = tasks) }
            }
        }
    }

    private fun toggleExistingTaskSecret(action: UiAction.OnToggleTaskSecret) {
        _uiEffect.trySend(UiEffect.ShowBiometricForSecretToggle(action.task))
    }

    private fun performSecretToggle(action: UiAction.OnBiometricSuccessForSecretToggle) {
        viewModelScope.launch {
            taskRepository.update(action.task.copy(isSecret = !action.task.isSecret))
        }
    }

    private fun updateDialogDate(uiAction: UiAction.OnDialogDateSelect) {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(dialogSelectedDate = uiAction.date)) }
    }

    private fun deselectDialogDate() {
        updateSuccessState { it.copy(taskFormState = it.taskFormState.copy(dialogSelectedDate = null)) }
    }

    private fun clearTaskForm() {
        updateSuccessState { it.copy(taskFormState = TaskFormState()) }
    }

    private fun validateTaskForm(state: UiState.Success): Boolean {
        val form = state.taskFormState
        return when {
            form.taskTitle.isBlank() -> {
                showTransientError(R.string.error_task_title_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(titleErrorRes = v))
                }
                false
            }

            form.dialogSelectedDate == null -> {
                showTransientError(R.string.error_task_date_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(dateErrorRes = v))
                }
                false
            }

            !form.isAllDay && (form.taskTimeStart == null || form.taskTimeEnd == null) -> {
                showTransientError(R.string.error_task_time_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(timeErrorRes = v))
                }
                false
            }

            !form.isAllDay &&
                form.taskTimeStart != null &&
                form.taskTimeEnd != null &&
                !form.taskTimeEnd.isAfter(form.taskTimeStart) -> {
                showTransientError(R.string.error_task_end_before_start) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(timeErrorRes = v))
                }
                false
            }

            else -> true
        }
    }

    private fun scheduleTaskReminders(task: Task) {
        // Recurring tasks are scheduled by TaskRepositoryImpl via AlarmScheduler.scheduleRecurring
        // and self-rescheduled in AlarmFireReceiver — skip the one-shot path here.
        if (task.recurrence != com.todoapp.mobile.domain.model.Recurrence.NONE) return
        val offset = task.reminderOffsetMinutes ?: return // null = user opted out
        viewModelScope.launch(ioDispatcher) {
            alarmScheduler.schedule(
                task.toAlarmItem(remindBeforeMinutes = offset),
                type = AlarmType.TASK,
            )
        }
    }

    private fun showTransientError(
        @StringRes errorRes: Int,
        durationMs: Long = 2000L,
        setErrorRes: (UiState.Success, Int?) -> UiState.Success,
    ) {
        viewModelScope.launch {
            updateSuccessState { setErrorRes(it, errorRes) }
            delay(durationMs)
            updateSuccessState { setErrorRes(it, null) }
        }
    }

    private fun showBottomSheet() {
        viewModelScope.launch {
            val lastOffset = dataStoreHelper.observeLastUsedReminderOffset().first()
            val smartDefault = TaskFormState.smartDefault(
                today = LocalDate.now(clock),
                now = java.time.LocalTime.now(clock),
                lastReminderOffset = lastOffset,
            )
            updateSuccessState { it.copy(isSheetOpen = true, taskFormState = smartDefault) }
        }
        viewModelScope.launch {
            groupRepository
                .observeAllGroups()
                .collect { groups ->
                    val items =
                        groups.mapNotNull { group ->
                            group.remoteId?.let { remoteId ->
                                HomeContract.GroupSelectionItem(
                                    remoteId,
                                    group.name,
                                )
                            }
                        }
                    updateSuccessState { it.copy(availableGroups = items) }
                }
        }
    }

    private fun dismissBottomSheet() {
        updateSuccessState { it.copy(isSheetOpen = false) }
    }

    private fun toggleAdvancedSettings() {
        updateSuccessState {
            it.copy(
                taskFormState =
                it.taskFormState.copy(
                    isAdvancedSettingsExpanded = !it.taskFormState.isAdvancedSettingsExpanded,
                ),
            )
        }
    }

    private fun openDeleteDialog() {
        updateSuccessState { it.copy(isDeleteDialogOpen = true) }
    }

    private fun closeDeleteDialog() {
        updateSuccessState { it.copy(isDeleteDialogOpen = false) }
    }

    private fun openTaskDetail(task: Task) {
        selectedTask = task

        if (!task.isSecret) {
            navigateToTaskDetail()
            return
        }

        viewModelScope.launch {
            val isActive =
                secretModePreferences
                    .getCondition()
                    .isActive(System.currentTimeMillis())

            if (isActive) navigateToTaskDetail() else authenticate()
        }
    }

    private fun navigateToTaskDetail() {
        viewModelScope.launch {
            _navEffect.send(NavigationEffect.Navigate(Screen.Task(selectedTask.id)))
        }
    }

    private fun authenticate() {
        _uiEffect.trySend(UiEffect.ShowBiometricAuthenticator)
    }

    private fun handleSuccessfulBiometricAuthentication() {
        viewModelScope.launch {
            val selectedOption =
                SecretModeReopenOptions.byId(
                    secretModePreferences.getLastSelectedOptionId(),
                )
            val condition =
                SecretModeConditionFactory(
                    clock = Clock.systemDefaultZone(),
                ).create(selectedOption)
            secretModePreferences.saveCondition(condition)
            navigateToTaskDetail()
        }
    }

    companion object {
        private const val LOADING_DELAY = 200L
        private const val AUX_TICK_MILLIS = 60_000L
        private const val UNDO_DELAY_MS = 5000L

        // Lower bound of the late-evening half of NIGHT mode (22:00-23:59).
        // Below this, NIGHT means 00:00-05:59 — a freshly started day, not the day's end.
        private const val END_OF_DAY_HOUR = 22
        private val TIME_FORMATTER: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm").withLocale(java.util.Locale.ROOT)
    }
}
