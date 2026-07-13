package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalActionSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TDTheme.colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            ActionRow(
                iconRes = R.drawable.ic_edit_task,
                labelRes = string.journal_action_edit,
                tint = TDTheme.colors.onBackground,
                onClick = onEdit,
            )
            ActionRow(
                iconRes = R.drawable.ic_delete,
                labelRes = string.journal_action_delete,
                tint = TDTheme.colors.crossRed,
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionRow(
    iconRes: Int,
    labelRes: Int,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        TDText(
            text = stringResource(labelRes),
            style = TDTheme.typography.subheading1,
            color = tint,
        )
    }
}
