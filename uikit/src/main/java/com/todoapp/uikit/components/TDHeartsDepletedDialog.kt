package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uikit.R.drawable
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

/**
 * Character-driven "you ran out of hearts" dialog for the Activity health bar. Same mascot, halo and
 * fade-in speech bubble as every other [TDMascotDialog], with a single acknowledgement CTA and no
 * type-to-confirm — a row of empty hearts drives the "streak ended" message home.
 */
@Composable
fun TDHeartsDepletedDialog(
    speechBubbleText: String,
    buttonText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes avatarRes: Int = drawable.img_donebot_sad,
    reduceMotion: Boolean = false,
) {
    TDMascotDialog(
        speechBubbleText = speechBubbleText,
        onDismiss = onDismiss,
        modifier = modifier,
        avatarRes = avatarRes,
        reduceMotion = reduceMotion,
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        TDHealthBar(
            halfHearts = 0,
            contentDescription = "",
            heartSize = 18.dp,
            animate = false,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TDButton(
            text = buttonText,
            onClick = onDismiss,
            type = TDButtonType.PRIMARY,
            fullWidth = true,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewEn() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Your task streak ended! Pick up where you left off and win your hearts back.",
            buttonText = "Keep going",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewTr() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Görev serin sona erdi! Kaldığın yerden devam et, kalplerini yeniden kazan.",
            buttonText = "Devam et",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewReducedMotion() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Your task streak ended! Pick up where you left off and win your hearts back.",
            buttonText = "Keep going",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
            reduceMotion = true,
        )
    }
}
