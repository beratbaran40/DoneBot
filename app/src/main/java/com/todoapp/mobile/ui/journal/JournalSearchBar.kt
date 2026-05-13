package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.components.TDOutlinedTextField
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun JournalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TDOutlinedTextField(
        modifier = modifier.padding(horizontal = 16.dp),
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(string.journal_search_hint),
        singleLine = true,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = TDTheme.colors.gray,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(string.journal_search_clear_cd),
                        tint = TDTheme.colors.gray,
                    )
                }
            }
        },
    )
}
