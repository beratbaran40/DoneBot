package com.todoapp.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Tall, characterful feature card for the Creation Hub carousel. The whole card carries the feature's
 * [cardColor] (a dark-safe container token); a filled [accentColor] medallion holds the white [icon].
 * Fills the size it's given — the pager page decides the height.
 */
@Composable
fun TDFeatureCard(
    title: String,
    subtitle: String,
    icon: Painter,
    cardColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = cardColor,
        border = if (TDTheme.isDark) BorderStroke(1.dp, TDTheme.colors.lightGray.copy(alpha = 0.18f)) else null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = TDTheme.colors.white,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            TDText(
                text = title,
                style = TDTheme.typography.heading2.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                color = TDTheme.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            TDText(
                text = subtitle,
                style = TDTheme.typography.subheading1.copy(textAlign = TextAlign.Center),
                color = TDTheme.colors.onBackground.copy(alpha = 0.65f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDFeatureCardRowPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDFeatureCard(
                title = "Görev ekle",
                subtitle = "Tek seferlik, rutin ya da aşamalı",
                icon = painterResource(R.drawable.ic_edit_task),
                cardColor = TDTheme.colors.lightPending,
                accentColor = TDTheme.colors.darkPending,
                modifier = Modifier
                    .width(190.dp)
                    .height(300.dp),
            )
            TDFeatureCard(
                title = "Pomodoro başlat",
                subtitle = "Bir odaklanma seansı başlat",
                icon = painterResource(R.drawable.ic_pomodoro),
                cardColor = TDTheme.colors.warmContainer,
                accentColor = TDTheme.colors.orange,
                modifier = Modifier
                    .width(190.dp)
                    .height(300.dp),
            )
        }
    }
}
