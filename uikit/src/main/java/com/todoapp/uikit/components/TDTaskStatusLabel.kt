package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewForm
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDTaskStatusLabel(
    modifier: Modifier = Modifier,
    isCompleted: Boolean,
) {
    val circleColor = if (isCompleted) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray
    val iconTint = if (isCompleted) TDTheme.colors.darkGreen else TDTheme.colors.darkPending

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        modifier
            .size(36.dp)
            .border(
                width = 1.5.dp,
                color = circleColor,
                shape = CircleShape,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(circleColor),
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter =
                tdPainter(
                    if (isCompleted) R.drawable.ic_check_svg else R.drawable.ic_sand_clock,
                ),
                contentDescription = null,
                tint = iconTint,
            )
        }
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelCompletedPreview() {
    TDTheme {
        TDTaskStatusLabel(isCompleted = true)
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelPendingPreview() {
    TDTheme {
        TDTaskStatusLabel(isCompleted = false)
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelCompletedDarkPreview() {
    TDTheme(darkTheme = true) {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskStatusLabel(isCompleted = true)
        }
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelPendingDarkPreview() {
    TDTheme(darkTheme = true) {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskStatusLabel(isCompleted = false)
        }
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelGroupPreview() {
    TDTheme {
        Column(Modifier.padding(16.dp)) {
            TDTaskStatusLabel(isCompleted = true)
            Spacer(Modifier.height(12.dp))
            TDTaskStatusLabel(isCompleted = false)
        }
    }
}

@TDPreviewForm
@Composable
private fun TDTaskStatusLabelGroupDarkPreview() {
    TDTheme(darkTheme = true) {
        Column(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskStatusLabel(isCompleted = true)
            Spacer(Modifier.height(12.dp))
            TDTaskStatusLabel(isCompleted = false)
        }
    }
}
