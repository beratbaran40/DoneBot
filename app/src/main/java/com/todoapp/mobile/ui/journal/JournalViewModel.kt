package com.todoapp.mobile.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.repository.JournalBiometricPreferences
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.journal.JournalContract.UiAction
import com.todoapp.mobile.ui.journal.JournalContract.UiEffect
import com.todoapp.mobile.ui.journal.JournalContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class JournalViewModel
@Inject
constructor(
    private val journalRepository: JournalRepository,
    private val journalBiometricPreferences: JournalBiometricPreferences,
    private val clock: Clock,
) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect by lazy { _uiEffect.receiveAsFlow() }

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    private var observeJob: Job? = null

    init {
        gateAndObserve()
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnRetry -> {
                _uiState.value = UiState.Loading
                observeJournal()
            }
            UiAction.OnAddClick ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.JournalEntry(entryId = 0L)))

            is UiAction.OnEntryClick ->
                _navEffect.trySend(NavigationEffect.Navigate(Screen.JournalEntry(entryId = action.id)))

            is UiAction.OnEntryLongPress -> openActionSheet(action.id)
            UiAction.OnDismissActionSheet -> updateSuccess { it.copy(actionSheetEntry = null) }
            UiAction.OnEditFromSheet -> handleEditFromSheet()
            UiAction.OnRequestDeleteFromSheet -> handleRequestDeleteFromSheet()
            UiAction.OnConfirmDelete -> handleConfirmDelete()
            UiAction.OnDismissDelete -> updateSuccess { it.copy(pendingDeleteEntry = null) }

            is UiAction.OnSearchQueryChange -> {
                searchQueryFlow.value = action.query
                updateSuccess { it.copy(searchQuery = action.query) }
            }
            UiAction.OnClearFilters -> {
                searchQueryFlow.value = ""
                updateSuccess { it.copy(searchQuery = "") }
            }

            UiAction.OnBiometricSuccess -> {
                _uiState.value = UiState.Loading
                observeJournal()
            }
            UiAction.OnBiometricCancelled -> _navEffect.trySend(NavigationEffect.Back)
        }
    }

    private fun gateAndObserve() {
        viewModelScope.launch {
            val protected = journalBiometricPreferences.get()
            if (protected) {
                _uiState.value = UiState.Locked
                _uiEffect.trySend(UiEffect.ShowBiometricAuthenticator)
            } else {
                observeJournal()
            }
        }
    }

    private fun observeJournal() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                journalRepository.observeEntries(),
                searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS),
            ) { entries, query ->
                entries to query
            }.catch { throwable ->
                Timber.tag(TAG).e(throwable, "Failed to observe journal entries")
                _uiState.value = UiState.Error(R.string.journal_load_error)
            }.collect { (entries, query) ->
                _uiState.update { current ->
                    val preserved = current as? UiState.Success
                    buildSuccessState(
                        entries = entries,
                        query = query,
                    ).copy(
                        searchQuery = preserved?.searchQuery ?: query,
                        actionSheetEntry = preserved?.actionSheetEntry,
                        pendingDeleteEntry = preserved?.pendingDeleteEntry,
                    )
                }
            }
        }
    }

    private fun buildSuccessState(
        entries: List<JournalEntry>,
        query: String,
    ): UiState.Success {
        val filtered = if (query.isBlank()) {
            entries
        } else {
            val needle = query.trim()
            entries.filter { entry ->
                entry.title.contains(needle, ignoreCase = true) ||
                    entry.content.contains(needle, ignoreCase = true)
            }
        }
        return UiState.Success(
            sections = groupEntries(entries = filtered, today = LocalDate.now(clock)),
            searchQuery = query,
            isRawListEmpty = entries.isEmpty(),
            isFilteredEmpty = filtered.isEmpty(),
        )
    }

    private fun findEntry(id: Long): JournalEntry? = (_uiState.value as? UiState.Success)
        ?.sections
        ?.flatMap { it.entries }
        ?.firstOrNull { it.id == id }

    private fun openActionSheet(id: Long) {
        val entry = findEntry(id) ?: return
        updateSuccess { it.copy(actionSheetEntry = entry) }
    }

    private fun handleEditFromSheet() {
        val sheetEntry = (_uiState.value as? UiState.Success)?.actionSheetEntry ?: return
        updateSuccess { it.copy(actionSheetEntry = null) }
        _navEffect.trySend(NavigationEffect.Navigate(Screen.JournalEntry(entryId = sheetEntry.id)))
    }

    private fun handleRequestDeleteFromSheet() {
        val sheetEntry = (_uiState.value as? UiState.Success)?.actionSheetEntry ?: return
        updateSuccess { it.copy(actionSheetEntry = null, pendingDeleteEntry = sheetEntry) }
    }

    private fun handleConfirmDelete() {
        val target = (_uiState.value as? UiState.Success)?.pendingDeleteEntry ?: return
        updateSuccess { it.copy(pendingDeleteEntry = null) }
        viewModelScope.launch {
            runCatching { journalRepository.deleteEntry(target.id) }
                .onSuccess { _uiEffect.trySend(UiEffect.ShowToast(R.string.journal_entry_deleted)) }
                .onFailure { throwable ->
                    Timber.tag(TAG).e(throwable, "Failed to delete entry")
                    _uiEffect.trySend(UiEffect.ShowToast(R.string.journal_entry_save_error))
                }
        }
    }

    private fun updateSuccess(transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { current -> if (current is UiState.Success) transform(current) else current }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val TAG = "JournalViewModel"
    }
}
