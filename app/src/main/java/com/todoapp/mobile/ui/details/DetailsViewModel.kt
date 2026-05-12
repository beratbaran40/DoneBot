package com.todoapp.mobile.ui.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.ui.details.DetailsContract.UiAction
import com.todoapp.mobile.ui.details.DetailsContract.UiEffect
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import com.todoapp.mobile.ui.home.PendingPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        currentTaskId = savedStateHandle["taskId"]
        loadTask(currentTaskId!!)
    }

    fun loadTask(taskId: Long) {
        currentTaskId = taskId
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val task = taskRepository.getTaskById(taskId)
                if (task == null) {
                    _uiState.value = UiState.Error(message = R.string.error_task_not_found.toString())
                    return@launch
                }
                originalTask = task
                val initial = UiState.Success(
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
                )
                _uiState.value = initial.copy(isReminderInPast = computeIsReminderInPast(initial))
                // Photos live server-side; fetch authoritative list via remoteId
                task.remoteId?.let { remoteId ->
                    taskRepository.fetchRemoteTask(remoteId).onSuccess { remote ->
                        updateSuccessState { it.copy(photoUrls = remote.photoUrls) }
                    }
                }
            } catch (e: IOException) {
                _uiState.value = UiState.Error(message = "Failed to load task", throwable = e)
            }
        }
    }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            UiAction.OnBackClick -> navigateBack()
            UiAction.OnCancelClick -> cancelChanges()
            UiAction.OnSaveChanges -> saveChanges()
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
            is UiAction.OnAllDayChange -> changeAllDay(uiAction.isAllDay)
        }
    }

    private fun changeCategory(category: TaskCategory) {
        updateSuccessState { state ->
            // BIRTHDAY auto-defaults to YEARLY when the user hasn't picked one;
            // moving off BIRTHDAY reverts that auto-set so the explainer doesn't linger.
            val nextRecurrence = when {
                category == TaskCategory.BIRTHDAY && state.selectedRecurrence == Recurrence.NONE ->
                    Recurrence.YEARLY

                state.selectedCategory == TaskCategory.BIRTHDAY &&
                    category != TaskCategory.BIRTHDAY &&
                    state.selectedRecurrence == Recurrence.YEARLY ->
                    Recurrence.NONE

                else -> state.selectedRecurrence
            }
            state.copy(
                selectedCategory = category,
                selectedRecurrence = nextRecurrence,
                customCategoryName = if (category == TaskCategory.OTHER) state.customCategoryName else "",
            )
        }
    }

    private fun changeCustomCategoryName(name: String) {
        updateSuccessState { it.copy(customCategoryName = name) }
    }

    private fun changeRecurrence(recurrence: Recurrence) {
        updateSuccessState { it.copy(selectedRecurrence = recurrence) }
    }

    private fun changeReminderOffset(minutes: Long?) {
        updateSuccessState { it.copy(reminderOffsetMinutes = minutes) }
    }

    private fun changeAllDay(isAllDay: Boolean) {
        updateSuccessState { it.copy(isAllDay = isAllDay) }
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
        updateSuccessState { it.copy(taskDate = date) }
    }

    private fun updateTimeStart(time: LocalTime) {
        updateSuccessState { it.copy(taskTimeStart = time) }
    }

    private fun updateTimeEnd(time: LocalTime) {
        updateSuccessState { it.copy(taskTimeEnd = time) }
    }

    private fun selectDialogDate(date: LocalDate) {
        updateSuccessState {
            it.copy(dialogSelectedDate = date, taskDate = date)
        }
    }

    private fun deselectDialogDate() {
        updateSuccessState { it.copy(dialogSelectedDate = null) }
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

        val reverted = UiState.Success(
            taskId = existingTask.remoteId ?: -1L,
            taskTitle = existingTask.title,
            titleError = null,
            taskTimeStart = existingTask.timeStart,
            taskTimeEnd = existingTask.timeEnd,
            taskDate = existingTask.date,
            taskDescription = existingTask.description.orEmpty(),
            dialogSelectedDate = existingTask.date,
            isDirty = false,
            isSaving = false,
            photoUrls = existingTask.photoUrls,
            pendingPhotoUploads = emptyList(),
            pendingPhotoDeleteIds = emptySet(),
            locationName = existingTask.locationName,
            locationAddress = existingTask.locationAddress,
            locationLat = existingTask.locationLat,
            locationLng = existingTask.locationLng,
            selectedCategory = existingTask.category,
            customCategoryName = existingTask.customCategoryName.orEmpty(),
            selectedRecurrence = existingTask.recurrence,
            reminderOffsetMinutes = existingTask.reminderOffsetMinutes,
            isAllDay = existingTask.isAllDay,
        )
        _uiState.value = reverted.copy(isReminderInPast = computeIsReminderInPast(reverted))
        _uiEffect.trySend(UiEffect.ShowToast(R.string.changes_cancelled))
    }

    private suspend fun onSaveSuccess(updatedTask: Task) {
        originalTask = updatedTask
        updateSuccessState { it.copy(isDirty = false) }
        _uiEffect.send(UiEffect.ShowToast(R.string.changes_saved))
        navigateBack()
    }

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
    )

    private fun computeIsDirty(state: UiState.Success): Boolean {
        val original = originalTask ?: return false
        if (state.pendingPhotoUploads.isNotEmpty() || state.pendingPhotoDeleteIds.isNotEmpty()) {
            return true
        }
        val candidateTask =
            original.copy(
                title = state.taskTitle,
                description = state.taskDescription.ifBlank { null },
                date = state.taskDate,
                timeStart = state.taskTimeStart ?: original.timeStart,
                timeEnd = state.taskTimeEnd ?: original.timeEnd,
                photoUrls = state.photoUrls,
                locationName = state.locationName,
                locationAddress = state.locationAddress,
                locationLat = state.locationLat,
                locationLng = state.locationLng,
                category = state.selectedCategory,
                customCategoryName = state.customCategoryName.takeIf {
                    state.selectedCategory == TaskCategory.OTHER && it.isNotBlank()
                },
                recurrence = state.selectedRecurrence,
                reminderOffsetMinutes = state.reminderOffsetMinutes,
                isAllDay = state.isAllDay,
            )
        return candidateTask != original
    }

    private companion object {
        const val MIN_TITLE_LENGTH = 3
    }
}
