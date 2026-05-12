package com.todoapp.mobile.ui.settings

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.constants.DailyPlanDefaults.DEFAULT_PLAN_TIME
import com.todoapp.mobile.domain.model.LanguagePreference
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.mobile.domain.security.SecretModeReopenOption
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import java.time.LocalTime

enum class PermissionType { OVERLAY, NOTIFICATION }

object SettingsContract {
    @Immutable
    data class UiState(
        val currentTheme: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
        val currentLanguage: LanguagePreference = LanguagePreference.ENGLISH,
        val selectedSecretMode: SecretModeReopenOption = SecretModeReopenOptions.Immediate,
        val remainedSecretModeTime: String = "",
        val isSecretModeActive: Boolean = false,
        val dailyPlanTime: LocalTime = DEFAULT_PLAN_TIME,
        val visiblePermissions: List<PermissionType> = emptyList(),
        val showLogoutDialog: Boolean = false,
        val isUserAuthenticated: Boolean = false,
        val pushNotificationsEnabled: Boolean = true,
        val isPushTogglePending: Boolean = false,
        val reduceMotionEnabled: Boolean = false,
        val journalBiometricProtected: Boolean = false,
        val showDeleteAccountDialog: Boolean = false,
        val isDeletingAccount: Boolean = false,
        val displayName: String = "",
        val email: String = "",
        val avatarUrl: String? = null,
    )

    sealed interface UiAction {
        data class OnSelectedSecretModeChange(
            val label: SecretModeReopenOption,
        ) : UiAction

        data class OnThemeChange(
            val theme: ThemePreference,
        ) : UiAction

        data class OnLanguageChange(
            val language: LanguagePreference,
        ) : UiAction

        data object OnSettingsSave : UiAction

        data object OnNavigateToPlanYourDay : UiAction

        data object OnNavigateToPomodoroSettings : UiAction

        data object OnDisableSecretModeTap : UiAction

        data class OnDailyPlanTimeChange(
            val time: LocalTime,
        ) : UiAction

        data object OnNavigateToSecretModeSettings : UiAction

        data object OnNavigateToProfile : UiAction

        data object OnLogoutClick : UiAction

        data object OnLogoutConfirm : UiAction

        data object OnLogoutDismiss : UiAction

        data object OnLoginOrRegisterClick : UiAction

        data object OnNavigateToAlarmSounds : UiAction

        data class OnPushNotificationsToggle(val enabled: Boolean) : UiAction

        data class OnReduceMotionToggle(val enabled: Boolean) : UiAction

        data class OnJournalBiometricProtectionToggle(val enabled: Boolean) : UiAction

        data object OnDeleteAccountClick : UiAction

        data object OnDeleteAccountDismiss : UiAction

        data object OnDeleteAccountConfirm : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect

        data object RecreateActivity : UiEffect

        data class ApplyLocale(
            val tag: String,
        ) : UiEffect
    }
}
