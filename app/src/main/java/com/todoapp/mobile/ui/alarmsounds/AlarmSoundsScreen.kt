package com.todoapp.mobile.ui.alarmsounds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.repository.AlarmSoundOption
import com.todoapp.mobile.ui.alarmsounds.AlarmSoundsContract.UiAction
import com.todoapp.mobile.ui.alarmsounds.AlarmSoundsContract.UiState
import com.todoapp.uikit.components.TDErrorState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme

@Composable
fun AlarmSoundsScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (uiState) {
            is UiState.Loading -> Loading()
            is UiState.Error -> TDErrorState(message = uiState.message)
            is UiState.Success -> if (uiState.items.isEmpty()) {
                EmptyBlock()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = uiState.items, key = { it.uri.toString() }) { option ->
                        AlarmSoundRow(
                            option = option,
                            isSelected = option.uri == uiState.selectedUri,
                            onClick = { onAction(UiAction.OnSelect(option.uri)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TDTheme.colors.pendingGray)
    }
}

@Composable
private fun EmptyBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDText(
            text = stringResource(R.string.alarm_sounds_empty),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.gray,
        )
    }
}

@Composable
private fun AlarmSoundRow(
    option: AlarmSoundOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = TDTheme.isDark
    val cardModifier = Modifier
        .fillMaxWidth()
        .let { base ->
            if (isDark) {
                base.border(
                    width = 1.dp,
                    color = TDTheme.colors.lightGray.copy(alpha = 0.20f),
                    shape = TDTheme.shapes.large,
                )
            } else {
                base.tdShadow(
                    lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                    darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
                    cornerRadius = 16.dp,
                    elevation = 6.dp,
                )
            }
        }
        .clip(TDTheme.shapes.large)
        .background(if (isSelected) TDTheme.colors.bgColorPurple else TDTheme.colors.lightPending)
        .clickable(onClick = onClick)
        .padding(14.dp)

    Row(modifier = cardModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) TDTheme.colors.purple else TDTheme.colors.bgColorPurple),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = tdPainter(com.example.uikit.R.drawable.ic_clock),
                contentDescription = null,
                tint = if (isSelected) TDTheme.colors.white else TDTheme.colors.purple,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = option.title,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )
        }
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(TDTheme.colors.purple),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AlarmSoundsLoadingPreview() {
    TDTheme {
        AlarmSoundsScreen(uiState = UiState.Loading, onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AlarmSoundsErrorPreview() {
    TDTheme {
        AlarmSoundsScreen(
            uiState = UiState.Error("Couldn't load system sounds"),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AlarmSoundsEmptyPreview() {
    TDTheme {
        AlarmSoundsScreen(
            uiState = UiState.Success(
                items = emptyList(),
                selectedUri = android.net.Uri.EMPTY,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AlarmSoundsSuccessPreview() {
    val sample = listOf(
        AlarmSoundOption(title = "Default", uri = "content://settings/system/alarm_sound".toUri()),
        AlarmSoundOption(title = "Argon", uri = "content://media/internal/audio/media/1".toUri()),
        AlarmSoundOption(title = "Beacon", uri = "content://media/internal/audio/media/2".toUri()),
        AlarmSoundOption(title = "Bright Morning", uri = "content://media/internal/audio/media/3".toUri()),
        AlarmSoundOption(title = "Chime", uri = "content://media/internal/audio/media/4".toUri()),
    )
    TDTheme {
        AlarmSoundsScreen(
            uiState = UiState.Success(items = sample, selectedUri = sample[1].uri),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AlarmSoundsSuccessNoneSelectedPreview() {
    val sample = listOf(
        AlarmSoundOption(title = "Default", uri = "content://settings/system/alarm_sound".toUri()),
        AlarmSoundOption(title = "Argon", uri = "content://media/internal/audio/media/1".toUri()),
    )
    TDTheme {
        AlarmSoundsScreen(
            uiState = UiState.Success(items = sample, selectedUri = android.net.Uri.EMPTY),
            onAction = {},
        )
    }
}
