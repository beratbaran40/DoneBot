package com.todoapp.mobile.ui.journal.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR
import com.todoapp.mobile.R as AppR

/**
 * Journal-specific top bar painted in the notebook paper color ([TDTheme] white = cream in both
 * themes) so it blends seamlessly with the page, carrying just the back and info buttons. The icons
 * use fixed dark ink ([TDTheme] black, dark in both themes) so they stay readable on the cream band;
 * [JournalEntryScreen] keeps the status-bar icons dark to match.
 */
@Composable
internal fun JournalTopBar(
    showInfo: Boolean,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TDTheme.colors.white)
            .statusBarsPadding()
            .height(56.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
        ) {
            Icon(
                painter = painterResource(UiKitR.drawable.ic_arrow_back),
                contentDescription = stringResource(AppR.string.cd_navigate_back),
                tint = TDTheme.colors.black,
            )
        }

        if (showInfo) {
            IconButton(
                onClick = onInfo,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
            ) {
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_info),
                    contentDescription = stringResource(AppR.string.cd_top_bar_info),
                    tint = TDTheme.colors.black,
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun JournalTopBarEditingPreview() {
    TDTheme {
        JournalTopBar(showInfo = true, onBack = {}, onInfo = {})
    }
}

@TDPreview
@Composable
private fun JournalTopBarReadonlyPreview() {
    TDTheme {
        JournalTopBar(showInfo = false, onBack = {}, onInfo = {})
    }
}
