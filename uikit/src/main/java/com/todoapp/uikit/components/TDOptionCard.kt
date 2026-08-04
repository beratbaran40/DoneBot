package com.todoapp.uikit.components

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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * Large, tappable choice card for the Creation Hub and its type fork. Colour identity lives in a
 * filled [accentColor] medallion behind the white [icon]; the card itself stays the neutral
 * `lightPending` surface so it reads correctly in light and dark (see DESIGN_TOKENS, Option A).
 *
 * The caller supplies [icon] as a [Painter] so :uikit stays decoupled from :app drawables.
 */
@Composable
fun TDOptionCard(
    title: String,
    subtitle: String,
    icon: Painter,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val shape = TDTheme.shapes.large
    val cardModifier =
        modifier
            .fillMaxWidth()
            .then(
                if (TDTheme.isDark) {
                    Modifier.border(
                        TDTheme.style.borderWidth,
                        tdOutlineColor(TDTheme.colors.lightGray.copy(alpha = 0.25f)),
                        shape,
                    )
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                        darkShadow = accentColor.copy(alpha = 0.15f),
                        cornerRadius = 16.dp,
                        elevation = 5.dp,
                    )
                },
            )
    Surface(
        onClick = onClick,
        modifier = cardModifier,
        shape = shape,
        color = TDTheme.colors.lightPending,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = TDTheme.colors.white,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = title,
                    style = TDTheme.typography.heading5.copy(fontWeight = FontWeight.SemiBold),
                    color = TDTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                TDText(
                    text = subtitle,
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TDOptionCardHubPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDOptionCard(
                title = "Görev ekle",
                subtitle = "Tek seferlik, rutin ya da aşamalı",
                icon = tdPainter(R.drawable.ic_info),
                accentColor = TDTheme.colors.primary,
            )
            TDOptionCard(
                title = "Pomodoro başlat",
                subtitle = "Odaklanma seansı",
                icon = tdPainter(R.drawable.ic_info),
                accentColor = TDTheme.colors.orange,
            )
            TDOptionCard(
                title = "Grup oluştur",
                subtitle = "Görevleri paylaş",
                icon = tdPainter(R.drawable.ic_info),
                accentColor = TDTheme.colors.darkPurple,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDOptionCardSinglePreview() {
    TDTheme {
        TDOptionCard(
            title = "Aşamalı görev",
            subtitle = "Birkaç adımda biten büyük bir iş",
            icon = tdPainter(R.drawable.ic_info),
            accentColor = TDTheme.colors.mediumGreen,
            modifier = Modifier.padding(16.dp),
        )
    }
}
