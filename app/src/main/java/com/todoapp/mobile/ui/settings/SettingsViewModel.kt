package com.todoapp.mobile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.needsCameraPermission
import com.todoapp.mobile.common.needsOverlayPermission
import com.todoapp.mobile.common.needsPostNotificationsPermission
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.data.security.SecretModeEndCondition
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.alarm.buildDailyPlanAlarmItem
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.CrashAnalyticsPreferences
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.JournalBiometricPreferences
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.domain.repository.LanguageRepository
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TelemetryPreferences
import com.todoapp.mobile.domain.repository.ThemeRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.domain.security.SecretModeConditionFactory
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.mobile.ui.settings.SettingsContract.UiEffect
import com.todoapp.mobile.ui.settings.SettingsContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val secretModePreferences: SecretPreferences,
    private val dailyPlanPreferences: DailyPlanPreferences,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
    private val authRepository: AuthRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val userRepository: UserRepository,
    private val journalBiometricPreferences: JournalBiometricPreferences,
    private val journalRepository: JournalRepository,
    private val telemetryPreferences: TelemetryPreferences,
    private val crashAnalyticsPreferences: CrashAnalyticsPreferences,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var didScheduleDailyPlanOnce: Boolean = false

    private val dailyPlanTime: StateFlow<LocalTime> =
        dailyPlanPreferences
            .observePlanTime()
            .map { it ?: DailyPlanDefaults.DEFAULT_PLAN_TIME }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DailyPlanDefaults.DEFAULT_PLAN_TIME,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val secretModeMessageFlow: StateFlow<String> =
        secretModePreferences
            .observeCondition()
            .flatMapLatest { condition -> observeSecretModeMessage(condition) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = "Secret mode is closed.",
            )

    init {
        observeTheme()
        observeLanguage()
        observeAuthState()
        loadPushPreferences()
        viewModelScope.launch {
            val lastSelectedOptionId = secretModePreferences.getLastSelectedOptionId()
            _uiState.update {
                it.copy(selectedSecretMode = SecretModeReopenOptions.byId(lastSelectedOptionId))
            }
        }

        viewModelScope.launch {
            secretModePreferences.observeCondition().collect { condition ->
                _uiState.update {
                    it.copy(isSecretModeActive = condition.isActive(System.currentTimeMillis()))
                }
            }
        }

        viewModelScope.launch {
            secretModeMessageFlow.collect { value ->
                _uiState.update {
                    it.copy(remainedSecretModeTime = value)
                }
            }
        }
        viewModelScope.launch {
            dailyPlanTime.collect { time ->
                _uiState.update { it.copy(dailyPlanTime = time) }

                if (!didScheduleDailyPlanOnce) {
                    didScheduleDailyPlanOnce = true
                    rescheduleDailyPlanAlarm(time)
                }
            }
        }
        viewModelScope.launch {
            dataStoreHelper.observeReduceMotion().collect { enabled ->
                _uiState.update { it.copy(reduceMotionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            journalBiometricPreferences.observe().collect { enabled ->
                _uiState.update { it.copy(journalBiometricProtected = enabled) }
            }
        }
        viewModelScope.launch {
            telemetryPreferences.observe().collect { enabled ->
                _uiState.update { it.copy(sharePerformanceDiagnostics = enabled) }
            }
        }
        viewModelScope.launch {
            crashAnalyticsPreferences.observe().collect { enabled ->
                _uiState.update { it.copy(crashAnalyticsEnabled = enabled) }
            }
        }
    }

    fun checkPermission(context: Context) {
        val list =
            buildList {
                if (context.needsOverlayPermission()) add(PermissionType.OVERLAY)
                if (context.needsPostNotificationsPermission()) add(PermissionType.NOTIFICATION)
            }
        _uiState.update { it.copy(visiblePermissions = list, cameraGranted = !context.needsCameraPermission()) }
    }

    fun dismissPermission(type: PermissionType) {
        _uiState.update { current ->
            current.copy(visiblePermissions = current.visiblePermissions - type)
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            combine(
                dataStoreHelper.observeUser(),
                dataStoreHelper.observeAvatarVersion(),
            ) { user, avatarVersion -> user to avatarVersion }
                .collect { (user, avatarVersion) ->
                    _uiState.update {
                        it.copy(
                            isUserAuthenticated = user != null,
                            displayName = user?.displayName.orEmpty(),
                            email = user?.email.orEmpty(),
                            avatarUrl = user?.avatarUrl,
                            avatarVersion = avatarVersion,
                        )
                    }
                }
        }
    }

    private fun observeTheme() {
        themeRepository.themeFlow
            .onEach { theme ->
                _uiState.update { it.copy(currentTheme = theme) }
            }.launchIn(viewModelScope)
    }

    private fun observeLanguage() {
        languageRepository.languageFlow
            .onEach { language ->
                _uiState.update { it.copy(currentLanguage = language) }
            }.launchIn(viewModelScope)
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnThemeChange -> viewModelScope.launch { themeRepository.saveTheme(action.theme) }
            is UiAction.OnLanguageChange ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentLanguage = action.language) }
                    languageRepository.saveLanguage(action.language)
                    val tag = action.language.toLocale().toLanguageTag()
                    _uiEffect.send(UiEffect.ApplyLocale(tag))
                }
            is UiAction.OnSelectedSecretModeChange -> updateSelectedSecretMode(action)
            is UiAction.OnSettingsSave -> updateOption()
            is UiAction.OnDisableSecretModeTap -> disableSecretMode()
            is UiAction.OnDailyPlanTimeChange -> updateDailyPlanTime(action.time)
            is UiAction.OnNavigateToSecretModeSettings -> navigateToSecretModeSettings()
            UiAction.OnNavigateToProfile -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Profile))
            UiAction.OnNavigateToPlanYourDay -> navigateToDailyPlanSettings()
            UiAction.OnNavigateToPomodoroSettings -> navigateToPomodoroSettings()
            UiAction.OnLogoutClick -> _uiState.update { it.copy(showLogoutDialog = true) }
            UiAction.OnLogoutDismiss -> _uiState.update { it.copy(showLogoutDialog = false) }
            UiAction.OnLogoutConfirm -> viewModelScope.launch { authRepository.logout() }
            UiAction.OnLoginOrRegisterClick -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Login()))
            UiAction.OnNavigateToAlarmSounds ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.AlarmSounds))
            UiAction.OnNavigateToLicenses ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.Licenses))
            is UiAction.OnPushNotificationsToggle -> togglePushNotifications(action.enabled)
            is UiAction.OnReduceMotionToggle -> toggleReduceMotion(action.enabled)
            is UiAction.OnJournalBiometricProtectionToggle ->
                viewModelScope.launch { journalBiometricPreferences.set(action.enabled) }
            is UiAction.OnSharePerformanceDiagnosticsToggle ->
                viewModelScope.launch { telemetryPreferences.set(action.enabled) }
            is UiAction.OnCrashAnalyticsToggle ->
                viewModelScope.launch { crashAnalyticsPreferences.set(action.enabled) }
            UiAction.OnDeleteAccountClick ->
                _uiState.update { it.copy(showDeleteAccountDialog = true) }
            UiAction.OnDeleteAccountDismiss ->
                _uiState.update { it.copy(showDeleteAccountDialog = false) }
            UiAction.OnDeleteAccountConfirm -> deleteAccount()
            UiAction.OnDownloadDataClick -> downloadMyData()
        }
    }

    private fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update { it.copy(isDeletingAccount = true) }
        viewModelScope.launch {
            userRepository
                .deleteAccount()
                .onSuccess {
                    // Purge this user's local-only journal (+photos) BEFORE logout: clearLocalSession nulls
                    // the cached user, so the owner must be resolved while still signed in.
                    runCatching { journalRepository.deleteAllForCurrentUser() }
                        .onFailure { Timber.tag("AccountDelete").w(it, "journal purge failed") }
                    _uiState.update {
                        it.copy(showDeleteAccountDialog = false, isDeletingAccount = false)
                    }
                    authRepository.logout()
                }.onFailure { error ->
                    Timber.tag("AccountDelete").w(error, "deleteAccount failed")
                    _uiState.update { it.copy(isDeletingAccount = false) }
                    val msg = when (error) {
                        is com.todoapp.mobile.common.DomainException.Unauthorized ->
                            R.string.delete_account_error_unauthorized
                        else -> R.string.delete_account_error_generic
                    }
                    _uiEffect.send(UiEffect.ShowToast(context.getString(msg)))
                }
        }
    }

    private fun downloadMyData() {
        viewModelScope.launch {
            userRepository.exportData()
                .onSuccess { json -> _uiEffect.send(UiEffect.SaveDataExport(json)) }
                .onFailure { error ->
                    Timber.tag("DataExport").w(error, "exportData failed")
                    _uiEffect.send(UiEffect.ShowToast(context.getString(R.string.settings_download_data_error)))
                }
        }
    }

    private fun toggleReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreHelper.setReduceMotion(enabled)
        }
    }

    private fun loadPushPreferences() {
        viewModelScope.launch {
            userRepository.getPushEnabled().onSuccess { enabled ->
                _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
            }
        }
    }

    private fun togglePushNotifications(enabled: Boolean) {
        val previous = _uiState.value.pushNotificationsEnabled
        _uiState.update { it.copy(pushNotificationsEnabled = enabled, isPushTogglePending = true) }
        viewModelScope.launch {
            userRepository.setPushEnabled(enabled)
                .onSuccess { effective ->
                    _uiState.update {
                        it.copy(pushNotificationsEnabled = effective, isPushTogglePending = false)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(pushNotificationsEnabled = previous, isPushTogglePending = false)
                    }
                }
        }
    }

    private fun navigateToDailyPlanSettings() {
        _navEffect.trySend(NavigationEffect.Navigate(Screen.PlanYourDay))
    }

    private fun navigateToPomodoroSettings() {
        _navEffect.trySend(NavigationEffect.Navigate(Screen.AddPomodoroTimer))
    }

    private fun navigateToSecretModeSettings() {
        _navEffect.trySend(NavigationEffect.Navigate(Screen.SecretMode))
    }

    private fun updateSelectedSecretMode(uiAction: UiAction.OnSelectedSecretModeChange) {
        _uiState.update { it.copy(selectedSecretMode = uiAction.label) }
    }

    private fun disableSecretMode() {
        viewModelScope.launch {
            secretModePreferences.saveCondition(SecretModeEndCondition.Disabled)
        }
    }

    private fun updateOption() {
        val selectedOption = uiState.value.selectedSecretMode
        viewModelScope.launch {
            secretModePreferences.setLastSelectedOptionId(selectedOption.id)
            if (uiState.value.isSecretModeActive) {
                val newCondition = SecretModeConditionFactory(clock).create(selectedOption)
                secretModePreferences.saveCondition(newCondition)
            }
            _uiEffect.send(UiEffect.ShowToast(PREFERENCE_SAVED_MESSAGE))
        }
    }

    private fun updateDailyPlanTime(time: LocalTime) {
        viewModelScope.launch {
            dailyPlanPreferences.savePlanTime(time)
            rescheduleDailyPlanAlarm(time)
        }
    }

    private fun rescheduleDailyPlanAlarm(time: LocalTime) {
        alarmScheduler.cancelScheduledAlarm(AlarmType.DAILY_PLAN)

        val item =
            buildDailyPlanAlarmItem(
                selectedTime = time,
                now = LocalDateTime.now(),
                message = "",
            )

        alarmScheduler.schedule(item, AlarmType.DAILY_PLAN)
    }

    private fun observeSecretModeMessage(condition: SecretModeEndCondition): Flow<String> = flow {
        val now = System.currentTimeMillis()

        if (!condition.isActive(now)) {
            emit("Secret mode is closed.")
            return@flow
        }

        when (condition) {
            is SecretModeEndCondition.Disabled -> {
                emit("Secret mode is closed.")
            }

            is SecretModeEndCondition.UntilEvent -> {
                emit("Secret mode is open until the app closed.")
            }

            is SecretModeEndCondition.UntilTime -> {
                var remainingMillis = condition.epochMillis - now
                while (remainingMillis >= 0L) {
                    val formatted = formatMillisToMinuteSecond(remainingMillis)
                    emit("Secret mode will be open for $formatted.")
                    delay(INTERVAL)
                    remainingMillis -= INTERVAL
                }

                emit("Secret mode is closed.")
            }
        }
    }

    private fun formatMillisToMinuteSecond(millis: Long): String {
        if (millis <= ZERO_MILLIS) return DEFAULT_TIME_FORMAT

        val totalSeconds = millis / MILLIS_IN_SECOND
        val minutes = totalSeconds / SECONDS_IN_MINUTE
        val seconds = totalSeconds % SECONDS_IN_MINUTE

        return String.format(TIME_FORMAT_PATTERN, minutes, seconds)
    }

    private companion object {
        private const val PREFERENCE_SAVED_MESSAGE = "Saved Successfully"
        private const val INTERVAL = 1_000L
        private const val ZERO_MILLIS = 0L
        private const val MILLIS_IN_SECOND = 1_000L
        private const val SECONDS_IN_MINUTE = 60L
        private const val DEFAULT_TIME_FORMAT = "00:00"
        private const val TIME_FORMAT_PATTERN = "%02d:%02d"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
