package com.todoapp.uikit.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R.drawable
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

/**
 * Soft-tone destructive confirmation dialog where DoneBot speaks to the user.
 *
 * Use only for flows where character empathy is appropriate (e.g. account
 * delete, leaving a long-term group). For neutral confirmations use a regular
 * [androidx.compose.material3.AlertDialog] directly.
 *
 * Mascot, halo, breathing and speech bubble come from [TDMascotDialog]; what this adds below them is
 * the legal detail line, the type-to-confirm field and the buttons — or, once the delete is running,
 * a progress indicator in their place with the dialog pinned open.
 *
 * Animations are skipped automatically in `LocalInspectionMode` (preview
 * stability) and when [reduceMotion] is true (accessibility setting).
 */
@Composable
fun TDGoodbyeDialog(
    speechBubbleText: String,
    legalDetailText: String,
    typedConfirmLabel: String,
    typedConfirmWord: String,
    confirmButtonText: String,
    dismissButtonText: String,
    inProgressText: String,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes avatarRes: Int = drawable.img_donebot_sad,
    reduceMotion: Boolean = false,
) {
    var typed by remember { mutableStateOf("") }
    val canConfirm = typed == typedConfirmWord && !isProcessing

    TDMascotDialog(
        speechBubbleText = speechBubbleText,
        onDismiss = onDismiss,
        modifier = modifier,
        avatarRes = avatarRes,
        reduceMotion = reduceMotion,
        // A delete already in flight cannot be taken back, so nothing may close the dialog under it.
        dismissable = !isProcessing,
        obscuredTouchGuard = true,
        // Bubble colour matched to the surface: here the copy floats on the card rather than sitting
        // in a bubble of its own. Kept as shipped.
        bubbleColor = TDTheme.colors.lightPending,
        bubbleTextColor = TDTheme.colors.darkPending,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        TDText(
            text = legalDetailText,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.gray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        TDOutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = typedConfirmLabel,
            destructive = true,
            enabled = !isProcessing,
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isProcessing) {
            ProcessingFooter(text = inProgressText)
        } else {
            ButtonsRow(
                confirmText = confirmButtonText,
                dismissText = dismissButtonText,
                canConfirm = canConfirm,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ProcessingFooter(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = TDTheme.colors.purple,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TDText(
                text = text,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.gray,
            )
        }
    }
}

@Composable
private fun ButtonsRow(
    confirmText: String,
    dismissText: String,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) {
            TDText(
                text = dismissText,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.darkPending,
            )
        }
        FilledTonalButton(
            onClick = onConfirm,
            enabled = canConfirm,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = TDTheme.colors.crossRed,
                contentColor = TDTheme.colors.white,
                disabledContainerColor = TDTheme.colors.lightGray.copy(alpha = 0.3f),
                disabledContentColor = TDTheme.colors.gray,
            ),
        ) {
            TDText(
                text = confirmText,
                style = TDTheme.typography.subheading1,
                color = if (canConfirm) TDTheme.colors.white else TDTheme.colors.gray,
            )
        }
    }
}

@SuppressLint("NonConstantResourceId")
private const val PREVIEW_AVATAR_RES = drawable.ic_bot

@TDPreviewDialog
@Composable
private fun TDGoodbyeDialogPreview_Idle() {
    TDTheme {
        TDGoodbyeDialog(
            speechBubbleText = "Wait… are you really leaving?",
            legalDetailText = "All your tasks, chats, and groups will be deleted.",
            typedConfirmLabel = "Type DELETE to confirm",
            typedConfirmWord = "DELETE",
            confirmButtonText = "Delete forever",
            dismissButtonText = "Cancel",
            inProgressText = "Deleting account…",
            isProcessing = false,
            onDismiss = {},
            onConfirm = {},
            avatarRes = PREVIEW_AVATAR_RES,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDGoodbyeDialogPreview_Typed() {
    TDTheme {
        TDGoodbyeDialog(
            speechBubbleText = "Dur biraz… gerçekten gidiyor musun?",
            legalDetailText = "Tüm görevlerin, sohbetlerin ve grupların silinecek.",
            typedConfirmLabel = "Onaylamak için HESABI SİL yaz",
            typedConfirmWord = "HESABI SİL",
            confirmButtonText = "Kalıcı olarak sil",
            dismissButtonText = "Vazgeç",
            inProgressText = "Hesap siliniyor…",
            isProcessing = false,
            onDismiss = {},
            onConfirm = {},
            avatarRes = PREVIEW_AVATAR_RES,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDGoodbyeDialogPreview_Processing() {
    TDTheme {
        TDGoodbyeDialog(
            speechBubbleText = "Wait… are you really leaving?",
            legalDetailText = "All your tasks, chats, and groups will be deleted.",
            typedConfirmLabel = "Type DELETE to confirm",
            typedConfirmWord = "DELETE",
            confirmButtonText = "Delete forever",
            dismissButtonText = "Cancel",
            inProgressText = "Deleting account…",
            isProcessing = true,
            onDismiss = {},
            onConfirm = {},
            avatarRes = PREVIEW_AVATAR_RES,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDGoodbyeDialogPreview_ReducedMotion() {
    TDTheme {
        TDGoodbyeDialog(
            speechBubbleText = "Wait… are you really leaving?",
            legalDetailText = "All your tasks, chats, and groups will be deleted.",
            typedConfirmLabel = "Type DELETE to confirm",
            typedConfirmWord = "DELETE",
            confirmButtonText = "Delete forever",
            dismissButtonText = "Cancel",
            inProgressText = "Deleting account…",
            isProcessing = false,
            onDismiss = {},
            onConfirm = {},
            avatarRes = PREVIEW_AVATAR_RES,
            reduceMotion = true,
        )
    }
}
