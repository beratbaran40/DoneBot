package com.todoapp.mobile.data.ai

import android.content.Context
import com.todoapp.mobile.R
import com.todoapp.mobile.common.heartsLabel
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.Session
import com.todoapp.mobile.domain.repository.HEART_COUNT
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.usecase.ComputeHealthPointsUseCase
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
    private val computeHealthPoints: ComputeHealthPointsUseCase,
    private val clock: Clock,
) {
    enum class Intent {
        TODAY_TASKS,
        TOMORROW_TASKS,
        OVERDUE_TASKS,
        WEEKLY_PROGRESS,
        WEEKLY_REMAINING,
        HEALTH_POINTS,
        GREETING,
        POMODORO_START,
        POMODORO_STOP,
        POMODORO_STATUS,
    }

    data class Match(val intent: Intent, val response: String)

    suspend fun tryAnswer(prompt: String): Match? {
        val normalized = prompt.trim().lowercase().replace(COMBINING_DOT_ABOVE, "")
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
            // FIRST among the task intents. A hearts question routinely names a day or a week too
            // ("bugün kaç kalbim var", "bu hafta kaç kalbim var"), and both the today and the weekly
            // anchors match those — so anywhere lower in this `when` the user gets a task count in
            // answer to a question about hearts. The reverse can't happen: the health anchors require
            // "kalbim/kalplerim" or "…puan", which no task question contains.
            matchesHealthPoints(normalized) -> Match(
                Intent.HEALTH_POINTS,
                buildHealthPointsResponse(),
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

    /**
     * The Activity health-points bar, answered entirely on-device. It qualifies as a local intent for
     * the same reason the day/week counts do — the value is already device-local, so a round-trip
     * would only buy latency — and it keeps the bot from ever quoting a number the bar doesn't show.
     */
    private suspend fun buildHealthPointsResponse(): String {
        val halfHearts = computeHealthPoints().first().halfHearts
        return if (halfHearts <= 0) {
            context.getString(R.string.chat_local_health_empty)
        } else {
            context.getString(R.string.chat_local_health_format, heartsLabel(halfHearts), HEART_COUNT)
        }
    }

    private fun matchesHealthPoints(text: String): Boolean = HEALTH_TR_ANCHOR.containsMatchIn(text) || HEALTH_EN_ANCHOR.containsMatchIn(text)

    private fun containsMutationVerb(text: String): Boolean = MUTATION_VERBS.containsMatchIn(text)

    companion object {
        private const val MAX_INTENT_LENGTH = 60
        private const val POMODORO_DEFAULT_MINUTES = 25
        private const val POMODORO_MIN_MINUTES = 1
        private const val POMODORO_MAX_MINUTES = 180

        /**
         * `İ` (U+0130) lowercases under `Locale.ROOT` to `i` + U+0307 COMBINING DOT ABOVE, so a
         * sentence-capitalised Turkish prompt — which is exactly what Android's soft keyboard produces
         * — carries an invisible mark that no anchor accounts for: "İyi günler" misses GREETING,
         * "İşim ne?" misses the today anchor, "İptal et" misses the pomodoro stop.
         *
         * Stripping only this one mark is deliberate. Lowercasing with the Turkish locale instead would
         * map English `I` to `ı` and break every English anchor ("I have tasks" → "ı have tasks"), and
         * an NFD-then-strip-all-marks pass would flatten ö→o and ü→u, breaking the Turkish ones.
         */
        private const val COMBINING_DOT_ABOVE = "̇"

        /**
         * Trailing word-boundary guard. **Do not use `\b` at the end of a Turkish word here.**
         *
         * Java's `\b` counts only ASCII letters as word characters, so after ı, ç, ş, ğ, ö or ü it is
         * never a boundary and the alternative simply cannot match. That silently killed five anchors,
         * including the shipped suggestion chip "Bu hafta kaç işim kaldı?" — it looked like a local
         * intent and had always been paying for a full backend round-trip instead.
         *
         * A leading `\b` is safe **only because** every anchor word starts with an ASCII letter *and*
         * [COMBINING_DOT_ABOVE] is stripped first — without that strip, a word the user began with `İ`
         * starts with `i` + a combining mark and the leading `\b` lands in the wrong place.
         */
        private const val WORD_END = "(?![\\p{L}\\p{N}])"

        private val MINUTE_PATTERN = Regex("(\\d{1,3})\\s*(dk|dakika|min(?:ute)?s?|m\\b)")
        private val POMODORO_STOP_REGEX = Regex(
            "\\b(durdur|iptal|sonland[ıi]r|bitir|stop|cancel|end|finish)$WORD_END",
        )
        private val POMODORO_STATUS_REGEX = Regex(
            "\\b(durum|kalan|kald[ıi]|kal[ıi]yor|status|left|remaining|how\\s+much)$WORD_END",
        )

        /**
         * Requires a possessive ("kalbim", "kalplerim", "kalp puanım"), never a bare "kalp" — otherwise
         * a real task like "kalp doktoru randevum ne zaman" would be answered with a heart count.
         * `kal[pb]` covers the Turkish p→b softening in the possessive form, and the optional tail
         * carries the case suffixes a natural question adds on top of it ("kalbimi", "kalbime",
         * "kalbimde", "kalbimden", "kalbimin") — without it every inflected phrasing paid for a Vertex
         * round-trip to fetch a number the device already had.
         */
        private val HEALTH_TR_ANCHOR = Regex(
            "\\bkal[pb](ler)?(im|in)(i|e|de|den|in)?$WORD_END|" +
                "\\b(can|sağlık|kalp)\\s*puan(ım|im|ı|ımı|ini)?$WORD_END",
        )

        /**
         * The English mirror of [HEALTH_TR_ANCHOR]'s narrowness. A bare `my heart` is not enough —
         * "when is my heart doctor appointment" is a task, and answering it with a heart count both
         * gives the wrong answer and never reaches the backend that could have found the task. So
         * `heart` has to be plural, or followed by the end of the phrase, or paired with "points".
         */
        private val HEALTH_EN_ANCHOR = Regex(
            "\\bmy\\s+hearts\\b|" +
                "\\bmy\\s+heart$WORD_END\\s*[?.!]?\\s*$|" +
                "\\bmy\\s+health\\s*points?\\b|" +
                "\\bhow\\s+many\\s+hearts\\b|" +
                "\\bhealth\\s*points?\\b.*\\b(left|now|status)\\b",
        )
        private val GREETING = Regex(
            "^(merhaba|selam|s\\.?a\\.?|naber|iyi\\s+(günler|sabahlar|akşamlar)|" +
                "hi|hello|hey|good\\s+(morning|afternoon|evening))[!?.\\s]*\$",
        )
        private val TODAY_TR_ANCHOR = Regex(
            "\\bbugün(kü)?$WORD_END.*\\b(ne|neler|görev(ler|im|in)?|iş(ler|im|in)?|var)$WORD_END",
        )
        private val TODAY_EN_ANCHOR = Regex(
            "(what'?s\\s+(due|on)\\s+today|today'?s?\\s+tasks?|(any|my)\\s+tasks?\\s+today)",
        )
        private val TOMORROW_TR_ANCHOR = Regex(
            "\\byarın(ki)?$WORD_END.*\\b(ne|neler|görev(ler|im|in)?|iş(ler|im|in)?|var)$WORD_END",
        )
        private val TOMORROW_EN_ANCHOR = Regex(
            "(what'?s\\s+(due|on)\\s+tomorrow|tomorrow'?s?\\s+tasks?|(any|my)\\s+tasks?\\s+tomorrow)",
        )
        private val OVERDUE_KEYWORDS = Regex(
            "\\b(gecikmiş|geciken|overdue|past\\s+due)$WORD_END",
        )
        private val WEEKLY_TR_ANCHOR = Regex(
            "\\bbu\\s+hafta$WORD_END.*\\b(nasıl(ım|sın|sınız)?|gidiyor(um|sun)?|ne\\s+kadar|kaç|ilerleme)$WORD_END|" +
                "\\bhafta(lık)?\\s+ilerleme",
        )
        private val WEEKLY_EN_ANCHOR = Regex(
            "(how\\s+am\\s+i\\s+doing\\s+this\\s+week|this\\s+week'?s?\\s+progress|weekly\\s+progress)",
        )
        private val WEEKLY_REMAINING_TR = Regex(
            "\\bbu\\s+hafta$WORD_END.*\\b(kalan|kaldı|bekleyen)$WORD_END",
        )
        private val WEEKLY_REMAINING_EN = Regex(
            "(what'?s\\s+left\\s+this\\s+week|how\\s+many\\s+.*\\s+left\\s+this\\s+week|pending\\s+this\\s+week)",
        )
        private val MUTATION_VERBS = Regex(
            "\\b(ekle|sil|güncelle|oluştur|tamamla|değiştir|kaldır|" +
                "add|delete|create|update|remove|complete|mark|set)$WORD_END",
        )
    }
}
