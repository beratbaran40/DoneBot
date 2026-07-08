package com.todoapp.mobile.ui.groups.invitemember

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberContract.UiAction
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberContract.UiEffect
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberContract.UiState
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
class InviteMemberViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<Screen.InviteMember>().groupId

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnEmailChange -> _uiState.update { it.copy(email = action.email, emailError = null) }
            UiAction.OnSendInviteTap -> sendInvite()
            UiAction.OnShareLinkTap -> shareLink()
        }
    }

    private fun sendInvite() {
        val email = _uiState.value.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = context.getString(R.string.email_error)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null) }
            groupRepository
                .inviteMember(groupId, email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSent = true, email = "") }
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.invite_sent)))
                    _navEffect.trySend(NavigationEffect.Back)
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.trySend(UiEffect.ShowToast(it.toUserMessage(context)))
                }
        }
    }

    private fun shareLink() {
        _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.share_link_coming_soon)))
    }
}
