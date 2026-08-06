package com.todoapp.mobile.ui.pomodoro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

@StringRes
fun PomodoroAmbience.labelRes(): Int = when (this) {
    PomodoroAmbience.None -> R.string.pomodoro_ambience_none
    PomodoroAmbience.Fireplace -> R.string.pomodoro_ambience_fireplace
    PomodoroAmbience.Rain -> R.string.pomodoro_ambience_rain
    PomodoroAmbience.Handpan -> R.string.pomodoro_ambience_handpan
}

@DrawableRes
fun PomodoroAmbience.iconRes(): Int = when (this) {
    PomodoroAmbience.None -> R.drawable.ic_ambience_none
    PomodoroAmbience.Fireplace -> R.drawable.ic_ambience_fireplace
    PomodoroAmbience.Rain -> R.drawable.ic_ambience_rain
    PomodoroAmbience.Handpan -> R.drawable.ic_ambience_handpan
}

/**
 * Soundscape picker. Chrome colours rather than the mode palette, like the finish-early dialog —
 * the sheet sits above the ambience, it isn't part of it.
 *
 * There is no separate "preview" affordance: reaching this sheet means a session is running, so a
 * tap swaps what is already playing and the choice is audible immediately.
 *
 * [notificationsBlocked] is passed in rather than read here so the previews can render both sides of
 * it. See [com.todoapp.mobile.ui.permissions.rememberNotificationPermissionGate].
 */
@Composable
fun PomodoroAmbienceSheet(
    selected: PomodoroAmbience,
    volume: Float,
    backgroundEnabled: Boolean,
    notificationsBlocked: Boolean,
    onFixNotifications: () -> Unit,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TDText(
            text = stringResource(R.string.pomodoro_ambience_title),
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        TDText(
            text = stringResource(R.string.pomodoro_ambience_subtitle),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.gray,
        )

        Spacer(Modifier.height(16.dp))

        PomodoroAmbience.selectable.forEach { ambience ->
            AmbienceRow(
                ambience = ambience,
                isSelected = ambience == selected,
                onClick = { onAction(UiAction.OnAmbienceSelected(ambience)) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        TDText(
            text = stringResource(R.string.pomodoro_ambience_volume),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
        Slider(
            value = volume,
            onValueChange = { onAction(UiAction.OnAmbienceVolumeChange(it)) },
            enabled = selected != PomodoroAmbience.None,
            colors =
            SliderDefaults.colors(
                thumbColor = TDTheme.colors.primary,
                activeTrackColor = TDTheme.colors.primary,
                inactiveTrackColor = TDTheme.colors.lightGray,
                disabledThumbColor = TDTheme.colors.lightGray,
                disabledActiveTrackColor = TDTheme.colors.lightGray,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                TDText(
                    text = stringResource(R.string.pomodoro_ambience_background_title),
                    style = TDTheme.typography.subheading1,
                    color = if (notificationsBlocked) TDTheme.colors.gray else TDTheme.colors.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                TDText(
                    text = stringResource(
                        if (notificationsBlocked) {
                            R.string.pomodoro_ambience_background_needs_notifications
                        } else {
                            R.string.pomodoro_ambience_background_desc
                        },
                    ),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
            // Without notification permission there is no foreground service, and background audio
            // with nothing holding the process up gets cut off mid-loop. A switch that cannot do
            // what it says should not pretend otherwise.
            TDSwitch(
                checked = backgroundEnabled && !notificationsBlocked,
                onCheckedChange = { onAction(UiAction.OnAmbienceBackgroundToggle(it)) },
                enabled = !notificationsBlocked,
            )
        }

        if (notificationsBlocked) {
            TextButton(
                onClick = onFixNotifications,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
            ) {
                TDText(
                    text = stringResource(R.string.pomodoro_ambience_background_allow_notifications),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.purple,
                )
            }
        }
    }
}

@Composable
private fun AmbienceRow(
    ambience: PomodoroAmbience,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = if (isSelected) TDTheme.colors.primary else TDTheme.colors.lightPending
    val ink = if (isSelected) TDTheme.colors.background else TDTheme.colors.onBackground

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(TDTheme.shapes.large)
            .clickable(onClick = onClick)
            .background(fill)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = tdPainter(ambience.iconRes()),
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(22.dp),
        )
        TDText(
            text = stringResource(ambience.labelRes()),
            style = TDTheme.typography.heading6,
            color = ink,
        )
    }
}

/**
 * The affordance that opens [PomodoroAmbienceSheet]. Shows the active soundscape's own glyph, so
 * what is playing is legible without opening anything.
 */
@Composable
fun PomodoroAmbienceButton(
    ambience: PomodoroAmbience,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.pomodoro_ambience_open)
    Box(
        modifier =
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = BUTTON_FILL_ALPHA))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = tdPainter(if (ambience == PomodoroAmbience.None) R.drawable.ic_ambience else ambience.iconRes()),
            contentDescription = null,
            tint = contentColor.copy(alpha = BUTTON_INK_ALPHA),
            modifier = Modifier.size(20.dp),
        )
    }
}

private const val BUTTON_FILL_ALPHA = 0.10f
private const val BUTTON_INK_ALPHA = 0.75f

// ── Previews ──────────────────────────────────────────────────────────────────

@TDPreviewDialog
@Composable
private fun PomodoroAmbienceSheetPreview() {
    TDTheme {
        PomodoroAmbienceSheet(
            selected = PomodoroAmbience.Rain,
            volume = 0.6f,
            backgroundEnabled = false,
            notificationsBlocked = false,
            onFixNotifications = {},
            onAction = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun PomodoroAmbienceSheetSilentPreview() {
    TDTheme {
        PomodoroAmbienceSheet(
            selected = PomodoroAmbience.None,
            volume = 0.6f,
            backgroundEnabled = true,
            notificationsBlocked = false,
            onFixNotifications = {},
            onAction = {},
        )
    }
}

/** Notifications denied: the background toggle is off the table and says why. */
@TDPreviewDialog
@Composable
private fun PomodoroAmbienceSheetNotificationsBlockedPreview() {
    TDTheme {
        PomodoroAmbienceSheet(
            selected = PomodoroAmbience.Fireplace,
            volume = 0.6f,
            // Stored as on from before the permission was revoked — the row must still read as off.
            backgroundEnabled = true,
            notificationsBlocked = true,
            onFixNotifications = {},
            onAction = {},
        )
    }
}
