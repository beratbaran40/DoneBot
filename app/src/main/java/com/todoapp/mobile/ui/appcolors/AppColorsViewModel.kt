package com.todoapp.mobile.ui.appcolors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.repository.PaletteRepository
import com.todoapp.mobile.ui.appcolors.AppColorsContract.UiAction
import com.todoapp.mobile.ui.appcolors.AppColorsContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppColorsViewModel
@Inject
constructor(
    private val paletteRepository: PaletteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        paletteRepository.paletteFlow
            .onEach { palette -> _uiState.update { it.copy(selected = palette) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnSelectPalette ->
                viewModelScope.launch { paletteRepository.savePalette(action.palette) }
        }
    }
}
