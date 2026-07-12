package com.todoapp.uikit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.uikit.R

internal val LocalTypography = staticCompositionLocalOf { TDTypography() }

val Poppins =
    FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semi_bold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

class TDTypography {
    val pomodoro: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 96.sp,
                fontWeight = FontWeight.W800,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.pendingGray,
            )
    val heading1: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 24.sp,
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading2: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 22.sp,
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading3: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading4: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading5: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading6: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading7: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val dayOfTheCalendar: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )

    val regularTextStyle: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading1: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading2: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 10.sp,
                fontWeight = FontWeight.W400,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
            )
    val subheading3: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading4: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.brown,
            )

    /**
     * Body text used for the journal entry editor. The relaxed 28sp lineHeight gives the writing
     * surface generous, handwriting-style breathing room between lines.
     */
    val journalHandwritingStyle: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
}
