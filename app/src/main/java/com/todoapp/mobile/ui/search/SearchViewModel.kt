package com.todoapp.mobile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.security.SecretModeConditionFactory
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.domain.usecase.SetTaskCompletionUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.search.SearchContract.DateRangeFilter
import com.todoapp.mobile.ui.search.SearchContract.SearchFilter
import com.todoapp.mobile.ui.search.SearchContract.SearchFilters
import com.todoapp.mobile.ui.search.SearchContract.SearchResultItem
import com.todoapp.mobile.ui.search.SearchContract.StatusFilter
import com.todoapp.mobile.ui.search.SearchContract.UiAction
import com.todoapp.mobile.ui.search.SearchContract.UiEffect
import com.todoapp.mobile.ui.search.SearchContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val setTaskCompletion: SetTaskCompletionUseCase,
    private val groupRepository: GroupRepository,
    private val secretPreferences: SecretPreferences,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: kotlinx.coroutines.flow.StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect: kotlinx.coroutines.flow.Flow<UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var pendingSecretTask: Task? = null
    private var groupSearchJob: Job? = null

    private var lastPersonalTasks: List<Task> = emptyList()
    private var lastGroupPairs: List<Pair<Group, List<GroupTask>>> = emptyList()
    private var activeFilters: SearchFilters = SearchFilters()

    init {
        observeSearchQuery()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnQueryChange -> _query.update { action.query }
            is UiAction.OnOpenFilterDialog -> setFilterDialogOpen(true)
            is UiAction.OnDismissFilterDialog -> setFilterDialogOpen(false)
            is UiAction.OnApplyFilters -> applyFilters(action.filters)
            is UiAction.OnClearFilters -> applyFilters(SearchFilters())
            is UiAction.OnTaskClick -> openTaskDetail(action.task)
            is UiAction.OnTaskCheck -> toggleCompletion(action.task)
            is UiAction.OnGroupClick -> navigateToGroup(action.group)
            is UiAction.OnGroupTaskClick -> navigateToGroupTask(action.group, action.groupTask)
            is UiAction.OnBiometricSuccess -> handleSuccessfulBiometricAuthentication()
            is UiAction.OnBack -> _navEffect.trySend(NavigationEffect.Back)
        }
    }

    private fun setFilterDialogOpen(isOpen: Boolean) {
        _uiState.update { current ->
            if (current is UiState.Success) current.copy(isFilterDialogOpen = isOpen) else current
        }
    }

    private fun applyFilters(filters: SearchFilters) {
        activeFilters = filters
        _uiState.update { current ->
            if (current is UiState.Success) {
                current.copy(
                    results = buildResultList(lastPersonalTasks, lastGroupPairs, filters),
                    filters = filters,
                    isFilterDialogOpen = false,
                )
            } else {
                current
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _query
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { raw ->
                    groupSearchJob?.cancel()
                    activeFilters = SearchFilters()
                    if (raw.isBlank()) {
                        lastPersonalTasks = emptyList()
                        lastGroupPairs = emptyList()
                        _uiState.update { UiState.Idle }
                        flowOf(emptyList<Task>() to "")
                    } else {
                        _uiState.update { UiState.Loading }
                        taskRepository
                            .searchTasks(raw)
                            .map { tasks -> tasks to raw }
                            .catch { e -> _uiState.update { UiState.Error(e.message ?: "Search failed") } }
                    }
                }.collect { (personalTasks, raw) ->
                    if (raw.isNotBlank()) {
                        lastPersonalTasks = personalTasks
                        lastGroupPairs = emptyList()
                        _uiState.update {
                            UiState.Success(
                                results = buildResultList(personalTasks, emptyList(), SearchFilters()),
                                query = raw,
                                filters = SearchFilters(),
                            )
                        }
                        groupSearchJob =
                            viewModelScope.launch {
                                fetchAndMergeGroupResults(raw, personalTasks)
                            }
                    }
                }
        }
    }

    private suspend fun fetchAndMergeGroupResults(
        query: String,
        personalTasks: List<Task>,
    ) {
        groupRepository.searchGroupTasksAcrossGroups(query).onSuccess { groupPairs ->
            lastGroupPairs = groupPairs
            _uiState.update { current ->
                if (current is UiState.Success && current.query == query) {
                    current.copy(
                        results = buildResultList(personalTasks, groupPairs, activeFilters),
                    )
                } else {
                    current
                }
            }
        }
    }

    private fun buildResultList(
        personalTasks: List<Task>,
        groupPairs: List<Pair<Group, List<GroupTask>>>,
        filters: SearchFilters,
    ): List<SearchResultItem> {
        val items = mutableListOf<SearchResultItem>()
        val resultType = filters.resultType
        if (resultType == SearchFilter.ALL || resultType == SearchFilter.TASKS) {
            personalTasks
                .filter { matchesAttributeFilters(it, filters) }
                .forEach { items.add(SearchResultItem.PersonalTask(it)) }
        }
        if (resultType == SearchFilter.ALL || resultType == SearchFilter.GROUPS || resultType == SearchFilter.GROUP_TASKS) {
            groupPairs.forEach { (group, tasks) ->
                val includeTasks = resultType == SearchFilter.ALL || resultType == SearchFilter.GROUP_TASKS
                val includeHeader = resultType == SearchFilter.ALL || resultType == SearchFilter.GROUPS || tasks.isNotEmpty()
                if (includeHeader) items.add(SearchResultItem.GroupHeader(group))
                if (includeTasks) tasks.forEach { items.add(SearchResultItem.GroupTaskResult(group, it)) }
            }
        }
        return items
    }

    // Category, recurrence, status and date filters apply to personal tasks only.
    // Group tasks aren't tagged with recurrence/category in the current data model, so
    // attribute filters are silently skipped for them.
    private fun matchesAttributeFilters(task: Task, filters: SearchFilters): Boolean {
        val categoryOk = filters.categories.isEmpty() || task.category in filters.categories
        val recurrenceOk = filters.recurrences.isEmpty() || task.recurrence in filters.recurrences
        val statusOk = when (filters.status) {
            StatusFilter.ALL -> true
            StatusFilter.PENDING -> !task.isCompleted
            StatusFilter.COMPLETED -> task.isCompleted
        }
        val dateOk = matchesDateRange(task.date, filters.dateRange)
        return categoryOk && recurrenceOk && statusOk && dateOk
    }

    private fun matchesDateRange(date: LocalDate, range: DateRangeFilter): Boolean {
        if (range == DateRangeFilter.ALL_TIME) return true
        val today = LocalDate.now()
        val (start, end) = when (range) {
            DateRangeFilter.TODAY -> today to today
            DateRangeFilter.THIS_WEEK -> today.with(DayOfWeek.MONDAY) to today.with(DayOfWeek.SUNDAY)
            DateRangeFilter.THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            DateRangeFilter.ALL_TIME -> return true
        }
        return !date.isBefore(start) && !date.isAfter(end)
    }

    private fun navigateToGroup(group: Group) {
        _navEffect.trySend(
            NavigationEffect.Navigate(Screen.GroupDetail(group.remoteId ?: group.id, group.name)),
        )
    }

    private fun navigateToGroupTask(
        group: Group,
        groupTask: GroupTask,
    ) {
        val groupRemoteId = group.remoteId ?: return
        _navEffect.trySend(
            NavigationEffect.Navigate(Screen.GroupTaskDetail(groupRemoteId, groupTask.id)),
        )
    }

    private fun openTaskDetail(task: Task) {
        if (!task.isSecret) {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.Task(task.id)))
            return
        }
        pendingSecretTask = task
        viewModelScope.launch {
            val isActive =
                secretPreferences
                    .getCondition()
                    .isActive(System.currentTimeMillis())
            if (isActive) {
                _navEffect.trySend(NavigationEffect.Navigate(Screen.Task(task.id)))
            } else {
                _uiEffect.trySend(UiEffect.ShowBiometricAuthenticator)
            }
        }
    }

    private fun handleSuccessfulBiometricAuthentication() {
        viewModelScope.launch {
            val selectedOption =
                SecretModeReopenOptions.byId(
                    secretPreferences.getLastSelectedOptionId(),
                )
            val condition =
                SecretModeConditionFactory(
                    clock = Clock.systemDefaultZone(),
                ).create(selectedOption)
            secretPreferences.saveCondition(condition)
            pendingSecretTask?.let {
                _navEffect.send(NavigationEffect.Navigate(Screen.Task(it.id)))
            }
            pendingSecretTask = null
        }
    }

    private fun toggleCompletion(task: Task) {
        viewModelScope.launch {
            setTaskCompletion(task, completed = !task.isCompleted)
        }
    }
}
