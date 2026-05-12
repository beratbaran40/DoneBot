package com.todoapp.mobile.ui.journal.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun JournalEntryPhotoStrip(
    paths: List<String>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit,
    onPhotoTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = paths, key = { it }) { path ->
            PolaroidPhoto(
                path = path,
                onClick = { onPhotoTap(path) },
                onRemove = { onRemove(path) },
            )
        }
        item(key = "add-photo") {
            AddPhotoTile(onClick = onAddClick)
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ADD_TILE_SIZE.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.bgColorPurple)
            .border(
                width = 1.dp,
                color = TDTheme.colors.lightGray,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = stringResource(string.journal_entry_add_photo_cd),
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(28.dp),
        )
    }
}

private const val ADD_TILE_SIZE = 96
