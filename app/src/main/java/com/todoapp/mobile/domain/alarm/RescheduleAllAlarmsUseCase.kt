package com.todoapp.mobile.domain.alarm

import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.recurrenceRule
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class RescheduleAllAlarmsUseCase
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val dailyPlanPreferences: DailyPlanPreferences,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke() {
        rescheduleTaskAlarms()
        rescheduleRecurringTaskAlarms()
        rescheduleDailyPlan()
    }

    private suspend fun rescheduleTaskAlarms() {
        val tasks = taskRepository.observeAllTasks().first()
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        var scheduled = 0
        tasks.forEach { task ->
            if (task.recurrence != Recurrence.NONE) return@forEach
            val offset = task.reminderOffsetMinutes ?: return@forEach
            if (offset < 0) return@forEach
            if (task.isCompleted) return@forEach
            if (task.date.isBefore(today)) return@forEach
            val item = task.toAlarmItem(
                remindBeforeMinutes = offset,
                overrideStartTime = effectiveAlarmTime(task).takeIf { task.isAllDay },
            )
            if (item.time.isBefore(now)) return@forEach
            alarmScheduler.schedule(item, AlarmType.TASK)
            scheduled++
        }
        Timber.tag(TAG).d("Rescheduled %d task alarms (of %d tasks)", scheduled, tasks.size)
    }

    private suspend fun rescheduleRecurringTaskAlarms() {
        val today = LocalDate.now()
        val tasks = taskRepository.observeAllTasks().first()
            // Finished routines no longer fire on upcoming days, so they arm no recurring alarm.
            .filter { it.recurrence != Recurrence.NONE && it.finishedOn == null }
            // Neither do routines whose scheduled end has already passed.
            .filter { task -> task.recurrenceUntil?.let { !today.isAfter(it) } ?: true }
        tasks.forEach { task ->
            runCatching {
                // Clear the whole slot range first: a reminder removed while the device was off would
                // otherwise stay armed, and an armed slot re-arms itself from its own extras forever.
                alarmScheduler.cancelRecurring(task.id)
                val fallback = effectiveAlarmTime(task)
                // Reminder times live in their own table and observeAllTasks() doesn't carry them.
                val times = taskRepository.getReminderTimes(task.id)
                    .take(MAX_REMINDER_SLOTS)
                    .ifEmpty { listOf(fallback) }
                times.forEachIndexed { slot, time ->
                    alarmScheduler.scheduleRecurring(
                        taskId = task.id,
                        rule = task.recurrenceRule,
                        anchorDate = task.date,
                        hour = time.hour,
                        minute = time.minute,
                        message = task.title,
                        slot = slot,
                    )
                }
            }.onFailure { Timber.tag(TAG).w(it, "scheduleRecurring failed for taskId=%d", task.id) }
        }
        Timber.tag(TAG).d("Rescheduled %d recurring task alarms", tasks.size)
    }

    /**
     * All-day tasks carry a 00:00 placeholder timeStart, so firing at it would ring every all-day
     * routine at midnight after a reboot. Mirrors TaskRepositoryImpl.effectiveAlarmTime — the two used
     * to disagree, which is exactly how that midnight drift got in.
     */
    private suspend fun effectiveAlarmTime(task: Task): LocalTime = if (task.isAllDay) {
        dailyPlanPreferences.observePlanTime().first() ?: DailyPlanDefaults.DEFAULT_PLAN_TIME
    } else {
        task.timeStart
    }

    private suspend fun rescheduleDailyPlan() {
        val time =
            dailyPlanPreferences.observePlanTime().first()
                ?: DailyPlanDefaults.DEFAULT_PLAN_TIME
        val item = buildDailyPlanAlarmItem(time, LocalDateTime.now(), message = "")
        alarmScheduler.schedule(item, AlarmType.DAILY_PLAN)
        Timber.tag(TAG).d("Rescheduled daily plan alarm for %s", time)
    }

    private companion object {
        const val TAG = "RescheduleAllAlarms"
    }
}
