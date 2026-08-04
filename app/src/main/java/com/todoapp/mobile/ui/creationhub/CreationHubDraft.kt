package com.todoapp.mobile.ui.creationhub

import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The process-death-durable slice of [CreationHubContract.UiState]. Only the user-entered scalar
 * fields are kept — enough to restore a half-filled form after Android kills the process (e.g. a
 * low-RAM device reclaims the app while the photo picker is open, the sinister case behind §5.7).
 *
 * Enums are stored by name and dates/times as epoch-day / second-of-day, so the whole thing is
 * plainly serializable with no kotlin-parcelize plugin (this module doesn't apply it).
 *
 * Deliberately excluded, with reasons:
 * - `pendingPhotos` — Uri/bytes aren't durable across a kill; a separate, larger effort.
 * - `adminGroups` / `groupMembers` — re-derived from the repo on init.
 * - group selection / assignee / priority — restoring these needs an async member reload that would
 *   race the restore; the common task fields (below) still survive, and the group is a quick re-pick.
 * - `clientTaskId` — a fresh idempotency key on restore is correct: nothing was submitted yet.
 * - `titleError` / `isSaving` — transient UI flags.
 */
@Serializable
data class CreationHubDraft(
    val step: String = Step.HUB_ROOT.name,
    val scope: String? = null,
    val taskType: String? = null,
    val title: String = "",
    val dateEpochDay: Long? = null,
    val reminderOffsetMinutes: Long? = null,
    val recurrence: String = Recurrence.DAILY.name,
    val subtaskDrafts: List<String> = listOf(""),
    val detailsExpanded: Boolean = false,
    val description: String = "",
    val isAllDay: Boolean = true,
    val timeStartSecondOfDay: Int? = null,
    val timeEndSecondOfDay: Int? = null,
    val category: String = TaskCategory.PERSONAL.name,
    val customCategoryName: String = "",
    val isSecret: Boolean = false,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val placeholderIndex: Int = 0,
    val recurrenceUntilEpochDay: Long? = null,
    val reminderTimeSecondsOfDay: List<Int> = emptyList(),
    val recurrenceInterval: Int = 1,
    /** DayOfWeek names; unknown entries are dropped on restore, like every other enum here. */
    val recurrenceByDay: List<String> = emptyList(),
)

/** Projects the current form state onto its durable slice. */
fun UiState.toDraft(): CreationHubDraft = CreationHubDraft(
    step = step.name,
    scope = scope?.name,
    taskType = taskType?.name,
    title = title,
    dateEpochDay = date.toEpochDay(),
    reminderOffsetMinutes = reminderOffsetMinutes,
    recurrence = recurrence.name,
    subtaskDrafts = subtaskDrafts,
    detailsExpanded = detailsExpanded,
    description = description,
    isAllDay = isAllDay,
    timeStartSecondOfDay = timeStart?.toSecondOfDay(),
    timeEndSecondOfDay = timeEnd?.toSecondOfDay(),
    category = category.name,
    customCategoryName = customCategoryName,
    isSecret = isSecret,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
    placeholderIndex = placeholderIndex,
    recurrenceUntilEpochDay = recurrenceUntil?.toEpochDay(),
    reminderTimeSecondsOfDay = reminderTimes.map { it.toSecondOfDay() },
    recurrenceInterval = recurrenceInterval,
    recurrenceByDay = recurrenceByDay.map { it.name },
)

/**
 * Rebuilds a [UiState] from [base] — which carries the non-durable fields (a fresh clientTaskId,
 * empty photos, empty adminGroups) — with the persisted scalars layered on top. Enum names that no
 * longer resolve (e.g. an app update dropped a value) fall back to [base]'s value.
 */
fun CreationHubDraft.toState(base: UiState): UiState {
    val restoredScope = scope?.let { enumOrNull<TaskScope>(it) }
    val restoredType = taskType?.let { enumOrNull<TaskType>(it) }
    // A draft written before "Group" stopped being a task type restores with a type that no longer
    // resolves. Rather than landing on the form with nothing chosen, send the user back to the step
    // that can answer the question — the typed title and dates below still survive.
    val restoredStep = when {
        restoredType != null -> enumOrNull<Step>(step) ?: base.step
        restoredScope != null -> Step.TASK_TYPE
        else -> Step.HUB_ROOT
    }
    return base.copy(
        step = restoredStep,
        scope = restoredScope,
        taskType = restoredType,
        title = title,
        date = dateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: base.date,
        reminderOffsetMinutes = reminderOffsetMinutes,
        recurrence = enumOrNull<Recurrence>(recurrence) ?: base.recurrence,
        subtaskDrafts = subtaskDrafts.ifEmpty { listOf("") },
        detailsExpanded = detailsExpanded,
        description = description,
        isAllDay = isAllDay,
        timeStart = timeStartSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
        timeEnd = timeEndSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
        category = enumOrNull<TaskCategory>(category) ?: base.category,
        customCategoryName = customCategoryName,
        isSecret = isSecret,
        locationName = locationName,
        locationAddress = locationAddress,
        locationLat = locationLat,
        locationLng = locationLng,
        placeholderIndex = placeholderIndex,
        recurrenceUntil = recurrenceUntilEpochDay?.let { LocalDate.ofEpochDay(it) },
        reminderTimes = reminderTimeSecondsOfDay.map { LocalTime.ofSecondOfDay(it.toLong()) },
        recurrenceInterval = recurrenceInterval,
        recurrenceByDay = recurrenceByDay.mapNotNull { enumOrNull<DayOfWeek>(it) }.toSet(),
    )
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? {
    return runCatching { enumValueOf<T>(name) }.getOrNull()
}
