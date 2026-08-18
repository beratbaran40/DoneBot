package com.todoapp.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

/**
 * Characterful feature card for the Creation Hub grid. The whole card carries the feature's
 * [cardColor] (a dark-safe container token); a filled [accentColor] medallion holds the white [icon].
 * Fills the size it is given — the grid cell decides both dimensions.
 *
 * [compact] is the phone tier, where four cards share the screen and a cell is only ~150dp across.
 * Leave it false for the roomier cells a tablet's 720dp content column produces, which is the size
 * this card was originally drawn at.
 *
 * The tier is a parameter rather than something the card reads off its own constraints on purpose:
 * `BoxWithConstraints` is a `SubcomposeLayout` and cannot answer an intrinsic measurement, which
 * would break the `height(IntrinsicSize.Max)` a caller uses to keep a row of these the same height.
 *
 * Neither label is capped or ellipsized. The app's rule is grow-don't-cut, and a row of cards is
 * expected to take the height its wordiest card asks for.
 */
@Composable
fun TDFeatureCard(
    title: String,
    subtitle: String,
    icon: Painter,
    cardColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = tdCorner(if (compact) 20.dp else 28.dp),
        color = cardColor,
        border = if (TDTheme.isDark) BorderStroke(1.dp, TDTheme.colors.lightGray.copy(alpha = 0.18f)) else null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (compact) {
                        Modifier.padding(horizontal = COMPACT_PADDING_H, vertical = COMPACT_PADDING_V)
                    } else {
                        Modifier.padding(28.dp)
                    },
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (compact) 64.dp else 76.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = TDTheme.colors.white,
                    modifier = Modifier.size(if (compact) 32.dp else 38.dp),
                )
            }
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            TDText(
                text = title,
                style = (if (compact) TDTheme.typography.heading5 else TDTheme.typography.heading2).copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                color = TDTheme.colors.onBackground,
                slot = "TDFeatureCard.title",
            )
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            TDText(
                text = subtitle,
                style = TDTheme.typography.subheading1.copy(textAlign = TextAlign.Center),
                color = TDTheme.colors.onBackground.copy(alpha = 0.65f),
                slot = "TDFeatureCard.subtitle",
            )
        }
    }
}

/**
 * Side inset of the [compact] tier. Deliberately thin: everything in the card is centred, so at a
 * normal font size the padding is invisible, and the width it gives back is what keeps 'Pomodoro'
 * and 'başkalarıyla' from breaking mid-word on a 320dp screen at font scale 1.3 (One UI display
 * zoom plus a larger system font — see `tools/textfit.py`, which reads this line).
 */
private val COMPACT_PADDING_H = 8.dp

/** Top/bottom inset of the [compact] tier — the vertical room is not contested, so it stays roomy. */
private val COMPACT_PADDING_V = 16.dp

@TDPreview
@Composable
private fun TDFeatureCardCompactGridPreview() {
    TDTheme {
        val cells = listOf(
            CardSample("Görev ekle", "Kendin için ya da bir grup için", R.drawable.ic_edit_task, TDTheme.colors.lightPending, TDTheme.colors.purple),
            CardSample("Günlük tut", "Bugünü bir notla yakala", R.drawable.ic_journal, TDTheme.colors.lightGreen, TDTheme.colors.darkGreen),
            CardSample("Pomodoro başlat", "Bir odaklanma seansı başlat", R.drawable.ic_pomodoro, TDTheme.colors.warmContainer, TDTheme.colors.orange),
            CardSample("Grup oluştur", "Görevleri başkalarıyla paylaş", R.drawable.ic_members, TDTheme.colors.purpleContainer, TDTheme.colors.darkPurple),
        )
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cells.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { cell ->
                        TDFeatureCard(
                            title = cell.title,
                            subtitle = cell.subtitle,
                            icon = tdPainter(cell.icon),
                            cardColor = cell.cardColor,
                            accentColor = cell.accentColor,
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

private data class CardSample(
    val title: String,
    val subtitle: String,
    val icon: Int,
    val cardColor: Color,
    val accentColor: Color,
)

@TDPreview
@Composable
private fun TDFeatureCardRoomyPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDFeatureCard(
                title = "Görev ekle",
                subtitle = "Tek seferlik, rutin ya da aşamalı",
                icon = tdPainter(R.drawable.ic_edit_task),
                cardColor = TDTheme.colors.lightPending,
                accentColor = TDTheme.colors.darkPending,
                modifier = Modifier
                    .width(190.dp)
                    .height(300.dp),
            )
            TDFeatureCard(
                title = "Pomodoro başlat",
                subtitle = "Bir odaklanma seansı başlat",
                icon = tdPainter(R.drawable.ic_pomodoro),
                cardColor = TDTheme.colors.warmContainer,
                accentColor = TDTheme.colors.orange,
                modifier = Modifier
                    .width(190.dp)
                    .height(300.dp),
            )
        }
    }
}
