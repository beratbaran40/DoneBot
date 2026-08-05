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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
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
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var longPressedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var reportTarget by remember { mutableStateOf<ChatMessage?>(null) }
    val reportSuccessToast = stringResource(R.string.chat_report_success_toast)
    val reportFailedToast = stringResource(R.string.chat_report_failed_toast)

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            ChatContract.UiEffect.ScrollToBottom -> {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(state.messages.lastIndex)
                }
            }
            is ChatContract.UiEffect.ShowReportResult -> {
                Toast.makeText(
                    context,
                    if (effect.success) reportSuccessToast else reportFailedToast,
                    Toast.LENGTH_SHORT,
                ).show()
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
            .consumeWindowInsets(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
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
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                },
            )
        }
        state.error?.let { error ->
            ChatErrorBanner(
                error = error,
                lastFailedPrompt = state.lastFailedPrompt,
                cooldownSecondsRemaining = state.rateLimitCooldownSecondsRemaining,
                autoRetrySecondsRemaining = state.autoRetrySecondsRemaining,
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
            onReport = {
                longPressedMessage = null
                reportTarget = target
            },
        )
    }

    reportTarget?.let { target ->
        ReportMessageDialog(
            onDismiss = { reportTarget = null },
            onConfirm = {
                reportTarget = null
                onAction(ChatContract.UiAction.OnReportMessage(target))
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
    onReport: () -> Unit,
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
                icon = tdPainter(com.example.uikit.R.drawable.ic_content_copy),
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
                    icon = tdPainter(com.example.uikit.R.drawable.ic_refresh),
                    label = stringResource(R.string.chat_action_try_again),
                    onClick = { onTryAgain(previousUserPrompt) },
                )
            }
            if (target.role == ChatMessage.Role.ASSISTANT) {
                MessageActionItem(
                    icon = tdPainter(com.example.uikit.R.drawable.ic_warning),
                    label = stringResource(R.string.chat_action_report),
                    onClick = onReport,
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
                painter = tdPainter(com.example.uikit.R.drawable.ic_arrow_down),
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
                    painter = rememberPixelPainter(painterResource(R.drawable.img_splash), 55.dp),
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
                    painter = tdPainter(com.example.uikit.R.drawable.ic_delete),
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
    // fillMaxSize keeps the min height at the viewport so the content stays vertically centred when
    // it fits, while verticalScroll lets the avatar + copy + suggestion chips scroll into reach in
    // landscape instead of being clipped below the fold with no way to scroll.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
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
                painter = rememberPixelPainter(painterResource(R.drawable.img_splash), 88.dp),
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

        // Every one of these is answered on-device by LocalIntentClassifier, which is why the wording
        // matches its anchors — and why the first-run screen keeps working signed out. A write prompt
        // here would dead-end a guest into the sign-in gate on the screen they see most.
        val suggestions = listOf(
            stringResource(R.string.chat_suggested_today),
            stringResource(R.string.chat_suggested_tomorrow),
            stringResource(R.string.chat_suggested_overdue),
            stringResource(R.string.chat_suggested_hearts),
            stringResource(R.string.chat_suggested_progress),
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
            .clip(TDTheme.shapes.xLarge)
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

@OptIn(ExperimentalLayoutApi::class)
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
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tdCorner(24.dp))
                    .background(TDTheme.colors.lightPending)
                    .border(
                        width = 1.dp,
                        color = TDTheme.colors.lightGray.copy(alpha = 0.4f),
                        shape = tdCorner(24.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { onAction(ChatContract.UiAction.OnDraftChanged(it)) },
                    // Editable even while the bot is thinking, so the user can type their next
                    // message ahead of time and still reach the Stop button. Sending stays gated.
                    enabled = true,
                    singleLine = false,
                    maxLines = MAX_INPUT_LINES,
                    cursorBrush = SolidColor(TDTheme.colors.pendingGray),
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
            if (isThinking) {
                StopButton(onClick = { onAction(ChatContract.UiAction.OnStopClicked) })
            } else {
                SendButton(
                    enabled = canSend,
                    onClick = {
                        onAction(ChatContract.UiAction.OnSendClicked)
                        keyboardController?.hide()
                    },
                )
            }
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
            painter = tdPainter(com.example.uikit.R.drawable.ic_send),
            contentDescription = stringResource(R.string.chat_send_button_description),
            tint = TDTheme.colors.white,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StopButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.pendingGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = tdPainter(com.example.uikit.R.drawable.ic_stop),
            contentDescription = stringResource(R.string.chat_stop_button_description),
            tint = TDTheme.colors.white,
            modifier = Modifier.size(16.dp),
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

@Composable
private fun ReportMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TDTheme.colors.surface,
        title = {
            TDText(
                text = stringResource(R.string.chat_report_confirm_title),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            TDText(
                text = stringResource(R.string.chat_report_confirm_message),
                color = TDTheme.colors.gray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                TDText(
                    text = stringResource(R.string.chat_report_confirm_button),
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
private fun ChatScreenServerWakingPreview() {
    TDTheme {
        ChatScreen(
            uiState = ChatPreviewData.serverWakingReady,
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
