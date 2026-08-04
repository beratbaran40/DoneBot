package com.todoapp.uikit.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
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

    val style: TDStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalStyle.current

    val shapes: TDShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalStyle.current.shapes

    val motion: TDMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalStyle.current.motion
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

    // remember: TDColor is a 45-field data class and TDStyle carries the whole non-colour language.
    // Rebuilding them on every recomposition of this root is pure waste, and stable instances are
    // what let the static locals below invalidate exactly once per palette change.
    val lightColors = remember(palette) { palette.colors(dark = false) }
    val darkColors = remember(palette) { palette.colors(dark = true) }
    val style = remember(palette) { palette.style() }
    val typography =
        remember(style) {
            TDTypography(
                fontFamily = style.fontFamily,
                displayFontFamily = style.displayFontFamily,
                minFontSize = style.minFontSize,
            )
        }
    // Catches the handful of bare `Text(...)` / `TextField` sites that never set a style. A kit with
    // no fallback family re-provides the parent value unchanged, so this is a no-op for those kits.
    val parentTextStyle = LocalTextStyle.current
    val kitTextStyle =
        remember(parentTextStyle, style.fallbackFontFamily) {
            style.fallbackFontFamily?.let { parentTextStyle.copy(fontFamily = it) } ?: parentTextStyle
        }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalPalette provides palette,
        LocalLightColors provides lightColors,
        LocalDarkColors provides darkColors,
        // Was `LocalTypography provides TDTheme.typography` — a self-referential identity no-op that
        // re-provided the parent value forever, making typography permanently un-themeable.
        LocalTypography provides typography,
        LocalStyle provides style,
        LocalTextStyle provides kitTextStyle,
    ) {
        content()
    }
}
