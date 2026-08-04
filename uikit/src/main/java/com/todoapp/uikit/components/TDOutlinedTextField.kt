package com.todoapp.uikit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewForm
import com.todoapp.uikit.theme.TDTheme

/**
 * Outlined text field that follows DoneBot's design system.
 *
 * Use this in place of raw [OutlinedTextField] when the field needs to look the
 * same as the rest of the app. For top-level form inputs prefer [TDTextField];
 * use this when you want an outlined look inside a dialog or compact surface.
 *
 * Pass [destructive] = true to flip the border + cursor to crossRed for inputs
 * that gate destructive actions (e.g. typed-confirm fields in delete flows).
 */
@Composable
fun TDOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    destructive: Boolean = false,
    passwordVisible: Boolean? = null,
    onTogglePasswordVisible: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) bringIntoViewRequester.bringIntoView()
    }

    val effectiveTrailingIcon: (@Composable () -> Unit)? = trailingIcon
        ?: if (passwordVisible != null && onTogglePasswordVisible != null) {
            {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off,
                        ),
                        contentDescription = stringResource(R.string.cd_toggle_password_visibility),
                        tint = when {
                            isError -> TDTheme.colors.red
                            isFocused -> if (destructive) TDTheme.colors.crossRed else TDTheme.colors.pendingGray
                            else -> TDTheme.colors.gray
                        },
                    )
                }
            }
        } else {
            null
        }

    val effectiveTransformation =
        if (passwordVisible != null && onTogglePasswordVisible != null && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            visualTransformation
        }

    val focusedBorder = if (destructive) TDTheme.colors.crossRed else TDTheme.colors.pendingGray
    // Destructive unfocused border: lightRed (#FFE6E7) was invisible against the dialog
    // `surface` (#FFFAF0) in light theme — both near-white. Tint with crossRed at low alpha
    // so the field is discoverable while staying visually softer than the focused state.
    val unfocusedBorder = if (destructive) {
        TDTheme.colors.crossRed.copy(alpha = 0.5f)
    } else {
        TDTheme.colors.lightGray
    }
    val cursor = if (destructive) TDTheme.colors.crossRed else TDTheme.colors.pendingGray

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChange?.invoke(focusState.isFocused)
            },
        value = value,
        onValueChange = onValueChange,
        label = label?.let {
            { TDText(text = it, style = TDTheme.typography.subheading1) }
        },
        placeholder = placeholder?.let {
            { TDText(text = it, style = TDTheme.typography.subheading1, color = TDTheme.colors.gray) }
        },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        isError = isError,
        leadingIcon = leadingIcon,
        trailingIcon = effectiveTrailingIcon,
        visualTransformation = effectiveTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        // Strip the baked-in color from regularTextStyle so the focused/unfocusedTextColor
        // below win — otherwise Material3 OutlinedTextField uses the textStyle's color and
        // typed text stays #000000 in dark theme (regularTextStyle hardcodes black).
        textStyle = TDTheme.typography.regularTextStyle.copy(color = Color.Unspecified),
        shape = TDTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TDTheme.colors.onBackground,
            unfocusedTextColor = TDTheme.colors.onBackground,
            disabledTextColor = TDTheme.colors.gray,
            // Transparent containers so the field inherits whatever surface it's placed on
            // (screen background, dialog surface, sheet, etc.) and the typed text reads
            // against that surface — matches TDTextField's pattern.
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedBorderColor = focusedBorder,
            unfocusedBorderColor = unfocusedBorder,
            disabledBorderColor = TDTheme.colors.lightGray.copy(alpha = 0.5f),
            errorBorderColor = TDTheme.colors.crossRed,
            focusedLabelColor = focusedBorder,
            unfocusedLabelColor = TDTheme.colors.gray,
            disabledLabelColor = TDTheme.colors.gray.copy(alpha = 0.5f),
            errorLabelColor = TDTheme.colors.crossRed,
            cursorColor = cursor,
            errorCursorColor = TDTheme.colors.crossRed,
            focusedTrailingIconColor = if (destructive) TDTheme.colors.crossRed else TDTheme.colors.pendingGray,
            unfocusedTrailingIconColor = TDTheme.colors.gray,
            errorTrailingIconColor = TDTheme.colors.crossRed,
        ),
        supportingText = supportingText?.let {
            {
                TDText(
                    text = it,
                    style = TDTheme.typography.subheading2,
                    color = if (isError) TDTheme.colors.crossRed else TDTheme.colors.gray,
                )
            }
        },
    )
}

@TDPreview
@Composable
private fun TDOutlinedTextFieldPreview_Empty() {
    TDTheme {
        TDOutlinedTextField(
            value = "",
            onValueChange = {},
            label = "Email",
        )
    }
}

@TDPreview
@Composable
private fun TDOutlinedTextFieldPreview_Filled() {
    TDTheme {
        TDOutlinedTextField(
            value = "berat@example.com",
            onValueChange = {},
            label = "Email",
        )
    }
}

@TDPreview
@Composable
private fun TDOutlinedTextFieldPreview_Error() {
    TDTheme {
        TDOutlinedTextField(
            value = "not-an-email",
            onValueChange = {},
            label = "Email",
            isError = true,
            supportingText = "Invalid email address",
        )
    }
}

@TDPreview
@Composable
private fun TDOutlinedTextFieldPreview_Disabled() {
    TDTheme {
        TDOutlinedTextField(
            value = "Read only",
            onValueChange = {},
            label = "Email",
            enabled = false,
        )
    }
}

@TDPreview
@Composable
private fun TDOutlinedTextFieldPreview_Destructive() {
    TDTheme {
        TDOutlinedTextField(
            value = "DELETE",
            onValueChange = {},
            label = "Type DELETE to confirm",
            destructive = true,
        )
    }
}

@TDPreviewForm
@Composable
private fun TDOutlinedTextFieldPreview_Password() {
    TDTheme {
        Column {
            TDOutlinedTextField(
                value = "secret123",
                onValueChange = {},
                label = "Password",
                passwordVisible = false,
                onTogglePasswordVisible = {},
            )
            TDOutlinedTextField(
                value = "secret123",
                onValueChange = {},
                label = "Password",
                passwordVisible = true,
                onTogglePasswordVisible = {},
            )
        }
    }
}
