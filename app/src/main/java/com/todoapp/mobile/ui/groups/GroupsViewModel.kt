package com.todoapp.mobile.ui.groups

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.data.model.network.data.GroupSummaryData
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.GroupsContract.UiAction
import com.todoapp.mobile.ui.groups.GroupsContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val sessionPreferences: SessionPreferences,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val pendingDeleteGroupId = savedStateHandle.toRoute<Screen.Groups>().pendingDeleteGroupId
    private var pendingDeleteHandled = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private val _uiEffect = Channel<GroupsContract.UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private var remoteSummaryCache = mapOf<Long, GroupSummaryData>()
    private var selectedGroup: GroupsContract.GroupUiItem? = null
    private var pendingDeleteJob: Job? = null

    init {
        fetchRemoteGroups()
        observeLocalGroups()
    }

    override fun onCleared() {
        super.onCleared()
        val previous = (_uiState.value as? UiState.Success)?.pendingDeleteGroup ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        CoroutineScope(SupervisorJob()).launch {
            groupRepository
                .deleteGroup(previous.id)
                .onFailure { t -> Log.e("GroupsViewModel", "Failed to flush pending group delete", t) }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnCreateNewGroupTap -> createNewGroup()
            is UiAction.OnDeleteGroupTap -> onDeleteGroupTapped(action)
            UiAction.OnDeleteGroupDialogConfirm -> onDeleteGroupDialogConfirmed()
            UiAction.OnDeleteGroupDialogDismiss -> closeDeleteDialog()
            UiAction.OnUndoDeleteGroup -> undoDeleteGroup()
            is UiAction.OnGroupTap -> {
                action.remoteId.let { remoteId ->
                    _navEffect.trySend(
                        NavigationEffect.Navigate(
                            Screen.GroupDetail(
                                groupId = remoteId,
                                groupName = action.groupName,
                                initialTab = action.initialTab,
                            ),
                        ),
                    )
                }
            }
            is UiAction.OnMoveGroup -> reorderGroups(action)
            UiAction.OnScreenResumed -> fetchRemoteGroups()
            UiAction.OnPullToRefresh -> refreshFromPull()
        }
    }

    private fun refreshFromPull() {
        updateSuccessState { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            groupRepository
                .getGroups(force = true)
                .onSuccess { result ->
                    remoteSummaryCache = result.groups.associateBy { it.id }
                    result.groups.forEach { group ->
                        launch { groupRepository.syncGroupTasks(group.id) }
                    }
                }
            updateSuccessState { it.copy(isRefreshing = false) }
        }
    }

    private fun createNewGroup() {
        viewModelScope.launch {
            val isUserAuthenticated = sessionPreferences.getAccessToken().isNullOrBlank().not()

            if (isUserAuthenticated) {
                _navEffect.trySend(NavigationEffect.Navigate(Screen.CreateNewGroup))
            } else {
                _navEffect.trySend(
                    NavigationEffect.Navigate(
                        Screen.Login(redirectAfterLogin = Screen.Groups::class.qualifiedName),
                    ),
                )
            }
        }
    }

    private fun fetchRemoteGroups() {
        viewModelScope.launch {
            repeat(MAX_RETRY_COUNT) { attempt ->
                groupRepository
                    .getGroups()
                    .onSuccess { result ->
                        remoteSummaryCache = result.groups.associateBy { it.id }
                        result.groups.forEach { group ->
                            launch { groupRepository.syncGroupTasks(group.id) }
                        }
                        return@launch
                    }.onFailure { t ->
                        if (attempt < MAX_RETRY_COUNT - 1) {
                            delay(RETRY_DELAY_MS)
                        }
                    }
            }
        }
    }

    private fun observeLocalGroups() {
        viewModelScope.launch {
            groupRepository.observeAllGroups().collect { groups ->
                val isUserAuthenticated = sessionPreferences.getAccessToken().isNullOrBlank().not()
                // Stale rows can linger after a previous logout if cleanup failed. Without a token
                // we can't delete them on the server (returns 401), and they're not "the user's"
                // groups anymore — purge locally and force the empty state.
                if (!isUserAuthenticated && groups.isNotEmpty()) {
                    groupRepository.deleteAllLocalGroups()
                    return@collect
                }
                _uiState.update {
                    (
                        if (groups.isEmpty()) {
                            UiState.Empty(isUserAuthenticated = isUserAuthenticated)
                        } else {
                            UiState.Success(
                                groups = groups.map { it.toUiItem() },
                                isUserAuthenticated = isUserAuthenticated,
                            )
                        }
                        )
                }
                if (!pendingDeleteHandled && pendingDeleteGroupId > 0 && groups.isNotEmpty()) {
                    pendingDeleteHandled = true
                    val target = groups.find { it.remoteId == pendingDeleteGroupId }
                    target?.let { startPendingDelete(it.toUiItem()) }
                }
            }
        }
    }

    private fun onDeleteGroupTapped(action: UiAction.OnDeleteGroupTap) {
        val current = (_uiState.value as? UiState.Success) ?: return
        selectedGroup = current.groups.find { it.id == action.id }
        updateSuccessState { it.copy(isDeleteDialogOpen = true) }
    }

    private fun onDeleteGroupDialogConfirmed() {
        selectedGroup?.let { startPendingDelete(it) }
        selectedGroup = null
        updateSuccessState { it.copy(isDeleteDialogOpen = false) }
    }

    private fun closeDeleteDialog() {
        selectedGroup = null
        updateSuccessState { it.copy(isDeleteDialogOpen = false) }
    }

    private fun startPendingDelete(group: GroupsContract.GroupUiItem) {
        flushPendingDelete()
        updateSuccessState { it.copy(pendingDeleteGroup = group) }
        pendingDeleteJob =
            viewModelScope.launch {
                delay(UNDO_DELAY_MS)
                groupRepository
                    .deleteGroup(group.id)
                    .onFailure { t ->
                        Log.e("GroupsViewModel", "Failed to delete group", t)
                        _uiEffect.trySend(
                            GroupsContract.UiEffect.ShowToast(t.toUserMessage(context)),
                        )
                    }
                updateSuccessState { it.copy(pendingDeleteGroup = null) }
            }
    }

    private fun flushPendingDelete() {
        val previous = (_uiState.value as? UiState.Success)?.pendingDeleteGroup ?: return
        if (pendingDeleteJob?.isActive != true) return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccessState { it.copy(pendingDeleteGroup = null) }
        viewModelScope.launch {
            groupRepository
                .deleteGroup(previous.id)
                .onFailure { t ->
                    Log.e("GroupsViewModel", "Failed to flush pending group delete", t)
                    _uiEffect.trySend(
                        GroupsContract.UiEffect.ShowToast(t.toUserMessage(context)),
                    )
                }
        }
    }

    private fun undoDeleteGroup() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccessState { it.copy(pendingDeleteGroup = null) }
    }

    private fun updateSuccessState(transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { current -> (current as? UiState.Success)?.let(transform) ?: current }
    }

    private fun reorderGroups(action: UiAction.OnMoveGroup) {
        val current = (_uiState.value as? UiState.Success) ?: return

        val mutable = current.groups.toMutableList()
        val item = mutable.removeAt(action.from)
        mutable.add(action.to, item)
        _uiState.value = current.copy(groups = mutable)

        viewModelScope.launch(ioDispatcher) {
            groupRepository
                .reorderGroups(
                    fromIndex = action.from,
                    toIndex = action.to,
                ).onFailure { t ->
                    Log.e("GroupsViewModel", "Failed to persist group reorder", t)
                }
        }
    }

    private fun Group.toUiItem(): GroupsContract.GroupUiItem {
        val remote = remoteSummaryCache[remoteId]
        return GroupsContract.GroupUiItem(
            id = id,
            remoteId = remoteId,
            name = name,
            role = remote?.role ?: role,
            description = description,
            memberCount = remote?.memberCount ?: memberCount,
            pendingTaskCount = remote?.pendingTaskCount ?: pendingTaskCount,
            createdAt = formatTimestamp(createdAt),
            avatarUrl =
            remote?.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                val base =
                    com.todoapp.mobile.BuildConfig.BASE_URL
                        .trimEnd('/')
                "$base/${it.trimStart('/')}"
            },
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 500L
        private const val UNDO_DELAY_MS = 5000L
    }
}
