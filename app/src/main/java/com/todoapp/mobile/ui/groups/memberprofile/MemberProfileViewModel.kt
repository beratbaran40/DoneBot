package com.todoapp.mobile.ui.groups.memberprofile

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.data.model.network.request.ReportTargetType
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.repository.BlockedUsersPreferences
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.MemberUiItem
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.UiAction
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.UiEffect
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.UiState
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MemberProfileViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val blockedUsersPreferences: BlockedUsersPreferences,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Screen.MemberProfile>()
    private val groupId = route.groupId
    private val userId = route.userId
    private val isCurrentUserAdmin = route.isCurrentUserAdmin

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var pendingRemoveJob: Job? = null
    private var pendingUserId: Long? = null

    init {
        loadMember()
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnRemoveTap -> updateSuccessState { it.copy(showConfirmDialog = true) }
            UiAction.OnDismissDialog -> updateSuccessState { it.copy(showConfirmDialog = false) }
            UiAction.OnConfirmRemove -> {
                updateSuccessState { it.copy(showConfirmDialog = false) }
                scheduleMemberRemoval()
            }
            UiAction.OnUndoRemove -> {
                pendingRemoveJob?.cancel()
                pendingRemoveJob = null
                pendingUserId = null
                updateSuccessState { it.copy(pendingRemoval = false) }
            }
            UiAction.OnReportTap -> updateSuccessState { it.copy(showReportDialog = true) }
            UiAction.OnReportDismiss -> updateSuccessState { it.copy(showReportDialog = false) }
            UiAction.OnReportConfirm -> {
                updateSuccessState { it.copy(showReportDialog = false) }
                reportMember()
            }
            UiAction.OnBlockToggle -> toggleBlock()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (pendingUserId != null && pendingRemoveJob?.isActive == true) {
            pendingRemoveJob?.cancel()
            CoroutineScope(SupervisorJob()).launch {
                groupRepository
                    .removeMember(groupId, userId)
                    .onSuccess { unassignMemberTasks() }
            }
        }
    }

    private fun loadMember() {
        viewModelScope.launch {
            groupRepository
                .getGroupMembers(groupId)
                .onSuccess { members ->
                    val member = members.find { it.userId == userId }
                    if (member != null) {
                        _uiState.value = UiState.Success(
                            member = member.toUiItem(),
                            isBlocked = blockedUsersPreferences.isBlocked(userId),
                            isCurrentUserAdmin = isCurrentUserAdmin,
                        )
                    } else {
                        _uiState.value = UiState.Error(context.getString(R.string.member_not_found))
                    }
                }.onFailure {
                    _uiState.value = UiState.Error(context.getString(R.string.failed_to_load_member))
                }
        }
    }

    private fun reportMember() {
        viewModelScope.launch {
            groupRepository
                .reportContent(groupId, ReportTargetType.MEMBER, targetUserId = userId)
                .onSuccess {
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.report_success_toast)))
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.report_failed_toast)))
                }
        }
    }

    private fun toggleBlock() {
        val current = _uiState.value as? UiState.Success ?: return
        viewModelScope.launch {
            if (current.isBlocked) {
                blockedUsersPreferences.unblock(userId)
                updateSuccessState { it.copy(isBlocked = false) }
                _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.user_unblocked_toast)))
            } else {
                blockedUsersPreferences.block(userId, current.member.displayName)
                updateSuccessState { it.copy(isBlocked = true) }
                _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.user_blocked_toast)))
            }
        }
    }

    private fun scheduleMemberRemoval() {
        pendingUserId = userId
        updateSuccessState { it.copy(pendingRemoval = true) }
        pendingRemoveJob?.cancel()
        pendingRemoveJob =
            viewModelScope.launch {
                delay(UNDO_DELAY_MS)
                executeRemoval()
            }
    }

    private suspend fun executeRemoval() {
        groupRepository
            .removeMember(groupId, userId)
            .onSuccess {
                unassignMemberTasks()
                pendingUserId = null
                _navEffect.trySend(NavigationEffect.Back)
            }.onFailure {
                pendingUserId = null
                updateSuccessState { it.copy(pendingRemoval = false) }
                _uiEffect.trySend(UiEffect.ShowToast(context.getString(R.string.failed_to_remove_member)))
            }
    }

    private suspend fun unassignMemberTasks() {
        groupRepository
            .getGroupTasks(groupId)
            .onSuccess { tasks ->
                tasks
                    .filter { it.assignee?.userId == userId }
                    .forEach { groupRepository.unassignGroupTask(groupId, it.id) }
            }
    }

    private fun updateSuccessState(transform: (UiState.Success) -> UiState.Success) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = transform(current)
    }

    private fun GroupMember.toUiItem(): MemberUiItem {
        val parts = displayName.trim().split(" ")
        val firstName = parts.firstOrNull() ?: displayName
        val lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
        val initials =
            parts
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedJoinedAt = dateFormat.format(Date(joinedAt))
        return MemberUiItem(
            userId = userId,
            displayName = displayName,
            firstName = firstName,
            lastName = lastName,
            email = email,
            avatarUrl = avatarUrl,
            initials = initials,
            role = role,
            joinedAt = formattedJoinedAt,
        )
    }

    private companion object {
        const val UNDO_DELAY_MS = 5000L
    }
}
