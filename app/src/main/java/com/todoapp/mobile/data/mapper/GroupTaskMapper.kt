package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.GroupSubtaskEntity
import com.todoapp.mobile.data.model.entity.GroupTaskEntity
import com.todoapp.mobile.data.model.network.data.GroupMemberData
import com.todoapp.mobile.data.model.network.data.GroupTaskData
import com.todoapp.mobile.data.model.network.data.TaskData
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.dayOfWeekSetFromStorage
import com.todoapp.mobile.domain.model.toStorageCsv
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun TaskData.toGroupTask(): GroupTask = GroupTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate =
    LocalDate
        .ofEpochDay(date)
        .atTime(LocalTime.ofSecondOfDay(timeStart))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli(),
    assignee =
    assignedTo?.let { user ->
        GroupMember(
            userId = user.userId,
            displayName = user.displayName,
            email = "",
            avatarUrl = null,
            role = "",
            joinedAt = 0L,
        )
    },
    isAllDay = isAllDay,
    timeStart = LocalTime.ofSecondOfDay(timeStart.coerceIn(0L, MAX_SECOND_OF_DAY)),
    timeEnd = LocalTime.ofSecondOfDay(timeEnd.coerceIn(0L, MAX_SECOND_OF_DAY)),
    photoUrls = photoUrls,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
    category = TaskCategory.fromStorage(category),
    customCategoryName = customCategoryName,
    recurrence = Recurrence.fromStorage(recurrence),
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = dayOfWeekSetFromStorage(recurrenceByDay),
    recurrenceUntil = recurrenceUntil?.let { LocalDate.ofEpochDay(it) },
    // The wire carries SECOND-of-day; the domain model is LocalTime either way, but the group Room
    // mirror stores MINUTE-of-day like the personal one — the 60× trap lives at that boundary below.
    reminderTimes = reminderTimes.map { LocalTime.ofSecondOfDay(it.toLong().coerceIn(0L, MAX_SECOND_OF_DAY)) },
    subtasks = subtasks.map {
        Subtask(id = it.id, title = it.title, isCompleted = it.isCompleted, orderIndex = it.orderIndex)
    },
)

fun GroupTaskData.toDomain(): GroupTask = GroupTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    assignee = assignee?.toDomain(),
    photoUrls = photoUrls,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
    isAllDay = isAllDay,
    category = TaskCategory.fromStorage(category),
    customCategoryName = customCategoryName,
    recurrence = Recurrence.fromStorage(recurrence),
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = dayOfWeekSetFromStorage(recurrenceByDay),
    recurrenceUntil = recurrenceUntil?.let { LocalDate.ofEpochDay(it) },
    reminderTimes = reminderTimes.map { LocalTime.ofSecondOfDay(it.toLong().coerceIn(0L, MAX_SECOND_OF_DAY)) },
    subtasks = subtasks.map {
        Subtask(id = it.id, title = it.title, isCompleted = it.isCompleted, orderIndex = it.orderIndex)
    },
)

fun GroupMemberData.toDomain(): GroupMember = GroupMember(
    userId = userId,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
    joinedAt = joinedAt,
)

fun GroupTaskEntity.toDomain(): GroupTask = GroupTask(
    id = remoteId ?: id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    assignee =
    if (assigneeUserId != null && assigneeDisplayName != null) {
        GroupMember(
            userId = assigneeUserId,
            displayName = assigneeDisplayName,
            email = "",
            avatarUrl = assigneeAvatarUrl,
            role = "",
            joinedAt = 0L,
        )
    } else {
        null
    },
    isAllDay = isAllDay,
    timeStart = timeStart?.let { LocalTime.ofSecondOfDay(it.coerceIn(0L, MAX_SECOND_OF_DAY)) },
    timeEnd = timeEnd?.let { LocalTime.ofSecondOfDay(it.coerceIn(0L, MAX_SECOND_OF_DAY)) },
    photoUrls = photoUrls.split(',').filter { it.isNotBlank() },
    groupId = remoteGroupId,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
    category = TaskCategory.fromStorage(category),
    customCategoryName = customCategoryName,
    recurrence = Recurrence.fromStorage(recurrence),
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = dayOfWeekSetFromStorage(recurrenceByDay),
    recurrenceUntil = recurrenceUntil?.let { LocalDate.ofEpochDay(it) },
    reminderTimes = reminderTimes.minutesOfDayFromCsv(),
)

fun GroupTask.toEntity(
    localGroupId: Long,
    remoteGroupId: Long,
): GroupTaskEntity = GroupTaskEntity(
    remoteId = id,
    localGroupId = localGroupId,
    remoteGroupId = remoteGroupId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    isAllDay = isAllDay,
    timeStart = timeStart?.toSecondOfDay()?.toLong(),
    timeEnd = timeEnd?.toSecondOfDay()?.toLong(),
    assigneeUserId = assignee?.userId,
    assigneeDisplayName = assignee?.displayName,
    assigneeAvatarUrl = assignee?.avatarUrl,
    photoUrls = photoUrls.joinToString(","),
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    locationAddress = locationAddress,
    category = category.name,
    customCategoryName = if (category == TaskCategory.OTHER) customCategoryName?.takeIf { it.isNotBlank() } else null,
    recurrence = recurrence.name,
    recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
    // Dead data on any other frequency, and it would still diff on an equality check.
    recurrenceByDay = if (recurrence == Recurrence.WEEKLY) recurrenceByDay.toStorageCsv() else null,
    recurrenceUntil = recurrenceUntil?.toEpochDay(),
    reminderTimes = reminderTimes.toMinuteOfDayCsv(),
)

/** Steps of a group task, flattened for the local mirror. Keyed by the server ids, not local ones. */
fun GroupTask.toSubtaskEntities(): List<GroupSubtaskEntity> = subtasks.mapIndexed { index, step ->
    GroupSubtaskEntity(
        remoteId = step.id,
        remoteTaskId = id,
        title = step.title,
        isCompleted = step.isCompleted,
        orderIndex = if (step.orderIndex >= 0) step.orderIndex else index,
    )
}

fun List<GroupSubtaskEntity>.toDomainSubtasks(): List<Subtask> = sortedBy { it.orderIndex }.map {
    Subtask(id = it.remoteId, title = it.title, isCompleted = it.isCompleted, orderIndex = it.orderIndex)
}

/**
 * The one place the minute/second split is decided for group tasks: the Room mirror stores
 * MINUTE-of-day (matching the personal `task_reminders` table) while the wire speaks SECOND-of-day.
 * Getting this backwards puts every reminder 60× off, which reads as "alarms just don't fire".
 */
private fun List<LocalTime>.toMinuteOfDayCsv(): String? = takeIf { it.isNotEmpty() }
    ?.joinToString(",") { (it.toSecondOfDay() / SECONDS_PER_MINUTE).toString() }

private fun String?.minutesOfDayFromCsv(): List<LocalTime> = this
    ?.split(',')
    ?.mapNotNull { it.trim().toIntOrNull() }
    ?.map { LocalTime.ofSecondOfDay((it.toLong() * SECONDS_PER_MINUTE).coerceIn(0L, MAX_SECOND_OF_DAY)) }
    .orEmpty()

private const val MAX_SECOND_OF_DAY = 86_399L
private const val SECONDS_PER_MINUTE = 60
