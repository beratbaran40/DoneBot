package com.todoapp.mobile.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.LocalWindowSizeClass

/** Default max width for a single centred content column on Medium/Expanded width screens. */
val ResponsiveContentMaxWidth: Dp = 720.dp

/**
 * Caps and centres [content] on large screens (tablets / foldables / landscape) so a single column
 * does not stretch edge-to-edge and become hard to read. On Compact width (phones) it is a no-op:
 * the content fills the available width exactly as before.
 *
 * Width class is read from [LocalWindowSizeClass]. Apply per screen at the NavGraph call site; skip
 * it for full-bleed surfaces (camera, crop, web view) and screens that manage their own
 * landscape/portrait layout (auth, pomodoro timer).
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ResponsiveContentMaxWidth,
    content: @Composable () -> Unit,
) {
    val isCompactWidth =
        LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact
    val contentModifier =
        if (isCompactWidth) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidth)
        }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = contentModifier) {
            content()
        }
    }
}
