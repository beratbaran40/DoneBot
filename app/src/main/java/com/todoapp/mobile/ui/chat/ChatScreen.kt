package com.todoapp.mobile.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.ChatMessage
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    uiState: ChatContract.UiState,
    uiEffect: Flow<ChatContract.UiEffect>,
    onAction: (ChatContract.UiAction) -> Unit,
) {
    when (uiState) {
        ChatContract.UiState.Loading -> TDLoadingBar()
        is ChatContract.UiState.Ready -> ChatReadyContent(
            state = uiState,
            uiEffect = uiEffect,
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatReadyContent(
    state: ChatContract.UiState.Ready,
    uiEffect: Flow<ChatContract.UiEffect>,
    onAction: (ChatContract.UiAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }
    var longPressedMessage by remember { mutableStateOf<ChatMessage?>(null) }

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            ChatContract.UiEffect.ScrollToBottom -> {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(state.messages.lastIndex)
                }
            }
        }
    }

    val lastMessageId = state.messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (lastMessageId != null && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val showScrollFab by remember {
        derivedStateOf { listState.canScrollForward && state.messages.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) {
        ChatPersonaHeader(
            hasMessages = state.messages.isNotEmpty(),
            onClearClick = { showClearConfirm = true },
        )
        Box(modifier = Modifier.weight(1f)) {
            if (state.messages.isEmpty() && !state.isThinking) {
                ChatEmptyState(
                    onSuggestedPromptClick = { prompt ->
                        onAction(ChatContract.UiAction.OnDraftChanged(prompt))
                        onAction(ChatContract.UiAction.OnSendClicked)
                    },
                )
            } else {
                ChatMessageList(
                    messages = state.messages,
                    isThinking = state.isThinking,
                    toolInFlight = state.toolInFlight,
                    onQuickReplyClick = { prompt ->
                        onAction(ChatContract.UiAction.OnDraftChanged(prompt))
                        onAction(ChatContract.UiAction.OnSendClicked)
                    },
                    onAssistantMessageLongPress = { message -> longPressedMessage = message },
                    listState = listState,
                )
            }
            ScrollToBottomFab(
                visible = showScrollFab,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(state.messages.lastIndex)
                    }
                },
            )
        }
        state.error?.let { error ->
            ChatErrorBanner(
                error = error,
                lastFailedPrompt = state.lastFailedPrompt,
                cooldownSecondsRemaining = state.rateLimitCooldownSecondsRemaining,
                onAction = onAction,
            )
        }
        ChatInputPill(
            draft = state.draft,
            isThinking = state.isThinking,
            onAction = onAction,
        )
    }

    if (showClearConfirm) {
        ClearHistoryDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                showClearConfirm = false
                onAction(ChatContract.UiAction.OnClearHistory)
            },
        )
    }

    longPressedMessage?.let { target ->
        MessageActionsSheet(
            messages = state.messages,
            target = target,
            onDismiss = { longPressedMessage = null },
            onTryAgain = { prompt ->
                longPressedMessage = null
                onAction(ChatContract.UiAction.OnDraftChanged(prompt))
                onAction(ChatContract.UiAction.OnSendClicked)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    messages: List<ChatMessage>,
    target: ChatMessage,
    onDismiss: () -> Unit,
    onTryAgain: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val context = LocalContext.current
    val previousUserPrompt = remember(messages, target) {
        val idx = messages.indexOfFirst { it.id == target.id }
        if (idx <= 0) {
            null
        } else {
            messages.subList(0, idx).lastOrNull { it.role == ChatMessage.Role.USER }?.content
        }
    }
    val copiedText = stringResource(R.string.chat_action_copied_toast)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TDTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            MessageActionItem(
                icon = painterResource(com.example.uikit.R.drawable.ic_content_copy),
                label = stringResource(R.string.chat_action_copy),
                onClick = {
                    clipboardScope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(android.content.ClipData.newPlainText("chat_message", target.content)),
                        )
                    }
                    Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )
            if (previousUserPrompt != null) {
                MessageActionItem(
                    icon = painterResource(com.example.uikit.R.drawable.ic_refresh),
                    label = stringResource(R.string.chat_action_try_again),
                    onClick = { onTryAgain(previousUserPrompt) },
                )
            }
        }
    }
}

@Composable
private fun MessageActionItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = TDTheme.colors.darkPending,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        TDText(
            text = label,
            color = TDTheme.colors.onBackground,
            style = TDTheme.typography.regularTextStyle,
        )
    }
}

@Composable
private fun BoxScope.ScrollToBottomFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.lightPending)
                .border(1.dp, TDTheme.colors.lightGray.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_down),
                contentDescription = stringResource(R.string.chat_scroll_to_bottom),
                tint = TDTheme.colors.darkPending,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ChatPersonaHeader(
    hasMessages: Boolean,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(55.dp)) {
            Box(
                modifier = Modifier
                    .size(55.dp)
                    .clip(CircleShape)
                    .background(TDTheme.colors.pendingGray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.img_splash),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(55.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(TDTheme.colors.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(TDTheme.colors.green),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = stringResource(R.string.chat_title),
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = stringResource(R.string.chat_persona_subtitle),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
        if (hasMessages) {
            IconButton(onClick = onClearClick) {
                Icon(
                    painter = painterResource(com.example.uikit.R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.chat_action_clear_history),
                    tint = TDTheme.colors.gray,
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    onSuggestedPromptClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.pendingGray),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.img_splash),
                contentDescription = null,
                modifier = Modifier.size(88.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        TDText(
            text = stringResource(R.string.chat_empty_title),
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        TDText(
            text = stringResource(R.string.chat_empty_subtitle),
            color = TDTheme.colors.gray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        val suggestions = listOf(
            stringResource(R.string.chat_suggested_today),
            stringResource(R.string.chat_suggested_tomorrow),
            stringResource(R.string.chat_suggested_overdue),
            stringResource(R.string.chat_suggested_progress),
            stringResource(R.string.chat_suggested_week_remaining),
        )
        suggestions.forEach { prompt ->
            SuggestedPromptChip(prompt = prompt, onClick = { onSuggestedPromptClick(prompt) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SuggestedPromptChip(
    prompt: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = prompt,
            color = TDTheme.colors.darkPending,
            style = TDTheme.typography.regularTextStyle,
        )
    }
}

@Composable
private fun ChatErrorBanner(
    error: ChatContract.ChatError,
    lastFailedPrompt: String?,
    cooldownSecondsRemaining: Int,
    onAction: (ChatContract.UiAction) -> Unit,
) {
    val message = stringResource(
        when (error) {
            ChatContract.ChatError.GENERIC -> R.string.chat_error_generic
            ChatContract.ChatError.BLOCKED -> R.string.chat_error_blocked
            ChatContract.ChatError.OFFLINE -> R.string.chat_error_offline
            ChatContract.ChatError.LOOP_OVERFLOW -> R.string.chat_loop_overflow
            ChatContract.ChatError.RATE_LIMITED -> R.string.chat_error_rate_limited
            ChatContract.ChatError.NOT_AUTHENTICATED -> R.string.chat_error_guest_limited
        },
    )
    val canRetry = lastFailedPrompt != null && error != ChatContract.ChatError.BLOCKED
    val retryDisabledByCooldown = error == ChatContract.ChatError.RATE_LIMITED &&
        cooldownSecondsRemaining > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightRed)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = message,
            color = TDTheme.colors.crossRed,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        if (canRetry) {
            val retryLabel = if (retryDisabledByCooldown) {
                stringResource(R.string.chat_error_rate_limited_cooldown_format, cooldownSecondsRemaining)
            } else {
                stringResource(R.string.chat_retry)
            }
            TextButton(
                onClick = { onAction(ChatContract.UiAction.OnRetry) },
                enabled = !retryDisabledByCooldown,
            ) {
                TDText(
                    text = retryLabel,
                    color = if (retryDisabledByCooldown) TDTheme.colors.gray else TDTheme.colors.crossRed,
                    style = TDTheme.typography.subheading1,
                )
            }
        }
        if (error == ChatContract.ChatError.NOT_AUTHENTICATED) {
            TextButton(onClick = { onAction(ChatContract.UiAction.OnSignInTap) }) {
                TDText(
                    text = stringResource(R.string.chat_sign_in),
                    color = TDTheme.colors.crossRed,
                    style = TDTheme.typography.subheading1,
                )
            }
        }
        TextButton(onClick = { onAction(ChatContract.UiAction.OnDismissError) }) {
            TDText(
                text = stringResource(R.string.chat_dismiss),
                color = TDTheme.colors.crossRed,
                style = TDTheme.typography.subheading1,
            )
        }
    }
}

@Composable
private fun ChatInputPill(
    draft: String,
    isThinking: Boolean,
    onAction: (ChatContract.UiAction) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSend = draft.isNotBlank() && !isThinking && draft.length <= ChatViewModel.MAX_DRAFT_LENGTH
    val showCharCount = draft.length > CHAR_COUNT_THRESHOLD
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(TDTheme.colors.lightPending)
                    .border(
                        width = 1.dp,
                        color = TDTheme.colors.lightGray.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { onAction(ChatContract.UiAction.OnDraftChanged(it)) },
                    enabled = !isThinking,
                    singleLine = false,
                    maxLines = MAX_INPUT_LINES,
                    cursorBrush = SolidColor(TDTheme.colors.purple),
                    textStyle = TDTheme.typography.regularTextStyle.copy(color = TDTheme.colors.onBackground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onAction(ChatContract.UiAction.OnSendClicked)
                                keyboardController?.hide()
                            }
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            TDText(
                                text = stringResource(R.string.chat_input_hint),
                                color = TDTheme.colors.gray,
                                style = TDTheme.typography.regularTextStyle,
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            SendButton(
                enabled = canSend,
                onClick = {
                    onAction(ChatContract.UiAction.OnSendClicked)
                    keyboardController?.hide()
                },
            )
        }
        if (showCharCount) {
            TDText(
                text = stringResource(
                    R.string.chat_char_count_format,
                    draft.length,
                    ChatViewModel.MAX_DRAFT_LENGTH,
                ),
                color = if (draft.length >= ChatViewModel.MAX_DRAFT_LENGTH) {
                    TDTheme.colors.crossRed
                } else {
                    TDTheme.colors.gray
                },
                style = TDTheme.typography.subheading1,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp, end = 60.dp),
            )
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) TDTheme.colors.pendingGray else TDTheme.colors.lightGray
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_send),
            contentDescription = stringResource(R.string.chat_send_button_description),
            tint = TDTheme.colors.white,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ClearHistoryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TDTheme.colors.surface,
        title = {
            TDText(
                text = stringResource(R.string.chat_clear_confirm_title),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            TDText(
                text = stringResource(R.string.chat_clear_confirm_message),
                color = TDTheme.colors.gray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                TDText(
                    text = stringResource(R.string.chat_clear_confirm_button),
                    color = TDTheme.colors.crossRed,
                    style = TDTheme.typography.subheading1,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TDText(
                    text = stringResource(R.string.chat_dismiss),
                    color = TDTheme.colors.gray,
                    style = TDTheme.typography.subheading1,
                )
            }
        },
    )
}

private const val MAX_INPUT_LINES = 5
private const val CHAR_COUNT_THRESHOLD = 900

@TDPreviewWide
@Composable
private fun ChatScreenLoadingPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatContract.UiState.Loading,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenEmptyPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.emptyReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenPopulatedPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.populatedReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenThinkingPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.thinkingReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenTypingPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.typingReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenErrorPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.errorReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun ChatScreenGuestLimitedPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.guestLimitedReady,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ChatScreenEmptyCompactPreview() {
    TDTheme {
        Box(modifier = Modifier.height(640.dp)) {
            ChatScreen(
                uiState = ChatPreviewData.emptyReady,
                uiEffect = flowOf(),
                onAction = {},
            )
        }
    }
}
