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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    init {
        observeRepository()
        load(force = false)
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnRetry -> load(force = true)
            UiAction.OnPullToRefresh -> load(force = true)
            UiAction.OnMarkAllRead -> markAllRead()
            is UiAction.OnItemTap -> handleTap(action.notification)
            is UiAction.OnDeleteNotification -> deleteNotification(action.notification)
            is UiAction.OnAcceptInvitation -> acceptInvitation(action.notification)
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.notifications.collect { items ->
                val current = _uiState.value
                if (current is UiState.Success) {
                    _uiState.update { UiState.Success(items, isRefreshing = current.isRefreshing) }
                } else if (items.isNotEmpty()) {
                    _uiState.update { UiState.Success(items) }
                }
            }
        }
    }

    private fun load(force: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is UiState.Success) {
                _uiState.update { current.copy(isRefreshing = true) }
            } else {
                _uiState.update { UiState.Loading }
            }
            repository.refresh(force = force)
                .onSuccess {
                    val items = repository.notifications.value
                    _uiState.update { UiState.Success(items, isRefreshing = false) }
                }
                .onFailure { e ->
                    if (current is UiState.Success) {
                        _uiState.update { current.copy(isRefreshing = false) }
                    } else {
                        _uiState.update {
                            UiState.Error(e.toUserMessage(context))
                        }
                    }
                }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess {
                    _effect.trySend(UiEffect.ShowToast(R.string.notifications_mark_all_succeeded))
                }
                .onFailure {
                    _effect.trySend(UiEffect.ShowToast(R.string.notifications_mark_all_failed))
                }
        }
    }

    private fun deleteNotification(notification: Notification) {
        viewModelScope.launch {
            repository.delete(notification.id).onFailure {
                _effect.trySend(UiEffect.ShowToast(R.string.notifications_delete_failed))
            }
        }
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
