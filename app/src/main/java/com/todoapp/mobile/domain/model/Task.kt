package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.data.model.network.data.TaskData
import com.todoapp.mobile.data.model.network.request.SubtaskRequest
import com.todoapp.mobile.data.model.network.request.TaskRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Immutable
data class Task(
    val id: Long = 0L,
    val remoteId: Long? = null,
    /** Client-generated idempotency key (UUID) for create dedup; null until the first local insert. §4.12 */
    val clientTaskId: String? = null,
    // True when the local row hasn't reached the server yet (syncStatus != SYNCED). The UI only
    // surfaces a "not synced" hint for signed-in users — a guest's tasks are local-only, never
    // "pending sync" — so the badge is gated on auth state at the call site, not here.
    val isPendingSync: Boolean = false,
    val title: String,
    val description: String?,
    val date: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val isCompleted: Boolean,
    val isSecret: Boolean,
    val photoUrls: List<String> = emptyList(),
    /**
     * Minutes before timeStart at which to fire the reminder. 0 = on time,
     * positive = N minutes before, null = no reminder. Synced with the backend
     * since V9 so chat-set reminders survive cross-device usage. The actual
     * alarm is still scheduled device-side via AlarmScheduler.
     */
    val reminderOffsetMinutes: Long? = 0L,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val customCategoryName: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    /**
     * When true, the task spans the whole day; timeStart/timeEnd are placeholders
     * (00:00 / 23:59) and notifications fire at the user's default morning hour.
     */
    val isAllDay: Boolean = false,
    /**
     * Optional location attached to the task. Any subset can be present:
     *  - name + address only → tap-to-Maps uses geo:0,0?q=<name+address>
     *  - lat + lng + name → opens with the precise pin and the name as label
     *  - all four → richest experience (precise pin + readable label)
     * Set from the Add/Edit task sheet (place picker) or from chat ("at Kadıköy").
     */
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    /**
     * The day the user marked the whole recurring routine done from the Recurring tab. Null = still
     * active. A recurring task stops firing on days AFTER this date (see [firesOn]); days up to and
     * including it keep their per-day completion. Meaningless for non-recurring tasks.
     */
    val finishedOn: LocalDate? = null,
    /** Ordered steps of a staged task. Non-empty ⇒ this task is "staged". Personal tasks only. */
    val subtasks: List<Subtask> = emptyList(),
    /**
     * Lightweight staged-progress for list surfaces, populated cheaply via a COUNT query (the full
     * [subtasks] list is only loaded in the detail screen). `subtaskTotal == 0` ⇒ not a staged task.
     */
    val subtaskTotal: Int = 0,
    val subtaskDone: Int = 0,
    /** Fire every N periods of [recurrence]. 1 = every period. Meaningless when recurrence is NONE. */
    val recurrenceInterval: Int = 1,
    /** WEEKLY only: the weekdays to fire on. Empty = the anchor's own weekday (legacy behaviour). */
    val recurrenceByDay: Set<DayOfWeek> = emptySet(),
    /**
     * The routine's *scheduled* last day, inclusive — "take this for a month". Null = open-ended.
     * Distinct from [finishedOn], which is the manual retire; [RecurrenceRule.firesOn] honours both
     * and the earlier one wins.
     */
    val recurrenceUntil: LocalDate? = null,
    /**
     * Absolute times of day this task reminds at, applied to every occurrence. Non-empty ⇒ these
     * REPLACE [reminderOffsetMinutes], which stays the model for the classic single "N minutes
     * before" reminder on a one-off task.
     */
    val reminderTimes: List<LocalTime> = emptyList(),
)

/**
 * The task's repeat rule as one value object — the single input to [RecurrenceRule.firesOn], the
 * alarm next-fire calculator and the day-N-of-M progress helpers.
 */
val Task.recurrenceRule: RecurrenceRule
    get() = RecurrenceRule(recurrence, recurrenceInterval, recurrenceByDay, recurrenceUntil)

fun Task.toAlarmItem(
    remindBeforeMinutes: Long = 0,
    overrideStartTime: LocalTime? = null,
): AlarmItem = AlarmItem(
    // overrideStartTime lets callers swap in the user's morning-plan hour for all-day tasks,
    // whose own timeStart is a 00:00 placeholder (see isAllDay docstring).
    time = LocalDateTime.of(date, (overrideStartTime ?: timeStart).minusMinutes(remindBeforeMinutes)),
    // Append the location name with a bullet so the system tray notification reads like
    // "Doctor • Acıbadem Hastanesi". Locale-neutral separator (works in EN + TR copy).
    message = locationName?.takeIf { it.isNotBlank() }?.let { "$title • $it" } ?: title,
    minutesBefore = remindBeforeMinutes,
    taskId = id,
)

fun Task.toCreateTaskRequestDto(
    familyGroupId: Long? = null,
    assignedToUserId: Long? = null,
    priority: String? = null,
): TaskRequest = TaskRequest(
    id = if (id != 0L) id else null,
    clientTaskId = clientTaskId,
    title = title,
    description = description,
    date = date.toEpochDay(),
    timeStart = timeStart.toSecondOfDay().toLong(),
    timeEnd = timeEnd.toSecondOfDay().toLong(),
    isCompleted = isCompleted,
    isSecret = isSecret,
    familyGroupId = familyGroupId,
    assignedToUserId = assignedToUserId,
    priority = priority,
    category = category.name,
    customCategoryName = customCategoryName,
    recurrence = recurrence.name,
    finishedOn = finishedOn?.toEpochDay(),
    isAllDay = isAllDay,
    reminderOffsetMinutes = reminderOffsetMinutes ?: 0L,
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    locationAddress = locationAddress,
    // null (not empty) when there are no steps, so syncing a plain task never tells the
    // backend to wipe steps another device may have added. See TaskRequest.subtasks.
    subtasks = subtasks.takeIf { it.isNotEmpty() }?.map {
        SubtaskRequest(
            remoteId = it.remoteId,
            title = it.title,
            isCompleted = it.isCompleted,
            orderIndex = it.orderIndex,
        )
    },
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = recurrenceByDay.toStorageCsv(),
    recurrenceUntil = recurrenceUntil?.toEpochDay(),
    // Seconds on the wire (like timeStart/timeEnd), minutes in Room. Null — not empty — when there are
    // none, mirroring the subtasks convention so a plain task never tells the server to wipe times.
    reminderTimes = reminderTimes.takeIf { it.isNotEmpty() }?.map { it.toSecondOfDay() },
)

fun TaskData.toDomain(): Task = Task(
    id = id,
    remoteId = id,
    clientTaskId = clientTaskId,
    title = title,
    description = description,
    date = LocalDate.ofEpochDay(date),
    timeStart = LocalTime.ofSecondOfDay(timeStart),
    timeEnd = LocalTime.ofSecondOfDay(timeEnd),
    isCompleted = isCompleted,
    isSecret = isSecret,
    photoUrls = photoUrls,
    reminderOffsetMinutes = reminderOffsetMinutes,
    category = TaskCategory.fromStorage(category),
    customCategoryName = customCategoryName,
    recurrence = Recurrence.fromStorage(recurrence),
    isAllDay = isAllDay,
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    locationAddress = locationAddress,
    finishedOn = finishedOn?.let { LocalDate.ofEpochDay(it) },
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = dayOfWeekSetFromStorage(recurrenceByDay),
    recurrenceUntil = recurrenceUntil?.let { LocalDate.ofEpochDay(it) },
    reminderTimes = reminderTimes.map { LocalTime.ofSecondOfDay(it.toLong()) },
)
