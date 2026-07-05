package com.todoapp.mobile.ui.planyourday

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import java.time.LocalTime

object PlanYourDayContract {
    @Immutable
    data class UiState(
        val selectedTime: LocalTime = LocalTime.of(9, 0),
        val savedTime: LocalTime? = null,
    ) {
        val hasChanges: Boolean
            get() = selectedTime != (savedTime ?: LocalTime.of(9, 0))
    }

    sealed interface UiAction {
        data class OnHourChange(
            val hour: Int,
        ) : UiAction

        data class OnMinuteChange(
            val minute: Int,
        ) : UiAction

        data object OnSave : UiAction

        data object OnCancel : UiAction
    }

    sealed interface UiEffect {
        data object NavigateBack : UiEffect

        data class ShowToast(
            @StringRes val message: Int,
        ) : UiEffect
    }
}
