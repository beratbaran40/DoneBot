package com.todoapp.uikit.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TDUndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Int = 5_000,
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = SystemClock.elapsedRealtime()
        while (isActive) {
            delay(16L)
            val elapsed = SystemClock.elapsedRealtime() - startTime
            progress = (elapsed.toFloat() / autoDismissMillis.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) break
        }
    }

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 520.dp),
        shape = tdCorner(22.dp),
        tonalElevation = 6.dp,
        color = TDTheme.colors.onBackground,
        contentColor = TDTheme.colors.background,
    ) {
        Column {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(TDTheme.colors.background.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(3.dp)
                        .background(TDTheme.colors.crossRed),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = tdPainter(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = TDTheme.colors.crossRed,
                    modifier = Modifier.size(20.dp),
                )
                TDText(
                    text = message,
                    style = TDTheme.typography.heading7,
                    color = TDTheme.colors.background,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onUndo) {
                    TDText(
                        text = stringResource(R.string.undo),
                        style = TDTheme.typography.heading7,
                        color = TDTheme.colors.purple,
                    )
                }
            }
        }
    }
}

@TDPreview
@Composable
private fun TDUndoSnackbarPreview() {
    TDTheme {
        TDUndoSnackbar(
            message = "Task deleted",
            onUndo = {},
        )
    }
}

@TDPreview
@Composable
private fun TDUndoSnackbarLongMessagePreview() {
    TDTheme {
        TDUndoSnackbar(
            message = "Quarterly financial review task moved to archive",
            onUndo = {},
        )
    }
}
