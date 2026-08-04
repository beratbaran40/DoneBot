package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * A square, icon-over-label tile meant to sit **beside** a sibling in a `Row`, for a fork with
 * exactly two or three answers ("is this mine or the group's?").
 *
 * Distinct from [TDOptionCard], which is a full-width row card with a subtitle: that shape reads as
 * a list of destinations, while two tiles side by side read as one question with two answers — which
 * is what a scope choice is.
 *
 * [enabled] renders a dimmed, non-tappable tile rather than hiding it: an option the user cannot
 * reach yet still teaches that the feature exists, and the caller can explain why underneath.
 */
@Composable
fun TDChoiceTile(
    label: String,
    icon: Painter,
    accentColor: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(16.dp)
    val contentAlpha = if (enabled) 1f else DISABLED_ALPHA
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (selected) {
                    Modifier.border(2.dp, accentColor, shape)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = TDTheme.colors.lightPending,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = contentAlpha)),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = TDTheme.colors.white,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            TDText(
                text = label,
                style = TDTheme.typography.heading5.copy(
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                color = TDTheme.colors.onBackground.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f

@TDPreview
@Composable
private fun TdChoiceTilePairPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDChoiceTile(
                label = "Bireysel",
                icon = painterResource(R.drawable.ic_personal_label),
                accentColor = TDTheme.colors.primary,
                modifier = Modifier.weight(1f),
            )
            TDChoiceTile(
                label = "Grup",
                icon = painterResource(R.drawable.ic_members),
                accentColor = TDTheme.colors.darkPurple,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@TDPreview
@Composable
private fun TdChoiceTileSelectedPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDChoiceTile(
                label = "Bireysel",
                icon = painterResource(R.drawable.ic_personal_label),
                accentColor = TDTheme.colors.primary,
                selected = true,
                modifier = Modifier.weight(1f),
            )
            TDChoiceTile(
                label = "Grup",
                icon = painterResource(R.drawable.ic_members),
                accentColor = TDTheme.colors.darkPurple,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@TDPreview
@Composable
private fun TdChoiceTileDisabledPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDChoiceTile(
                label = "Bireysel",
                icon = painterResource(R.drawable.ic_personal_label),
                accentColor = TDTheme.colors.primary,
                modifier = Modifier.weight(1f),
            )
            TDChoiceTile(
                label = "Grup",
                icon = painterResource(R.drawable.ic_members),
                accentColor = TDTheme.colors.darkPurple,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
