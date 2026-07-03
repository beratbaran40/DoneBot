package com.todoapp.mobile.ui.chat

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.ai.LocalIntentClassifier
import com.todoapp.mobile.data.model.network.request.ChatHistoryTurn
import com.todoapp.mobile.data.network.NetworkMonitor
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.model.ChatMessage
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * ChatViewModel after the move to the backend chat proxy. The model used to
 * call Vertex AI directly via Firebase AI Logic; now it just POSTs to
 * /chat/message and waits for a final text reply. Tools and the function-
 * calling loop live on the server.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val networkMonitor: NetworkMonitor,
    private val dataStoreHelper: DataStoreHelper,
    private val intentClassifier: LocalIntentClassifier,
    private val taskSyncRepository: TaskSyncRepository,
    private val sessionPreferences: SessionPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ChatContract.UiState>(ChatContract.UiState.Loading)
    val uiState: StateFlow<ChatContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ChatContract.UiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var cooldownJob: Job? = null
    private var cooldownEndElapsedMs: Long = 0L
    private var draftSaveJob: Job? = null
    private var sendJob: Job? = null
    private var refusalCount: Int = 0
    private var lastSendElapsedMs: Long = 0L

    init {
        viewModelScope.launch {
            val initial = chatRepository.getMessages()
            val savedDraft = dataStoreHelper.observeChatDraft().first()
            _uiState.value = ChatContract.UiState.Ready(messages = initial, draft = savedDraft)
            maybeResumePendingPrompt()
            chatRepository.observeMessages().collect { messages ->
                _uiState.update { current ->
                    when (current) {
                        is ChatContract.UiState.Ready -> current.copy(messages = messages)
                        else -> current
                    }
                }
                _uiEffect.trySend(ChatContract.UiEffect.ScrollToBottom)
            }
        }
    }

    fun onAction(action: ChatContract.UiAction) {
        when (action) {
            is ChatContract.UiAction.OnDraftChanged -> updateDraft(action.text)
            ChatContract.UiAction.OnSendClicked -> sendCurrentDraft()
            ChatContract.UiAction.OnStopClicked -> cancelSend()
            ChatContract.UiAction.OnClearHistory -> clearHistory()
            ChatContract.UiAction.OnRetry -> retry()
            ChatContract.UiAction.OnDismissError -> dismissError()
            ChatContract.UiAction.OnSignInTap -> navigateToSignIn()
            is ChatContract.UiAction.OnReportMessage -> reportMessage(action.message)
        }
    }

    /**
     * Flags an offensive/inappropriate assistant reply for moderation review. Only
     * assistant turns are reportable. The outcome surfaces as a toast via [ChatContract.UiEffect].
     */
    private fun reportMessage(message: ChatMessage) {
        if (message.role != ChatMessage.Role.ASSISTANT) return
        viewModelScope.launch {
            chatRepository.reportMessage(content = message.content)
                .onSuccess {
                    Timber.tag(METRICS_TAG).i("chat_report_submitted")
                    _uiEffect.trySend(ChatContract.UiEffect.ShowReportResult(success = true))
                }
                .onFailure { error ->
                    Timber.tag(LOG_TAG).w(error, "chat report failed")
                    _uiEffect.trySend(ChatContract.UiEffect.ShowReportResult(success = false))
                }
        }
    }

    private fun navigateToSignIn() {
        _navEffect.trySend(
            NavigationEffect.Navigate(
                Screen.Login(redirectAfterLogin = Screen.Chat::class.qualifiedName),
            ),
        )
    }

    /**
     * Stop tapped while the bot was thinking. Cancelling the job makes the in-flight network
     * suspend throw CancellationException (rethrown by handleRequest), so the isThinking=false
     * at the tail of executeSendInternal never runs — clear the thinking state here. The user's
     * already-echoed message stays; there's simply no assistant reply for this turn.
     */
    private fun cancelSend() {
        if (sendJob?.isActive != true) return
        sendJob?.cancel()
        _uiState.update { current ->
            when (current) {
                is ChatContract.UiState.Ready -> current.copy(isThinking = false, toolInFlight = null)
                else -> current
            }
        }
        Timber.tag(METRICS_TAG).i("chat_send_cancelled")
    }

    private fun updateDraft(text: String) {
        val capped = if (text.length > MAX_DRAFT_LENGTH) text.take(MAX_DRAFT_LENGTH) else text
        _uiState.update { current ->
            when (current) {
                is ChatContract.UiState.Ready -> current.copy(draft = capped)
                else -> current
            }
        }
        scheduleDraftSave(capped)
    }

    private fun scheduleDraftSave(value: String) {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            dataStoreHelper.setChatDraft(value)
        }
    }

    private fun dismissError() {
        cooldownJob?.cancel()
        // Dismissing the guest "sign in" banner means "forget it" — drop any pending prompt
        // so it won't auto-fire on a later, unrelated login.
        viewModelScope.launch { dataStoreHelper.setPendingChatPrompt("") }
        _uiState.update { current ->
            when (current) {
                is ChatContract.UiState.Ready -> current.copy(
                    error = null,
                    rateLimitCooldownSecondsRemaining = 0,
                    lastFailedPrompt = null,
                )
                else -> current
            }
        }
    }

    private fun sendCurrentDraft() {
        val current = _uiState.value as? ChatContract.UiState.Ready ?: return
        val prompt = current.draft.trim()
        if (prompt.isEmpty() || current.isThinking) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastSendElapsedMs < SEND_COOLDOWN_MS) {
            Timber.tag(LOG_TAG).d("send cooldown active, ignoring tap")
            return
        }
        lastSendElapsedMs = now
        if (!networkMonitor.isOnline.value) {
            _uiState.value = current.copy(draft = "")
            setError(ChatContract.ChatError.OFFLINE, lastFailedPrompt = prompt)
            return
        }
        _uiState.value = current.copy(
            draft = "",
            isThinking = true,
            error = null,
            lastFailedPrompt = null,
            rateLimitCooldownSecondsRemaining = 0,
        )
        cooldownJob?.cancel()
        draftSaveJob?.cancel()
        sendJob = viewModelScope.launch {
            dataStoreHelper.setChatDraft("")
            chatRepository.appendUserMessage(prompt)
            val localMatch = intentClassifier.tryAnswer(prompt)
            if (localMatch != null) {
                Timber.tag(METRICS_TAG).i("local_intent_hit:%s", localMatch.intent.name)
                chatRepository.appendAssistantMessage(localMatch.response)
                _uiState.update { latest ->
                    when (latest) {
                        is ChatContract.UiState.Ready -> latest.copy(
                            isThinking = false,
                            toolInFlight = null,
                        )
                        else -> latest
                    }
                }
                return@launch
            }
            // Guests / signed-out users (no access token) can only use local intents.
            // Anything that needs the backend is gated here, before any network call.
            if (sessionPreferences.getAccessToken().isNullOrBlank()) {
                Timber.tag(METRICS_TAG).i("guest_backend_blocked")
                // Persist the blocked prompt so a fresh ChatViewModel (recreated by the
                // login redirect) can auto-resend it once the user signs in.
                dataStoreHelper.setPendingChatPrompt(prompt)
                setError(ChatContract.ChatError.NOT_AUTHENTICATED, lastFailedPrompt = null)
                _uiState.update { latest ->
                    when (latest) {
                        is ChatContract.UiState.Ready -> latest.copy(isThinking = false, toolInFlight = null)
                        else -> latest
                    }
                }
                return@launch
            }
            executeSendInternal(prompt)
        }
    }

    private fun retry() {
        val current = _uiState.value as? ChatContract.UiState.Ready ?: return
        val prompt = current.lastFailedPrompt ?: return
        if (current.isThinking) return
        if (current.error == ChatContract.ChatError.RATE_LIMITED &&
            current.rateLimitCooldownSecondsRemaining > 0
        ) {
            return
        }
        if (!networkMonitor.isOnline.value) {
            setError(ChatContract.ChatError.OFFLINE, lastFailedPrompt = prompt)
            return
        }
        cooldownJob?.cancel()
        _uiState.value = current.copy(
            isThinking = true,
            error = null,
            rateLimitCooldownSecondsRemaining = 0,
            lastFailedPrompt = null,
        )
        sendJob = viewModelScope.launch {
            executeSendInternal(prompt)
        }
    }

    /**
     * On a freshly-recreated ViewModel (e.g. after the guest taps "Sign in" and the login
     * redirect rebuilds the Chat destination), pick up the prompt the user was gated on and
     * send it automatically — no retype. The prompt lives in DataStore so it survives both
     * the navigation round-trip and ViewModel recreation; works for the register path too.
     */
    private fun maybeResumePendingPrompt() {
        sendJob = viewModelScope.launch {
            val pending = dataStoreHelper.getPendingChatPrompt()
            if (pending.isBlank()) return@launch
            // Still signed out (VM recreated before they actually logged in): keep the prompt
            // for the eventual login. Dismissing the banner is the explicit "forget it" path.
            if (sessionPreferences.getAccessToken().isNullOrBlank()) return@launch
            dataStoreHelper.setPendingChatPrompt("")
            if (!networkMonitor.isOnline.value) {
                setError(ChatContract.ChatError.OFFLINE, lastFailedPrompt = pending)
                return@launch
            }
            Timber.tag(METRICS_TAG).i("guest_pending_resumed")
            resendPendingPrompt(pending)
        }
    }

    private suspend fun resendPendingPrompt(prompt: String) {
        // The guest turn was already echoed into Room before the gate, so reuse that bubble
        // instead of duplicating it. If it's gone (e.g. history cleared by a different-user
        // login), re-add it so the prompt still has a visible user message.
        val alreadyEchoed = chatRepository.getMessages().lastOrNull()?.let {
            it.role == ChatMessage.Role.USER && it.content == prompt
        } ?: false
        if (!alreadyEchoed) {
            chatRepository.appendUserMessage(prompt)
        }
        _uiState.update { current ->
            when (current) {
                is ChatContract.UiState.Ready -> current.copy(
                    isThinking = true,
                    error = null,
                    lastFailedPrompt = null,
                    rateLimitCooldownSecondsRemaining = 0,
                )
                else -> current
            }
        }
        executeSendInternal(prompt)
    }

    private suspend fun executeSendInternal(prompt: String) {
        val turnStartNs = System.nanoTime()
        val locale = currentLocale()
        val history = buildHistorySnapshot()
        chatRepository
            .sendMessage(prompt = prompt, locale = locale, history = history)
            .onSuccess { response ->
                chatRepository.appendAssistantMessage(response.text)
                logTurnSummary(response.meta.roundTrips, turnStartNs, response.text)
                if (response.meta.roundTrips > 1) {
                    taskSyncRepository.resetCooldown()
                    taskSyncRepository.fetchTasks(force = true)
                    Timber.tag(METRICS_TAG).d("post-chat sync rt=%d", response.meta.roundTrips)
                }
            }
            .onFailure { error ->
                handleSendFailure(error, prompt)
            }
        _uiState.update { latest ->
            when (latest) {
                is ChatContract.UiState.Ready -> latest.copy(isThinking = false, toolInFlight = null)
                else -> latest
            }
        }
    }

    /**
     * Snapshot of the persisted conversation, trimmed to the last MAX_HISTORY_TURNS
     * turns and converted to the wire DTO. Drops the brand-new user turn we just
     * persisted because that's already in the request `prompt`.
     */
    private suspend fun buildHistorySnapshot(): List<ChatHistoryTurn> {
        val all = chatRepository.getMessages()
        if (all.isEmpty()) return emptyList()
        // Drop the most recent user message (we just appended it) so we don't
        // double-send it as both prompt + last history entry.
        val priorMessages = all.dropLast(1).takeLast(MAX_HISTORY_TURNS)
        return priorMessages.map { msg ->
            ChatHistoryTurn(
                role = when (msg.role) {
                    ChatMessage.Role.USER -> "user"
                    ChatMessage.Role.ASSISTANT -> "assistant"
                },
                content = msg.content,
            )
        }
    }

    private fun currentLocale(): String = if (Locale.getDefault().language.equals("tr", ignoreCase = true)) "tr" else "en"

    private fun handleSendFailure(error: Throwable, prompt: String) {
        when (error) {
            is DomainException.NoInternet -> {
                Timber.tag(LOG_TAG).w(error, "Network error")
                setError(ChatContract.ChatError.OFFLINE, lastFailedPrompt = prompt)
            }
            is DomainException.Unauthorized -> {
                // OkHttp auth-refresh path will normally rotate the token; if it
                // really expired the global session-end flow takes over.
                Timber.tag(LOG_TAG).w(error, "Unauthorized chat call")
                setError(ChatContract.ChatError.GENERIC, lastFailedPrompt = prompt)
            }
            is DomainException.Server -> {
                val message = error.message.orEmpty()
                when {
                    // Rate-limit first: a Vertex quota hit carries "quota" and must win over the generic path.
                    RATE_LIMIT_MARKERS.any { it in message } -> {
                        Timber.tag(LOG_TAG).w(error, "Rate limited")
                        setError(
                            ChatContract.ChatError.RATE_LIMITED,
                            lastFailedPrompt = prompt,
                            retryAfterSeconds = parseRetryAfterSeconds(message),
                        )
                    }
                    // Vertex outage / timeout / bad-creds (backend 503) → honest "temporarily unavailable".
                    SERVER_UNAVAILABLE_MARKERS.any { it in message } -> {
                        Timber.tag(LOG_TAG).w(error, "AI service unavailable")
                        setError(ChatContract.ChatError.SERVER_UNAVAILABLE, lastFailedPrompt = prompt)
                    }
                    else -> {
                        Timber.tag(LOG_TAG).w(error, "Server error: %s", message)
                        setError(ChatContract.ChatError.GENERIC, lastFailedPrompt = prompt)
                    }
                }
            }
            else -> {
                Timber.tag(LOG_TAG).w(error, "Unexpected chat error")
                setError(ChatContract.ChatError.GENERIC, lastFailedPrompt = prompt)
            }
        }
    }

    private fun logTurnSummary(roundTripCount: Int, turnStartNs: Long, replyText: String) {
        val totalMs = (System.nanoTime() - turnStartNs) / NS_PER_MS
        val refused = REFUSAL_PREFIXES.any { replyText.startsWith(it, ignoreCase = true) }
        if (refused) refusalCount++
        Timber.tag(METRICS_TAG).i(
            "turn rt=%d ms=%d refused=%s refusalTotal=%d",
            roundTripCount,
            totalMs,
            refused,
            refusalCount,
        )
    }

    private fun setError(
        error: ChatContract.ChatError,
        lastFailedPrompt: String?,
        retryAfterSeconds: Int? = null,
    ) {
        cooldownJob?.cancel()
        val cooldownSec = if (error == ChatContract.ChatError.RATE_LIMITED) {
            (retryAfterSeconds ?: RATE_LIMIT_COOLDOWN_SECONDS)
                .coerceIn(MIN_DYNAMIC_COOLDOWN_SECONDS, MAX_DYNAMIC_COOLDOWN_SECONDS)
        } else {
            0
        }
        if (error == ChatContract.ChatError.RATE_LIMITED) {
            cooldownEndElapsedMs = SystemClock.elapsedRealtime() + cooldownSec * MS_PER_SEC
            Timber.tag(METRICS_TAG).i("cooldown set sec=%d (server=%s)", cooldownSec, retryAfterSeconds)
        }
        _uiState.update { current ->
            when (current) {
                is ChatContract.UiState.Ready -> current.copy(
                    error = error,
                    lastFailedPrompt = lastFailedPrompt,
                    rateLimitCooldownSecondsRemaining = cooldownSec,
                    toolInFlight = null,
                )
                else -> current
            }
        }
        if (error == ChatContract.ChatError.RATE_LIMITED) {
            startCooldownTicker()
        }
    }

    private fun parseRetryAfterSeconds(message: String): Int? {
        val match = RETRY_AFTER_REGEX.find(message) ?: return null
        val seconds = match.groupValues[1].toDoubleOrNull() ?: return null
        if (seconds <= 0) return null
        return seconds.toInt() + RETRY_AFTER_PADDING_SECONDS
    }

    private fun startCooldownTicker() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val remainingMs = cooldownEndElapsedMs - SystemClock.elapsedRealtime()
                val remainingSec = ((remainingMs + MS_PER_SEC - 1) / MS_PER_SEC).toInt().coerceAtLeast(0)
                _uiState.update { current ->
                    when (current) {
                        is ChatContract.UiState.Ready -> current.copy(rateLimitCooldownSecondsRemaining = remainingSec)
                        else -> current
                    }
                }
                if (remainingSec <= 0) break
                delay(COOLDOWN_TICK_MS)
            }
        }
    }

    private fun clearHistory() {
        cooldownJob?.cancel()
        viewModelScope.launch {
            chatRepository.clear()
            dataStoreHelper.setPendingChatPrompt("")
            _uiState.update { current ->
                when (current) {
                    is ChatContract.UiState.Ready -> current.copy(
                        error = null,
                        isThinking = false,
                        lastFailedPrompt = null,
                        rateLimitCooldownSecondsRemaining = 0,
                        toolInFlight = null,
                    )
                    else -> current
                }
            }
        }
    }

    private inline fun MutableStateFlow<ChatContract.UiState>.update(
        transform: (ChatContract.UiState) -> ChatContract.UiState,
    ) {
        value = transform(value)
    }

    companion object {
        private const val MAX_HISTORY_TURNS = 10
        const val MAX_DRAFT_LENGTH = 1000
        private const val SEND_COOLDOWN_MS = 3_000L
        private const val RATE_LIMIT_COOLDOWN_SECONDS = 30
        private const val MIN_DYNAMIC_COOLDOWN_SECONDS = 5
        private const val MAX_DYNAMIC_COOLDOWN_SECONDS = 300
        private const val RETRY_AFTER_PADDING_SECONDS = 1
        private const val COOLDOWN_TICK_MS = 1_000L
        private const val MS_PER_SEC = 1_000L
        private val RETRY_AFTER_REGEX = Regex("""[Rr]etry in (\d+(?:\.\d+)?)s""")
        private const val DRAFT_SAVE_DEBOUNCE_MS = 500L
        private const val LOG_TAG = "ChatViewModel"
        private const val METRICS_TAG = "DoneBotMetrics"
        private const val NS_PER_MS = 1_000_000L
        private val REFUSAL_PREFIXES = listOf(
            "Sorry, I can only help",
            "Üzgünüm, sadece bu uygulamadaki",
        )
        private val RATE_LIMIT_MARKERS = listOf("429", "rate limit", "quota")

        // Backend embeds this token in the 503 reason (ChatService.vertexUnavailableMessage) so we can
        // tell a Vertex outage apart from a generic server error without reading the numeric HTTP status.
        private val SERVER_UNAVAILABLE_MARKERS = listOf("vertex_unavailable")
    }
}
