package com.todoapp.uikit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor
import kotlinx.coroutines.launch

@Composable
fun TDStatisticCard(
    text: String,
    taskAmount: Int,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val cardBg = if (isCompleted) TDTheme.colors.lightGreen else TDTheme.colors.lightPending
    val numberColor = if (isCompleted) TDTheme.colors.darkGreen else TDTheme.colors.darkPending
    val iconBg = if (isCompleted) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray
    val isDark = TDTheme.isDark
    val shadowAccent = if (isCompleted) TDTheme.colors.darkGreen else TDTheme.colors.darkPending
    val cornerShape = TDTheme.shapes.xLarge

    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
        alpha.animateTo(1f, spring(stiffness = 300f))
    }

    val surfaceModifier =
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }.then(
                // Dark keeps an outline rather than a shadow; in a hard-elevation kit that outline
                // becomes bright ink, which is exactly how a CRT-dark card reads its edge.
                if (isDark) {
                    Modifier.border(
                        TDTheme.style.borderWidth,
                        tdOutlineColor(TDTheme.colors.lightGray.copy(alpha = 0.25f)),
                        cornerShape,
                    )
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                        darkShadow = shadowAccent.copy(alpha = 0.18f),
                        cornerRadius = 20.dp,
                        elevation = 6.dp,
                    )
                },
            )

    // The icon sits ABOVE the labels rather than beside them. Two of these cards share a phone row,
    // so a 44dp icon plus its 14dp gutter used to leave the text column 68dp at 360dp — narrower
    // than the single word "Tamamlandı" (77dp), which is why the label shipped with an ellipsis and
    // truncated in Turkish AND in English ("Task Pendin…"). Stacking gives the text the card's full
    // 126dp interior, which clears every current label with the system font at 1.3, so the labels no
    // longer need to be cut. Shaving the icon instead only buys 16dp and does not reach.
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            StatisticIcon(
                isCompleted = isCompleted,
                backgroundColor = iconBg,
            )
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = taskAmount,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { -it } + fadeIn(tween(250)) togetherWith
                            slideOutVertically { it } + fadeOut(tween(250))
                    } else {
                        slideInVertically { it } + fadeIn(tween(250)) togetherWith
                            slideOutVertically { -it } + fadeOut(tween(250))
                    }
                },
                label = "taskAmountAnim",
            ) { amount ->
                TDText(
                    text = amount.toString(),
                    style = TDTheme.typography.heading5.copy(fontWeight = FontWeight.Bold),
                    color = numberColor,
                )
            }
            Spacer(Modifier.height(2.dp))
            TDText(
                text = stringResource(R.string.weekly),
                style = TDTheme.typography.subheading4,
                color = numberColor,
                fitPolicy = TDFitPolicy.BOUNDED,
                slot = "TDStatisticCard.period",
            )
            TDText(
                text = text,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                fitPolicy = TDFitPolicy.BOUNDED,
                slot = "TDStatisticCard.label",
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            shape = cornerShape,
            color = cardBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content,
        )
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = cornerShape,
            color = cardBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content,
        )
    }
}

@Composable
private fun StatisticIcon(
    isCompleted: Boolean,
    backgroundColor: Color,
) {
    val iconScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(44.dp)
            .clip(TDTheme.shapes.medium)
            .background(backgroundColor),
    ) {
        if (isCompleted) {
            Icon(
                modifier =
                Modifier
                    .size(20.dp)
                    .scale(iconScale.value),
                painter = tdPainter(R.drawable.ic_rectangle_svg),
                contentDescription = null,
                tint = TDTheme.colors.white,
            )
            Icon(
                modifier =
                Modifier
                    .size(13.dp)
                    .scale(iconScale.value),
                painter = tdPainter(R.drawable.ic_check_svg),
                contentDescription = null,
                tint = TDTheme.colors.white,
            )
        } else {
            Icon(
                modifier =
                Modifier
                    .size(20.dp)
                    .scale(iconScale.value),
                painter = tdPainter(R.drawable.ic_sand_clock),
                contentDescription = null,
                tint = TDTheme.colors.white,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDStatisticCardCompletedPreview() {
    TDTheme {
        TDStatisticCard(
            text = "Task Complete",
            taskAmount = 10,
            isCompleted = true,
        )
    }
}

@TDPreview
@Composable
private fun TDStatisticCardPendingPreview() {
    TDTheme {
        TDStatisticCard(
            text = "Task Pending",
            taskAmount = 3,
            isCompleted = false,
        )
    }
}

@TDPreview
@Composable
private fun TDStatisticCardZeroPreview() {
    TDTheme {
        TDStatisticCard(
            text = "Task Complete",
            taskAmount = 0,
            isCompleted = true,
        )
    }
}

@TDPreview
@Composable
private fun TDStatisticCardLargeCountPreview() {
    TDTheme {
        TDStatisticCard(
            text = "Task Complete",
            taskAmount = 1234,
            isCompleted = true,
        )
    }
}

@TDPreview
@Composable
private fun TDStatisticCardClickablePreview() {
    TDTheme {
        TDStatisticCard(
            text = "Pending",
            taskAmount = 7,
            isCompleted = false,
            onClick = {},
        )
    }
}

/**
 * The regression test for this card: Home's real 2-up row with the real Turkish labels, squeezed.
 * "Tamamlandı" is one unbreakable word, so if the text column ever narrows again it cannot wrap —
 * it breaks mid-word or truncates. Both cards must also stay the same height.
 */
@TDPreviewNarrow
@Composable
private fun TDStatisticCardNarrowRowPreview() {
    TDTheme {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(16.dp),
        ) {
            TDStatisticCard(
                text = "Tamamlandı",
                taskAmount = 12,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                isCompleted = true,
            )
            Spacer(Modifier.width(12.dp))
            TDStatisticCard(
                text = "Bekliyor",
                taskAmount = 3,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                isCompleted = false,
            )
        }
    }
}
