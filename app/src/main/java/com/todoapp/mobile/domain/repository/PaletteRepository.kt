package com.todoapp.mobile.domain.repository

import com.todoapp.uikit.theme.PaletteKit
import kotlinx.coroutines.flow.Flow

/** Persists the user's selected app color palette ("kit"). Default is [PaletteKit.ORIGINAL]. */
interface PaletteRepository {
    val paletteFlow: Flow<PaletteKit>

    suspend fun savePalette(palette: PaletteKit)
}
