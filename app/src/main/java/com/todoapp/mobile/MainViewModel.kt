package com.todoapp.mobile

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.MainContract.UiEffect
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.data.source.remote.fcm.TDFireBaseMessagingService
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.AuthEvent
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.CurrentRouteTracker
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.RouteArgs
import com.todoapp.mobile.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class MainViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences,
    private val taskRepository: TaskRepository,
    private val groupRepository: GroupRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val userRepository: UserRepository,
    private val pomodoroEngine: PomodoroEngine,
    private val taskSyncRepository: TaskSyncRepository,
    private val currentRouteTracker: CurrentRouteTracker,
    private val pendingPhotoRepository: PendingPhotoRepository,
    private val journalRepository: JournalRepository,
) : ViewModel() {
    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    var isLoggedIn by mutableStateOf<Boolean?>(null)
        private set

    private val _pendingDeepLink = MutableStateFlow<DeepLink?>(null)
    val pendingDeepLink = _pendingDeepLink.asStateFlow()

    val reduceMotion = dataStoreHelper.observeReduceMotion()

    init {
        viewModelScope.launch {
            sessionPreferences
                .observeRefreshToken()
                .map { !it.isNullOrBlank() }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    Timber.tag("AuthLogout").d("observeRefreshToken loggedIn=$loggedIn (transition)")
                    isLoggedIn = loggedIn
                    if (loggedIn) userRepository.syncPendingFcmToken()
                }
        }

        viewModelScope.launch {
            authRepository.events.collect { event ->
                when (event) {
                    is AuthEvent.Logout -> {
                        clearLocalSession()
                        _navEffect.send(
                            NavigationEffect.NavigateClearingBackstack(Screen.Onboarding),
                        )
                    }

                    is AuthEvent.ForceLogout -> {
                        _uiEffect.send(
                            UiEffect.ShowDialog(
                                context.getString(R.string.session_expired_dialog_message),
                            ),
                        )
                        clearLocalSession()
                    }
                }
            }
        }

        viewModelScope.launch {
            refreshUserCache()
        }

        viewModelScope.launch {
            // One-time backfill: claim pre-v20 (unscoped) journal entries for whoever is logged in when
            // the updated app first launches — i.e. the owner. Self-guarded (no-ops if signed out or
            // already claimed). Done at startup, not on journal open, so a later different user can never
            // trigger the claim.
            runCatching { journalRepository.claimOrphansForCurrentUser() }
                .onFailure { Timber.tag("Journal").w(it, "claimOrphansForCurrentUser failed") }
        }
    }

    fun onAction(action: MainContract.UiAction) {
        when (action) {
            MainContract.UiAction.OnDialogOkTap ->
                _navEffect.trySend(
                    NavigationEffect.NavigateClearingBackstack(Screen.Onboarding),
                )
        }
    }

    fun onPushIntent(intent: Intent?) {
        intent ?: return

        // Password-reset deep link: todoapp://reset-password?token=...
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW &&
            data?.scheme == "todoapp" &&
            data.host == "reset-password"
        ) {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                _pendingDeepLink.value = DeepLink.ResetPassword(token)
            }
            intent.data = null
            return
        }

        val target = intent.getStringExtra(TDFireBaseMessagingService.EXTRA_PUSH_TARGET)
        intent.removeExtra(TDFireBaseMessagingService.EXTRA_PUSH_TARGET)
        when (target) {
            TDFireBaseMessagingService.PUSH_TARGET_INVITATIONS -> {
                _pendingDeepLink.value = DeepLink.Invitations
                return
            }
            TDFireBaseMessagingService.PUSH_TARGET_NOTIFICATIONS -> {
                _pendingDeepLink.value = DeepLink.NotificationsInbox
                return
            }
        }

        val groupId =
            intent.getLongExtra(TDFireBaseMessagingService.EXTRA_PUSH_GROUP_ID, -1L)
                .takeIf { it > 0 } ?: return
        val taskId =
            intent.getLongExtra(TDFireBaseMessagingService.EXTRA_PUSH_TASK_ID, -1L)
                .takeIf { it > 0 }
        intent.removeExtra(TDFireBaseMessagingService.EXTRA_PUSH_GROUP_ID)
        intent.removeExtra(TDFireBaseMessagingService.EXTRA_PUSH_TASK_ID)
        _pendingDeepLink.value =
            if (taskId != null) DeepLink.GroupTask(groupId, taskId) else DeepLink.Group(groupId)
    }

    fun consumePendingDeepLink() {
        _pendingDeepLink.value = null
    }

    fun updateCurrentRoute(route: String?, args: RouteArgs? = null) {
        currentRouteTracker.update(route = route, args = args)
    }

    sealed interface DeepLink {
        data class Group(val groupId: Long) : DeepLink

        data class GroupTask(
            val groupId: Long,
            val taskId: Long,
        ) : DeepLink

        data object Invitations : DeepLink

        data object NotificationsInbox : DeepLink

        data class ResetPassword(val token: String) : DeepLink
    }

    private suspend fun clearLocalSession() {
        Timber.tag("AuthLogout").w("clearLocalSession: start")
        runCatching { userRepository.deleteFcmToken() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: deleteFcmToken failed") }
        runCatching { sessionPreferences.clear() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: sessionPreferences.clear failed") }
        taskRepository.deleteAllTasks()
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: deleteAllTasks failed") }
        groupRepository.deleteAllLocalGroups()
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: deleteAllLocalGroups failed") }
        runCatching { pomodoroEngine.finish() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: pomodoroEngine.finish failed") }
        runCatching { dataStoreHelper.clearUser() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: clearUser failed") }
        runCatching { taskSyncRepository.resetCooldown() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: resetCooldown failed") }
        runCatching { pendingPhotoRepository.clearAll() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: pendingPhoto clearAll failed") }
        // Journal entries are intentionally NOT wiped here. Unlike tasks/groups/chat (which re-sync or are
        // stateless on the backend), the journal is local-only with no backend copy; a ForceLogout from a
        // failed token refresh must never destroy the owner's diary. Per-user isolation is handled by
        // owner_user_id scoping in JournalRepository, so a different account on this device sees only its
        // own bucket. Account deletion (SettingsViewModel.deleteAccount) is the only journal purge path.
        Timber.tag("AuthLogout").w("clearLocalSession: done")
    }

    private suspend fun refreshUserCache() {
        userRepository
            .getUserInfo()
            .onSuccess { dataStoreHelper.setUser(it) }
            .onFailure { Timber.tag("AuthLogout").w(it, "refreshUserCache: getUserInfo failed; keeping cached user") }
    }
}
