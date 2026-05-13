@file:Suppress("MatchingDeclarationName")

package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.modifier.neumorphicShadow
import com.todoapp.uikit.theme.TDTheme

/**
 * Display field for an optional location attached to a task. Stateless / primitive-only —
 * the actual place-picking UX (autocomplete + map preview + permission prompt) lives in the
 * app module so this component can stay free of Maps SDK dependencies. Tap [onClick] to open
 * the picker; tap [onClear] (only visible when filled) to remove the location without going
 * through the picker.
 *
 * @param name short label, e.g. "Acıbadem Hastanesi". Empty/null → empty state.
 * @param address fuller address line. Optional secondary text when both are set.
 * @param addLabel localized "Add location" placeholder shown in the empty state.
 * @param clearContentDescription localized a11y label for the clear button.
 */
@Composable
fun TDLocationPicker(
    name: String?,
    address: String?,
    addLabel: String,
    clearContentDescription: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = TDTheme.isDark
    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
    val elevated = if (isDark) {
        baseModifier.border(
            width = 1.dp,
            color = TDTheme.colors.lightGray.copy(alpha = 0.20f),
            shape = RoundedCornerShape(20.dp),
        )
    } else {
        baseModifier.neumorphicShadow(
            lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
            darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
            cornerRadius = 20.dp,
            elevation = 4.dp,
        )
    }

    val filledName = name?.takeIf { it.isNotBlank() }
    val isFilled = filledName != null

    Row(
        modifier = modifier
            .then(elevated)
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pin),
            contentDescription = null,
            tint = if (isFilled) TDTheme.colors.purple else TDTheme.colors.darkPending,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (filledName != null) {
                TDText(
                    text = filledName,
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!address.isNullOrBlank() && address != filledName) {
                    TDText(
                        text = address,
                        style = TDTheme.typography.regularTextStyle.copy(color = TDTheme.colors.gray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                TDText(
                    text = addLabel,
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.gray,
                    maxLines = 1,
                )
            }
        }
        if (isFilled) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = clearContentDescription,
                tint = TDTheme.colors.gray,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClear),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDLocationPickerEmptyPreview() {
    TDTheme {
        TDLocationPicker(
            name = null,
            address = null,
            addLabel = "Add location",
            clearContentDescription = "Clear location",
            onClick = {},
            onClear = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDLocationPickerFilledPreview() {
    TDTheme {
        TDLocationPicker(
            name = "Acıbadem Hastanesi",
            address = "Bağdat Cd. No:123, Kadıköy/İstanbul",
            addLabel = "Add location",
            clearContentDescription = "Clear location",
            onClick = {},
            onClear = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDLocationPickerNameOnlyPreview() {
    TDTheme {
        TDLocationPicker(
            name = "Galata",
            address = null,
            addLabel = "Add location",
            clearContentDescription = "Clear location",
            onClick = {},
            onClear = {},
        )
    }
}
