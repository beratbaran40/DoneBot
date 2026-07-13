package com.todoapp.mobile.ui.appcolors

import com.todoapp.uikit.theme.PaletteKit

object AppColorsContract {
    data class UiState(
        val selected: PaletteKit = PaletteKit.ORIGINAL,
    )

    sealed interface UiAction {
        data class OnSelectPalette(val palette: PaletteKit) : UiAction
    }
}
