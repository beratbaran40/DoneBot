package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDPermissionPromptCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient =
        Brush.linearGradient(
            colors =
            listOf(
                TDTheme.colors.lightPending,
                TDTheme.colors.background,
            ),
        )

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(TDTheme.shapes.xLarge)
            .background(gradient)
            .border(
                width = 1.dp,
                color = TDTheme.colors.darkPending.copy(alpha = 0.25f),
                shape = TDTheme.shapes.xLarge,
            ),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 32.dp),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TDTheme.colors.darkPending),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = tdPainter(iconRes),
                        contentDescription = null,
                        tint = TDTheme.colors.surface,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TDText(
                        text = title,
                        style = TDTheme.typography.heading4,
                        color = TDTheme.colors.onBackground,
                    )
                    TDText(
                        text = description,
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            TDButton(
                modifier = Modifier.fillMaxWidth(),
                text = ctaText,
                type = TDButtonType.PRIMARY,
                size = TDButtonSize.SMALL,
                fullWidth = true,
                onClick = onCtaClick,
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_close),
                contentDescription = "Close",
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@TDPreview
@Composable
private fun TDPermissionPromptCardPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDPermissionPromptCard(
                iconRes = R.drawable.ic_notification,
                title = "Stay on top of your tasks",
                description = "Allow notifications to get timely reminders.",
                ctaText = "Enable",
                onCtaClick = {},
                onDismiss = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun TDPermissionPromptCardOverlayPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDPermissionPromptCard(
                iconRes = R.drawable.ic_visibility,
                title = "Show reminders over apps",
                description = "Let DoneBot pop up gentle nudges while you're using other apps.",
                ctaText = "Allow",
                onCtaClick = {},
                onDismiss = {},
            )
        }
    }
}
