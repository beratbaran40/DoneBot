package com.todoapp.mobile.ui.journal

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.model.JournalMood

object JournalContract {
    enum class DateGroup { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

    @Immutable
    data class GroupedSection(
        val group: DateGroup,
        val entries: List<JournalEntry>,
    )

    sealed interface UiState {
        data object Loading : UiState

        data object Locked : UiState

        @Immutable
        data class Success(
            val sections: List<GroupedSection>,
            val searchQuery: String,
            val activeMoodFilter: JournalMood?,
            val isRawListEmpty: Boolean,
            val isFilteredEmpty: Boolean,
            val actionSheetEntry: JournalEntry? = null,
            val pendingDeleteEntry: JournalEntry? = null,
        ) : UiState

        data class Error(
            @StringRes val messageRes: Int,
        ) : UiState
    }

    sealed interface UiAction {
        data object OnRetry : UiAction

        data object OnAddClick : UiAction

        data class OnEntryClick(val id: Long) : UiAction

        data class OnEntryLongPress(val id: Long) : UiAction

        data object OnDismissActionSheet : UiAction

        data class OnQuickMoodChange(val mood: JournalMood?) : UiAction

        data object OnEditFromSheet : UiAction

        data object OnRequestDeleteFromSheet : UiAction

        data object OnConfirmDelete : UiAction

        data object OnDismissDelete : UiAction

        data class OnSearchQueryChange(val query: String) : UiAction

        data class OnMoodFilterChange(val mood: JournalMood?) : UiAction

        data object OnClearFilters : UiAction

        data object OnBiometricSuccess : UiAction

        data object OnBiometricCancelled : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(@StringRes val messageRes: Int) : UiEffect

        data object ShowBiometricAuthenticator : UiEffect
    }
}
