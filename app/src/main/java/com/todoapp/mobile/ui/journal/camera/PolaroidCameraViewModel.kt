package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.data.storage.JournalPhotoStorage
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraContract.UiAction
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraContract.UiEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns only persistence + navigation for the Polaroid camera. The ephemeral capture and animation
 * state (PhotoState, the captured bitmap, Animatables, lens facing) lives in the composable — it is
 * genuine view/animation state, not app state, so it intentionally stays out of the ViewModel.
 */
@HiltViewModel
class PolaroidCameraViewModel @Inject constructor(
    private val photoStorage: JournalPhotoStorage,
) : ViewModel() {
    private val _uiEffect = Channel<UiEffect>()
    val uiEffect: Flow<UiEffect> = _uiEffect.receiveAsFlow()

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnSavePhoto -> savePhoto(action.bitmap)
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val path = photoStorage.savePhotoFromBitmap(bitmap)
            if (path == null) {
                _uiEffect.send(UiEffect.ShowError(R.string.polaroid_save_error))
            } else {
                _uiEffect.send(UiEffect.NavigateBackWithPhoto(path))
            }
        }
    }
}
