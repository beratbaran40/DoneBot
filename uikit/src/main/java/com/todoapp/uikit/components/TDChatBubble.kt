package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDChatBubble(
    text: String,
    isFromUser: Boolean,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
) {
    val bubbleColor: Color =
        if (isFromUser) TDTheme.colors.lightPurple else TDTheme.colors.lightPending
    val textColor: Color =
        if (isFromUser) TDTheme.colors.darkPurple else TDTheme.colors.darkPending
    val shape =
        if (isFromUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
        }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            TDText(text = text, color = textColor)
        }
    }
}

@TDPreview
@Composable
private fun TDChatBubbleAssistantPreview() {
    TDTheme {
        TDChatBubble(
            text = "Hi, I'm DoneBot. Ask me what's on your plate today.",
            isFromUser = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TDChatBubbleUserPreview() {
    TDTheme {
        TDChatBubble(
            text = "What do I have due this weekend?",
            isFromUser = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TDChatBubbleConversationPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            TDChatBubble(
                text = "What's on my list today?",
                isFromUser = true,
            )
            Spacer(Modifier.height(8.dp))
            TDChatBubble(
                text = "You have 3 tasks today: Buy groceries, Pay rent, and a 30-minute focus block at 4 PM.",
                isFromUser = false,
            )
            Spacer(Modifier.height(8.dp))
            TDChatBubble(
                text = "Anything overdue?",
                isFromUser = true,
            )
            Spacer(Modifier.height(8.dp))
            TDChatBubble(
                text =
                "Yes — \"Renew passport\" was due last Monday and is still open. " +
                    "Want me to suggest a time slot for it?",
                isFromUser = false,
            )
        }
    }
}
