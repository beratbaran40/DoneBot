package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Slim X/Y progress for a staged task: `lightGray` track, `mediumGreen` fill, `darkGreen` "3/5" label.
 * [total] is expected to be >= 1 (a task with no steps is not staged); a non-positive total renders an
 * empty track.
 */
@Composable
fun TDSubtaskProgress(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TDTheme.colors.lightGray),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TDTheme.colors.mediumGreen),
                )
            }
        }
        TDText(
            text = "$completed/$total",
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.darkGreen,
        )
    }
}

@TDPreview
@Composable
private fun TDSubtaskProgressStatesPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TDSubtaskProgress(completed = 0, total = 5, modifier = Modifier.fillMaxWidth())
            TDSubtaskProgress(completed = 2, total = 5, modifier = Modifier.fillMaxWidth())
            TDSubtaskProgress(completed = 5, total = 5, modifier = Modifier.fillMaxWidth())
        }
    }
}
