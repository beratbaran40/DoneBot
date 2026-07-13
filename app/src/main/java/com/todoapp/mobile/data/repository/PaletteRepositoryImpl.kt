package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.PaletteRepository
import com.todoapp.uikit.theme.PaletteKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaletteRepositoryImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : PaletteRepository {
    private companion object {
        const val PALETTE_KEY = "palette_kit"
    }

    override val paletteFlow: Flow<PaletteKit> =
        dataStoreHelper
            .getString(PALETTE_KEY, PaletteKit.ORIGINAL.name)
            .map { runCatching { PaletteKit.valueOf(it) }.getOrDefault(PaletteKit.ORIGINAL) }

    override suspend fun savePalette(palette: PaletteKit) {
        dataStoreHelper.saveString(PALETTE_KEY, palette.name)
    }
}
