package com.todoapp.mobile.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.ThemeViewModel
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.uikit.theme.PaletteKit

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ThemedApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themePreference by themeViewModel.themeFlow
        .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM_DEFAULT)
    val palette by themeViewModel.paletteFlow
        .collectAsStateWithLifecycle(initialValue = PaletteKit.ORIGINAL)

    val darkTheme =
        when (themePreference) {
            ThemePreference.DARK_MODE -> true
            ThemePreference.LIGHT_MODE -> false
            ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
        }

    // TDTheme is applied inside DoneBotApp (per-branch: the splash directly, the main UI via
    // ThemeChangeReveal, which swaps the theme through a top-down wipe). Passing the target theme
    // down lets the reveal capture the old frame BEFORE the theme switches — see ThemeChangeReveal.
    DoneBotApp(darkTheme = darkTheme, palette = palette)
}
