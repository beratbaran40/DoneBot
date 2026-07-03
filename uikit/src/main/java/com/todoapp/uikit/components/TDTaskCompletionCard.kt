package com.todoapp.uikit.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * A prominent, full-width completion toggle for task detail screens.
 *
 * Renders the current status (green check / sand-clock circle) with a title + hint and a trailing
 * checkbox-style indicator. The whole card is the tap target. Unlike the summary-card checkbox this
 * is meant to be the unmistakable "done / undone" affordance inside a detail screen.
 *
 * @param enabled when false the card is muted and non-interactive (e.g. a group member without
 *   permission to complete the task); [disabledHint] is shown in place of the action hint.
 * @param disabledHint shown as the subtitle when [enabled] is false (already-resolved string).
 */
@Composable
fun TDTaskCompletionCard(
    modifier: Modifier = Modifier,
    isCompleted: Boolean,
    enabled: Boolean = true,
    disabledHint: String? = null,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> TDTheme.colors.lightPending.copy(alpha = 0.5f)
            isCompleted -> TDTheme.colors.lightGreen
            else -> TDTheme.colors.lightPending
        },
        animationSpec = tween(durationMillis = 250),
        label = "completionCardBg",
    )

    val title =
        if (isCompleted) {
            stringResource(R.string.status_completed)
        } else {
            stringResource(R.string.task_completion_pending_title)
        }
    val subtitle =
        when {
            !enabled -> disabledHint
            isCompleted -> stringResource(R.string.task_completion_undo_hint)
            else -> stringResource(R.string.task_completion_pending_action)
        }

    var showConfetti by remember { mutableStateOf(false) }
    var prevCompleted by remember { mutableStateOf(isCompleted) }
    LaunchedEffect(isCompleted) {
        // Celebrate only on the pending → completed transition, mirroring TDTaskCardWithCheckbox.
        if (isCompleted && !prevCompleted) showConfetti = true
        prevCompleted = isCompleted
    }

    Box(modifier = modifier) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(backgroundColor, shape)
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.task_completion_cd),
                ) { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDTaskStatusLabel(isCompleted = isCompleted)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TDText(
                    text = title,
                    style = TDTheme.typography.subheading2,
                    color = if (isCompleted) TDTheme.colors.darkGreen else TDTheme.colors.onBackground,
                )
                if (!subtitle.isNullOrBlank()) {
                    TDText(
                        text = subtitle,
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.gray,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            CompletionIndicator(isCompleted = isCompleted)
        }

        if (showConfetti) {
            ConfettiEffect(
                modifier =
                Modifier
                    .matchParentSize()
                    .clip(shape),
                onAnimFinished = { showConfetti = false },
            )
        }
    }
}

/** Trailing checkbox-style indicator mirroring the summary card's checkbox; purely decorative. */
@Composable
private fun CompletionIndicator(isCompleted: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    val indicatorBg by animateColorAsState(
        targetValue =
        if (isCompleted) {
            TDTheme.colors.mediumGreen
        } else {
            TDTheme.colors.pendingGray.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "completionIndicatorBg",
    )
    val indicatorBorder by animateColorAsState(
        targetValue =
        if (isCompleted) {
            TDTheme.colors.mediumGreen
        } else {
            TDTheme.colors.pendingGray.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "completionIndicatorBorder",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(28.dp)
            .clip(shape)
            .background(indicatorBg, shape)
            .border(1.5.dp, indicatorBorder, shape),
    ) {
        if (isCompleted) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_check_svg),
                contentDescription = null,
                tint = TDTheme.colors.white,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDTaskCompletionCardPendingPreview() {
    TDTheme {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskCompletionCard(isCompleted = false, onToggle = {})
        }
    }
}

@TDPreview
@Composable
private fun TDTaskCompletionCardCompletedPreview() {
    TDTheme {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskCompletionCard(isCompleted = true, onToggle = {})
        }
    }
}

@TDPreview
@Composable
private fun TDTaskCompletionCardDisabledPreview() {
    TDTheme {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDTaskCompletionCard(
                isCompleted = false,
                enabled = false,
                disabledHint = "Only the assignee or an admin can complete this task",
                onToggle = {},
            )
        }
    }
}
