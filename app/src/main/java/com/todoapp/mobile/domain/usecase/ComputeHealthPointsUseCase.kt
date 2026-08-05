package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.repository.HealthPointsPreferences
import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS
import com.todoapp.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Health-points bar snapshot for the UI. [halfHearts] is 0..[MAX_HALF_HEARTS] — twelve hearts. */
data class HealthPoints(
    val halfHearts: Int,
    val showDepletionDialog: Boolean,
)

/**
 * Derives the Activity health-points bar from existing completion history — no dedicated Room table.
 *
 * Phase A settles every fully-ended day since the last checkpoint (idempotent, persisted): +1
 * half-heart per active day, -1 per idle day, clamped. Phase B maps today's live completion state on
 * top — today only ever ADDS a half-heart (a live opportunity), never a penalty; the -½ for an idle
 * day lands at midnight when it becomes an ended day.
 *
 * "Active day" uses [TaskRepository.observeCompletedCountsByDateRange] with `includeRecurring = true`,
 * so recurring completions count (the old numeric streak silently ignored them).
 *
 * The depletion dialog is level-triggered (`display == 0 && !dialogShown`), derived from persisted
 * state, so it survives rotation/process-death and fires once per depletion episode.
 */
class ComputeHealthPointsUseCase
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val healthPointsPreferences: HealthPointsPreferences,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<HealthPoints> = flow {
        val today = LocalDate.now(clock)
        val yesterday = today.minusDays(1)
        val yesterdayEpochDay = yesterday.toEpochDay()

        // ---- Phase A: settle every fully-ended day, once ----
        val stored = healthPointsPreferences.getCheckpoint()
        val anchorEpochDay: Long
        val storedDialogShown: Boolean
        var settledHalfHearts: Int
        if (stored.lastSettledEpochDay == null) {
            // First-ever run: start FULL, anchor at yesterday ⇒ no real history is folded.
            anchorEpochDay = yesterdayEpochDay
            settledHalfHearts = MAX_HALF_HEARTS
            storedDialogShown = false
            healthPointsPreferences.setCheckpoint(MAX_HALF_HEARTS, yesterdayEpochDay, dialogShown = false)
        } else {
            anchorEpochDay = stored.lastSettledEpochDay
            settledHalfHearts = stored.settledHalfHearts
            storedDialogShown = stored.dialogShown
        }

        if (yesterdayEpochDay > anchorEpochDay) {
            val activeDays = taskRepository
                .observeCompletedCountsByDateRange(
                    LocalDate.ofEpochDay(anchorEpochDay + 1),
                    yesterday,
                    includeRecurring = true,
                )
                .first()
                .filterValues { it > 0 }
                .keys
                .map { it.toEpochDay() }
                .toSet()

            settledHalfHearts = HealthPointsCalculator.fold(
                startHalfHearts = settledHalfHearts,
                days = (anchorEpochDay + 1)..yesterdayEpochDay,
                activeDays = activeDays,
            )
            // Re-arm the depletion dialog once the user has climbed back above zero.
            val dialogShown = settledHalfHearts <= 0 && storedDialogShown
            healthPointsPreferences.setCheckpoint(settledHalfHearts, yesterdayEpochDay, dialogShown)
        }

        val settled = settledHalfHearts

        // ---- Phase B: today is a live opportunity (reactive) ----
        emitAll(
            combine(
                taskRepository
                    .observeCompletedCountsByDateRange(today, today, includeRecurring = true)
                    .map { counts -> (counts[today] ?: 0) > 0 },
                healthPointsPreferences.observeDialogShown(),
            ) { todayActive, dialogShown ->
                val display = (settled + if (todayActive) 1 else 0).coerceIn(0, MAX_HALF_HEARTS)
                HealthPoints(
                    halfHearts = display,
                    showDepletionDialog = display == 0 && !dialogShown,
                )
            },
        )
    }
}
