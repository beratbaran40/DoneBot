package com.todoapp.mobile.ui.profile.avatarcrop

import android.graphics.Bitmap
import androidx.annotation.StringRes

/** SavedStateHandle key the crop screen uses to hand the cropped photo path back to the caller. */
const val AVATAR_CROP_RESULT_KEY = "avatar_crop_path"

object AvatarCropContract {
    sealed interface UiAction {
        data class OnCropConfirmed(val bitmap: Bitmap) : UiAction
    }

    sealed interface UiEffect {
        data class NavigateBackWithCroppedPath(val path: String) : UiEffect
        data class ShowError(@StringRes val messageRes: Int) : UiEffect
    }
}
