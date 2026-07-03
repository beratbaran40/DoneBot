package com.todoapp.mobile.ui.groups.managemembers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.ManageMemberUiItem
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.UiAction
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.UiEffect
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageMembersViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<Screen.ManageMembers>().groupId

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    init {
        loadMembers()
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnAddMemberTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.InviteMember(groupId)))
            UiAction.OnScreenResumed -> loadMembers()
            is UiAction.OnMemberTap ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(Screen.MemberProfile(groupId, action.userId)),
                )
            is UiAction.OnRemoveMemberTap -> removeMember(action.userId)
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            groupRepository
                .getGroupMembers(groupId)
                .onSuccess { members ->
                    _uiState.value = UiState.Success(members.map { it.toUiItem() })
                }.onFailure {
                    _uiState.value = UiState.Error(it.toUserMessage(context))
                }
        }
    }

    private fun removeMember(userId: Long) {
        viewModelScope.launch {
            groupRepository
                .removeMember(groupId, userId)
                .onSuccess {
                    val current = _uiState.value as? UiState.Success ?: return@onSuccess
                    _uiState.value = current.copy(members = current.members.filter { it.userId != userId })
                    _uiEffect.trySend(UiEffect.ShowToast("Member removed"))
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast("Failed to remove member"))
                }
        }
    }

    private fun GroupMember.toUiItem(): ManageMemberUiItem {
        val initials =
            displayName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
        return ManageMemberUiItem(
            userId = userId,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            initials = initials,
            role = role,
        )
    }
}
