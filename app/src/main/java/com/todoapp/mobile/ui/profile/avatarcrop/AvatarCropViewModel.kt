package com.todoapp.mobile.ui.profile.avatarcrop

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.data.storage.AvatarPhotoStorage
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropContract.UiAction
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropContract.UiEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns only persistence + navigation for the avatar crop screen. The source bitmap and the
 * pan/zoom gesture state live in the composable — genuine view state, not app state.
 */
@HiltViewModel
class AvatarCropViewModel @Inject constructor(
    private val photoStorage: AvatarPhotoStorage,
) : ViewModel() {
    private val _uiEffect = Channel<UiEffect>()
    val uiEffect: Flow<UiEffect> = _uiEffect.receiveAsFlow()

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnCropConfirmed -> saveCroppedPhoto(action.bitmap)
        }
    }

    private fun saveCroppedPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val path = photoStorage.savePhotoFromBitmap(bitmap)
            // Nothing else references the cropped bitmap (the screen only holds the source), so we
            // own its lifecycle and recycle it once it's persisted.
            bitmap.recycle()
            if (path == null) {
                _uiEffect.send(UiEffect.ShowError(R.string.avatar_crop_save_error))
            } else {
                _uiEffect.send(UiEffect.NavigateBackWithCroppedPath(path))
            }
        }
    }
}
