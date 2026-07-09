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
import com.todoapp.uikit.theme.TDTheme

/**
 * Project-standard toggle: Material3 [Switch] with the app's checked colors. The unchecked state
 * keeps Material defaults so it renders pixel-identical to the existing inline Switch call sites.
 */
@Composable
fun TDSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TDTheme.colors.white,
            checkedTrackColor = TDTheme.colors.pendingGray,
        ),
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
        }
    }
}
