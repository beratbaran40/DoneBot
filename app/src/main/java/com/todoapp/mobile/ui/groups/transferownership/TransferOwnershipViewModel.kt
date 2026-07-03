package com.todoapp.mobile.ui.groups.transferownership

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipContract.TransferMemberUiItem
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipContract.UiAction
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipContract.UiEffect
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipContract.UiState
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
class TransferOwnershipViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<Screen.TransferOwnership>().groupId

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
            is UiAction.OnSearchChange -> filterMembers(action.query)
            is UiAction.OnMemberSelected -> selectMember(action.userId)
            UiAction.OnTransferConfirm -> transferOwnership()
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            val currentUserId = userRepository.getUserInfo().getOrNull()?.id ?: -1L
            groupRepository
                .getGroupMembers(groupId)
                .onSuccess { members ->
                    val filtered = members.filter { it.userId != currentUserId }
                    val uiItems = filtered.map { it.toUiItem() }
                    _uiState.value =
                        UiState.Success(
                            members = uiItems,
                            filteredMembers = uiItems,
                            searchQuery = "",
                            selectedUserId = null,
                        )
                }.onFailure {
                    _uiState.value = UiState.Error(it.toUserMessage(context))
                }
        }
    }

    private fun filterMembers(query: String) {
        val current = _uiState.value as? UiState.Success ?: return
        val filtered =
            if (query.isBlank()) {
                current.members
            } else {
                current.members.filter {
                    it.displayName.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                }
            }
        _uiState.update { _ -> current.copy(searchQuery = query, filteredMembers = filtered) }
    }

    private fun selectMember(userId: Long) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.update { _ -> current.copy(selectedUserId = userId) }
    }

    private fun transferOwnership() {
        val current = _uiState.value as? UiState.Success ?: return
        val selectedId = current.selectedUserId ?: return
        viewModelScope.launch {
            groupRepository
                .transferOwnership(groupId, selectedId)
                .onSuccess {
                    _uiEffect.trySend(UiEffect.ShowToast("Ownership transferred"))
                    _navEffect.trySend(
                        NavigationEffect.Navigate(Screen.Groups(), popUpTo = Screen.Groups(), isInclusive = false),
                    )
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast(it.toUserMessage(context)))
                }
        }
    }

    private fun GroupMember.toUiItem(): TransferMemberUiItem {
        val initials =
            displayName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
        return TransferMemberUiItem(
            userId = userId,
            displayName = displayName,
            subtitle = role.replaceFirstChar { it.uppercaseChar() },
            avatarUrl = avatarUrl,
            initials = initials,
        )
    }
}
