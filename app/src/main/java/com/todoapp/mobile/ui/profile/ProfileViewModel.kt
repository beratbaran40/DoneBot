package com.todoapp.mobile.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.data.storage.AvatarPhotoStorage
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.domain.usecase.ComputeHealthPointsUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.profile.ProfileContract.UiAction
import com.todoapp.mobile.ui.profile.ProfileContract.UiEffect
import com.todoapp.mobile.ui.profile.ProfileContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val userRepository: UserRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val avatarPhotoStorage: AvatarPhotoStorage,
    private val computeHealthPoints: ComputeHealthPointsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // Same source as the Activity hearts, so the ring and the bar can never disagree.
        viewModelScope.launch {
            computeHealthPoints().collect { points ->
                _uiState.update { it.copy(healthHalfHearts = points.halfHearts) }
            }
        }
        load()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnDisplayNameChange ->
                _uiState.update {
                    it.copy(editedDisplayName = action.value)
                }
            UiAction.OnSaveName -> saveName()
            is UiAction.OnAvatarPicked ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.AvatarCrop(action.uri)))
            is UiAction.OnAvatarCropped -> uploadCroppedAvatar(action.path)
            UiAction.OnBack -> _navEffect.trySend(NavigationEffect.Back)
            UiAction.OnChangePasswordTap ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.ChangePassword))
        }
    }

    private fun load() {
        viewModelScope.launch {
            val avatarVersion = dataStoreHelper.observeAvatarVersion().first()
            // `copy`, not a fresh UiState: the health flow above races with this and a whole-state
            // replacement would reset healthHalfHearts to its full-health default.
            dataStoreHelper.observeUser().first()?.let { cached ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userId = cached.id,
                        email = cached.email,
                        displayName = cached.displayName,
                        editedDisplayName = cached.displayName,
                        avatarUrl = cached.avatarUrl,
                        avatarVersion = avatarVersion,
                    )
                }
            }
            userRepository
                .getUserInfo()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userId = user.id,
                            email = user.email,
                            displayName = user.displayName,
                            editedDisplayName = user.displayName,
                            avatarUrl = user.avatarUrl,
                            avatarVersion = avatarVersion,
                        )
                    }
                }.onFailure { t ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = it.errorMessage ?: t.toUserMessage(context))
                    }
                }
        }
    }

    private fun saveName() {
        val state = _uiState.value
        val trimmed = state.editedDisplayName.trim()
        if (trimmed.isBlank() || trimmed == state.displayName) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            userRepository
                .updateDisplayName(trimmed)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            displayName = user.displayName,
                            editedDisplayName = user.displayName,
                        )
                    }
                    _uiEffect.trySend(UiEffect.ShowToast("Profile updated"))
                }.onFailure { t ->
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.trySend(UiEffect.ShowToast(t.toUserMessage(context)))
                }
        }
    }

    private fun uploadCroppedAvatar(path: String) {
        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            val bytes = avatarPhotoStorage.readPhotoBytes(path)
            if (bytes == null) {
                _uiState.update { it.copy(isUploading = false) }
                _uiEffect.trySend(UiEffect.ShowToast("Failed to upload photo"))
                return@launch
            }
            userRepository
                .uploadAvatar(bytes, "image/jpeg")
                .onSuccess { user ->
                    // The repository bumped the persisted token; read it so Profile and the top bar
                    // render the identical `?v=` and share one Coil cache entry.
                    val avatarVersion = dataStoreHelper.observeAvatarVersion().first()
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            avatarUrl = user.avatarUrl,
                            avatarVersion = avatarVersion,
                        )
                    }
                    _uiEffect.trySend(UiEffect.ShowToast("Photo updated"))
                }.onFailure { t ->
                    _uiState.update { it.copy(isUploading = false) }
                    _uiEffect.trySend(UiEffect.ShowToast(t.toUserMessage(context)))
                }
            // One-shot temp file — clean up regardless of upload outcome.
            avatarPhotoStorage.deletePhoto(path)
        }
    }
}
