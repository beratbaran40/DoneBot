package com.todoapp.mobile.data.ai

import android.content.Context
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.usecase.ComputeHealthPointsUseCase
import com.todoapp.mobile.domain.usecase.HealthPoints
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The local-intent layer answers a handful of questions on-device with zero round-trips, so what it
 * claims is a match matters twice: a false positive answers a real task question with a canned count,
 * and a false negative silently costs a Vertex turn.
 *
 * These focus on the health-points intent, whose anchors overlap the weekly ones by construction
 * ("bu hafta kaç kalbim var" satisfies both) — which is the one way adding it could break the
 * intents that were already there.
 */
class LocalIntentClassifierTest {
    private val context = mockk<Context>()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val pomodoroEngine = mockk<PomodoroEngine>(relaxed = true)
    private val computeHealthPoints = mockk<ComputeHealthPointsUseCase>()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-06T09:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        // Return the resource id itself so a response can be traced back to the string it came from
        // without booting Robolectric for a routing test.
        // Resource id plus the formatting arguments, so a test can assert on the numbers the classifier
        // computed without booting Robolectric to render the real string.
        every { context.getString(any()) } answers { "res:${firstArg<Int>()}" }
        every { context.getString(any(), *anyVararg()) } answers {
            // MockK hands the varargs through as a single Object[], so it has to be flattened before
            // it reads as the numbers the classifier actually computed.
            val args = invocation.args.drop(1).flatMap { arg ->
                (arg as? Array<*>)?.toList() ?: listOf(arg)
            }
            if (args.isEmpty()) "res:${firstArg<Int>()}" else "res:${firstArg<Int>()}|${args.joinToString("|")}"
        }
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 13, showDepletionDialog = false))
        // A relaxed mock returns a Flow that never emits, and every response builder ends in .first() —
        // so each intent under test needs its source stubbed or it dies with NoSuchElementException.
        every { taskRepository.countCompletedTasksInAWeek(any(), any()) } returns flowOf(3)
        every { taskRepository.observePendingTasksInAWeek(any(), any()) } returns flowOf(2)
        every { taskRepository.observeOverdueTasks(any()) } returns flowOf(emptyList())
        every { taskRepository.observeTasksByDate(any(), any()) } returns flowOf(emptyList())
    }

    private fun classifier() = LocalIntentClassifier(
        context = context,
        taskRepository = taskRepository,
        pomodoroEngine = pomodoroEngine,
        computeHealthPoints = computeHealthPoints,
        clock = clock,
    )

    @Test
    fun `hearts questions are answered on-device in both languages`() = runTest {
        listOf(
            "kalplerim nasıl?",
            "kaç kalbim var",
            "can puanım ne durumda",
            "how are my hearts?",
            "my health points",
        ).forEach { prompt ->
            assertEquals(
                "expected HEALTH_POINTS for \"$prompt\"",
                LocalIntentClassifier.Intent.HEALTH_POINTS,
                classifier().tryAnswer(prompt)?.intent,
            )
        }
    }

    @Test
    fun `anchors ending in a Turkish letter actually match`() = runTest {
        // Java's \b treats ı ç ş ğ ö ü as non-word characters, so a trailing \b after any of them can
        // never hold. Every one of these looked like a local intent and was silently paying for a
        // backend round-trip — the first is a suggestion chip we ship.
        mapOf(
            "Bu hafta kaç işim kaldı?" to LocalIntentClassifier.Intent.WEEKLY_REMAINING,
            "gecikmiş görevlerim" to LocalIntentClassifier.Intent.OVERDUE_TASKS,
            "bu hafta kaç görev bitirdim" to LocalIntentClassifier.Intent.WEEKLY_PROGRESS,
            "bugünkü işler neler" to LocalIntentClassifier.Intent.TODAY_TASKS,
        ).forEach { (prompt, expected) ->
            assertEquals(
                "expected $expected for \"$prompt\"",
                expected,
                classifier().tryAnswer(prompt.lowercase())?.intent,
            )
        }
    }

    @Test
    fun `the weekly intents still win their own phrasings`() = runTest {
        // The failure mode of adding a hearts intent: its anchors are broad enough to swallow these,
        // and the user would get a heart count when they asked about their week.
        assertEquals(
            LocalIntentClassifier.Intent.WEEKLY_PROGRESS,
            classifier().tryAnswer("bu hafta nasıl gidiyorum")?.intent,
        )
        assertEquals(
            LocalIntentClassifier.Intent.WEEKLY_REMAINING,
            classifier().tryAnswer("bu hafta ne kaldı")?.intent,
        )
    }

    @Test
    fun `the answer carries the percentage the question asked about`() = runTest {
        // 18 of 24 half-hearts is a three-quarters-full bar.
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 18, showDepletionDialog = false))
        assertEquals("res:${R.string.chat_local_health_format}|75|9|12", classifier().tryAnswer("kalplerim nasıl?")?.response)

        // 11 is 45.8%, and truncating would report 45 — a worse day than the user actually had.
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 11, showDepletionDialog = false))
        assertEquals("res:${R.string.chat_local_health_format}|46|5½|12", classifier().tryAnswer("kalplerim nasıl?")?.response)

        // Full bar is exactly 100, never 99 from a rounding slip.
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 24, showDepletionDialog = false))
        assertEquals("res:${R.string.chat_local_health_format}|100|12|12", classifier().tryAnswer("kalplerim nasıl?")?.response)
    }

    @Test
    fun `an empty bar gets the encouraging line, not a zero`() = runTest {
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 0, showDepletionDialog = true))

        val match = classifier().tryAnswer("kalplerim nasıl?")

        assertEquals(LocalIntentClassifier.Intent.HEALTH_POINTS, match?.intent)
        assertEquals("res:${R.string.chat_local_health_empty}", match?.response)
    }

    @Test
    fun `a hearts question that also names a day is still about hearts`() = runTest {
        // "bugün … var" satisfies TODAY_TR_ANCHOR, so anywhere below it in the `when` this returns
        // today's task count to someone asking about their hearts.
        listOf("bugün kaç kalbim var?", "bu hafta kaç kalbim var").forEach { prompt ->
            assertEquals(
                "expected HEALTH_POINTS for \"$prompt\"",
                LocalIntentClassifier.Intent.HEALTH_POINTS,
                classifier().tryAnswer(prompt)?.intent,
            )
        }
    }

    @Test
    fun `a sentence-capitalised Turkish prompt still matches`() = runTest {
        // The soft keyboard capitalises the first letter, and `İ`.lowercase() is `i` + U+0307
        // COMBINING DOT ABOVE — an invisible character sitting between `i` and the rest of the word,
        // which no anchor accounts for. Every one of these was paying for a backend round-trip.
        assertEquals(LocalIntentClassifier.Intent.GREETING, classifier().tryAnswer("İyi günler")?.intent)
        assertEquals(LocalIntentClassifier.Intent.TODAY_TASKS, classifier().tryAnswer("Bugün işim ne?")?.intent)
    }

    @Test
    fun `English prompts survive the İ normalisation`() = runTest {
        // Guards the fix itself: lowercasing with the Turkish locale would have mapped `I` to `ı` and
        // broken every English anchor, which is why only the combining mark is stripped.
        assertEquals(LocalIntentClassifier.Intent.TODAY_TASKS, classifier().tryAnswer("What's due today?")?.intent)
        assertEquals(LocalIntentClassifier.Intent.GREETING, classifier().tryAnswer("Hi")?.intent)
    }

    @Test
    fun `both vocabularies reach the same intent`() = runTest {
        // The chips say "üretkenlik sağlığım" because that names what the number measures, but the
        // Activity screen draws hearts — so whichever word the user picked up, the question lands.
        listOf(
            "Üretkenlik sağlığım nasıl?",
            "üretkenlik sağlığım ne durumda",
            "how's my productivity health?",
            "kalplerim nasıl?",
            "how are my hearts?",
        ).forEach { prompt ->
            assertEquals(
                "expected HEALTH_POINTS for \"$prompt\"",
                LocalIntentClassifier.Intent.HEALTH_POINTS,
                classifier().tryAnswer(prompt)?.intent,
            )
        }
    }

    @Test
    fun `inflected hearts questions are still answered on-device`() = runTest {
        listOf(
            "kalbimi nasıl doldururum",
            "kalbime ne oldu",
            "kalplerimi göster",
            "kalbimin durumu ne",
            "kalp puanım kaç",
        ).forEach { prompt ->
            assertEquals(
                "expected HEALTH_POINTS for \"$prompt\"",
                LocalIntentClassifier.Intent.HEALTH_POINTS,
                classifier().tryAnswer(prompt)?.intent,
            )
        }
    }

    @Test
    fun `an English task that mentions a heart still goes to the backend`() = runTest {
        // The mirror of the Turkish guard below. A bare "my heart" is not a hearts question, and
        // answering it on-device both gives the wrong answer and never reaches the backend that
        // could have found the task.
        listOf(
            "when is my heart doctor appointment",
            "my heart medication at 8",
            "my heart rate check tomorrow",
        ).forEach { prompt -> assertNull("expected no local match for \"$prompt\"", classifier().tryAnswer(prompt)) }

        // …while the real question still lands locally.
        assertEquals(LocalIntentClassifier.Intent.HEALTH_POINTS, classifier().tryAnswer("how are my hearts?")?.intent)
        assertEquals(LocalIntentClassifier.Intent.HEALTH_POINTS, classifier().tryAnswer("my heart?")?.intent)
    }

    @Test
    fun `a task that merely mentions a heart still goes to the backend`() = runTest {
        // "kalp doktoru" is a real task, not a question about the bar — answering it locally with a
        // heart count would be worse than a round-trip.
        assertNull(classifier().tryAnswer("kalp doktoru randevum ne zaman"))
    }
}
