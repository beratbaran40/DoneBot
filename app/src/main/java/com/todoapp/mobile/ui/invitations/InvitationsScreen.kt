package com.todoapp.mobile.ui.invitations

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Invitation
import com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar
import com.todoapp.mobile.ui.invitations.InvitationsContract.PendingAction
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiAction
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiEffect
import com.todoapp.mobile.ui.invitations.InvitationsContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationsScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowToast -> Toast.makeText(context, it.resId, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (uiState) {
            is UiState.Loading -> Loading()
            is UiState.Error -> ErrorBlock(uiState.message) { onAction(UiAction.OnRetry) }
            is UiState.Success -> PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onAction(UiAction.OnPullToRefresh) },
            ) {
                if (uiState.items.isEmpty()) {
                    EmptyBlock()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(items = uiState.items, key = { it.id }) { item ->
                            InvitationCard(
                                invitation = item,
                                isProcessing = item.id in uiState.processingIds,
                                onAccept = { onAction(UiAction.OnAccept(item.id)) },
                                onDecline = { onAction(UiAction.OnDecline(item.id)) },
                            )
                        }
                    }
                }
            }
        }

        if (uiState is UiState.Success) {
            uiState.pendingAction?.let { pending ->
                ConfirmDialog(
                    pending = pending,
                    onConfirm = { onAction(UiAction.OnConfirmPending) },
                    onDismiss = { onAction(UiAction.OnDismissPending) },
                )
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TDTheme.colors.purple)
    }
}

@Composable
private fun EmptyBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TDTheme.colors.bgColorPurple),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = tdPainter(com.example.uikit.R.drawable.ic_members),
                    contentDescription = null,
                    tint = TDTheme.colors.purple,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            TDText(
                text = stringResource(R.string.invitations_empty),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            TDText(
                text = stringResource(R.string.invitations_empty_subtitle),
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.pendingGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TDText(
                text = message,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.crossRed,
            )
            Spacer(Modifier.height(12.dp))
            TDButton(
                text = stringResource(R.string.retry),
                type = TDButtonType.OUTLINE,
                size = TDButtonSize.SMALL,
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: Invitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = TDTheme.isDark
    val cardModifier = Modifier
        .fillMaxWidth()
        .let { base ->
            if (isDark) {
                base.border(
                    width = 1.dp,
                    color = TDTheme.colors.lightGray.copy(alpha = 0.20f),
                    shape = TDTheme.shapes.large,
                )
            } else {
                base.tdShadow(
                    lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                    darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
                    cornerRadius = 16.dp,
                    elevation = 8.dp,
                )
            }
        }
        .clip(TDTheme.shapes.large)
        .background(TDTheme.colors.lightPending)
        .padding(16.dp)
    Column(modifier = cardModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemberAvatar(
                initials = initialsFor(invitation.groupName),
                size = 48,
                avatarUrl = invitation.groupAvatarUrl,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = invitation.groupName.ifBlank { stringResource(R.string.invitations_title) },
                    style = TDTheme.typography.heading4,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                TDText(
                    text = stringResource(
                        R.string.invitations_invited_by,
                        invitation.inviterName.ifBlank { invitation.inviteeEmail },
                    ),
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.pendingGray,
                )
                Spacer(Modifier.height(2.dp))
                TDText(
                    text = relativeTimeFor(invitation.createdAt, context),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.pendingGray,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Two SMALL buttons ask for 140 + 10 + 140 = 290dp; a card on a 360dp screen offers 296dp.
        // Six dp is not headroom, and at 320dp the pair is over-subscribed outright, which makes the
        // second button render narrower than the first. Weighting removes the cliff.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.invitations_decline),
                isEnable = !isProcessing,
                type = TDButtonType.OUTLINE,
                size = TDButtonSize.SMALL,
                onClick = onDecline,
            )
            TDButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.invitations_accept),
                isEnable = !isProcessing,
                type = TDButtonType.PRIMARY,
                size = TDButtonSize.SMALL,
                onClick = onAccept,
            )
        }
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun ConfirmDialog(
    pending: PendingAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, message, confirmLabel, dismissLabel, isDestructive) = when (pending) {
        is PendingAction.Accept -> ConfirmCopy(
            title = stringResource(R.string.invitation_accept_dialog_title),
            message = stringResource(R.string.invitation_accept_dialog_message, pending.invitation.groupName),
            confirmLabel = stringResource(R.string.invitations_accept),
            dismissLabel = stringResource(R.string.cancel),
            isDestructive = false,
        )
        is PendingAction.Decline -> ConfirmCopy(
            title = stringResource(R.string.invitation_decline_dialog_title),
            message = stringResource(
                R.string.invitation_decline_dialog_message,
                pending.invitation.inviterName.ifBlank { pending.invitation.inviteeEmail },
            ),
            confirmLabel = stringResource(R.string.invitations_decline),
            dismissLabel = stringResource(R.string.invitations_keep),
            isDestructive = true,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TDTheme.colors.background,
        title = {
            TDText(
                text = title,
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            TDText(
                text = message,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                TDText(
                    text = confirmLabel,
                    color = if (isDestructive) TDTheme.colors.crossRed else TDTheme.colors.purple,
                    style = TDTheme.typography.subheading2,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TDText(
                    text = dismissLabel,
                    color = TDTheme.colors.onBackground,
                    style = TDTheme.typography.subheading2,
                )
            }
        },
    )
}

private data class ConfirmCopy(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String,
    val isDestructive: Boolean,
)

private fun initialsFor(name: String): String = name
    .split(" ")
    .mapNotNull { it.firstOrNull()?.toString() }
    .take(2)
    .joinToString("")
    .uppercase()

private fun relativeTimeFor(createdAt: Long, context: Context): String {
    val deltaMs = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMs)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMs)
    return when {
        minutes < 1L -> context.getString(R.string.just_now)
        minutes < 60L -> context.getString(R.string.minutes_ago, minutes.toInt())
        hours < 24L -> context.getString(R.string.hours_ago, hours.toInt())
        days < 2L -> context.getString(R.string.yesterday)
        else -> context.getString(R.string.days_ago, days.toInt())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@TDPreview
@Composable
private fun InvitationsScreenPreview(
    @PreviewParameter(InvitationsPreviewProvider::class) state: UiState,
) {
    TDTheme {
        InvitationsScreen(
            uiState = state,
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

/**
 * The Turkish accept/decline pair inside a card: two SMALL buttons want 290dp of the ~296dp a card
 * on a narrow phone offers, so this is where an unweighted pair went lopsided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@com.todoapp.uikit.previews.TDPreviewNarrow
@Composable
private fun InvitationsScreenNarrowPreview() {
    TDTheme {
        InvitationsScreen(
            uiState =
            UiState.Success(
                items =
                listOf(
                    Invitation(
                        id = 1,
                        groupId = 100,
                        groupName = "Pazartesi Akşamı Kitap Kulübü",
                        groupAvatarUrl = null,
                        inviterUserId = 10,
                        inviterName = "Berat Baran",
                        inviterAvatarUrl = null,
                        inviteeEmail = "you@example.com",
                        createdAt = 0L,
                    ),
                ),
            ),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}
