package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
fun TDErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    @DrawableRes iconRes: Int? = R.drawable.ic_error,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = TDTheme.colors.crossRed,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        if (!title.isNullOrBlank()) {
            TDText(
                text = title,
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        TDText(
            text = message,
            style = if (title.isNullOrBlank()) TDTheme.typography.heading3 else TDTheme.typography.regularTextStyle,
            color = if (title.isNullOrBlank()) {
                TDTheme.colors.onBackground
            } else {
                TDTheme.colors.onBackground.copy(alpha = 0.7f)
            },
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(24.dp))
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
private fun TdErrorStateRetryPreview() {
    TDTheme {
        TDErrorState(
            message = "Something went wrong while loading tasks.",
            actionText = "Retry",
            onActionClick = {},
        )
    }
}

@TDPreview
@Composable
private fun TdErrorStateMessageOnlyPreview() {
    TDTheme {
        TDErrorState(
            message = "No connection. Please check your internet.",
        )
    }
}

@TDPreview
@Composable
private fun TdErrorStateTitleAndMessagePreview() {
    TDTheme {
        TDErrorState(
            title = "Couldn't reach DoneBot",
            message = "We hit a transient issue talking to the server. Try again in a moment.",
            actionText = "Retry",
            onActionClick = {},
        )
    }
}

@TDPreview
@Composable
private fun TdErrorStateNoIconPreview() {
    TDTheme {
        TDErrorState(
            message = "Validation failed.",
            iconRes = null,
        )
    }
}
