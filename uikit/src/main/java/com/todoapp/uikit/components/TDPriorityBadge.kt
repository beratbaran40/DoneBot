package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDPriorityBadge(
    priority: String,
    modifier: Modifier = Modifier,
) {
    val normalized = priority.uppercase()
    val (bg, fg, label) =
        when (normalized) {
            "HIGH" -> Triple(
                TDTheme.colors.lightRed,
                TDTheme.colors.crossRed,
                stringResource(R.string.priority_badge_high),
            )
            "MEDIUM" -> Triple(
                TDTheme.colors.lightOrange,
                TDTheme.colors.orange,
                stringResource(R.string.priority_badge_medium),
            )
            "LOW" -> Triple(
                TDTheme.colors.lightPending,
                TDTheme.colors.darkPending,
                stringResource(R.string.priority_badge_low),
            )
            else -> Triple(TDTheme.colors.lightPending, TDTheme.colors.pendingGray, normalized)
        }
    Box(
        modifier =
        modifier
            .clip(TDTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = fg,
        )
    }
}

@TDPreview
@Composable
private fun TDPriorityBadgeHighPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDPriorityBadge(priority = "HIGH")
        }
    }
}

@TDPreview
@Composable
private fun TDPriorityBadgeMediumPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDPriorityBadge(priority = "MEDIUM")
        }
    }
}

@TDPreview
@Composable
private fun TDPriorityBadgeLowPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDPriorityBadge(priority = "LOW")
        }
    }
}

@TDPreview
@Composable
private fun TDPriorityBadgeUnknownPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDPriorityBadge(priority = "URGENT")
        }
    }
}

@TDPreview
@Composable
private fun TDPriorityBadgeAllPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDPriorityBadge(priority = "HIGH")
            TDPriorityBadge(priority = "MEDIUM")
            TDPriorityBadge(priority = "LOW")
            TDPriorityBadge(priority = "URGENT")
        }
    }
}
