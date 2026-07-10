package com.todoapp.mobile.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.mobile.domain.repository.InvitationRepository
import com.todoapp.mobile.domain.repository.NotificationRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiAction
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiEffect
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val invitationRepository: InvitationRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<UiEffect>()
    val effect = _effect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var pendingDeleteJob: Job? = null
    private var pendingMarkAllJob: Job? = null

    init {
        observeRepository()
        load(force = false)
    }

    override fun onCleared() {
        super.onCleared()
        val state = _uiState.value as? UiState.Success ?: return
        val flushDeleteId = state.undoDeleteNotificationId.takeIf { pendingDeleteJob?.isActive == true }
        val flushMarkAll = state.pendingMarkAllRead && pendingMarkAllJob?.isActive == true
        pendingDeleteJob?.cancel()
        pendingMarkAllJob?.cancel()
        if (flushDeleteId == null && !flushMarkAll) return
        CoroutineScope(SupervisorJob()).launch {
            flushDeleteId?.let { id ->
                repository.delete(id).onFailure {
                    Timber.e(it, "Failed to flush pending notification delete")
                }
            }
            if (flushMarkAll) {
                repository.markAllRead().onFailure {
                    Timber.e(it, "Failed to flush pending mark-all-read")
                }
            }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnRetry -> load(force = true)
            UiAction.OnPullToRefresh -> load(force = true)
            UiAction.OnMarkAllRead -> scheduleMarkAllRead()
            UiAction.OnUndoMarkAllRead -> undoMarkAllRead()
            is UiAction.OnItemTap -> handleTap(action.notification)
            is UiAction.OnDeleteNotification -> scheduleDelete(action.notification)
            UiAction.OnUndoDelete -> undoDelete()
            is UiAction.OnAcceptInvitation -> acceptInvitation(action.notification)
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.notifications.collect { items ->
                _uiState.update { state ->
                    when {
                        state is UiState.Success -> state.copy(items = items)
                        items.isNotEmpty() -> UiState.Success(items)
                        else -> state
                    }
                }
            }
        }
    }

    private fun load(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                if (state is UiState.Success) state.copy(isRefreshing = true) else UiState.Loading
            }
            repository.refresh(force = force)
                .onSuccess {
                    val items = repository.notifications.value
                    _uiState.update { state ->
                        if (state is UiState.Success) {
                            state.copy(items = items, isRefreshing = false)
                        } else {
                            UiState.Success(items)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is UiState.Success) {
                            state.copy(isRefreshing = false)
                        } else {
                            UiState.Error(e.toUserMessage(context))
                        }
                    }
                }
        }
    }

    private fun scheduleMarkAllRead() {
        val state = _uiState.value as? UiState.Success ?: return
        if (state.pendingMarkAllRead || state.items.none { !it.isRead }) return
        updateSuccess { it.copy(pendingMarkAllRead = true) }
        pendingMarkAllJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            updateSuccess { it.copy(pendingMarkAllRead = false) }
            commitMarkAllRead()
        }
    }

    private fun undoMarkAllRead() {
        pendingMarkAllJob?.cancel()
        pendingMarkAllJob = null
        updateSuccess { it.copy(pendingMarkAllRead = false) }
    }

    private fun commitMarkAllRead() {
        viewModelScope.launch {
            repository.markAllRead().onFailure {
                _effect.trySend(UiEffect.ShowToast(R.string.notifications_mark_all_failed))
            }
        }
    }

    private fun scheduleDelete(notification: Notification) {
        if (_uiState.value !is UiState.Success) return
        flushPendingDelete()
        updateSuccess { it.copy(undoDeleteNotificationId = notification.id) }
        pendingDeleteJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            updateSuccess { it.copy(undoDeleteNotificationId = null) }
            commitDelete(notification.id)
        }
    }

    private fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccess { it.copy(undoDeleteNotificationId = null) }
    }

    /** A new swipe while a previous undo window is open commits the previous delete immediately. */
    private fun flushPendingDelete() {
        val previousId = (_uiState.value as? UiState.Success)?.undoDeleteNotificationId
        if (previousId != null && pendingDeleteJob?.isActive == true) {
            commitDelete(previousId)
        }
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        updateSuccess { it.copy(undoDeleteNotificationId = null) }
    }

    private fun commitDelete(id: Long) {
        viewModelScope.launch {
            repository.delete(id).onFailure {
                _effect.trySend(UiEffect.ShowToast(R.string.notifications_delete_failed))
            }
        }
    }

    private fun updateSuccess(transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { state -> if (state is UiState.Success) transform(state) else state }
    }

    private fun acceptInvitation(notification: Notification) {
        viewModelScope.launch {
            val invitationId = notification.payload["invitationId"]?.toLongOrNull()
            if (invitationId == null) {
                _effect.trySend(UiEffect.ShowToast(R.string.invitation_action_failed))
                return@launch
            }
            invitationRepository.accept(invitationId)
                .onSuccess {
                    _effect.trySend(UiEffect.ShowToast(R.string.invitation_accepted_toast))
                    if (!notification.isRead) repository.markRead(notification.id)
                    repository.refresh(force = true)
                }
                .onFailure {
                    _effect.trySend(UiEffect.ShowToast(R.string.invitation_action_failed))
                }
        }
    }

    private fun handleTap(notification: Notification) {
        viewModelScope.launch {
            if (!notification.isRead) repository.markRead(notification.id)
            val nav = navTargetFor(notification)
            if (nav != null) _navEffect.trySend(NavigationEffect.Navigate(nav))
        }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 5_000L
    }

    private fun navTargetFor(n: Notification): Screen? {
        val groupId = n.payload["groupId"]?.toLongOrNull()
        val taskId = n.payload["taskId"]?.toLongOrNull()
        val groupName = n.payload["groupName"].orEmpty()
        return when (n.type) {
            NotificationType.TASK_ASSIGNED,
            NotificationType.TASK_COMPLETED,
            NotificationType.TASK_DUE_SOON -> {
                if (groupId != null && taskId != null) {
                    Screen.GroupTaskDetail(groupId = groupId, taskId = taskId)
                } else {
                    null
                }
            }
            NotificationType.INVITATION_ACCEPTED,
            NotificationType.INVITATION_DECLINED -> {
                if (groupId != null) Screen.GroupDetail(groupId = groupId, groupName = groupName) else null
            }
            NotificationType.INVITATION_RECEIVED -> Screen.Invitations
            NotificationType.UNKNOWN -> null
        }
    }
}
