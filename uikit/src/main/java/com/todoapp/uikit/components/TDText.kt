package com.todoapp.uikit.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDText(
    modifier: Modifier = Modifier,
    text: String?,
    color: Color = TDTheme.colors.onSurface,
    style: TextStyle = TDTheme.typography.regularTextStyle,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    if (text != null) {
        Text(
            text = text,
            modifier = modifier,
            textAlign = textAlign,
            style =
            style.merge(
                color = color,
            ),
            overflow = overflow,
            maxLines = maxLines,
        )
    }
}

@Composable
fun TDSpannableText(
    modifier: Modifier = Modifier,
    fullText: String,
    spanText: String,
    color: Color = TDTheme.colors.onSurface,
    style: TextStyle = TDTheme.typography.regularTextStyle,
    spanStyle: SpanStyle = SpanStyle(),
    textAlign: TextAlign? = null,
) {
    Text(
        text =
        buildAnnotatedString {
            withStyle(style = style.toSpanStyle()) {
                append(fullText)
                val mStartIndex = fullText.indexOf(spanText)
                if (mStartIndex != -1) {
                    val mEndIndex = mStartIndex.plus(spanText.length)
                    addStyle(
                        style = spanStyle,
                        start = mStartIndex,
                        end = mEndIndex,
                    )
                }
            }
        },
        modifier = modifier,
        textAlign = textAlign,
        style =
        style.merge(
            color = color,
        ),
    )
}

@TDPreview
@Composable
private fun TdTextDefaultPreview() {
    TDTheme {
        TDText(
            text = "This is a text.",
        )
    }
}

@TDPreview
@Composable
private fun TdTextLongPreview() {
    TDTheme {
        TDText(
            text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@TDPreview
@Composable
private fun TdTextNullPreview() {
    TDTheme {
        TDText(text = null)
    }
}

@TDPreview
@Composable
private fun TdSpannableTextPreview() {
    TDTheme {
        TDSpannableText(
            fullText = "This should be a text.",
            spanText = "should",
            spanStyle =
            SpanStyle(
                color = TDTheme.colors.pendingGray,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
