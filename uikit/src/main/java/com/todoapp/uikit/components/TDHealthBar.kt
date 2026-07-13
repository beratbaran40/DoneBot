package com.todoapp.uikit.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

private const val DEFAULT_HEART_COUNT = 10
private const val HALVES_PER_HEART = 2
private const val POP_TARGET_SCALE = 1.3f
private const val POP_RISE_MS = 120
private const val POP_SETTLE_DAMPING = 0.35f
private val DEFAULT_HEART_SIZE = 22.dp
private val HEART_SPACING = 3.dp

/**
 * Minecraft-style health bar: [heartCount] pixel-art hearts rendered from [halfHearts] half-heart
 * units (0..heartCount*2). Each heart shows full / left-half / empty, tinted with [fullColor] /
 * [emptyColor]. A heart gently pops when it gains (its state increases); losses are silent. Set
 * [animate] = false (also auto-off in `LocalInspectionMode`) to render statically. The whole bar
 * exposes a single [contentDescription] for accessibility.
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
    fullColor: Color,
    emptyColor: Color,
    animate: Boolean,
) {
    val animationsEnabled = animate && !LocalInspectionMode.current
    val popScale = remember { Animatable(1f) }
    var previousState by remember { mutableIntStateOf(state) }
    LaunchedEffect(state) {
        if (animationsEnabled && state > previousState) {
            popScale.snapTo(1f)
            popScale.animateTo(POP_TARGET_SCALE, tween(POP_RISE_MS))
            popScale.animateTo(1f, spring(dampingRatio = POP_SETTLE_DAMPING))
        }
        previousState = state
    }
    Box(
        modifier = Modifier
            .size(heartSize)
            .scale(popScale.value),
    ) {
        when (state) {
            HALVES_PER_HEART -> HeartLayer(color = fullColor)
            1 -> {
                HeartLayer(color = emptyColor)
                HeartLayer(color = fullColor, clipToLeftHalf = true)
            }

            else -> HeartLayer(color = emptyColor)
        }
    }
}

@Composable
private fun HeartLayer(
    color: Color,
    clipToLeftHalf: Boolean = false,
) {
    val base = Modifier.fillMaxSize()
    Icon(
        painter = painterResource(R.drawable.ic_heart_pixel),
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
