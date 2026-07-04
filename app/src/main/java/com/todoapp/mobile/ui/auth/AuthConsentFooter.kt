package com.todoapp.mobile.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Fine-print consent line shown under the sign-up / sign-in actions on the auth screens.
 *
 * Presents ToS/Privacy acceptance at the point of account creation and covers every entry path
 * (email register, Google-on-register, Google-on-login). The two link spans open the hosted legal
 * pages via LocalUriHandler — no Intent needed. Reuses the existing terms_consent_* strings. (§6.19)
 */
@Composable
internal fun AuthConsentFooter(modifier: Modifier = Modifier) {
    val linkStyles =
        TextLinkStyles(
            style =
            SpanStyle(
                color = TDTheme.colors.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    val consent =
        buildAnnotatedString {
            append(stringResource(R.string.terms_consent_prefix))
            withLink(LinkAnnotation.Url(BuildConfig.PRIVACY_POLICY_URL, linkStyles)) {
                append(stringResource(R.string.settings_privacy_policy))
            }
            append(stringResource(R.string.terms_consent_connector))
            withLink(LinkAnnotation.Url(BuildConfig.TERMS_OF_SERVICE_URL, linkStyles)) {
                append(stringResource(R.string.settings_terms_of_service))
            }
            append(stringResource(R.string.terms_consent_suffix))
        }
    Text(
        text = consent,
        modifier = modifier.fillMaxWidth(),
        style = TDTheme.typography.heading7,
        color = TDTheme.colors.gray,
        textAlign = TextAlign.Center,
    )
}

@TDPreview
@Composable
private fun AuthConsentFooterPreview() {
    TDTheme {
        AuthConsentFooter()
    }
}
