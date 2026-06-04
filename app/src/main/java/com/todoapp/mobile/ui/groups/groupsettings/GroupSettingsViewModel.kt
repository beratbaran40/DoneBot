package com.todoapp.mobile.ui.groups.groupsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.data.storage.AvatarPhotoStorage
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsContract.UiAction
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsContract.UiEffect
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupSettingsViewModel
@Inject
constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val avatarPhotoStorage: AvatarPhotoStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.toRoute<Screen.GroupSettings>().groupId

    private val _uiState = MutableStateFlow(UiState(groupId = groupId))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    init {
        loadGroupDetail()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnNameChange -> _uiState.update { it.copy(name = action.name) }
            is UiAction.OnDescriptionChange -> _uiState.update { it.copy(description = action.description) }
            UiAction.OnSaveTap -> saveChanges()
            UiAction.OnManageMembersTap ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(Screen.ManageMembers(groupId)),
                )
            UiAction.OnTransferOwnershipTap ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(Screen.TransferOwnership(groupId)),
                )
            is UiAction.OnAvatarPicked ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.AvatarCrop(action.uri)))
            is UiAction.OnAvatarCropped -> uploadCroppedAvatar(action.path)
        }
    }

    private fun uploadCroppedAvatar(path: String) {
        viewModelScope.launch {
            val bytes = avatarPhotoStorage.readPhotoBytes(path)
            if (bytes == null) {
                _uiEffect.trySend(UiEffect.ShowToast("Failed to upload avatar"))
                return@launch
            }
            groupRepository
                .uploadGroupAvatar(groupId, bytes, "image/jpeg")
                .onSuccess {
                    _uiState.update { it.copy(avatarVersion = System.currentTimeMillis()) }
                    loadGroupDetail()
                }.onFailure {
                    _uiEffect.trySend(UiEffect.ShowToast(it.message ?: "Failed to upload avatar"))
                }
            // One-shot temp file — clean up regardless of upload outcome.
            avatarPhotoStorage.deletePhoto(path)
        }
    }

    private fun loadGroupDetail() {
        viewModelScope.launch {
            val userResult = userRepository.getUserInfo()
            val currentUserId = userResult.getOrNull()?.id ?: -1L

            groupRepository
                .getGroupDetail(groupId)
                .onSuccess { detail ->
                    val role = detail.members.find { it.userId == currentUserId }?.role.orEmpty()
                    _uiState.update { state ->
                        state.copy(
                            name = detail.name,
                            description = detail.description,
                            avatarUrl = detail.avatarUrl,
                            avatarVersion = System.currentTimeMillis(),
                            currentUserRole = role,
                            isLoading = false,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load group") }
                }
        }
    }

    private fun saveChanges() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiEffect.trySend(UiEffect.ShowToast("Group name cannot be empty"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            groupRepository
                .updateGroup(groupId, state.name, state.description)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _navEffect.trySend(NavigationEffect.Back)
                }.onFailure {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.trySend(UiEffect.ShowToast("Failed to save changes"))
                }
        }
    }
}
