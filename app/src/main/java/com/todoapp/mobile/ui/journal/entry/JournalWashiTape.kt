package com.todoapp.mobile.ui.journal.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Golden washi-tape color shared by the journal's polaroid photos and the entry top-bar decorations. */
internal val WashiTapeColor = Color(0xFFFFE082)
internal const val WASHI_TAPE_ALPHA = 0.85f

/**
 * A short, slightly translucent washi-tape strip with a soft shadow — the journal's "taped onto the
 * page" motif. Position it (align / offset / zIndex) via [modifier]; this draws the tilted strip itself.
 */
@Composable
internal fun WashiTapeStrip(
    width: Dp,
    height: Dp,
    rotationDeg: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .rotate(rotationDeg)
            .shadow(elevation = 1.dp)
            .background(WashiTapeColor.copy(alpha = WASHI_TAPE_ALPHA))
            .size(width = width, height = height),
    )
}
