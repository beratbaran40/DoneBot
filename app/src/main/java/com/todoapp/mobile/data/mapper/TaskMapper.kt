package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
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
    reminderOffsetMinutes = reminderOffsetMinutes,
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
        reminderOffsetMinutes = reminderOffsetMinutes ?: 0L,
        category = category.name,
        customCategoryName = if (category == TaskCategory.OTHER) customCategoryName?.takeIf { it.isNotBlank() } else null,
        recurrence = recurrence.name,
        isAllDay = isAllDay,
        locationLat = locationLat,
        locationLng = locationLng,
        locationName = locationName?.takeIf { it.isNotBlank() },
        locationAddress = locationAddress?.takeIf { it.isNotBlank() },
        finishedOn = finishedOn?.toEpochDayLong(),
    )
}
