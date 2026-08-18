package com.todoapp.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.LocalWindowSizeClass
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.rememberKitArtPainter
import com.todoapp.uikit.modifier.gridBackground
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

/** Max width of the auth form column — a single readable column on any screen. */
val AuthFormMaxWidth = 400.dp

/** Robot art size as a fraction of the circle diameter (centered); tune for badge framing. */
private const val BRAND_ROBOT_ZOOM = 1.5f

/**
 * Responsive frame for the "welcome" auth screens (login / register / forgot password).
 *
 * - Side-by-side (Row: brand panel + form) when the screen is wide (Expanded width = tablet
 *   landscape) OR short (Compact height = any phone in landscape). The form pane scrolls and is
 *   keyboard-aware, so a short landscape height never clips it.
 * - Stacked (scrolling Column: brand header above a form card) otherwise — phone portrait (full
 *   width) and tablet portrait (form capped + centred).
 *
 * The [form] slot is plain column content (e.g. the existing LoginFormPanel) placed inside the
 * scaffold's width-capped, scrollable, inset-aware form column.
 */
@Composable
fun AuthScaffold(
    modifier: Modifier = Modifier,
    brandTitle: String,
    brandSubtitle: String? = null,
    form: @Composable () -> Unit,
) {
    val sizeClass = LocalWindowSizeClass.current
    val sideBySide =
        sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
            sizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    if (sideBySide) {
        Row(modifier = modifier.fillMaxSize()) {
            AuthBrandPanel(
                title = brandTitle,
                subtitle = brandSubtitle,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthFormColumn(card = false, content = form)
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .gridBackground(
                    baseColor = TDTheme.colors.pendingGray,
                    lineColor = TDTheme.colors.gridLine,
                    spacing = TDTheme.style.gridSpacing,
                    lineWidth = TDTheme.style.gridLineWidth,
                    style = TDTheme.style.gridStyle,
                )
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBrandPanel(
                title = brandTitle,
                subtitle = brandSubtitle,
                modifier = Modifier.fillMaxWidth(),
            )
            AuthFormColumn(card = true, content = form)
        }
    }
}

/** The form column: capped to [AuthFormMaxWidth] and keyboard/nav-bar safe. As a rounded card on the stacked layout. */
@Composable
private fun AuthFormColumn(
    card: Boolean,
    content: @Composable () -> Unit,
) {
    val base = Modifier.widthIn(max = AuthFormMaxWidth)
    val styled =
        if (card) {
            base
                .clip(tdCorner(32.dp))
                .background(TDTheme.colors.background)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp)
        } else {
            base.padding(horizontal = 24.dp, vertical = 24.dp)
        }
    Column(modifier = styled.navigationBarsPadding()) {
        content()
    }
}

/**
 * Brand panel: solid pendingGray background + circular splash badge + title + tagline. Badge scales
 * by window size (smaller on short landscape phones, larger on tablets) so nothing clips; the tagline
 * wraps instead of using a fixed-size box.
 */
@Composable
fun AuthBrandPanel(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
) {
    val sizeClass = LocalWindowSizeClass.current
    val illustrationSize =
        when {
            sizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> 96.dp
            sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> 180.dp
            else -> 140.dp
        }
    Column(
        modifier = modifier
            .gridBackground(
                baseColor = TDTheme.colors.pendingGray,
                lineColor = TDTheme.colors.gridLine,
                spacing = TDTheme.style.gridSpacing,
                lineWidth = TDTheme.style.gridLineWidth,
                style = TDTheme.style.gridStyle,
            )
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = rememberKitArtPainter(
                painterResource(com.example.uikit.R.drawable.img_donebot_alarm_reminder_light),
                illustrationSize * BRAND_ROBOT_ZOOM,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(illustrationSize * BRAND_ROBOT_ZOOM),
        )

        Spacer(Modifier.height(4.dp))

        TDText(
            text = title,
            style = TDTheme.typography.heading1,
            color = TDTheme.colors.white,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            TDText(
                text = subtitle,
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.white.copy(0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
            )
        }
    }
}
