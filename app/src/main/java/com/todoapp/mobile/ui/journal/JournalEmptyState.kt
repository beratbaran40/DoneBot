package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun JournalEmptyState(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_journal),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(96.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        TDText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(string.journal_empty_title),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TDText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(string.journal_empty_subtitle),
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.gray,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        TDButton(
            text = stringResource(string.journal_empty_cta),
            size = TDButtonSize.MEDIUM,
            onClick = onAdd,
        )
    }
}

@Composable
internal fun JournalFilteredEmptyState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TDText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(string.journal_filtered_empty_title),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        TDButton(
            text = stringResource(string.journal_filtered_empty_clear),
            size = TDButtonSize.SMALL,
            onClick = onClearFilters,
        )
    }
}
