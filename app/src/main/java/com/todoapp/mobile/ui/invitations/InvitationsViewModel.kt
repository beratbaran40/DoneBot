package com.todoapp.mobile.ui.invitations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.model.Invitation
import com.todoapp.mobile.domain.repository.InvitationRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.invitations.InvitationsContract.PendingAction
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiAction
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiEffect
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiState
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
class InvitationsViewModel @Inject constructor(
    private val repository: InvitationRepository,
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
            is UiAction.OnAccept -> setPendingForId(action.id, PendingAction::Accept)
            is UiAction.OnDecline -> setPendingForId(action.id, PendingAction::Decline)
            UiAction.OnConfirmPending -> confirmPending()
            UiAction.OnDismissPending -> dismissPending()
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.pending.collect { items ->
                _uiState.update { current ->
                    when (current) {
                        is UiState.Success -> current.copy(items = items)
                        else -> if (items.isNotEmpty()) UiState.Success(items) else current
                    }
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
                    _uiState.update {
                        UiState.Success(
                            items = repository.pending.value,
                            isRefreshing = false,
                        )
                    }
                }
                .onFailure { e ->
                    if (current is UiState.Success) {
                        _uiState.update { current.copy(isRefreshing = false) }
                    } else {
                        _uiState.update { UiState.Error(e.toUserMessage(context)) }
                    }
                }
        }
    }

    private fun setPendingForId(id: Long, builder: (Invitation) -> PendingAction) {
        val current = _uiState.value as? UiState.Success ?: return
        val target = current.items.firstOrNull { it.id == id } ?: return
        _uiState.update { current.copy(pendingAction = builder(target)) }
    }

    private fun dismissPending() {
        _uiState.update { current ->
            if (current is UiState.Success) current.copy(pendingAction = null) else current
        }
    }

    private fun confirmPending() {
        val current = _uiState.value as? UiState.Success ?: return
        val pending = current.pendingAction ?: return
        // Clear dialog state immediately so the user sees the spinner state on the row.
        _uiState.update { current.copy(pendingAction = null) }
        when (pending) {
            is PendingAction.Accept -> accept(pending.invitation.id)
            is PendingAction.Decline -> decline(pending.invitation.id)
        }
    }

    private fun accept(id: Long) {
        markProcessing(id, true)
        viewModelScope.launch {
            val item = repository.pending.value.firstOrNull { it.id == id }
            repository.accept(id)
                .onSuccess { groupId ->
                    _effect.trySend(UiEffect.ShowToast(R.string.invitation_accepted_toast))
                    _navEffect.trySend(
                        NavigationEffect.Navigate(
                            Screen.GroupDetail(
                                groupId = groupId,
                                groupName = item?.groupName.orEmpty(),
                            ),
                        ),
                    )
                }
                .onFailure { _effect.trySend(UiEffect.ShowToast(R.string.invitation_action_failed)) }
            markProcessing(id, false)
        }
    }

    private fun decline(id: Long) {
        markProcessing(id, true)
        viewModelScope.launch {
            repository.decline(id)
                .onSuccess { _effect.trySend(UiEffect.ShowToast(R.string.invitation_declined_toast)) }
                .onFailure { _effect.trySend(UiEffect.ShowToast(R.string.invitation_action_failed)) }
            markProcessing(id, false)
        }
    }

    private fun markProcessing(id: Long, processing: Boolean) {
        _uiState.update { current ->
            if (current !is UiState.Success) return@update current
            val next = if (processing) current.processingIds + id else current.processingIds - id
            current.copy(processingIds = next)
        }
    }
}
