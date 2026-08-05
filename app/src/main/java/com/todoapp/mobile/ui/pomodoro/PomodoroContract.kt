package com.todoapp.mobile.ui.pomodoro

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.repository.AmbiencePreferences

object PomodoroContract {
    @Immutable
    data class UiState(
        val min: Int = 25,
        val second: Int = 0,
        val mode: PomodoroModeUi = PomodoroModeUiPreset.Focus.value,
        val isRunning: Boolean = false,
        val isOvertime: Boolean = false,
        val infoMessage: String? = null,
        val totalSessionSeconds: Long = 25L * 60L,
        val currentSessionIndex: Int = 0,
        val totalSessions: Int = 0,
        val showFinishEarlyDialog: Boolean = false,
        val ambience: PomodoroAmbience = PomodoroAmbience.None,
        val ambienceVolume: Float = AmbiencePreferences.DEFAULT_VOLUME,
        val ambienceBackgroundEnabled: Boolean = false,
        val showAmbienceSheet: Boolean = false,
    )

    sealed interface UiAction {
        data object SkipSession : UiAction

        data object StartCountDown : UiAction

        data object StopCountDown : UiAction

        data class ToggleBannerVisibility(
            val isVisible: Boolean,
        ) : UiAction

        data object OnEndSessionTap : UiAction

        data object ConfirmEndSession : UiAction

        data object DismissEndSessionDialog : UiAction

        data object OnAmbienceButtonTap : UiAction

        data object DismissAmbienceSheet : UiAction

        data class OnAmbienceSelected(
            val ambience: PomodoroAmbience,
        ) : UiAction

        data class OnAmbienceVolumeChange(
            val volume: Float,
        ) : UiAction

        data class OnAmbienceBackgroundToggle(
            val enabled: Boolean,
        ) : UiAction
    }

    sealed interface UiEffect {
        data object SessionFinished : UiEffect
    }
}

data class PomodoroModeUi(
    @StringRes val titleRes: Int,
    val iconRes: Int,
    val colorKey: ModeColorKey,
)

sealed interface PomodoroModeUiPreset {
    val value: PomodoroModeUi

    data object Focus : PomodoroModeUiPreset {
        override val value =
            PomodoroModeUi(
                titleRes = R.string.pomodoro_mode_focus,
                iconRes = R.drawable.ic_focus,
                colorKey = ModeColorKey.Focus,
            )
    }

    data object ShortBreak : PomodoroModeUiPreset {
        override val value =
            PomodoroModeUi(
                titleRes = R.string.pomodoro_mode_short_break,
                iconRes = R.drawable.ic_short_break,
                colorKey = ModeColorKey.ShortBreak,
            )
    }

    data object LongBreak : PomodoroModeUiPreset {
        override val value =
            PomodoroModeUi(
                titleRes = R.string.pomodoro_mode_long_break,
                iconRes = R.drawable.ic_long_break,
                colorKey = ModeColorKey.LongBreak,
            )
    }

    data object OverTime : PomodoroModeUiPreset {
        override val value =
            PomodoroModeUi(
                titleRes = R.string.pomodoro_mode_overtime,
                iconRes = R.drawable.ic_overtime,
                colorKey = ModeColorKey.OverTime,
            )
    }
}

enum class ModeColorKey {
    Focus,
    ShortBreak,
    LongBreak,
    OverTime,
}
