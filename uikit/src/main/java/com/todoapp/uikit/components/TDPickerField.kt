package com.todoapp.uikit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewForm
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDPickerField(
    title: String?,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    emphasizeValue: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(top = 8.dp),
    ) {
        TDText(
            text = title,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.onBackground,
        )

        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            TDCompactOutlinedTextField(
                enabled = false,
                label = null,
                value = value.orEmpty(),
                onValueChange = { },
                modifier =
                Modifier
                    .fillMaxWidth(),
                disabledTextColor = if (emphasizeValue) {
                    TDTheme.colors.onBackground
                } else {
                    TDTheme.colors.onSurface.copy(alpha = 0.38f)
                },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                isError = isError,
                supportingText = supportingText,
            )
        }
    }
}

@TDPreviewForm
@Composable
private fun TDPickerFieldEmptyPreview() {
    TDTheme {
        TDPickerField(
            title = "Pick a date",
            value = null,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_calendar2),
                    contentDescription = null,
                )
            },
        )
    }
}

@TDPreviewForm
@Composable
private fun TDPickerFieldFilledPreview() {
    TDTheme {
        TDPickerField(
            title = "Deadline",
            value = "21.02.2002",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_calendar2),
                    contentDescription = null,
                )
            },
        )
    }
}

@TDPreviewForm
@Composable
fun TDPickerFieldErrorPreview() {
    TDTheme {
        TDPickerField(
            title = "Pick a date",
            value = "21.02.2002",
            onClick = {},
            isError = true,
            supportingText = "End time must be after start time",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_calendar2),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_close),
                    contentDescription = null,
                )
            },
        )
    }
}
