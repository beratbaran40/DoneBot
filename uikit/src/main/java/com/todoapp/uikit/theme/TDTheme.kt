package com.todoapp.uikit.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

private val LocalIsDarkTheme = staticCompositionLocalOf { false }
private val LocalPalette = staticCompositionLocalOf { PaletteKit.ORIGINAL }

object TDTheme {
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    val palette: PaletteKit
        @Composable
        @ReadOnlyComposable
        get() = LocalPalette.current

    val colors: TDColor
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) LocalDarkColors.current else LocalLightColors.current

    val typography: TDTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

@Composable
fun TDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: PaletteKit = PaletteKit.ORIGINAL,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val activity = view.context as? Activity ?: return@LaunchedEffect
            (activity as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle =
                if (darkTheme) {
                    SystemBarStyle.dark(scrim = 0x00000000)
                } else {
                    SystemBarStyle.light(
                        scrim = 0x00000000,
                        darkScrim = 0x00000000,
                    )
                },
                navigationBarStyle =
                if (darkTheme) {
                    SystemBarStyle.dark(scrim = 0x00000000)
                } else {
                    SystemBarStyle.light(
                        scrim = 0x00000000,
                        darkScrim = 0x00000000,
                    )
                },
            )
        }
    }

    val lightColors = when (palette) {
        PaletteKit.ORIGINAL -> defaultLightColors()
        PaletteKit.MONOCHROME -> monochromeLightColors()
    }
    val darkColors = when (palette) {
        PaletteKit.ORIGINAL -> defaultDarkColors()
        PaletteKit.MONOCHROME -> monochromeDarkColors()
    }
    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalPalette provides palette,
        LocalLightColors provides lightColors,
        LocalDarkColors provides darkColors,
        LocalTypography provides TDTheme.typography,
    ) {
        content()
    }
}
