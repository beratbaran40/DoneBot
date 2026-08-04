package com.todoapp.mobile.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.LocalWindowSizeClass
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.ChatMessage
import com.todoapp.uikit.components.TDChatBubble
import com.todoapp.uikit.components.TDChatThinkingIndicator
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageList(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
    toolInFlight: String? = null,
    onQuickReplyClick: (String) -> Unit = {},
    onAssistantMessageLongPress: (ChatMessage) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    val showQuickReplies = !isThinking &&
        messages.lastOrNull()?.role == ChatMessage.Role.ASSISTANT
    // Wider message bubbles on tablets/large screens; phones keep the snug 280dp cap.
    val bubbleMaxWidth =
        if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact) 280.dp else 480.dp
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            val isFromUser = message.role == ChatMessage.Role.USER
            val bubbleModifier = if (isFromUser) {
                Modifier
            } else {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { onAssistantMessageLongPress(message) },
                )
            }
            TDChatBubble(
                text = message.content,
                isFromUser = isFromUser,
                modifier = bubbleModifier,
                maxWidth = bubbleMaxWidth,
            )
        }
        if (isThinking) {
            item(key = "thinking") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (toolInFlight != null) {
                        TDText(
                            text = stringResource(R.string.chat_tool_busy_generic),
                            color = TDTheme.colors.gray,
                            style = TDTheme.typography.subheading1,
                        )
                    }
                    TDChatThinkingIndicator()
                }
            }
        }
        if (showQuickReplies) {
            item(key = "quick-reply") {
                QuickReplyChipRow(onClick = onQuickReplyClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickReplyChipRow(onClick: (String) -> Unit) {
    val chips = listOf(
        stringResource(R.string.chat_suggested_tomorrow),
        stringResource(R.string.chat_suggested_overdue),
        stringResource(R.string.chat_suggested_progress),
        stringResource(R.string.chat_suggested_week_remaining),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        chips.forEach { chip ->
            QuickReplyPill(text = chip, onClick = { onClick(chip) })
        }
    }
}

@Composable
private fun QuickReplyPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(TDTheme.shapes.large)
            .background(TDTheme.colors.lightPurple)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = text,
            color = TDTheme.colors.darkPurple,
            style = TDTheme.typography.subheading1,
        )
    }
}
