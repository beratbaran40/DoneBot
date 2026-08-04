package com.todoapp.mobile.ui.groups.groupdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

/**
 * One-shot dialog shown the first time a freshly created group's detail opens: nudges the creator
 * to invite the first member without leaving the screen. Friendly (not destructive) surface — no
 * ObscuredTouchGuard by convention. The send/validation state lives in GroupDetailViewModel so the
 * RESUMED-triggered reloads can't wipe an open dialog or the typed email.
 */
@Composable
internal fun GroupDetailFirstInviteDialog(
    groupName: String,
    email: String,
    @StringRes errorRes: Int?,
    isSending: Boolean,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = tdCorner(24.dp),
            color = TDTheme.colors.lightPending,
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MascotHero()
                Spacer(Modifier.height(12.dp))
                TDText(
                    text = stringResource(R.string.first_invite_dialog_title, groupName),
                    style = TDTheme.typography.heading4,
                    color = TDTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                TDText(
                    text = stringResource(R.string.invite_subtitle),
                    style = TDTheme.typography.subheading3,
                    color = TDTheme.colors.gray,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TDOutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.invite_member_email_hint),
                    leadingIcon = {
                        Icon(
                            painter = tdPainter(com.example.uikit.R.drawable.ic_mail),
                            contentDescription = null,
                            tint = TDTheme.colors.gray,
                        )
                    },
                    isError = errorRes != null,
                    supportingText = errorRes?.let { stringResource(it) },
                    enabled = !isSending,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                TDButton(
                    text = stringResource(R.string.send_invite),
                    isEnable = email.isNotBlank() && !isSending,
                    size = TDButtonSize.SMALL,
                    fullWidth = true,
                    onClick = onSend,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss) {
                    TDText(
                        text = stringResource(R.string.first_invite_later),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.gray,
                    )
                }
            }
        }
    }
}

/**
 * Top-center mascot hero: the night yearly DoneBot art, circle-cropped inside a soft purple halo
 * (same visual family as the Home empty states and TDGoodbyeDialog's avatar, kept local so the
 * uikit dialog stays untouched). Branded multicolor art → Image, never a tinted Icon.
 */
@Composable
private fun MascotHero() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TDTheme.colors.purple.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberPixelPainter(
                tdPainter(com.example.uikit.R.drawable.img_donebot_recurring_yearly_night),
                96.dp,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, TDTheme.colors.lightPurple.copy(alpha = 0.6f), CircleShape),
        )
    }
}

@TDPreview
@Composable
private fun GroupDetailFirstInviteDialogEmptyPreview() {
    TDTheme {
        GroupDetailFirstInviteDialog(
            groupName = "Smith Family",
            email = "",
            errorRes = null,
            isSending = false,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}

@TDPreview
@Composable
private fun GroupDetailFirstInviteDialogTypedPreview() {
    TDTheme {
        GroupDetailFirstInviteDialog(
            groupName = "Smith Family",
            email = "berat@example.com",
            errorRes = null,
            isSending = false,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}

@TDPreview
@Composable
private fun GroupDetailFirstInviteDialogErrorPreview() {
    TDTheme {
        GroupDetailFirstInviteDialog(
            groupName = "Smith Family",
            email = "not-an-email",
            errorRes = R.string.email_error,
            isSending = false,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}

@TDPreview
@Composable
private fun GroupDetailFirstInviteDialogSendingPreview() {
    TDTheme {
        GroupDetailFirstInviteDialog(
            groupName = "Smith Family",
            email = "berat@example.com",
            errorRes = null,
            isSending = true,
            onEmailChange = {},
            onSend = {},
            onDismiss = {},
        )
    }
}
