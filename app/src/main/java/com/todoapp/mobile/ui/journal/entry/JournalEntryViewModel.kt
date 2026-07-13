package com.todoapp.mobile.ui.journal.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.data.storage.JournalPhotoStorage
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiAction
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiEffect
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class JournalEntryViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository,
    private val photoStorage: JournalPhotoStorage,
) : ViewModel() {
    private val entryId: Long = savedStateHandle.toRoute<Screen.JournalEntry>().entryId

    private val _uiState = MutableStateFlow<UiState>(
        if (entryId == 0L) emptyEditing() else UiState.Loading,
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    /**
     * Paths copied to disk during this editor session. If the user discards (back with
     * nothing meaningful) we delete these so storage isn't leaked.
     */
    private val sessionAddedPaths = mutableSetOf<String>()

    init {
        if (entryId != 0L) loadEntry()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnContentChange -> updateEditing { it.copy(content = action.value, isDirty = true) }
            is UiAction.OnPhotoPicked -> handlePhotoPicked(action.uri)
            UiAction.OnPolaroidCameraClicked -> _navEffect.trySend(NavigationEffect.Navigate(Screen.PolaroidCamera))
            is UiAction.OnPhotoCapturedFromCamera -> handlePhotoFromCamera(action.path)
            is UiAction.OnPhotoRemove -> handlePhotoRemove(action.path)
            is UiAction.OnPhotoTap -> updateEditing { it.copy(fullscreenPath = action.path) }
            UiAction.OnDismissFullscreen -> updateEditing { it.copy(fullscreenPath = null) }
            UiAction.OnBackPress -> handleBackPress()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = runCatching { journalRepository.getEntry(entryId) }.getOrNull()
            if (entry == null) {
                _uiState.value = UiState.Error(R.string.journal_entry_load_error)
            } else {
                _uiState.value = entry.toEditingState()
            }
        }
    }

    private fun handlePhotoPicked(uri: android.net.Uri) {
        viewModelScope.launch {
            val savedPath = photoStorage.savePhoto(uri)
            if (savedPath == null) {
                _uiEffect.trySend(UiEffect.ShowToast(R.string.journal_entry_save_error))
                return@launch
            }
            sessionAddedPaths += savedPath
            updateEditing { state ->
                state.copy(photoPaths = state.photoPaths + savedPath, isDirty = true)
            }
        }
    }

    private fun handlePhotoFromCamera(path: String) {
        // The Polaroid camera already wrote the JPEG to journal storage; the path is final, so
        // (unlike handlePhotoPicked) there is no copy step — just track and attach it.
        sessionAddedPaths += path
        updateEditing { state ->
            state.copy(photoPaths = state.photoPaths + path, isDirty = true)
        }
    }

    private fun handlePhotoRemove(path: String) {
        viewModelScope.launch {
            // Photos added in this session can be removed from disk immediately; saved photos
            // remain on disk until save (the repo handles the final cleanup).
            if (sessionAddedPaths.remove(path)) {
                photoStorage.deletePhoto(path)
            }
            updateEditing { state ->
                state.copy(
                    photoPaths = state.photoPaths.filterNot { it == path },
                    isDirty = true,
                )
            }
        }
    }

    private fun handleBackPress() {
        val editing = (_uiState.value as? UiState.Editing) ?: run {
            _navEffect.trySend(NavigationEffect.Back)
            return
        }
        viewModelScope.launch {
            if (editing.isDirty && editing.isMeaningful()) {
                persistEntry(editing)
            } else if (sessionAddedPaths.isNotEmpty()) {
                // Empty/meaningless entry — clean up any photos copied during this session.
                sessionAddedPaths.forEach { photoStorage.deletePhoto(it) }
                sessionAddedPaths.clear()
            }
            _navEffect.trySend(NavigationEffect.Back)
        }
    }

    private suspend fun persistEntry(editing: UiState.Editing) {
        val now = System.currentTimeMillis()
        val derivedTitle = editing.content
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.take(MAX_TITLE_LEN)
            .orEmpty()
        val entry = JournalEntry(
            id = editing.entryId,
            title = derivedTitle,
            content = editing.content.trim(),
            photoPaths = editing.photoPaths.toList(),
            createdAt = editing.createdAt ?: now,
            updatedAt = now,
        )
        runCatching { journalRepository.upsertEntry(entry) }
            .onSuccess { sessionAddedPaths.clear() }
            .onFailure { throwable ->
                Timber.tag(TAG).e(throwable, "Auto-save failed on back press")
            }
    }

    private fun updateEditing(transform: (UiState.Editing) -> UiState.Editing) {
        _uiState.update { current ->
            if (current is UiState.Editing) transform(current) else current
        }
    }

    private fun emptyEditing(): UiState.Editing = UiState.Editing(
        entryId = 0L,
        content = "",
        photoPaths = emptyList(),
        createdAt = null,
        isDirty = false,
    )

    private fun JournalEntry.toEditingState(): UiState.Editing = UiState.Editing(
        entryId = id,
        content = content,
        photoPaths = photoPaths,
        createdAt = createdAt,
        isDirty = false,
    )

    private companion object {
        const val TAG = "JournalEntryViewModel"
        const val MAX_TITLE_LEN = 80
    }
}
