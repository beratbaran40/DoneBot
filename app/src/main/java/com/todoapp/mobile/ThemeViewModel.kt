package com.todoapp.mobile

import androidx.lifecycle.ViewModel
import com.todoapp.mobile.domain.repository.PaletteRepository
import com.todoapp.mobile.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
@Inject
constructor(
    themeRepository: ThemeRepository,
    paletteRepository: PaletteRepository,
) : ViewModel() {
    val themeFlow = themeRepository.themeFlow
    val paletteFlow = paletteRepository.paletteFlow
}
