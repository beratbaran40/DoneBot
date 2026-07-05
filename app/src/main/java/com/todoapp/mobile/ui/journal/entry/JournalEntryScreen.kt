package com.todoapp.mobile.ui.journal.entry

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.todoapp.mobile.R.string
import com.todoapp.mobile.ui.common.SecureScreenEffect
import com.todoapp.mobile.ui.journal.ORDERED_MOODS
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiAction
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiEffect
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiState
import com.todoapp.mobile.ui.journal.moodIndex
import com.todoapp.mobile.ui.permissions.rememberCameraPermissionRequest
import com.todoapp.uikit.components.TDFeatureExplainer
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDMoodOption
import com.todoapp.uikit.components.TDMoodSelector
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreviewForm
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.paperBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JournalEntryScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    SecureScreenEffect()
    val context = LocalContext.current
    val requestCamera = rememberCameraPermissionRequest(
        onGranted = { onAction(UiAction.OnPolaroidCameraClicked) },
    )

    // The top bar matches the cream paper color in both themes, so keep the status-bar icons dark
    // here (readable over the cream) regardless of theme, and restore the previous value on exit.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val controller = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = true
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.ShowToast ->
                Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = uiState is UiState.Editing) {
        onAction(UiAction.OnBackPress)
    }

    val handwriting = TDTheme.typography.journalHandwritingStyle

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                textStyle = handwriting,
                marginX = 32.dp,
                headerLineWidth = 2.5.dp,
                headerLineColor = TDTheme.colors.crossRed.copy(alpha = 0.55f),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            JournalTopBar(
                showInfo = uiState is UiState.Editing,
                onBack = { onAction(UiAction.OnBackPress) },
                onInfo = { onAction(UiAction.OnInfoClick) },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (uiState) {
                    is UiState.Loading -> TDLoadingBar()
                    is UiState.Error -> ErrorContent(messageRes = uiState.messageRes)
                    is UiState.Editing -> EditingContent(state = uiState, onAction = onAction)
                }
            }
        }
        if (uiState is UiState.Editing) {
            FloatingNotebookButton(
                iconRes = com.todoapp.mobile.R.drawable.ic_polaroid,
                tintIcon = false,
                buttonSize = 56.dp,
                iconSize = 44.dp,
                contentDescription = stringResource(string.journal_polaroid_fab_content_description),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp),
                onClick = { requestCamera() },
            )
        }
    }

    if (uiState is UiState.Editing) {
        val fullscreenPath = uiState.fullscreenPath
        if (fullscreenPath != null) {
            com.todoapp.uikit.components.TDFullscreenImageViewer(
                model = java.io.File(fullscreenPath),
                onDismiss = { onAction(UiAction.OnDismissFullscreen) },
            )
        }
        if (uiState.showInfoDialog) {
            TDFeatureExplainer(
                title = stringResource(string.journal_entry_info_title),
                description = stringResource(string.journal_entry_info_description),
                bulletPoints = listOf(
                    stringResource(string.journal_entry_info_bullet_1),
                    stringResource(string.journal_entry_info_bullet_2),
                    stringResource(string.journal_entry_info_bullet_3),
                ),
                buttonText = stringResource(string.got_it),
                onDismiss = { onAction(UiAction.OnDismissInfoDialog) },
            )
        }
    }
}

@Composable
private fun FloatingNotebookButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintIcon: Boolean = true,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    IconButton(
        modifier = modifier
            .size(buttonSize)
            .background(TDTheme.colors.background.copy(alpha = 0.7f), CircleShape),
        onClick = onClick,
    ) {
        if (tintIcon) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(iconSize),
            )
        } else {
            // Multicolor artwork (the polaroid icon): render with Image so its colors survive —
            // Icon would flatten everything to the tint color.
            Image(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun EditingContent(
    state: UiState.Editing,
    onAction: (UiAction) -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onAction(UiAction.OnPhotoPicked(uri))
    }

    val scrollState = rememberScrollState()
    val handwriting = TDTheme.typography.journalHandwritingStyle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        DateBanner(createdAt = state.createdAt, isNew = state.isNew)
        Spacer(modifier = Modifier.height(8.dp))

        TDMoodSelector(
            options = ORDERED_MOODS.map { TDMoodOption(emoji = it.emoji, label = stringResource(it.labelRes)) },
            selectedIndex = moodIndex(state.mood),
            onSelect = { index ->
                val newMood = if (index in ORDERED_MOODS.indices) ORDERED_MOODS[index].mood else null
                onAction(UiAction.OnMoodSelect(newMood))
            },
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            if (state.content.isEmpty()) {
                TDText(
                    text = stringResource(string.journal_entry_placeholder),
                    style = handwriting,
                    // Paper is cream in both themes, so use a muted dark ink (not the theme-adaptive gray).
                    color = TDTheme.colors.black.copy(alpha = 0.4f),
                )
            }
            BasicTextField(
                value = state.content,
                onValueChange = { onAction(UiAction.OnContentChange(it)) },
                // journalHandwritingStyle bakes onBackground (light in dark mode); the paper is always
                // cream, so override to fixed dark ink so the writing stays readable in dark mode.
                textStyle = handwriting.copy(color = TDTheme.colors.black),
                cursorBrush = SolidColor(TDTheme.colors.crossRed),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(28.dp))

        JournalEntryPhotoStrip(
            paths = state.photoPaths,
            onAddClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = { onAction(UiAction.OnPhotoRemove(it)) },
            onPhotoTap = { onAction(UiAction.OnPhotoTap(it)) },
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DateBanner(createdAt: Long?, isNew: Boolean) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val text = if (isNew || createdAt == null) {
        stringResource(string.journal_entry_new)
    } else {
        val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        DateTimeFormatter.ofPattern(DATE_PATTERN, locale).format(date)
    }
    TDText(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = TDTheme.typography.subheading2,
        color = TDTheme.colors.pendingGray,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorContent(messageRes: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TDText(
            text = stringResource(messageRes),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.black,
        )
    }
}

private const val DATE_PATTERN = "EEEE, d MMMM yyyy"

@TDPreviewForm
@Composable
private fun JournalEntryNewPreview() {
    TDTheme {
        JournalEntryScreen(
            uiState = JournalEntryPreviewData.new(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewForm
@Composable
private fun JournalEntryEditingPreview() {
    TDTheme {
        JournalEntryScreen(
            uiState = JournalEntryPreviewData.existing(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewForm
@Composable
private fun JournalEntryLoadingPreview() {
    TDTheme {
        JournalEntryScreen(
            uiState = UiState.Loading,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewForm
@Composable
private fun JournalEntryErrorPreview() {
    TDTheme {
        JournalEntryScreen(
            uiState = UiState.Error(messageRes = string.journal_entry_load_error),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewForm
@Composable
private fun JournalEntryInfoDialogPreview() {
    TDTheme {
        JournalEntryScreen(
            uiState = JournalEntryPreviewData.infoDialog(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}
