package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Equal-width segmented control on a soft rounded track: each segment is a weighted pill and the
 * selected one fills with `background` and lifts on a 2dp shadow (the FilteredTasks tab-row look,
 * shared). Labels stay single-line and ellipsize instead of wrapping, and the horizontal text
 * padding is deliberately tighter (8dp) so the control can share a row with other controls.
 */
@Composable
fun TDSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(TDTheme.colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSegmentSelected(index) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) TDTheme.colors.background else Color.Transparent,
                shadowElevation = if (selected) 2.dp else 0.dp,
            ) {
                TDText(
                    text = label,
                    style = TDTheme.typography.subheading4,
                    color = if (selected) TDTheme.colors.pendingGray else TDTheme.colors.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdSegmentedControlPreview() {
    TDTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            TDSegmentedControl(
                segments = listOf("Tümü", "Bana atanan"),
                selectedIndex = 0,
                onSegmentSelected = {},
            )
            TDSegmentedControl(
                segments = listOf("Tümü", "Bana atanan"),
                selectedIndex = 1,
                onSegmentSelected = {},
            )
            TDSegmentedControl(
                segments = listOf("Günlük", "Haftalık", "Aylık"),
                selectedIndex = 1,
                onSegmentSelected = {},
            )
        }
    }
}
