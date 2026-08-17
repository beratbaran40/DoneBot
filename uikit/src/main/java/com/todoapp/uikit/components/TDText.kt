package com.todoapp.uikit.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
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
    isHeading: Boolean = false,
    /**
     * Declares whether a second line is acceptable here. Only [TDFitPolicy.BOUNDED] slots are
     * watched by the debug text-fit probe — see [TDFitPolicy] for why this is declared rather than
     * inferred. Has no effect on layout, in any build.
     */
    fitPolicy: TDFitPolicy = TDFitPolicy.FREE,
    /** Component name for the probe's log line. Only worth passing alongside [fitPolicy]. */
    slot: String? = null,
) {
    if (text != null) {
        Text(
            text = text,
            modifier = if (isHeading) modifier.semantics { heading() } else modifier,
            textAlign = textAlign,
            style =
            style.merge(
                color = color,
            ),
            overflow = overflow,
            maxLines = maxLines,
            onTextLayout = rememberTextFitProbe(text, slot, maxLines, fitPolicy),
        )
    }
}

/**
 * Returns `null` in any build without a reporter, which keeps [TDText] on Compose's String fast
 * path. The lambda is remembered because an unstable one would force a node update on every
 * recomposition — far more expensive than the probe itself.
 */
@Composable
private fun rememberTextFitProbe(
    text: String,
    slot: String?,
    maxLines: Int,
    fitPolicy: TDFitPolicy,
): ((TextLayoutResult) -> Unit)? {
    val reporter = LocalTDTextOverflowReporter.current ?: return null
    return remember(reporter, text, slot, maxLines, fitPolicy) {
        { result: TextLayoutResult ->
            val midWord = result.brokeMidWord(text)
            val tooManyLines = fitPolicy == TDFitPolicy.BOUNDED && result.lineCount > 1
            if (result.hasVisualOverflow || midWord || tooManyLines) {
                reporter.onOverflow(
                    TDTextOverflowReport(
                        text = text,
                        slot = slot,
                        widthPx = result.size.width,
                        fontSizeSp = result.layoutInput.style.fontSize.value,
                        lineCount = result.lineCount,
                        maxLines = maxLines,
                        clipped = result.hasVisualOverflow,
                        midWordBreak = midWord,
                    ),
                )
            }
        }
    }
}

/**
 * True when a line ended in the middle of a word.
 *
 * This is THE signal for the narrow-slot failure, and it is not the obvious one. Android's
 * `LineBreaker` falls back to grapheme-level breaking when a single word is wider than the line, so
 * a slot too narrow for "Tamamlandı" does not overflow and does not clip — it quietly renders
 * "Tamamlan / dı". `hasVisualOverflow` stays `false` throughout, which is why a probe built on it
 * alone would report nothing at all for the very case it exists to catch.
 *
 * Checking the break character instead has no false positives and, unlike a string-table scan, it
 * also covers user-typed content — a long group name in a narrow column shows up here and nowhere
 * else.
 */
private fun TextLayoutResult.brokeMidWord(text: String): Boolean = (0 until lineCount - 1).any { line ->
    // visibleEnd = false so the offset includes the whitespace the break consumed: a legitimate
    // break leaves that whitespace at `end - 1`.
    val end = getLineEnd(line, visibleEnd = false)
    end in 1 until text.length &&
        !text[end - 1].isBreakOpportunity() &&
        !text[end].isBreakOpportunity()
}

/** Characters a line may legitimately break on or after. */
private fun Char.isBreakOpportunity(): Boolean = isWhitespace() || this == '-' || this == '/' || this == '·'

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
