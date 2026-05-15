package com.todoapp.mobile.data.ai

import android.content.Context
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.Session
import com.todoapp.mobile.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalIntentClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val pomodoroEngine: PomodoroEngine,
    private val clock: Clock,
) {
    enum class Intent {
        TODAY_TASKS,
        TOMORROW_TASKS,
        OVERDUE_TASKS,
        WEEKLY_PROGRESS,
        WEEKLY_REMAINING,
        GREETING,
        POMODORO_START,
        POMODORO_STOP,
        POMODORO_STATUS,
    }

    data class Match(val intent: Intent, val response: String)

    suspend fun tryAnswer(prompt: String): Match? {
        val normalized = prompt.trim().lowercase()
        if (normalized.isEmpty()) return null

        // Pomodoro keyword bypasses the global length cap because phrasings like
        // "hey donebot, can you start a 25-minute pomodoro for me please" are realistic
        // and would otherwise fall through to the backend, which has no pomodoro tool.
        // Matched BEFORE the mutation-verb filter because "başlat"/"start" overlap.
        val pomodoroLike = "pomodoro" in normalized || "fokus" in normalized || "focus" in normalized
        if (pomodoroLike) {
            pomodoroMatch(normalized)?.let { return it }
        }

        if (normalized.length > MAX_INTENT_LENGTH) return null
        if (containsMutationVerb(normalized)) return null

        return when {
            GREETING.matches(normalized) -> Match(
                Intent.GREETING,
                context.getString(R.string.chat_local_greeting),
            )
            matchesToday(normalized) -> Match(
                Intent.TODAY_TASKS,
                buildTodayResponse(),
            )
            matchesTomorrow(normalized) -> Match(
                Intent.TOMORROW_TASKS,
                buildTomorrowResponse(),
            )
            matchesOverdue(normalized) -> Match(
                Intent.OVERDUE_TASKS,
                buildOverdueResponse(),
            )
            matchesWeeklyRemaining(normalized) -> Match(
                Intent.WEEKLY_REMAINING,
                buildWeeklyRemainingResponse(),
            )
            matchesWeekly(normalized) -> Match(
                Intent.WEEKLY_PROGRESS,
                buildWeeklyResponse(),
            )
            else -> null
        }
    }

    private fun pomodoroMatch(text: String): Match? {
        val isPomodoroLike = "pomodoro" in text || "fokus" in text || "focus" in text
        if (!isPomodoroLike) return null

        return when {
            POMODORO_STOP_REGEX.containsMatchIn(text) -> handlePomodoroStop()
            POMODORO_STATUS_REGEX.containsMatchIn(text) -> handlePomodoroStatus()
            else -> handlePomodoroStart(text)
        }
    }

    private fun handlePomodoroStart(text: String): Match {
        val state = pomodoroEngine.state.value
        if (state.isRunning) {
            val mins = (state.remainingSeconds / 60).coerceAtLeast(1).toInt()
            return Match(
                Intent.POMODORO_START,
                context.getString(R.string.chat_local_pomodoro_already_running_format, mins),
            )
        }
        val minutes = MINUTE_PATTERN.find(text)
            ?.groupValues
            ?.firstOrNull { it.toIntOrNull() != null }
            ?.toIntOrNull()
            ?.coerceIn(POMODORO_MIN_MINUTES, POMODORO_MAX_MINUTES)
            ?: POMODORO_DEFAULT_MINUTES
        pomodoroEngine.setSessionQueue(
            ArrayDeque(
                listOf(
                    Session(
                        durationSeconds = minutes.toLong() * 60L,
                        mode = PomodoroMode.Focus,
                    ),
                ),
            ),
        )
        pomodoroEngine.prepare()
        pomodoroEngine.start()
        return Match(
            Intent.POMODORO_START,
            context.getString(R.string.chat_local_pomodoro_started_format, minutes),
        )
    }

    private fun handlePomodoroStop(): Match {
        val isRunning = pomodoroEngine.state.value.isRunning
        if (!isRunning) {
            return Match(
                Intent.POMODORO_STOP,
                context.getString(R.string.chat_local_pomodoro_not_running),
            )
        }
        pomodoroEngine.resetState()
        return Match(
            Intent.POMODORO_STOP,
            context.getString(R.string.chat_local_pomodoro_stopped),
        )
    }

    private fun handlePomodoroStatus(): Match {
        val state = pomodoroEngine.state.value
        if (!state.isRunning) {
            return Match(
                Intent.POMODORO_STATUS,
                context.getString(R.string.chat_local_pomodoro_no_active),
            )
        }
        val remaining = state.remainingSeconds.coerceAtLeast(0)
        val mins = (remaining / 60).toInt()
        val secs = (remaining % 60).toInt()
        return Match(
            Intent.POMODORO_STATUS,
            context.getString(R.string.chat_local_pomodoro_status_running_format, mins, secs),
        )
    }

    private fun matchesToday(text: String): Boolean = TODAY_TR_ANCHOR.containsMatchIn(text) || TODAY_EN_ANCHOR.containsMatchIn(text)

    private fun matchesTomorrow(text: String): Boolean = TOMORROW_TR_ANCHOR.containsMatchIn(text) || TOMORROW_EN_ANCHOR.containsMatchIn(text)

    private fun matchesOverdue(text: String): Boolean = OVERDUE_KEYWORDS.containsMatchIn(text)

    private fun matchesWeekly(text: String): Boolean = WEEKLY_TR_ANCHOR.containsMatchIn(text) || WEEKLY_EN_ANCHOR.containsMatchIn(text)

    private fun matchesWeeklyRemaining(text: String): Boolean = WEEKLY_REMAINING_TR.containsMatchIn(text) || WEEKLY_REMAINING_EN.containsMatchIn(text)

    private suspend fun buildTodayResponse(): String {
        val today = LocalDate.now(clock)
        val count = taskRepository.observeTasksByDate(today, includeRecurringInstances = true).first().size
        return if (count == 0) {
            context.getString(R.string.chat_local_today_empty)
        } else {
            context.getString(R.string.chat_local_today_count_format, count)
        }
    }

    private suspend fun buildTomorrowResponse(): String {
        val tomorrow = LocalDate.now(clock).plusDays(1)
        val count = taskRepository.observeTasksByDate(tomorrow, includeRecurringInstances = true).first().size
        return if (count == 0) {
            context.getString(R.string.chat_local_tomorrow_empty)
        } else {
            context.getString(R.string.chat_local_tomorrow_count_format, count)
        }
    }

    private suspend fun buildOverdueResponse(): String {
        val today = LocalDate.now(clock)
        val count = taskRepository.observeOverdueTasks(today).first().size
        return if (count == 0) {
            context.getString(R.string.chat_local_overdue_empty)
        } else {
            context.getString(R.string.chat_local_overdue_count_format, count)
        }
    }

    private suspend fun buildWeeklyResponse(): String {
        val today = LocalDate.now(clock)
        val count = taskRepository.countCompletedTasksInAWeek(today, includeRecurring = true).first()
        return context.getString(R.string.chat_local_week_count_format, count)
    }

    private suspend fun buildWeeklyRemainingResponse(): String {
        val today = LocalDate.now(clock)
        val count = taskRepository.observePendingTasksInAWeek(today, includeRecurring = true).first()
        return if (count == 0) {
            context.getString(R.string.chat_local_week_remaining_empty)
        } else {
            context.getString(R.string.chat_local_week_remaining_format, count)
        }
    }

    private fun containsMutationVerb(text: String): Boolean = MUTATION_VERBS.containsMatchIn(text)

    companion object {
        private const val MAX_INTENT_LENGTH = 60
        private const val POMODORO_DEFAULT_MINUTES = 25
        private const val POMODORO_MIN_MINUTES = 1
        private const val POMODORO_MAX_MINUTES = 180
        private val MINUTE_PATTERN = Regex("(\\d{1,3})\\s*(dk|dakika|min(?:ute)?s?|m\\b)")
        private val POMODORO_STOP_REGEX = Regex(
            "\\b(durdur|iptal|sonland[ıi]r|bitir|stop|cancel|end|finish)\\b",
        )
        private val POMODORO_STATUS_REGEX = Regex(
            "\\b(durum|kalan|kald[ıi]|kal[ıi]yor|status|left|remaining|how\\s+much)\\b",
        )
        private val GREETING = Regex(
            "^(merhaba|selam|s\\.?a\\.?|naber|iyi\\s+(günler|sabahlar|akşamlar)|" +
                "hi|hello|hey|good\\s+(morning|afternoon|evening))[!?.\\s]*\$",
        )
        private val TODAY_TR_ANCHOR = Regex(
            "\\bbugün(kü)?\\b.*\\b(ne|neler|görev(ler|im|in)?|iş(ler|im|in)?|var)\\b",
        )
        private val TODAY_EN_ANCHOR = Regex(
            "(what'?s\\s+(due|on)\\s+today|today'?s?\\s+tasks?|(any|my)\\s+tasks?\\s+today)",
        )
        private val TOMORROW_TR_ANCHOR = Regex(
            "\\byarın(ki)?\\b.*\\b(ne|neler|görev(ler|im|in)?|iş(ler|im|in)?|var)\\b",
        )
        private val TOMORROW_EN_ANCHOR = Regex(
            "(what'?s\\s+(due|on)\\s+tomorrow|tomorrow'?s?\\s+tasks?|(any|my)\\s+tasks?\\s+tomorrow)",
        )
        private val OVERDUE_KEYWORDS = Regex(
            "\\b(gecikmiş|geciken|overdue|past\\s+due)\\b",
        )
        private val WEEKLY_TR_ANCHOR = Regex(
            "\\bbu\\s+hafta\\b.*\\b(nasıl(ım|sın|sınız)?|gidiyor(um|sun)?|ne\\s+kadar|kaç|ilerleme)\\b|" +
                "\\bhafta(lık)?\\s+ilerleme",
        )
        private val WEEKLY_EN_ANCHOR = Regex(
            "(how\\s+am\\s+i\\s+doing\\s+this\\s+week|this\\s+week'?s?\\s+progress|weekly\\s+progress)",
        )
        private val WEEKLY_REMAINING_TR = Regex(
            "\\bbu\\s+hafta\\b.*\\b(kalan|kaldı|bekleyen)\\b",
        )
        private val WEEKLY_REMAINING_EN = Regex(
            "(what'?s\\s+left\\s+this\\s+week|how\\s+many\\s+.*\\s+left\\s+this\\s+week|pending\\s+this\\s+week)",
        )
        private val MUTATION_VERBS = Regex(
            "\\b(ekle|sil|güncelle|oluştur|tamamla|değiştir|kaldır|" +
                "add|delete|create|update|remove|complete|mark|set)\\b",
        )
    }
}
