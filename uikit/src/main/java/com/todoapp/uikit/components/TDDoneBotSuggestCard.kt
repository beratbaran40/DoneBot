package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDDoneBotSuggestCard(
    @DrawableRes avatarRes: Int,
    title: String,
    body: String,
    primaryCtaText: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    dismissContentDescription: String,
    modifier: Modifier = Modifier,
    secondaryCtaText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(TDTheme.shapes.large)
            .background(TDTheme.colors.infoCardBgColor)
            .padding(PaddingValues(start = 12.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = rememberPixelPainter(painterResource(avatarRes), 96.dp),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(percent = 50)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TDText(
                            text = title,
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        TDText(
                            text = body,
                            style = TDTheme.typography.subheading1,
                            color = TDTheme.colors.onBackground.copy(alpha = 0.65f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(24.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = tdPainter(R.drawable.ic_close),
                            contentDescription = dismissContentDescription,
                            tint = TDTheme.colors.pendingGray,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestChip(
                        label = primaryCtaText,
                        isPrimary = true,
                        onClick = onPrimary,
                    )
                    if (secondaryCtaText != null && onSecondary != null) {
                        SuggestChip(
                            label = secondaryCtaText,
                            isPrimary = false,
                            onClick = onSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestChip(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    val baseModifier = Modifier
        .clip(shape)
        .clickable(onClick = onClick)
    val styledModifier = if (isPrimary) {
        baseModifier.background(TDTheme.colors.purple)
    } else {
        baseModifier.background(TDTheme.colors.bgColorPurple)
    }
    TDText(
        text = label,
        style = TDTheme.typography.subheading2,
        color = if (isPrimary) TDTheme.colors.white else TDTheme.colors.purple,
        modifier = styledModifier.padding(PaddingValues(horizontal = 12.dp, vertical = 6.dp)),
    )
}

@TDPreview
@Composable
private fun TDDoneBotSuggestCardMorningPreview() {
    TDTheme {
        TDDoneBotSuggestCard(
            avatarRes = R.drawable.img_donebot_plan_your_day_light,
            title = "Bugüne hazır mısın?",
            body = "Dün 4 görev tamamladın. Bugüne plan kurmak ister misin?",
            primaryCtaText = "Önerileri gör",
            onPrimary = {},
            onDismiss = {},
            dismissContentDescription = "Kapat",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TDDoneBotSuggestCardEveningPreview() {
    TDTheme {
        TDDoneBotSuggestCard(
            avatarRes = R.drawable.img_donebot_alarm_reminder_light,
            title = "Bugünü kapatma vakti",
            body = "7/9 görev tamamlandı. Kalan 2 göreve ne yapalım?",
            primaryCtaText = "Yarına aktar",
            onPrimary = {},
            secondaryCtaText = "Olduğu gibi bırak",
            onSecondary = {},
            onDismiss = {},
            dismissContentDescription = "Kapat",
            modifier = Modifier.padding(16.dp),
        )
    }
}
