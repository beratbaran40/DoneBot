package com.todoapp.uikit.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.uikit.R.drawable
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

/**
 * Soft-tone destructive confirmation dialog where DoneBot speaks to the user.
 *
 * Use only for flows where character empathy is appropriate (e.g. account
 * delete, leaving a long-term group). For neutral confirmations use a regular
 * [androidx.compose.material3.AlertDialog] directly.
 *
 * Layout (top → bottom, all centered horizontally):
 *   1. Avatar in a circle frame (chat persona pattern), wrapped in a soft
 *      radial halo. Avatar gently breathes (scale 1.0↔1.03, 3s cycle).
 *   2. Speech bubble with the empathic copy, fading in 300ms after the
 *      dialog opens.
 *   3. Smaller legal-detail line.
 *   4. Type-to-confirm input (TDOutlinedTextField, destructive variant).
 *   5. Buttons row, or processing indicator with status text.
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
    val inPreview = LocalInspectionMode.current
    val animationsEnabled = !inPreview && !reduceMotion

    var typed by remember { mutableStateOf("") }
    val canConfirm = typed == typedConfirmWord && !isProcessing

    var bubbleVisible by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            kotlinx.coroutines.delay(50)
            bubbleVisible = true
        } else {
            bubbleVisible = true
        }
    }

    val breathingScale = if (animationsEnabled) {
        val infinite = rememberInfiniteTransition(label = "donebotBreathing")
        val scale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathingScale",
        )
        scale
    } else {
        1f
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing,
        ),
    ) {
        ObscuredTouchGuard()
        Surface(
            modifier = modifier
                .widthIn(min = 280.dp, max = 360.dp),
            shape = tdCorner(24.dp),
            color = TDTheme.colors.lightPending,
            tonalElevation = 8.dp,
        ) {
            Column(
                // verticalScroll keeps the avatar, copy, type-to-confirm field and buttons all
                // reachable when the dialog is taller than a short landscape viewport.
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarWithHalo(
                    avatarRes = avatarRes,
                    breathingScale = breathingScale,
                )

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = bubbleVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                ) {
                    SpeechBubble(text = speechBubbleText)
                }

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
    }
}

@Composable
private fun AvatarWithHalo(
    @DrawableRes avatarRes: Int,
    breathingScale: Float,
) {
    val haloBrush = if (TDTheme.isDark) {
        Brush.radialGradient(
            colors = listOf(
                TDTheme.colors.onBackground,
                Color.Transparent,
            ),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                TDTheme.colors.onBackground,
                TDTheme.colors.pendingGray,
                Color.Transparent,
            ),
        )
    }
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(brush = haloBrush, shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .scale(breathingScale)
                .size(96.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.lightPending),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = rememberPixelPainter(painterResource(avatarRes), 88.dp),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(TDTheme.shapes.large)
            .background(TDTheme.colors.lightPending)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TDText(
            text = text,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.darkPending,
            textAlign = TextAlign.Center,
        )
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
// endregion
