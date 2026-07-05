package com.todoapp.mobile.ui.blockedusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.repository.BlockedUsersPreferences
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.BlockedUserUiItem
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.UiAction
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedUsersViewModel
@Inject
constructor(
    private val blockedUsersPreferences: BlockedUsersPreferences,
) : ViewModel() {
    val uiState: StateFlow<UiState> =
        blockedUsersPreferences
            .observeBlocked()
            .map { blocked ->
                UiState.Success(
                    blocked
                        .map { (id, name) -> BlockedUserUiItem(id, name, initialsOf(name)) }
                        .sortedBy { it.displayName.lowercase() },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnUnblock -> viewModelScope.launch { blockedUsersPreferences.unblock(action.userId) }
        }
    }

    private fun initialsOf(name: String): String = name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
