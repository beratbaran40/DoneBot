package com.todoapp.mobile.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.common.maskDescription
import com.todoapp.mobile.common.maskTitle
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.firesOnDate
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.security.SecretModeConditionFactory
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.domain.usecase.ObserveOverdueSummaryUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.calendar.CalendarContract.GroupTaskCalendarItem
import com.todoapp.mobile.ui.calendar.CalendarContract.PersonalTaskCalendarItem
import com.todoapp.mobile.ui.calendar.CalendarContract.UiAction
import com.todoapp.mobile.ui.calendar.CalendarContract.UiEffect
import com.todoapp.mobile.ui.calendar.CalendarContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val groupRepository: GroupRepository,
    private val taskSyncRepository: TaskSyncRepository,
    private val secretModePreferences: SecretPreferences,
    private val pomodoroEngine: PomodoroEngine,
    private val observeOverdueSummary: ObserveOverdueSummaryUseCase,
    private val clock: Clock,
    private val dataStoreHelper: DataStoreHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Success())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<UiEffect>()
    val effect = _effect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var pendingTaskId: Long = -1L

    init {
        taskSyncRepository.fetchTasks(force = true)
        observeSignedIn()
        seedInitialOverdue()
        syncTasksWithSelectedDate()
        syncTaskDatesForMonth()
        setupOverdueFlow()
    }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            is UiAction.OnDateDeselect -> deselectDate()
            is UiAction.OnDateSelect -> updateDate(uiAction)
            is UiAction.OnMonthForward ->
                updateSuccessState {
                    val newMonth = it.selectedMonth.plusMonths(1)
                    it.copy(
                        selectedMonth = newMonth,
                        hasOverdueBeforeDisplayedMonth =
                        it.overdueDates.any { d -> d.isBefore(newMonth.atDay(1)) },
                    )
                }
            is UiAction.OnMonthBack ->
                updateSuccessState {
                    val newMonth = it.selectedMonth.minusMonths(1)
                    it.copy(
                        selectedMonth = newMonth,
                        hasOverdueBeforeDisplayedMonth =
                        it.overdueDates.any { d -> d.isBefore(newMonth.atDay(1)) },
                    )
                }
            is UiAction.OnRetry -> retry()
            is UiAction.OnTaskClick -> navigateToTask(uiAction.taskId)
            is UiAction.OnPomodoroTap -> navigateToPomodoro()
            is UiAction.OnSuccessfulBiometricAuthenticationHandle -> handleSuccessfulBiometricAuthentication()
            is UiAction.OnGroupTaskPhotoOpen -> updateSuccessState { it.copy(viewerPhotoUrl = uiAction.url) }
            is UiAction.OnGroupTaskPhotoDismiss -> updateSuccessState { it.copy(viewerPhotoUrl = null) }
            is UiAction.OnGroupTaskClick ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(Screen.GroupTaskDetail(uiAction.groupId, uiAction.taskId)),
                )
            is UiAction.OnJumpToEarliestOverdue -> jumpToEarliestOverdue()
            is UiAction.OnJournalTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Journal))
            is UiAction.OnCreateHubTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.CreationHub))
            is UiAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        updateSuccessState { it.copy(isRefreshing = true) }
        taskSyncRepository.fetchTasks(force = true)
        viewModelScope.launch {
            delay(REFRESH_INDICATOR_MS)
            updateSuccessState { it.copy(isRefreshing = false) }
        }
    }

    private fun navigateToTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            pendingTaskId = taskId

            if (!task.isSecret) {
                navigateToTaskDetail()
                return@launch
            }

            val isActive =
                secretModePreferences
                    .getCondition()
                    .isActive(System.currentTimeMillis())

            if (isActive) navigateToTaskDetail() else _effect.trySend(UiEffect.ShowBiometricAuthenticator)
        }
    }

    private fun navigateToTaskDetail() {
        viewModelScope.launch {
            _navEffect.send(NavigationEffect.Navigate(Screen.Task(pendingTaskId)))
        }
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

    private fun navigateToPomodoro() {
        if (pomodoroEngine.state.value.isRunning) {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.Pomodoro))
        } else {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.AddPomodoroTimer))
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

    private fun retry() {
        _uiState.value = UiState.Success()
        syncTasksWithSelectedDate()
    }

    private fun syncTasksWithSelectedDate() {
        viewModelScope.launch {
            _uiState
                .filterIsInstance<UiState.Success>()
                .map { it.selectedDate }
                .distinctUntilChanged()
                .collectLatest { date ->
                    if (date != null) {
                        launch {
                            val remoteIds = taskRepository
                                .observeTasksByDate(date)
                                .first()
                                .mapNotNull { it.remoteId }
                            if (remoteIds.isNotEmpty()) taskRepository.refreshPhotoUrls(remoteIds)
                        }
                    }
                    observeTasks(date).collect(::applyDayData)
                }
        }
    }

    private fun applyDayData(data: DayData) {
        updateSuccessState { it.copy(personalTaskItems = data.personal, groupTaskItems = data.group) }
    }

    private fun observeTasks(date: LocalDate?): Flow<DayData> = when {
        date == null -> flowOf(DayData(emptyList(), emptyList()))
        else ->
            combine(
                taskRepository.observeTasksByDate(date),
                groupRepository.observeAllGroupTasks(),
                taskRepository.observeTaskPhotoUrls(),
            ) { personalTasks, groupTasks, photoUrlsByRemoteId ->
                val groupForDate = groupTasks.filter { it.firesOnDate(date) }
                val personalWithPhotos = personalTasks.map { task ->
                    val remoteUrls = task.remoteId?.let { photoUrlsByRemoteId[it] }.orEmpty()
                    if (remoteUrls.isNotEmpty()) task.copy(photoUrls = remoteUrls) else task
                }
                DayData(
                    personal = mapPersonalTasks(personalWithPhotos),
                    group = groupForDate.map { it.toCalendarItem() },
                )
            }
    }

    private data class DayData(
        val personal: List<PersonalTaskCalendarItem>,
        val group: List<GroupTaskCalendarItem>,
    )

    private fun deselectDate() {
        updateSuccessState { it.copy(selectedDate = null) }
    }

    private fun updateDate(uiAction: UiAction.OnDateSelect) {
        val newMonth = YearMonth.from(uiAction.date)
        updateSuccessState {
            it.copy(
                selectedDate = uiAction.date,
                selectedMonth = newMonth,
                hasOverdueBeforeDisplayedMonth =
                it.overdueDates.any { d -> d.isBefore(newMonth.atDay(1)) },
            )
        }
    }

    private fun jumpToEarliestOverdue() {
        val state = _uiState.value as? UiState.Success ?: return
        val earliest = state.overdueDates.minOrNull() ?: return
        val newMonth = YearMonth.from(earliest)
        updateSuccessState {
            it.copy(
                selectedDate = earliest,
                selectedMonth = newMonth,
                hasOverdueBeforeDisplayedMonth =
                it.overdueDates.any { d -> d.isBefore(newMonth.atDay(1)) },
            )
        }
    }

    private fun seedInitialOverdue() {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val summary = observeOverdueSummary(today).first()
            updateSuccessState { state ->
                val firstOfDisplayed = state.selectedMonth.atDay(1)
                state.copy(
                    overdueDates = summary.dates,
                    hasOverdueBeforeDisplayedMonth =
                    summary.dates.any { it.isBefore(firstOfDisplayed) },
                    overdueCount = summary.count,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupOverdueFlow() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(LocalDate.now(clock))
                    delay(OVERDUE_TICK_MILLIS)
                }
            }
                .distinctUntilChanged()
                .flatMapLatest { today -> observeOverdueSummary(today) }
                .collect { summary ->
                    updateSuccessState { state ->
                        val firstOfDisplayed = state.selectedMonth.atDay(1)
                        state.copy(
                            overdueDates = summary.dates,
                            hasOverdueBeforeDisplayedMonth =
                            summary.dates.any { it.isBefore(firstOfDisplayed) },
                            overdueCount = summary.count,
                        )
                    }
                }
        }
    }

    // A cached user means a real account — a guest's local-only tasks never sync, so the Calendar
    // "not synced" badge is gated on this the same way Home is (see HomeViewModel.AuxState). §5.5
    private fun observeSignedIn() {
        viewModelScope.launch {
            dataStoreHelper.observeUser().collect { user ->
                updateSuccessState { it.copy(isSignedIn = user != null) }
            }
        }
    }

    private fun mapPersonalTasks(personalTasks: List<Task>): List<PersonalTaskCalendarItem> = personalTasks.map { task ->
        val dueAt = task.date
            .atTime(task.timeEnd)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val maskedDescription = task.description?.let { if (task.isSecret) it.maskDescription() else it }
        val photoUrl = task.photoUrls
            .firstOrNull()
            ?.takeIf { it.isNotBlank() && !task.isSecret }
            ?.let(::absoluteUrl)
        PersonalTaskCalendarItem(
            taskId = task.id,
            title = if (task.isSecret) task.title.maskTitle() else task.title,
            description = maskedDescription,
            dueAtEpochMs = dueAt,
            isCompleted = task.isCompleted,
            photoUrl = photoUrl,
            isRecurringInstance = task.recurrence != com.todoapp.mobile.domain.model.Recurrence.NONE,
            locationName = task.locationName,
            locationAddress = task.locationAddress,
            locationLat = task.locationLat,
            locationLng = task.locationLng,
            subtaskTotal = task.subtaskTotal,
            subtaskDone = task.subtaskDone,
            isPendingSync = task.isPendingSync,
        )
    }

    private fun GroupTask.toCalendarItem(): GroupTaskCalendarItem {
        val assignee = this.assignee
        val assigneeName = assignee?.displayName
        val assigneeAvatarUrl =
            assignee?.avatarUrl?.takeIf { it.isNotBlank() }?.let(::absoluteUrl)
                ?: assignee?.userId?.let { "${BuildConfig.BASE_URL.trimEnd('/')}/users/$it/avatar" }
        val assigneeInitials = assigneeName
            ?.split(" ")
            ?.mapNotNull { it.firstOrNull()?.toString() }
            ?.take(2)
            ?.joinToString("")
            ?.uppercase()
            ?: "?"
        val photoUrl = photoUrls.firstOrNull()?.takeIf { it.isNotBlank() }?.let(::absoluteUrl)
        return GroupTaskCalendarItem(
            taskId = id,
            groupId = groupId,
            title = title,
            priority = priority,
            dueAtEpochMs = dueDate ?: 0L,
            assigneeName = assigneeName,
            assigneeAvatarUrl = assigneeAvatarUrl,
            assigneeInitials = assigneeInitials,
            photoUrl = photoUrl,
            isCompleted = isCompleted,
        )
    }

    private fun absoluteUrl(relative: String): String {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        return "$base/${relative.trimStart('/')}"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun syncTaskDatesForMonth() {
        viewModelScope.launch {
            _uiState
                .filterIsInstance<UiState.Success>()
                .map { it.selectedMonth }
                .distinctUntilChanged()
                .flatMapLatest { month ->
                    val firstDay = month.atDay(1)
                    val startDate = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
                    // The grid draws six week rows (see calendarGridWeeks) — 35 covered only five, so
                    // the last row never got its markers.
                    val endDate = startDate.plusDays(GRID_SPAN_DAYS)
                    combine(
                        taskRepository.observeRange(startDate, endDate),
                        groupRepository.observeAllGroupTasks(),
                    ) { personalTasks, groupTasks ->
                        val personalDates = personalTasks.map { it.date }
                        // A repeating group task marks every day it fires on, not just its start —
                        // otherwise a daily chore leaves the rest of the month looking empty.
                        val visibleDays = generateSequence(startDate) { it.plusDays(1) }
                            .takeWhile { !it.isAfter(endDate) }
                            .toList()
                        val groupDates = groupTasks.flatMap { task ->
                            visibleDays.filter { task.firesOnDate(it) }
                        }
                        (personalDates + groupDates).toSet()
                    }
                }.collect { taskDates ->
                    updateSuccessState { it.copy(taskDatesInMonth = taskDates) }
                }
        }
    }

    companion object {
        /** Six week rows, inclusive of both ends — matches CALENDAR_WEEK_ROWS in the picker grid. */
        private const val GRID_SPAN_DAYS = 41L
        private const val OVERDUE_TICK_MILLIS = 60_000L
        private const val REFRESH_INDICATOR_MS = 800L
    }
}
