package com.todoapp.mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.todoapp.mobile.MainContract.UiEffect
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.data.source.remote.fcm.TDFireBaseMessagingService
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.AuthEvent
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.ChatRepository
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
import kotlinx.coroutines.flow.filter
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
    private val pomodoroSessionRepository: com.todoapp.mobile.domain.repository.PomodoroSessionRepository,
    private val taskSyncRepository: TaskSyncRepository,
    private val currentRouteTracker: CurrentRouteTracker,
    private val pendingPhotoRepository: PendingPhotoRepository,
    private val chatRepository: ChatRepository,
    private val journalRepository: JournalRepository,
    private val analyticsHelper: com.todoapp.mobile.domain.analytics.AnalyticsHelper,
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
                    if (!loggedIn && sessionPreferences.hasStoredRefreshTokenBlob()) {
                        // Flipped to logged-out while an (encrypted) refresh token is STILL on disk — the
                        // fingerprint of the transient-keystore decrypt bug, not a real logout (which
                        // removes the blob before this emission). Record it so the next occurrence is
                        // attributable instead of silent.
                        runCatching {
                            FirebaseCrashlytics.getInstance().apply {
                                setCustomKey("logged_out_with_token_on_disk", true)
                                recordException(
                                    IllegalStateException("auth flipped logged-out but refresh blob present on disk"),
                                )
                            }
                        }
                    }
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

        viewModelScope.launch {
            // Bound to observeUser(), NOT observeRefreshToken(): the token flow can flip before
            // dataStoreHelper.setUser() completes, and the claim would then silently no-op against
            // owner 0. By the time an id appears here it is already persisted.
            //
            // No one-shot "claimed" flag either — unlike the journal claim above. Sign-out deletes
            // these rows, so a user really can produce a second batch of guest rows (sign out → run a
            // pomodoro → sign in), and distinctUntilChanged lets the 5 → 0 → 5 round trip through.
            dataStoreHelper.observeUser()
                .map { it?.id ?: 0L }
                .distinctUntilChanged()
                .filter { it != 0L }
                .collect {
                    runCatching { pomodoroSessionRepository.claimOrphansForCurrentUser() }
                        .onFailure { e -> Timber.tag("Pomodoro").w(e, "claimOrphans failed") }
                    val today = java.time.LocalDate.now().toEpochDay()
                    // One call covers the whole history; the server refuses a range over 366 days, and
                    // the repository's own six-hour cooldown makes re-emissions free.
                    runCatching { pomodoroSessionRepository.backfill(today - BACKFILL_DAYS, today) }
                        .onFailure { e -> Timber.tag("Pomodoro").w(e, "backfill failed") }
                }
        }

        // Tag every crash report with the signed-in user so Crashlytics groups issues per account.
        // The id is the internal pseudonymous user id — never email/token/PII (see §1.7 log hygiene).
        // observeUser emits the persisted user on startup, the new user on login/register, and null
        // on logout — so a single collector keeps the userId correct across the whole session.
        viewModelScope.launch {
            dataStoreHelper.observeUser()
                .map { it?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    FirebaseCrashlytics.getInstance().setUserId(userId?.toString().orEmpty())
                }
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

    private fun isResetPasswordLink(data: Uri): Boolean {
        val isCustomScheme = data.scheme == "todoapp" && data.host == "reset-password"
        val isHttpsAppLink = data.scheme == "https" &&
            data.host == "donebot-backend.onrender.com" &&
            data.path == "/reset-password"
        return isCustomScheme || isHttpsAppLink
    }

    // Guard-heavy intent parser: each deep-link type is a check-and-return. One early return per type
    // reads clearer than nesting; the count just crossed detekt's limit of 5 with the new reminder case.
    @Suppress("ReturnCount")
    fun onPushIntent(intent: Intent?) {
        intent ?: return

        // Password-reset deep link — either the verified https App Link (Android opens us directly, no
        // hijackable custom scheme) or the todoapp:// scheme (browser-fallback relaunch from the page).
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null && isResetPasswordLink(data)) {
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

        // Local reminder tap (notification/overlay) → open the personal task. Handled BEFORE the group
        // path below, which returns early when no groupId is present (a personal reminder has none).
        intent.getLongExtra(EXTRA_REMINDER_TASK_ID, -1L).takeIf { it > 0 }?.let { reminderTaskId ->
            intent.removeExtra(EXTRA_REMINDER_TASK_ID)
            _pendingDeepLink.value = DeepLink.Task(reminderTaskId)
            return
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

    private var lastLoggedRoute: String? = null

    fun updateCurrentRoute(route: String?, args: RouteArgs? = null) {
        currentRouteTracker.update(route = route, args = args)
        // screen_view (§7.17). Dedupe consecutive same-route reports (config change / recomposition).
        if (route != null && route != lastLoggedRoute) {
            lastLoggedRoute = route
            analyticsHelper.logScreenView(route)
        }
    }

    sealed interface DeepLink {
        data class Group(val groupId: Long) : DeepLink

        data class GroupTask(
            val groupId: Long,
            val taskId: Long,
        ) : DeepLink

        /** A local reminder (notification/overlay) tapped → open this personal task's detail. */
        data class Task(val taskId: Long) : DeepLink

        data object Invitations : DeepLink

        data object NotificationsInbox : DeepLink

        data class ResetPassword(val token: String) : DeepLink
    }

    companion object {
        /** MainActivity intent extra carrying the task id from a tapped local reminder (notification/overlay). */
        const val EXTRA_REMINDER_TASK_ID = "reminder_task_id"

        /** A year of focus history in one call — just under the server's 366-day range ceiling. */
        private const val BACKFILL_DAYS = 365L
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
        // stop(record = false), not finish(). Two bugs close here at once: finish() emits
        // PomodoroFinished, which could navigate a mounted Pomodoro screen to the Summary in the middle
        // of signing out; and recording during sign-out races the deleteAllLocal below — the recorder
        // writes on an IO scope while this chain deletes, so a row that wins would surface in the next
        // account. Dropping the in-flight session is consistent with deleteAllTasks discarding unsynced
        // work, and a forced sign-out could not have pushed it anyway.
        runCatching { pomodoroEngine.stop(record = false) }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: pomodoroEngine.stop failed") }
        runCatching { pomodoroSessionRepository.deleteAllLocal() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: deleteAllLocal failed") }
        runCatching { dataStoreHelper.clearUser() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: clearUser failed") }
        runCatching { taskSyncRepository.resetCooldown() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: resetCooldown failed") }
        runCatching { pendingPhotoRepository.clearAll() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: pendingPhoto clearAll failed") }
        runCatching { chatRepository.clear() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: chat clear failed") }
        // Chat prompt + draft live in DataStore (survive clearUser). Left behind, user A's blocked prompt
        // auto-resends in user B's session and A's unsent draft shows to B — a cross-account leak. Both keys
        // delete-on-empty-string, so "" removes them. (§7.12)
        runCatching { dataStoreHelper.setPendingChatPrompt("") }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: pendingChatPrompt clear failed") }
        runCatching { dataStoreHelper.setChatDraft("") }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: chatDraft clear failed") }
        // Health-points checkpoint lives in DataStore (not user-scoped); reset it so account B doesn't
        // inherit account A's hearts.
        runCatching { dataStoreHelper.clearHealthPoints() }
            .onFailure { Timber.tag("AuthLogout").w(it, "clearLocalSession: clearHealthPoints failed") }
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
