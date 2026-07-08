package com.todoapp.mobile.ui.groups.createnewgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupContract.UiAction
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupContract.UiState
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateNewGroupViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val analyticsHelper: com.todoapp.mobile.domain.analytics.AnalyticsHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(isUserAuthenticated = false))
    val uiState = _uiState.asStateFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    private var isErrorFlagActive = false

    init {
        checkAuthState()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnCreateTap -> createGroup()
            is UiAction.OnGroupDescriptionChange ->
                _uiState.update {
                    it.copy(
                        groupDescription = action.groupDescription,
                    )
                }

            is UiAction.OnGroupNameChange -> updateGroupName(action.groupName)
        }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = userRepository.getUserInfo().isSuccess
            _uiState.update { it.copy(isUserAuthenticated = isAuthenticated) }
        }
    }

    private fun updateGroupName(updatedGroupName: String) {
        if (isErrorFlagActive) {
            validateGroupName(updatedGroupName)
        }
        _uiState.update { it.copy(groupName = updatedGroupName) }
    }

    private fun createGroup() {
        val uiStateSnapshot = uiState.value

        if (!validateGroupName(uiStateSnapshot.groupName)) return

        viewModelScope.launch {
            val isUserAuthenticated = userRepository.getUserInfo().isSuccess
            updateAuthenticationState(isUserAuthenticated)

            if (!isUserAuthenticated) {
                _navEffect.send(
                    NavigationEffect.Navigate(
                        Screen.Login(redirectAfterLogin = "CreateNewGroup"),
                    ),
                )
                return@launch
            }

            groupRepository
                .createGroup(
                    CreateGroupRequest(
                        uiState.value.groupName,
                        uiState.value.groupDescription.orEmpty(),
                    ),
                ).onSuccess { created ->
                    analyticsHelper.logGroupCreated()
                    // Land on the fresh group's Members tab with the one-shot first-invite dialog
                    // open (tester feedback: creation should flow straight into adding a member).
                    // popUpTo drops this screen from the stack, so back = the Groups list.
                    _navEffect.send(
                        NavigationEffect.Navigate(
                            Screen.GroupDetail(
                                groupId = created.id,
                                groupName = created.name,
                                initialTab = GroupDetailContract.TAB_MEMBERS,
                                showFirstInvite = true,
                            ),
                            popUpTo = Screen.CreateNewGroup,
                            isInclusive = true,
                        ),
                    )
                }.onFailure {
                    _uiState.update { it.copy(error = "Something went wrong. Try again later.") }
                }
        }
    }

    private fun updateAuthenticationState(isAuthenticated: Boolean) {
        _uiState.update { it.copy(isUserAuthenticated = isAuthenticated) }
    }

    private fun validateGroupName(groupName: String): Boolean {
        if (groupName.isEmpty()) {
            _uiState.update { it.copy(error = "Group name is required.") }
            isErrorFlagActive = true
            return false
        }
        _uiState.update { it.copy(error = null) }
        return true
    }
}
