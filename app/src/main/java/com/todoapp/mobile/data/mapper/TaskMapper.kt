package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.domain.model.REMINDER_OFF
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.RecurrenceRule
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.domain.model.dayOfWeekSetFromStorage
import com.todoapp.mobile.domain.model.toStorageCsv
import java.time.LocalDate
import java.time.LocalTime

private const val MINUTE_IN_HOUR = 60

private fun LocalDate.toEpochDayLong(): Long = toEpochDay()

private fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

private fun Long.toLocalTimeFromMinuteOfDay(): LocalTime = LocalTime.of(
    (this / MINUTE_IN_HOUR).toInt(),
    (this % MINUTE_IN_HOUR).toInt(),
)

private fun LocalTime.toMinuteOfDayLong(): Long = (hour * MINUTE_IN_HOUR + minute).toLong()

/**
 * The entity's repeat rule on its own. Used by the day-expansion and stats loops, which run this per
 * task per day and must not pay for a full [toDomain] just to answer "does it fire?".
 */
fun TaskEntity.toRecurrenceRule(): RecurrenceRule = RecurrenceRule(
    frequency = Recurrence.fromStorage(recurrence),
    interval = recurrenceInterval,
    byDay = dayOfWeekSetFromStorage(recurrenceByDay),
    until = recurrenceUntil?.toLocalDate(),
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    remoteId = remoteId,
    clientTaskId = clientTaskId,
    title = title,
    description = description,
    date = date.toLocalDate(),
    timeStart = timeStart.toLocalTimeFromMinuteOfDay(),
    timeEnd = timeEnd.toLocalTimeFromMinuteOfDay(),
    isCompleted = isCompleted,
    isSecret = isSecret,
    photoUrls = photoUrls.split(',').filter { it.isNotBlank() },
    // Room stores REMINDER_OFF where the domain wants null. Any negative counts, so a row written by
    // an older build (or hand-edited) still reads as "off" rather than scheduling something.
    reminderOffsetMinutes = reminderOffsetMinutes.takeIf { it >= 0L },
    category = TaskCategory.fromStorage(category),
    customCategoryName = customCategoryName,
    recurrence = Recurrence.fromStorage(recurrence),
    isAllDay = isAllDay,
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    locationAddress = locationAddress,
    finishedOn = finishedOn?.toLocalDate(),
    isPendingSync = syncStatus != SyncStatus.SYNCED,
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = dayOfWeekSetFromStorage(recurrenceByDay),
    recurrenceUntil = recurrenceUntil?.toLocalDate(),
    // Null stays null. Deriving a value here would make a plain read-modify-write silently WRITE a
    // derived type into the column, freezing the very guess this field exists to replace — the
    // fallback belongs at the one read site (Task.resolvedType), not at the storage boundary.
    declaredType = TaskType.fromStorage(declaredType),
)

fun Task.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        date = date.toEpochDayLong(),
        timeStart = timeStart.toMinuteOfDayLong(),
        timeEnd = timeEnd.toMinuteOfDayLong(),
        isCompleted = isCompleted,
        isSecret = isSecret,
        remoteId = remoteId,
        clientTaskId = clientTaskId,
        syncStatus = syncStatus,
        photoUrls = photoUrls.joinToString(","),
        reminderOffsetMinutes = reminderOffsetMinutes ?: REMINDER_OFF,
        category = category.name,
        customCategoryName = if (category == TaskCategory.OTHER) customCategoryName?.takeIf { it.isNotBlank() } else null,
        recurrence = recurrence.name,
        isAllDay = isAllDay,
        locationLat = locationLat,
        locationLng = locationLng,
        locationName = locationName?.takeIf { it.isNotBlank() },
        locationAddress = locationAddress?.takeIf { it.isNotBlank() },
        finishedOn = finishedOn?.toEpochDayLong(),
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        recurrenceByDay = recurrenceByDay.toStorageCsv(),
        recurrenceUntil = recurrenceUntil?.toEpochDayLong(),
        declaredType = declaredType?.name,
    )
}
