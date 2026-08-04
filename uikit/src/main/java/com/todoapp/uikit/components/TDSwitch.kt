package com.todoapp.uikit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDTheme

private const val DISABLED_TRACK_ALPHA = 0.4f
private const val DISABLED_CONTENT_ALPHA = 0.5f

/** Which checked palette a switch uses in the rounded kits. */
enum class TDSwitchTone {
    /** `pendingGray` track + white thumb — settings rows. */
    NEUTRAL,

    /** `lightPurple` track + `purple` thumb — the "all day" rows in the task forms. */
    ACCENT,
}

/**
 * Project-standard toggle.
 *
 * **Rounded kits keep exactly what each call site rendered before** — [TDSwitchTone] exists only to
 * preserve the two switch identities that already existed in the app (settings rows vs the "all day"
 * rows), so routing the inline `Switch(...)` call sites through here changes nothing for them.
 *
 * **The 8-Bit kit overrides all twelve slots.** Two reasons it has to be all twelve rather than just
 * the checked pair:
 * - `TDTheme` never wraps `MaterialTheme`, so every un-passed slot resolves against M3's *baseline
 *   light* colour scheme — a stock lilac that follows neither the palette nor dark mode. The old
 *   unchecked track was brighter than the checked one in dark mode.
 * - The `disabled*` defaults composite over that same baseline `surface`, which is near-white and so
 *   effectively invisible on this app's dark surfaces.
 *
 * ON is green and OFF is neutral grey with a hard border, so the state reads from across the screen
 * rather than as a subtle tint.
 */
@Composable
fun TDSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: TDSwitchTone = TDSwitchTone.NEUTRAL,
) {
    val colors = if (TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL) {
        SwitchDefaults.colors(
            checkedThumbColor = TDTheme.colors.white,
            checkedTrackColor = TDTheme.colors.mediumGreen,
            checkedBorderColor = TDTheme.colors.onBackground,
            uncheckedThumbColor = TDTheme.colors.gray,
            uncheckedTrackColor = TDTheme.colors.lightPending,
            uncheckedBorderColor = TDTheme.colors.onBackground,
            disabledCheckedThumbColor = TDTheme.colors.white.copy(alpha = DISABLED_CONTENT_ALPHA),
            disabledCheckedTrackColor = TDTheme.colors.mediumGreen.copy(alpha = DISABLED_TRACK_ALPHA),
            disabledCheckedBorderColor = TDTheme.colors.onBackground.copy(alpha = DISABLED_TRACK_ALPHA),
            disabledUncheckedThumbColor = TDTheme.colors.gray.copy(alpha = DISABLED_CONTENT_ALPHA),
            disabledUncheckedTrackColor = TDTheme.colors.lightPending.copy(alpha = DISABLED_TRACK_ALPHA),
            disabledUncheckedBorderColor = TDTheme.colors.onBackground.copy(alpha = DISABLED_TRACK_ALPHA),
        )
    } else {
        when (tone) {
            TDSwitchTone.NEUTRAL -> SwitchDefaults.colors(
                checkedThumbColor = TDTheme.colors.white,
                checkedTrackColor = TDTheme.colors.pendingGray,
            )
            TDSwitchTone.ACCENT -> SwitchDefaults.colors(
                checkedThumbColor = TDTheme.colors.purple,
                checkedTrackColor = TDTheme.colors.lightPurple,
            )
        }
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}

@TDPreview
@Composable
private fun TdSwitchStatesPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDSwitch(checked = true, onCheckedChange = {})
            TDSwitch(checked = false, onCheckedChange = {})
            TDSwitch(checked = true, onCheckedChange = {}, enabled = false)
            TDSwitch(checked = false, onCheckedChange = {}, enabled = false)
            TDSwitch(checked = true, onCheckedChange = {}, tone = TDSwitchTone.ACCENT)
            TDSwitch(checked = false, onCheckedChange = {}, tone = TDSwitchTone.ACCENT)
        }
    }
}
