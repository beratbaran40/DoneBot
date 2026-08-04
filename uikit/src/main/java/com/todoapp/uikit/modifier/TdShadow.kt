package com.todoapp.uikit.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.TDElevationStyle
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

/**
 * Palette-aware elevation, a drop-in replacement for [neumorphicShadow] with an identical signature.
 *
 * Kits whose elevation style is [TDElevationStyle.SOFT] get the call forwarded unchanged — so
 * ORIGINAL and MONOCHROME render exactly as before — while a hard-elevation kit gets a zero-blur
 * offset block instead. Migrating a call site is a literal rename.
 *
 * The hard branch draws the shadow only, never a fill or an outline: like [neumorphicShadow] it sits
 * behind whatever background the caller paints afterwards.
 *
 * It draws in `colors.black`, NOT `onBackground` — a hard shadow is ink in every mode. `onBackground`
 * inverts to near-white in dark, which would render a solid white block behind each card and read as
 * a double image. Against a dark ground the block is nearly invisible by design; there the 2dp
 * outline carries the elevation.
 */
@Composable
fun Modifier.tdShadow(
    lightShadow: Color,
    darkShadow: Color,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
): Modifier = when (TDTheme.style.elevationStyle) {
    TDElevationStyle.SOFT -> neumorphicShadow(lightShadow, darkShadow, cornerRadius, elevation)
    TDElevationStyle.HARD ->
        hardShadow(
            color = TDTheme.colors.black,
            shape = tdCorner(cornerRadius),
            offset = TDTheme.style.hardShadowOffset,
        )
}

/**
 * Palette-aware drop-in for `Modifier.shadow(...)`.
 *
 * The hard branch must **not** delegate to `Modifier.shadow`: a pixel kit's corner is an
 * `Outline.Generic`, which cannot drive a platform elevation shadow below API 29 (`:uikit` minSdk is
 * 24), so it would silently render nothing. It draws its own offset block instead.
 */
@Composable
fun Modifier.tdDropShadow(
    elevation: Dp,
    shape: Shape,
    ambientColor: Color,
    spotColor: Color = ambientColor,
): Modifier = when (TDTheme.style.elevationStyle) {
    TDElevationStyle.SOFT ->
        shadow(elevation, shape, ambientColor = ambientColor, spotColor = spotColor)
    TDElevationStyle.HARD ->
        hardShadow(
            color = TDTheme.colors.black,
            shape = shape,
            offset = TDTheme.style.hardShadowOffset,
        )
}
