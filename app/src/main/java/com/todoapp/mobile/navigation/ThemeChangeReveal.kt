package com.todoapp.mobile.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.todoapp.mobile.ui.common.LocalReduceMotion
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.style

/**
 * Applies [TDTheme] and plays a top-down "curtain" wipe whenever the theme (dark/light) or palette
 * (ORIGINAL/MONOCHROME) target changes — as if a curtain of the new theme descends over the screen.
 *
 * Driven by a **displayed-vs-target split**: the content is always rendered with the currently
 * *displayed* theme, while [targetDark]/[targetPalette] are the desired theme. On a change the old
 * frame is captured FIRST — while the displayed theme is still the old one — so the capture is
 * race-free and covers the WHOLE screen (not just the top bar, which was the earlier bug: the live
 * theme switched before the snapshot, so most of the frame was captured already-new). Only after the
 * capture is `displayed` swapped to the target, so the new theme renders live underneath, and then
 * the old snapshot is wiped away from the top down at full width via [clipRect].
 *
 * - **Reduce-motion:** when [LocalReduceMotion] is on the theme swaps instantly (no capture/anim).
 * - **No launch flash:** `displayed` starts equal to the target and this host mounts after the
 *   splash (past [DoneBotApp]'s early return), so the persisted theme has already settled — the first
 *   composition finds target == displayed and never animates.
 */
@Composable
fun ThemeChangeReveal(
    targetDark: Boolean,
    targetPalette: PaletteKit,
    content: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    var displayed by remember { mutableStateOf(targetDark to targetPalette) }
    val graphicsLayer = rememberGraphicsLayer()
    var overlay by remember { mutableStateOf<ImageBitmap?>(null) }
    val sweep = remember { Animatable(0f) }
    var sweepHeight by remember { mutableFloatStateOf(0f) }

    TDTheme(darkTheme = displayed.first, palette = displayed.second) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { sweepHeight = it.height.toFloat() }
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                    val snapshot = overlay ?: return@drawWithContent
                    // Old frame kept only BELOW the descending edge; the new theme (live under the
                    // layer) is revealed top-down at full width, like a curtain coming down.
                    clipRect(top = sweep.value) { drawImage(snapshot) }
                },
        ) {
            content()
        }
    }

    LaunchedEffect(targetDark, targetPalette) {
        val target = targetDark to targetPalette
        if (target == displayed) return@LaunchedEffect
        if (reduceMotion || sweepHeight <= 0f) {
            displayed = target
            return@LaunchedEffect
        }
        // Capture the OLD frame while it is still on screen (displayed unchanged) — race-free — then
        // swap the theme underneath and wipe the snapshot away from the top down.
        val snapshot = graphicsLayer.toImageBitmap()
        sweep.snapTo(0f)
        overlay = snapshot
        displayed = target
        try {
            sweep.animateTo(
                targetValue = sweepHeight,
                // The wipe reveals the NEW theme, so it animates in the target kit's motion language
                // (linear for the rounded kits — unchanged — stepped for 8-Bit). Resolved from the kit
                // directly rather than from `TDTheme.motion`: this effect runs OUTSIDE the TDTheme it
                // hosts, so it has no composition locals.
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION_MS,
                    easing = targetPalette.style().motion.revealEasing,
                ),
            )
        } finally {
            overlay = null
            snapshot.asAndroidBitmap().recycle()
        }
    }
}

private const val REVEAL_DURATION_MS = 650
