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
 * How far a bounded routine has come: "Day 12 of 30".
 *
 * Shaped like [TDSubtaskProgress] but deliberately NOT green — green is reserved for done/success,
 * and being on day 12 of 30 is neither. Neutral pending ink says "in progress" without claiming an
 * achievement, and it reads the same in every palette kit.
 *
 * [label] is passed in already formatted so this stays free of app strings ( :uikit takes primitives
 * only ); [total] below 1 renders an empty track.
 */
@Composable
fun TDRoutineProgress(
    current: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TDTheme.colors.lightGray),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TDTheme.colors.pendingGray),
                )
            }
        }
        TDText(
            text = label,
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.darkPending,
        )
    }
}

@TDPreview
@Composable
private fun TDRoutineProgressStatesPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TDRoutineProgress(current = 1, total = 30, label = "Day 1 of 30", modifier = Modifier.fillMaxWidth())
            TDRoutineProgress(current = 12, total = 30, label = "Day 12 of 30", modifier = Modifier.fillMaxWidth())
            TDRoutineProgress(current = 30, total = 30, label = "Day 30 of 30", modifier = Modifier.fillMaxWidth())
        }
    }
}
