package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = TDTheme.colors.pendingGray,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        TDText(
            text = title,
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            TDText(
                text = subtitle,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            TDButton(
                text = actionText,
                onClick = onActionClick,
                size = TDButtonSize.SMALL,
            )
        }
    }
}

@TDPreview
@Composable
private fun TdEmptyStateFullPreview() {
    TDTheme {
        TDEmptyState(
            title = "No tasks yet",
            subtitle = "Add your first task and start ticking things off.",
            iconRes = R.drawable.ic_tasks_done,
            actionText = "Add task",
            onActionClick = {},
        )
    }
}

@TDPreview
@Composable
private fun TdEmptyStateIconTitleSubtitlePreview() {
    TDTheme {
        TDEmptyState(
            title = "Nothing here",
            subtitle = "Once you create entries they'll show up in this list.",
            iconRes = R.drawable.ic_tasks_done,
        )
    }
}

@TDPreview
@Composable
private fun TdEmptyStateTitleOnlyPreview() {
    TDTheme {
        TDEmptyState(
            title = "No results",
        )
    }
}

@TDPreview
@Composable
private fun TdEmptyStateNoIconWithActionPreview() {
    TDTheme {
        TDEmptyState(
            title = "Inbox is clear",
            subtitle = "Take a break or plan tomorrow's tasks.",
            actionText = "Plan tomorrow",
            onActionClick = {},
        )
    }
}
