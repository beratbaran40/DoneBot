package com.todoapp.mobile.ui.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.occurrenceIndex
import com.todoapp.mobile.domain.model.occurrenceTotal
import com.todoapp.mobile.domain.model.recurrenceRule
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.usecase.SetTaskCompletionUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.ui.common.taskform.capabilities
import com.todoapp.mobile.ui.common.taskform.taskFormType
import com.todoapp.mobile.ui.details.DetailsContract.UiAction
import com.todoapp.mobile.ui.details.DetailsContract.UiEffect
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import com.todoapp.mobile.ui.home.PendingPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val pendingPhotoRepository: PendingPhotoRepository,
    private val setTaskCompletion: SetTaskCompletionUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>(Channel.BUFFERED) }
    val uiEffect: Flow<UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var originalTask: Task? = null
    private var currentTaskId: Long? = null

    init {
        val taskId = requireNotNull(savedStateHandle.get<Long>("taskId")) {
            "DetailsScreen route must provide a taskId argument"
        }
        currentTaskId = taskId
        loadTask(taskId)
    }

    fun loadTask(taskId: Long) {
        currentTaskId = taskId
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val loaded = taskRepository.getTaskById(taskId)
                if (loaded == null) {
                    _uiState.value = UiState.Error(message = context.getString(R.string.error_task_not_found))
                    return@launch
                }
                // Reminder times live in their own table, so getTaskById doesn't carry them. Fold them
                // in before anything derives capabilities — isDirty compares against originalTask, and
                // a task missing its own reminders would read as "user cleared them" on the first save.
                val task = loaded
                    .copy(reminderTimes = taskRepository.getReminderTimes(taskId))
                    .withDayScopedSteps(taskId)
                originalTask = task
                val initial = successFromTask(task)
                _uiState.value = initial.copy(isReminderInPast = computeIsReminderInPast(initial))
                // Photos live server-side; fetch authoritative list via remoteId
                task.remoteId?.let { remoteId ->
                    taskRepository.fetchRemoteTask(remoteId).onSuccess { remote ->
                        updateSuccessState { it.copy(photoUrls = remote.photoUrls) }
                    }
                }
            } catch (e: IOException) {
                _uiState.value = UiState.Error(message = e.toUserMessage(context), throwable = e)
            }
        }
    }

    /**
     * Reminder times drive [TaskCapabilities.hasMultipleReminders], so the capability snapshot has to
     * be recomputed with them — otherwise the very edit that adds a second reminder wouldn't switch
     * the task to CUSTOM until it was reloaded.
     */
    private fun changeReminderTimes(transform: (List<LocalTime>) -> List<LocalTime>) = updateSuccessState { state ->
        val times = transform(state.reminderTimes)
        val capabilities = state.capabilities.copy(hasMultipleReminders = times.size > 1)
        state.copy(
            reminderTimes = times,
            capabilities = capabilities,
            taskType = taskFormType(capabilities),
        )
    }

    /**
     * A recurring staged task's step flags live per-day, so the row flags loaded with the task are
     * always false and would render an already-done day as untouched. Overlay this occurrence's state.
     */
    private suspend fun Task.withDayScopedSteps(taskId: Long): Task {
        if (recurrence == Recurrence.NONE || subtasks.isEmpty()) return this
        return copy(subtasks = taskRepository.observeSubtasksForDay(taskId, date).first())
    }

    private fun successFromTask(task: Task): UiState.Success {
        val drafts = if (task.subtasks.isNotEmpty()) {
            task.subtasks.sortedBy { it.orderIndex }.map { SubtaskDraft(it.id, it.title, it.isCompleted) } +
                SubtaskDraft(null, "", false)
        } else {
            emptyList()
        }
        val capabilities = task.capabilities()
        return UiState.Success(
            taskId = task.remoteId ?: -1L,
            taskTitle = task.title,
            taskTimeStart = task.timeStart,
            taskTimeEnd = task.timeEnd,
            taskDate = task.date,
            taskDescription = task.description.orEmpty(),
            dialogSelectedDate = task.date,
            isDirty = false,
            titleError = null,
            isSaving = false,
            photoUrls = task.photoUrls,
            locationName = task.locationName,
            locationAddress = task.locationAddress,
            locationLat = task.locationLat,
            locationLng = task.locationLng,
            selectedCategory = task.category,
            customCategoryName = task.customCategoryName.orEmpty(),
            selectedRecurrence = task.recurrence,
            reminderOffsetMinutes = task.reminderOffsetMinutes,
            isAllDay = task.isAllDay,
            taskType = taskFormType(capabilities),
            capabilities = capabilities,
            reminderTimes = task.reminderTimes,
            recurrenceUntil = task.recurrenceUntil,
            recurrenceInterval = task.recurrenceInterval,
            recurrenceByDay = task.recurrenceByDay,
            // Computed here, never in a composable — the by-weekday case counts real firing days.
            routineDayIndex = task.recurrenceRule.occurrenceIndex(task.date, LocalDate.now(), task.finishedOn),
            routineDayTotal = task.recurrenceRule.occurrenceTotal(task.date, task.finishedOn),
            subtaskDrafts = drafts,
            isCompleted = task.isCompleted,
        )
    }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            UiAction.OnBackClick -> requestBack()
            UiAction.OnCancelClick -> cancelChanges()
            UiAction.OnConfirmDiscard -> confirmDiscard()
            UiAction.OnDismissDiscardDialog -> updateSuccessState { it.copy(showDiscardDialog = false) }
            UiAction.OnSaveChanges -> saveChanges()
            UiAction.OnToggleComplete -> toggleComplete()
            is UiAction.OnTaskTitleEdit -> updateTitle(uiAction.title)
            is UiAction.OnTaskDescriptionEdit -> updateDescription(uiAction.description)
            is UiAction.OnTaskDateEdit -> updateDate(uiAction.date)
            is UiAction.OnTaskTimeStartEdit -> updateTimeStart(uiAction.time)
            is UiAction.OnTaskTimeEndEdit -> updateTimeEnd(uiAction.time)
            is UiAction.OnDialogDateSelect -> selectDialogDate(uiAction.date)
            UiAction.OnDialogDateDeselect -> deselectDialogDate()
            UiAction.OnRetry -> retry()
            is UiAction.OnPhotoPicked -> stagePhotoUpload(uiAction.bytes, uiAction.mimeType)
            is UiAction.OnPhotoDelete -> stagePhotoDelete(uiAction.photoId)
            is UiAction.OnPendingPhotoCancel -> cancelPendingPhoto(uiAction.index)
            is UiAction.OnLocationPicked -> setLocation(uiAction.name, uiAction.address, uiAction.lat, uiAction.lng)
            UiAction.OnLocationCleared -> setLocation(null, null, null, null)
            is UiAction.OnCategoryChange -> changeCategory(uiAction.category)
            is UiAction.OnCustomCategoryNameChange -> changeCustomCategoryName(uiAction.name)
            is UiAction.OnRecurrenceChange -> changeRecurrence(uiAction.recurrence)
            is UiAction.OnReminderOffsetChange -> changeReminderOffset(uiAction.minutes)
            is UiAction.OnReminderTimeAdd -> changeReminderTimes { (it + uiAction.time).distinct().sorted() }
            is UiAction.OnReminderTimeRemove -> changeReminderTimes { it - uiAction.time }
            is UiAction.OnRecurrenceUntilChange -> updateSuccessState {
                it.copy(recurrenceUntil = uiAction.until)
            }
            is UiAction.OnIntervalChange -> updateSuccessState {
                it.copy(recurrenceInterval = uiAction.interval)
            }
            is UiAction.OnWeekdayToggle -> updateSuccessState { state ->
                val next = if (uiAction.day in state.recurrenceByDay) {
                    state.recurrenceByDay - uiAction.day
                } else {
                    state.recurrenceByDay + uiAction.day
                }
                state.copy(recurrenceByDay = next)
            }
            is UiAction.OnSubtaskTitleChange -> changeSubtaskTitle(uiAction.index, uiAction.title)
            is UiAction.OnSubtaskToggle -> toggleSubtaskDraft(uiAction.index)
            is UiAction.OnSubtaskRemove -> removeSubtaskDraft(uiAction.index)
            is UiAction.OnAllDayChange -> changeAllDay(uiAction.isAllDay)
        }
    }

    private fun changeCategory(category: TaskCategory) {
        // Type is fixed on the detail screen, so category no longer drives recurrence (the old
        // BIRTHDAY→YEARLY auto-sync would have silently turned a one-time task into a routine).
        updateSuccessState {
            it.copy(
                selectedCategory = category,
                customCategoryName = if (category == TaskCategory.OTHER) it.customCategoryName else "",
            )
        }
    }

    private fun changeCustomCategoryName(name: String) {
        updateSuccessState { it.copy(customCategoryName = name) }
    }

    /**
     * Dropping the repeat also drops everything that only existed because of it. The rule fields
     * (end date, interval, weekdays, absolute reminder times) are hidden from the form when the
     * frequency is NONE, so keeping them would leave values the user can no longer see — and here
     * they were being written straight through to the saved task.
     */
    private fun changeRecurrence(recurrence: Recurrence) {
        updateSuccessState {
            if (recurrence == Recurrence.NONE) {
                it.copy(
                    selectedRecurrence = recurrence,
                    recurrenceUntil = null,
                    recurrenceInterval = 1,
                    recurrenceByDay = emptySet(),
                    reminderTimes = emptyList(),
                )
            } else {
                it.copy(selectedRecurrence = recurrence)
            }
        }
    }

    private fun changeReminderOffset(minutes: Long?) {
        updateSuccessState { it.copy(reminderOffsetMinutes = minutes) }
    }

    private fun changeAllDay(isAllDay: Boolean) {
        updateSuccessState { it.copy(isAllDay = isAllDay) }
    }

    private fun changeSubtaskTitle(index: Int, title: String) {
        updateSuccessState { state ->
            if (index !in state.subtaskDrafts.indices) {
                state
            } else {
                val drafts = state.subtaskDrafts.toMutableList()
                drafts[index] = drafts[index].copy(title = title)
                // Keep one trailing empty row so the next step can be typed inline.
                if (index == drafts.lastIndex && title.isNotBlank()) drafts.add(SubtaskDraft(null, "", false))
                state.copy(subtaskDrafts = drafts)
            }
        }
    }

    private fun toggleSubtaskDraft(index: Int) {
        updateSuccessState { state ->
            if (index !in state.subtaskDrafts.indices) {
                state
            } else {
                val drafts = state.subtaskDrafts.toMutableList()
                drafts[index] = drafts[index].copy(isCompleted = !drafts[index].isCompleted)
                state.copy(subtaskDrafts = drafts)
            }
        }
    }

    private fun removeSubtaskDraft(index: Int) {
        updateSuccessState { state ->
            val realCount = state.subtaskDrafts.count { it.title.isNotBlank() }
            val isLastRealStep = realCount <= 1 &&
                index in state.subtaskDrafts.indices &&
                state.subtaskDrafts[index].title.isNotBlank()
            if (index !in state.subtaskDrafts.indices || isLastRealStep) {
                // Staged tasks keep ≥1 step (type is fixed — never degrade to one-time).
                state
            } else {
                val drafts = state.subtaskDrafts.toMutableList().apply { removeAt(index) }
                val withTrailing = if (drafts.isEmpty() || drafts.last().title.isNotBlank()) {
                    drafts + SubtaskDraft(null, "", false)
                } else {
                    drafts
                }
                state.copy(subtaskDrafts = withTrailing)
            }
        }
    }

    /** Commits staged-step edits on Save: delete removed, add new, rename + toggle existing. */
    private suspend fun reconcileSubtasks(state: UiState.Success) {
        val localId = currentTaskId ?: return
        val original = originalTask?.subtasks ?: emptyList()
        val drafts = state.subtaskDrafts.filter { it.title.isNotBlank() }
        val draftIds = drafts.mapNotNull { it.id }.toSet()
        original.filter { it.id !in draftIds }.forEach { taskRepository.deleteSubtask(it.id) }
        val originalById = original.associateBy { it.id }
        for (draft in drafts) {
            if (draft.id == null) {
                taskRepository.addSubtask(localId, draft.title)
            } else {
                val orig = originalById[draft.id]
                if (orig != null) {
                    if (orig.title != draft.title) taskRepository.updateSubtaskTitle(draft.id, draft.title)
                    if (orig.isCompleted != draft.isCompleted) {
                        taskRepository.toggleSubtask(draft.id, draft.isCompleted, originalTask?.date)
                    }
                }
            }
        }
    }

    private fun setLocation(name: String?, address: String?, lat: Double?, lng: Double?) {
        updateSuccessState {
            it.copy(
                locationName = name?.takeIf { v -> v.isNotBlank() },
                locationAddress = address?.takeIf { v -> v.isNotBlank() },
                locationLat = lat,
                locationLng = lng,
            )
        }
    }

    /** Stage a picked photo for upload-on-save. No network call until [saveChanges] runs. */
    private fun stagePhotoUpload(
        bytes: ByteArray,
        mimeType: String,
    ) {
        updateSuccessState { state ->
            state.copy(pendingPhotoUploads = state.pendingPhotoUploads + PendingPhoto(bytes, mimeType))
        }
    }

    /**
     * Stage an existing-photo deletion: remove it from the visible list and remember the photoId
     * so [saveChanges] can issue the actual DELETE later. Does nothing if photoId can't be parsed
     * (defensive — shouldn't happen with current backend URLs).
     */
    private fun stagePhotoDelete(photoId: Long) {
        updateSuccessState { state ->
            val matchingUrl = state.photoUrls.firstOrNull { photoIdFromUrl(it) == photoId }
            if (matchingUrl == null) state else {
                state.copy(
                    photoUrls = state.photoUrls - matchingUrl,
                    pendingPhotoDeleteIds = state.pendingPhotoDeleteIds + photoId,
                )
            }
        }
    }

    private fun cancelPendingPhoto(index: Int) {
        updateSuccessState { state ->
            if (index !in state.pendingPhotoUploads.indices) state else {
                state.copy(
                    pendingPhotoUploads = state.pendingPhotoUploads.filterIndexed { i, _ -> i != index },
                )
            }
        }
    }

    private fun photoIdFromUrl(url: String): Long? = url.trimEnd('/').substringAfterLast('/').toLongOrNull()

    private fun retry() {
        currentTaskId?.let { loadTask(it) }
    }

    /**
     * The single write path for [UiState.Success]. `isDirty` and `isReminderInPast` are DERIVED here
     * and overwrite whatever [transform] set, so a handler must never try to assign them — four of
     * them used to pass `isDirty = true` and were silently discarded, which is what hid the fact that
     * [computeIsDirty] wasn't looking at their fields.
     */
    private inline fun updateSuccessState(crossinline transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { currentState ->
            when (currentState) {
                is UiState.Success -> {
                    val updated = transform(currentState)
                    updated.copy(
                        isDirty = computeIsDirty(updated),
                        isReminderInPast = computeIsReminderInPast(updated),
                    )
                }

                else -> currentState
            }
        }
    }

    private fun computeIsReminderInPast(state: UiState.Success): Boolean {
        // Recurring tasks pick the next instance via AlarmSchedulerImpl.computeNextFire, so a
        // "past" anchor time isn't actually a user-visible problem there.
        if (state.selectedRecurrence != Recurrence.NONE) return false
        val offset = state.reminderOffsetMinutes ?: return false
        val effectiveTime = if (state.isAllDay) {
            // Synchronous fallback to the 09:00 default — the repository will read the user's
            // configured value at schedule time, but the warning's accuracy at the picker level
            // only depends on whether the offset puts the alarm in the past relative to "today
            // at default morning hour", which is the realistic scenario.
            DailyPlanDefaults.DEFAULT_PLAN_TIME
        } else {
            state.taskTimeStart ?: return false
        }
        val reminderTime = LocalDateTime.of(state.taskDate, effectiveTime.minusMinutes(offset))
        return reminderTime.isBefore(LocalDateTime.now())
    }

    private fun updateTitle(title: String) {
        updateSuccessState {
            it.copy(taskTitle = title, titleError = null)
        }
    }

    private fun updateDescription(description: String) {
        updateSuccessState { it.copy(taskDescription = description) }
    }

    private fun updateDate(date: LocalDate) {
        updateSuccessState { it.copy(taskDate = date, recurrenceUntil = it.endAfter(date)) }
    }

    private fun updateTimeStart(time: LocalTime) {
        updateSuccessState { it.copy(taskTimeStart = time) }
    }

    private fun updateTimeEnd(time: LocalTime) {
        updateSuccessState { it.copy(taskTimeEnd = time) }
    }

    private fun selectDialogDate(date: LocalDate) {
        updateSuccessState {
            it.copy(dialogSelectedDate = date, taskDate = date, recurrenceUntil = it.endAfter(date))
        }
    }

    /**
     * The routine's end, dropped if the new start has moved past it — the same guard
     * `CreationHubViewModel` applies when creating one.
     *
     * `RecurrenceRule.firesOn` rejects every day before the anchor and every day after `until`, so a
     * pair that has crossed leaves nothing at all: the task saves, syncs, and then appears on no day
     * in Home, Calendar or Activity, with no alarm either. Moving a start is not a way to say "and
     * throw the end away", so the end goes rather than becoming unreachable.
     */
    private fun UiState.Success.endAfter(start: LocalDate): LocalDate? = recurrenceUntil?.takeUnless { it.isBefore(start) }

    private fun deselectDialogDate() {
        updateSuccessState { it.copy(dialogSelectedDate = null) }
    }

    /**
     * Toggles whole-task completion immediately (independent of the Save flow). The detail screen
     * isn't reactive (one-shot [loadTask]) so the flip is optimistic; [originalTask] is kept in sync
     * up-front so a later Save preserves completion, and both are reverted if the write fails.
     */
    private fun toggleComplete() {
        val current = _uiState.value as? UiState.Success ?: return
        val task = originalTask ?: return
        val newValue = !current.isCompleted
        // Not routed through updateSuccessState: completion must never mark the edit form dirty.
        _uiState.update { (it as? UiState.Success)?.copy(isCompleted = newValue) ?: it }
        originalTask = task.copy(isCompleted = newValue)
        viewModelScope.launch {
            try {
                setTaskCompletion(task, completed = newValue)
            } catch (e: IOException) {
                Log.e("DetailsViewModel", "Failed to toggle completion", e)
                originalTask = task
                _uiState.update { (it as? UiState.Success)?.copy(isCompleted = current.isCompleted) ?: it }
                _uiEffect.trySend(UiEffect.ShowToast(R.string.changes_not_saved))
            }
        }
    }

    private fun saveChanges() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return
        if (currentState.isSaving) return
        if (!validateFields(currentState)) return

        val existingTask = originalTask ?: return

        updateSuccessState { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val refreshedPhotoUrls = drainStagedPhotoChanges(currentState)
                val updatedTask = buildUpdatedTask(currentState, existingTask)
                    .copy(photoUrls = refreshedPhotoUrls)
                taskRepository.update(updatedTask)
                reconcileSubtasks(currentState)
                onSaveSuccess(updatedTask)
            } catch (e: IOException) {
                Log.e("EditViewModel", "Failed to save changes", e)
                updateSuccessState { it.copy(isSaving = false) }
                onSaveFailure()
            }
        }
    }

    /**
     * Pushes staged photo deletes and uploads to the server, then returns the authoritative
     * photoUrls list so the subsequent task update reflects current server state. If the task
     * has no remoteId yet (offline-created), uploads are buffered via [pendingPhotoRepository]
     * and the local UI list is used as-is.
     */
    @Suppress("RedundantSuspendModifier") // Detekt false positive: invokes suspend taskRepository.deleteTaskPhoto/uploadTaskPhoto
    private suspend fun drainStagedPhotoChanges(state: UiState.Success): List<String> {
        if (state.taskId > 0) {
            for (photoId in state.pendingPhotoDeleteIds) {
                taskRepository.deleteTaskPhoto(state.taskId, photoId)
                    .onFailure { Log.w("EditViewModel", "delete photo $photoId failed: ${it.message}") }
            }
            for (pending in state.pendingPhotoUploads) {
                taskRepository.uploadTaskPhoto(state.taskId, pending.bytes, pending.mimeType)
                    .onFailure { Log.w("EditViewModel", "upload photo failed: ${it.message}") }
            }
            return taskRepository.fetchRemoteTask(state.taskId).getOrNull()?.photoUrls
                ?: state.photoUrls
        }
        // Task isn't synced yet — queue uploads for the eventual syncCreatedTask drain.
        val localId = currentTaskId
        if (localId != null) {
            for (pending in state.pendingPhotoUploads) {
                pendingPhotoRepository.queue(localId, pending.bytes, pending.mimeType)
            }
            if (state.pendingPhotoUploads.isNotEmpty()) {
                _uiEffect.trySend(UiEffect.ShowToast(R.string.photo_queued_for_sync))
            }
        } else if (state.pendingPhotoUploads.isNotEmpty()) {
            _uiEffect.trySend(UiEffect.ShowToast(R.string.photo_requires_sync))
        }
        return state.photoUrls
    }

    private fun cancelChanges() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        val existingTask = originalTask ?: return

        if (!currentState.isDirty) {
            navigateBack()
            return
        }

        val reverted = successFromTask(existingTask)
        _uiState.value = reverted.copy(isReminderInPast = computeIsReminderInPast(reverted))
        _uiEffect.trySend(UiEffect.ShowToast(R.string.changes_cancelled))
    }

    @Suppress("RedundantSuspendModifier") // Detekt false positive: invokes suspend Channel.send
    private suspend fun onSaveSuccess(updatedTask: Task) {
        originalTask = updatedTask
        updateSuccessState { it.copy(isDirty = false) }
        _uiEffect.send(UiEffect.ShowToast(R.string.changes_saved))
        navigateBack()
    }

    @Suppress("RedundantSuspendModifier") // Detekt false positive: invokes suspend Channel.send
    private suspend fun onSaveFailure() {
        _uiEffect.send(UiEffect.ShowToast(R.string.changes_not_saved))
    }

    private fun validateFields(state: UiState.Success): Boolean {
        val titleError =
            when {
                state.taskTitle.isBlank() -> R.string.error_title_required
                state.taskTitle.length < MIN_TITLE_LENGTH -> R.string.error_title_too_short
                else -> null
            }

        if (titleError != null) {
            updateSuccessState { it.copy(titleError = titleError) }
            return false
        }
        return true
    }

    private fun navigateBack() {
        _navEffect.trySend(NavigationEffect.Back)
    }

    private fun requestBack() {
        val current = _uiState.value as? UiState.Success
        if (current?.isDirty == true) {
            updateSuccessState { it.copy(showDiscardDialog = true) }
        } else {
            navigateBack()
        }
    }

    private fun confirmDiscard() {
        updateSuccessState { it.copy(showDiscardDialog = false) }
        navigateBack()
    }

    private fun buildUpdatedTask(
        current: UiState.Success,
        existingTask: Task,
    ): Task = existingTask.copy(
        title = current.taskTitle,
        description = current.taskDescription.ifBlank { null },
        date = current.taskDate,
        timeStart = current.taskTimeStart ?: existingTask.timeStart,
        timeEnd = current.taskTimeEnd ?: existingTask.timeEnd,
        photoUrls = current.photoUrls,
        locationName = current.locationName,
        locationAddress = current.locationAddress,
        locationLat = current.locationLat,
        locationLng = current.locationLng,
        category = current.selectedCategory,
        customCategoryName = current.customCategoryName.takeIf {
            current.selectedCategory == TaskCategory.OTHER && it.isNotBlank()
        },
        recurrence = current.selectedRecurrence,
        reminderOffsetMinutes = current.reminderOffsetMinutes,
        isAllDay = current.isAllDay,
        reminderTimes = current.reminderTimes,
        recurrenceUntil = current.recurrenceUntil,
        recurrenceInterval = current.recurrenceInterval.coerceAtLeast(1),
        // Dead data on any other frequency, and contentEquals would still diff on it.
        recurrenceByDay = if (current.selectedRecurrence == Recurrence.WEEKLY) current.recurrenceByDay else emptySet(),
    )

    private fun subtaskDraftsChanged(state: UiState.Success, original: Task): Boolean {
        val originalKey = original.subtasks.sortedBy { it.orderIndex }
            .map { Triple(it.id, it.title, it.isCompleted) }
        val currentKey = state.subtaskDrafts.filter { it.title.isNotBlank() }
            .map { Triple(it.id, it.title, it.isCompleted) }
        return originalKey != currentKey
    }

    /**
     * "Would saving change anything?" — asked by building the task Save would actually write and
     * diffing it against the one that was loaded.
     *
     * It calls [buildUpdatedTask] on purpose rather than assembling a second copy() of its own. The
     * two lists had already drifted: the repeat interval, weekdays, end date and reminder times were
     * saved but not compared, so editing any of them left Save greyed out and the change unreachable —
     * a field the user could see, change on screen, and never keep. Deriving one from the other makes
     * that class of bug impossible instead of merely fixed.
     *
     * Photos and steps stay separate because they live outside the Task copy: uploads and deletes are
     * still pending when this runs, and step drafts carry their own identity.
     */
    private fun computeIsDirty(state: UiState.Success): Boolean {
        val original = originalTask ?: return false
        if (state.pendingPhotoUploads.isNotEmpty() || state.pendingPhotoDeleteIds.isNotEmpty()) {
            return true
        }
        if (subtaskDraftsChanged(state, original)) return true
        return buildUpdatedTask(state, original) != original
    }

    private companion object {
        const val MIN_TITLE_LENGTH = 3
    }
}
