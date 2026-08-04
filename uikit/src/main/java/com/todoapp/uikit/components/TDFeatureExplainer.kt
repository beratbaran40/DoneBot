package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDFeatureExplainer(
    title: String,
    description: String,
    buttonText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bulletPoints: List<String> = emptyList(),
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            // widthIn stops the card stretching edge-to-edge on wide/landscape screens; verticalScroll
            // lets long bullet lists scroll instead of clipping below a short landscape viewport.
            modifier = modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .clip(TDTheme.shapes.xLarge)
                .background(TDTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = TDTheme.colors.purple,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                TDText(
                    text = title,
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                )
            }
            Spacer(Modifier.height(16.dp))
            TDText(
                text = description,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground,
            )
            if (bulletPoints.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                bulletPoints.forEach { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            painter = tdPainter(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = TDTheme.colors.purple,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        TDText(
                            text = point,
                            style = TDTheme.typography.regularTextStyle,
                            color = TDTheme.colors.onBackground,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TDButton(
                    text = buttonText,
                    onClick = onDismiss,
                    size = TDButtonSize.SMALL,
                )
            }
        }
    }
}

@TDPreviewDialog
@Composable
private fun TdFeatureExplainerWithBulletsPreview() {
    TDTheme {
        TDFeatureExplainer(
            title = "Smart reminders",
            description = "We'll nudge you a few minutes before your task to help you stay on track.",
            buttonText = "Got it",
            bulletPoints = listOf(
                "Tap a task to choose the reminder offset",
                "Disable reminders in Settings anytime",
                "Group tasks share their reminder with members",
            ),
            onDismiss = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TdFeatureExplainerWithoutBulletsPreview() {
    TDTheme {
        TDFeatureExplainer(
            title = "Welcome to Pomodoro",
            description = "Focus for 25 minutes, then take a short break. Tap start when you're ready.",
            buttonText = "Start",
            onDismiss = {},
        )
    }
}
