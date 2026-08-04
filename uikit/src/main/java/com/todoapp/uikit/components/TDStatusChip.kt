@file:Suppress("MatchingDeclarationName")

package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

enum class TDStatusChipTone { Success, Neutral, Danger }

@Composable
fun TDStatusChip(
    tone: TDStatusChipTone,
    label: String,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) =
        when (tone) {
            TDStatusChipTone.Success -> TDTheme.colors.lightGreen to TDTheme.colors.darkGreen
            TDStatusChipTone.Neutral -> TDTheme.colors.bgColorPurple to TDTheme.colors.darkPending
            TDStatusChipTone.Danger -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
        }
    Row(
        modifier =
        modifier
            .clip(TDTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(color = fg)
        TDText(
            text = label,
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier =
        Modifier
            .size(6.dp)
            .clip(TDTheme.shapes.circle)
            .background(color),
    )
}

@TDPreview
@Composable
private fun TDStatusChipSuccessPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDStatusChip(tone = TDStatusChipTone.Success, label = "Completed")
        }
    }
}

@TDPreview
@Composable
private fun TDStatusChipNeutralPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDStatusChip(tone = TDStatusChipTone.Neutral, label = "Pending")
        }
    }
}

@TDPreview
@Composable
private fun TDStatusChipDangerPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDStatusChip(tone = TDStatusChipTone.Danger, label = "Overdue")
        }
    }
}

@TDPreview
@Composable
private fun TDStatusChipAllTonesPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDStatusChip(tone = TDStatusChipTone.Success, label = "Completed")
            TDStatusChip(tone = TDStatusChipTone.Neutral, label = "Pending")
            TDStatusChip(tone = TDStatusChipTone.Danger, label = "Overdue")
        }
    }
}
