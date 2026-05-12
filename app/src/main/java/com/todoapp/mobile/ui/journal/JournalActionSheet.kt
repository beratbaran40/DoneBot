package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.mobile.domain.model.JournalMood
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalActionSheet(
    selectedMood: JournalMood?,
    onMoodSelect: (JournalMood?) -> Unit,
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
            TDText(
                text = stringResource(string.journal_action_change_mood),
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.gray,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoodRow(selected = selectedMood, onSelect = onMoodSelect)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(4.dp))
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
private fun MoodRow(
    selected: JournalMood?,
    onSelect: (JournalMood?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ORDERED_MOODS.forEach { option ->
            val isSelected = selected == option.mood
            Box(
                modifier = Modifier
                    .size(MOOD_SIZE.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) TDTheme.colors.lightPurple else TDTheme.colors.bgColorPurple,
                        CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TDTheme.colors.purple else TDTheme.colors.lightGray,
                        shape = CircleShape,
                    )
                    .clickable {
                        // Tek tap: aynı moodsa null'a düşür, değilse seç.
                        onSelect(if (isSelected) null else option.mood)
                    },
                contentAlignment = Alignment.Center,
            ) {
                TDText(text = option.emoji, style = TDTheme.typography.heading3)
            }
        }
        Box(
            modifier = Modifier
                .size(MOOD_SIZE.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.lightRed, CircleShape)
                .border(width = 1.dp, color = TDTheme.colors.crossRed, shape = CircleShape)
                .clickable { onSelect(null) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(string.journal_action_clear_mood_cd),
                tint = TDTheme.colors.crossRed,
                modifier = Modifier.size(18.dp),
            )
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

private const val MOOD_SIZE = 44
