package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R.drawable
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

/**
 * "There's a newer version" prompt, in DoneBot's own voice rather than a system-styled alert.
 *
 * Deliberately not blocking: [onDismiss] closes it and the app stays fully usable. It is expected to
 * come back on the next launch, which is the caller's business — this composable only draws what it
 * is told to draw.
 *
 * The mascot is a required argument, with no default: this dialog's whole identity is the splash
 * robot, which lives in `:app`, and a `:uikit` default would only ever be the wrong one.
 */
@Composable
fun TDUpdateAvailableDialog(
    speechBubbleText: String,
    detailText: String,
    updateButtonText: String,
    laterButtonText: String,
    @DrawableRes avatarRes: Int,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    TDMascotDialog(
        speechBubbleText = speechBubbleText,
        onDismiss = onDismiss,
        modifier = modifier,
        avatarRes = avatarRes,
        reduceMotion = reduceMotion,
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        TDText(
            text = detailText,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.gray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Full-width primary over a quiet text button, rather than the side-by-side row the
        // destructive dialog uses: here one of the two answers is plainly the recommended one, and
        // nothing is lost by taking it.
        TDButton(
            text = updateButtonText,
            onClick = onUpdate,
            type = TDButtonType.PRIMARY,
            fullWidth = true,
        )

        TextButton(onClick = onDismiss) {
            TDText(
                text = laterButtonText,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
    }
}

@TDPreviewDialog
@Composable
private fun TDUpdateAvailableDialogPreviewEn() {
    TDTheme {
        TDUpdateAvailableDialog(
            speechBubbleText = "I've picked up a few new tricks! Grab the latest version so we stay in sync.",
            detailText = "The update installs from Google Play and takes a moment.",
            updateButtonText = "Update",
            laterButtonText = "Not now",
            avatarRes = drawable.ic_bot,
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDUpdateAvailableDialogPreviewTr() {
    TDTheme {
        TDUpdateAvailableDialog(
            speechBubbleText = "Birkaç yeni numara öğrendim! En son sürüme geçelim de aynı yerde olalım.",
            detailText = "Güncelleme Google Play üzerinden kurulur, kısa sürer.",
            updateButtonText = "Güncelle",
            laterButtonText = "Şimdi değil",
            avatarRes = drawable.ic_bot,
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDUpdateAvailableDialogPreviewLongCopy() {
    TDTheme {
        TDUpdateAvailableDialog(
            speechBubbleText =
            "There's a newer version of DoneBot waiting for you, with fixes for reminders, " +
                "the calendar and a handful of things you told me were annoying.",
            detailText = "The update installs from Google Play and takes a moment.",
            updateButtonText = "Update now",
            laterButtonText = "Maybe later",
            avatarRes = drawable.ic_bot,
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDUpdateAvailableDialogPreviewReducedMotion() {
    TDTheme {
        TDUpdateAvailableDialog(
            speechBubbleText = "I've picked up a few new tricks! Grab the latest version so we stay in sync.",
            detailText = "The update installs from Google Play and takes a moment.",
            updateButtonText = "Update",
            laterButtonText = "Not now",
            avatarRes = drawable.ic_bot,
            onUpdate = {},
            onDismiss = {},
            reduceMotion = true,
        )
    }
}
