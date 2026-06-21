package com.todoapp.mobile.domain.alarm

import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
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
            val item = task.toAlarmItem(remindBeforeMinutes = offset)
            if (item.time.isBefore(now)) return@forEach
            alarmScheduler.schedule(item, AlarmType.TASK)
            scheduled++
        }
        Timber.tag(TAG).d("Rescheduled %d task alarms (of %d tasks)", scheduled, tasks.size)
    }

    private suspend fun rescheduleRecurringTaskAlarms() {
        val tasks = taskRepository.observeAllTasks().first()
            // Finished routines no longer fire on upcoming days, so they arm no recurring alarm.
            .filter { it.recurrence != Recurrence.NONE && it.finishedOn == null }
        tasks.forEach { task ->
            runCatching {
                alarmScheduler.scheduleRecurring(
                    taskId = task.id,
                    recurrence = task.recurrence,
                    anchorDate = task.date,
                    hour = task.timeStart.hour,
                    minute = task.timeStart.minute,
                    message = task.title,
                )
            }.onFailure { Timber.tag(TAG).w(it, "scheduleRecurring failed for taskId=%d", task.id) }
        }
        Timber.tag(TAG).d("Rescheduled %d recurring task alarms", tasks.size)
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
