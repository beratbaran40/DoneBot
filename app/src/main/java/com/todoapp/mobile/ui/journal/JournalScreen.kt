package com.todoapp.mobile.ui.journal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.mobile.ui.common.SecureScreenEffect
import com.todoapp.mobile.ui.journal.JournalContract.UiAction
import com.todoapp.mobile.ui.journal.JournalContract.UiEffect
import com.todoapp.mobile.ui.journal.JournalContract.UiState
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun JournalScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    SecureScreenEffect()
    val context = LocalContext.current
    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.ShowToast ->
                Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
            is UiEffect.ShowBiometricAuthenticator -> {
                val activity = context as? FragmentActivity ?: return@collectWithLifecycle
                val ok = BiometricAuthenticator.authenticate(activity)
                onAction(if (ok) UiAction.OnBiometricSuccess else UiAction.OnBiometricCancelled)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (uiState) {
            is UiState.Loading -> TDLoadingBar()
            is UiState.Locked -> JournalLockedState()
            is UiState.Error -> JournalErrorState(
                messageRes = uiState.messageRes,
                onRetry = { onAction(UiAction.OnRetry) },
            )
            is UiState.Success -> JournalSuccessContent(uiState = uiState, onAction = onAction)
        }

        if (uiState is UiState.Success || uiState is UiState.Error) {
            FloatingAddButton(
                onClick = { onAction(UiAction.OnAddClick) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            )
        }
    }

    if (uiState is UiState.Success && uiState.actionSheetEntry != null) {
        JournalActionSheet(
            onEdit = { onAction(UiAction.OnEditFromSheet) },
            onDelete = { onAction(UiAction.OnRequestDeleteFromSheet) },
            onDismiss = { onAction(UiAction.OnDismissActionSheet) },
        )
    }

    if (uiState is UiState.Success && uiState.pendingDeleteEntry != null) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnDismissDelete) },
            containerColor = TDTheme.colors.background,
            titleContentColor = TDTheme.colors.onBackground,
            textContentColor = TDTheme.colors.gray,
            title = { Text(stringResource(string.journal_action_delete_confirm_title)) },
            text = { Text(stringResource(string.journal_action_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnConfirmDelete) }) {
                    Text(stringResource(string.journal_action_delete), color = TDTheme.colors.crossRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnDismissDelete) }) {
                    Text(stringResource(com.example.uikit.R.string.cancel), color = TDTheme.colors.gray)
                }
            },
        )
    }
}

@Composable
private fun JournalLockedState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = tdPainter(R.drawable.ic_settings),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TDText(
            text = stringResource(string.journal_locked_message),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun JournalSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isRawListEmpty) {
            Spacer(modifier = Modifier.height(8.dp))
            JournalSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onAction(UiAction.OnSearchQueryChange(it)) },
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        when {
            uiState.isRawListEmpty ->
                JournalEmptyState(onAdd = { onAction(UiAction.OnAddClick) })
            uiState.isFilteredEmpty ->
                JournalFilteredEmptyState(onClearFilters = { onAction(UiAction.OnClearFilters) })
            else ->
                JournalList(
                    sections = uiState.sections,
                    onEntryClick = { onAction(UiAction.OnEntryClick(it)) },
                    onEntryLongPress = { onAction(UiAction.OnEntryLongPress(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

@Composable
private fun JournalErrorState(
    messageRes: Int,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TDText(
            text = stringResource(messageRes),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TDButton(
            text = stringResource(string.journal_filtered_empty_clear),
            size = TDButtonSize.SMALL,
            onClick = onRetry,
        )
    }
}

@Composable
private fun FloatingAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.pendingGray, CircleShape),
        onClick = onClick,
    ) {
        Icon(
            painter = tdPainter(R.drawable.ic_plus),
            contentDescription = stringResource(string.journal_add_entry_cd),
            tint = TDTheme.colors.background,
            modifier = Modifier.size(28.dp),
        )
    }
}

@TDPreviewWide
@Composable
private fun JournalScreenSuccessPreview() {
    TDTheme {
        JournalScreen(
            uiState = JournalPreviewData.successState(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun JournalScreenEmptyPreview() {
    TDTheme {
        JournalScreen(
            uiState = JournalPreviewData.emptyState(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun JournalScreenFilteredEmptyPreview() {
    TDTheme {
        JournalScreen(
            uiState = JournalPreviewData.filteredEmptyState(),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun JournalScreenLoadingPreview() {
    TDTheme {
        JournalScreen(
            uiState = UiState.Loading,
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun JournalScreenErrorPreview() {
    TDTheme {
        JournalScreen(
            uiState = UiState.Error(messageRes = string.journal_load_error),
            uiEffect = flowOf(),
            onAction = {},
        )
    }
}
