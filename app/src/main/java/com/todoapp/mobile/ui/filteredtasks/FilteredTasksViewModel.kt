package com.todoapp.mobile.ui.filteredtasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.di.DefaultDispatcher
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.security.SecretModeConditionFactory
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.SortOrder
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.TaskTab
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiAction
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiEffect
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FilteredTasksViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val secretModePreferences: SecretPreferences,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect: Flow<UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private lateinit var selectedTask: Task
    private var fetchJob: Job? = null
    private var pendingDeleteJob: Job? = null

    private val args = savedStateHandle.toRoute<Screen.FilteredTasks>()
    private val initialTab = if (args.isCompleted) TaskTab.DONE else TaskTab.PENDING
    private val initialDate = LocalDate.ofEpochDay(args.weekDateEpochDay)

    init {
        fetchTasks(initialDate, initialTab)
    }

    override fun onCleared() {
        super.onCleared()
        val previous = (_uiState.value as? UiState.Success)?.pendingDeleteTask ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        CoroutineScope(SupervisorJob()).launch { taskRepository.delete(previous) }
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnTabSelect -> changeTab(action.tab)
            is UiAction.OnWeekSelect -> changeWeek(action.date)
            is UiAction.OnTaskCheck -> checkTask(action.task)
            is UiAction.OnTaskClick -> openTaskDetail(action.task)
            is UiAction.OnTaskLongPress -> {
                selectedTask = action.task
                updateSuccessState { it.copy(isDeleteDialogOpen = true) }
            }
            is UiAction.OnDeleteDialogConfirm -> onDeleteConfirmed()
            is UiAction.OnDeleteDialogDismiss -> updateSuccessState { it.copy(isDeleteDialogOpen = false) }
            is UiAction.OnToggleTaskSecret -> _uiEffect.trySend(UiEffect.ShowBiometricForSecretToggle(action.task))
            is UiAction.OnBiometricSuccessForSecretToggle -> performSecretToggle(action.task)
            is UiAction.OnSuccessfulBiometricAuthenticationHandle -> handleSuccessfulBiometricAuthentication()
            is UiAction.OnUndoDelete -> undoDelete()
            is UiAction.OnBack -> _navEffect.trySend(NavigationEffect.Back)
            is UiAction.OnToggleSortOrder ->
                updateSuccessState { current ->
                    val newOrder = if (current.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                    current.copy(sortOrder = newOrder, tasks = current.tasks.applySortOrder(newOrder))
                }
        }
    }

    private fun fetchTasks(
        date: LocalDate,
        tab: TaskTab,
    ) {
        fetchJob?.cancel()
        fetchJob =
            viewModelScope.launch {
                taskRepository
                    .observeTasksByWeekAndStatus(
                        date = date,
                        isCompleted = tab == TaskTab.DONE,
                    ).collect { tasks ->
                        val order = (_uiState.value as? UiState.Success)?.sortOrder ?: SortOrder.ASC
                        val sorted = kotlinx.coroutines.withContext(defaultDispatcher) { tasks.applySortOrder(order) }
                        _uiState.update { current ->
                            when (current) {
                                is UiState.Success -> current.copy(tasks = sorted)
                                else ->
                                    UiState.Success(
                                        tasks = sorted,
                                        selectedTab = tab,
                                        selectedWeekDate = date,
                                    )
                            }
                        }
                    }
            }
    }

    private fun List<Task>.applySortOrder(order: SortOrder) = if (order == SortOrder.ASC) sortedBy { it.date } else sortedByDescending { it.date }

    private fun changeTab(tab: TaskTab) {
        val currentState = _uiState.value as? UiState.Success ?: return
        updateSuccessState { it.copy(selectedTab = tab, tasks = emptyList()) }
        fetchTasks(currentState.selectedWeekDate, tab)
    }

    private fun changeWeek(date: LocalDate) {
        val currentState = _uiState.value as? UiState.Success ?: return
        updateSuccessState { it.copy(selectedWeekDate = date, tasks = emptyList()) }
        fetchTasks(date, currentState.selectedTab)
    }

    private fun checkTask(task: Task) {
        viewModelScope.launch(ioDispatcher) {
            taskRepository.updateTaskCompletion(task.id, isCompleted = !task.isCompleted)
        }
    }

    private fun onDeleteConfirmed() {
        updateSuccessState { it.copy(isDeleteDialogOpen = false) }
        deleteTask(selectedTask)
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

    private fun performSecretToggle(task: Task) {
        viewModelScope.launch {
            taskRepository.update(task.copy(isSecret = !task.isSecret))
        }
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

    private inline fun updateSuccessState(crossinline transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { current ->
            when (current) {
                is UiState.Success -> transform(current)
                else -> current
            }
        }
    }

    companion object {
        private const val UNDO_DELAY_MS = 5000L
    }
}
