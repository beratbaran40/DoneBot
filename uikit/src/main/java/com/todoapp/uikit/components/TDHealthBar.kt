package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDTheme

private const val DEFAULT_HEART_COUNT = 10
private const val HALVES_PER_HEART = 2
private const val POP_TARGET_SCALE = 1.3f
private const val POP_RISE_MS = 120
private const val POP_SETTLE_DAMPING = 0.35f
private const val POP_STEPPED_SETTLE_MS = 180
private val DEFAULT_HEART_SIZE = 22.dp
private val HEART_SPACING = 3.dp

/**
 * Health bar: [heartCount] hearts rendered from [halfHearts] half-heart
 * units (0..heartCount*2). Each heart shows full / left-half / empty, tinted with [fullColor] /
 * [emptyColor]. A heart gently pops when it gains (its state increases); losses are silent. Set
 * [animate] = false (also auto-off in `LocalInspectionMode`) to render statically. The whole bar
 * exposes a single [contentDescription] for accessibility.
 *
 * The heart ART follows the active kit: a pixel kit gets the blocky Minecraft-HUD silhouette, every
 * other kit gets the smooth heart. The pixel shape predates the 8-Bit kit and used to be the only
 * option, which left a chunky sprite sitting inside otherwise-rounded chrome.
 */
@Composable
fun TDHealthBar(
    halfHearts: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    heartCount: Int = DEFAULT_HEART_COUNT,
    heartSize: Dp = DEFAULT_HEART_SIZE,
    fullColor: Color = TDTheme.colors.heartFull,
    emptyColor: Color = TDTheme.colors.heartEmpty,
    animate: Boolean = true,
) {
    val heartRes = when (TDTheme.shapes.cornerStyle) {
        TDCornerStyle.PIXEL -> R.drawable.ic_heart_pixel
        TDCornerStyle.ROUNDED -> R.drawable.ic_heart
    }
    FlowRow(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(HEART_SPACING),
        verticalArrangement = Arrangement.spacedBy(HEART_SPACING),
    ) {
        repeat(heartCount) { index ->
            val heartState = (halfHearts - HALVES_PER_HEART * index).coerceIn(0, HALVES_PER_HEART)
            Heart(
                state = heartState,
                heartSize = heartSize,
                heartRes = heartRes,
                fullColor = fullColor,
                emptyColor = emptyColor,
                animate = animate,
            )
        }
    }
}

@Composable
private fun Heart(
    state: Int,
    heartSize: Dp,
    @DrawableRes heartRes: Int,
    fullColor: Color,
    emptyColor: Color,
    animate: Boolean,
) {
    val animationsEnabled = animate && !LocalInspectionMode.current
    val popScale = remember { Animatable(1f) }
    var previousState by remember { mutableIntStateOf(state) }
    // A spring settle overshoots on a smooth curve — the one motion an 8-bit sprite never makes.
    // A stepped kit lands the pop in discrete frames instead; every other kit keeps the spring.
    val stepped = TDTheme.motion.stepped
    val settleEasing = TDTheme.motion.emphasizedEasing
    LaunchedEffect(state) {
        if (animationsEnabled && state > previousState) {
            popScale.snapTo(1f)
            popScale.animateTo(POP_TARGET_SCALE, tween(POP_RISE_MS))
            if (stepped) {
                popScale.animateTo(1f, tween(POP_STEPPED_SETTLE_MS, easing = settleEasing))
            } else {
                popScale.animateTo(1f, spring(dampingRatio = POP_SETTLE_DAMPING))
            }
        }
        previousState = state
    }
    Box(
        modifier = Modifier
            .size(heartSize)
            .scale(popScale.value),
    ) {
        when (state) {
            HALVES_PER_HEART -> HeartLayer(heartRes = heartRes, color = fullColor)
            1 -> {
                HeartLayer(heartRes = heartRes, color = emptyColor)
                HeartLayer(heartRes = heartRes, color = fullColor, clipToLeftHalf = true)
            }

            else -> HeartLayer(heartRes = heartRes, color = emptyColor)
        }
    }
}

@Composable
private fun HeartLayer(
    @DrawableRes heartRes: Int,
    color: Color,
    clipToLeftHalf: Boolean = false,
) {
    val base = Modifier.fillMaxSize()
    Icon(
        painter = tdPainter(heartRes),
        contentDescription = null,
        tint = color,
        modifier = if (clipToLeftHalf) {
            base.drawWithContent {
                clipRect(right = size.width / 2f) {
                    this@drawWithContent.drawContent()
                }
            }
        } else {
            base
        },
    )
}

@TDPreview
@Composable
private fun TDHealthBarPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDHealthBar(halfHearts = 20, contentDescription = "20 / 20")
            TDHealthBar(halfHearts = 15, contentDescription = "15 / 20")
            TDHealthBar(halfHearts = 1, contentDescription = "1 / 20")
            TDHealthBar(halfHearts = 0, contentDescription = "0 / 20")
        }
    }
}
