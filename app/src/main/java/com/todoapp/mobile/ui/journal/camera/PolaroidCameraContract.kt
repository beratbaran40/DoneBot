package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.annotation.StringRes

/** Capture lifecycle of the ejected Polaroid print. */
enum class PhotoState { Idle, Capturing, Ejecting, Developing, Done }

/** SavedStateHandle key the camera uses to hand the captured photo path back to the Journal entry. */
const val POLAROID_PHOTO_RESULT_KEY = "polaroid_photo_path"

object PolaroidCameraContract {
    sealed interface UiAction {
        data class OnSavePhoto(val bitmap: Bitmap) : UiAction
    }

    sealed interface UiEffect {
        data class NavigateBackWithPhoto(val path: String) : UiEffect
        data class ShowError(@StringRes val messageRes: Int) : UiEffect
    }
}
