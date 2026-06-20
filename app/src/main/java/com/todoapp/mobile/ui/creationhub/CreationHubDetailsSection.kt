package com.todoapp.mobile.ui.creationhub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.categoryOptions
import com.todoapp.mobile.ui.common.components.SecretCheckbox
import com.todoapp.mobile.ui.common.rememberLocationPickerLauncher
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.mobile.ui.home.PendingPhotosRow
import com.todoapp.uikit.components.TDCategoryPicker
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDLocationPicker
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePickerDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalTime
import com.example.uikit.R as UiKitR

/**
 * Optional "Detaylar" panel for the create flow — collapsed by default. Covers the lightweight fields
 * (description, category, time/all-day, location, photos, secret).
 */
@Composable
internal fun CreationHubDetailsSection(
    state: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onAction(UiAction.OnToggleDetails) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = stringResource(R.string.creation_details_label),
                style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.SemiBold),
                color = TDTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(
                    if (state.detailsExpanded) UiKitR.drawable.ic_arrow_up else UiKitR.drawable.ic_arrow_down,
                ),
                contentDescription = null,
                tint = TDTheme.colors.pendingGray,
            )
        }
        AnimatedVisibility(visible = state.detailsExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TDCompactOutlinedTextField(
                    value = state.description,
                    label = stringResource(R.string.creation_description_label),
                    placeholder = stringResource(CreationHubPlaceholders.descriptionRes(state.placeholderIndex)),
                    onValueChange = { onAction(UiAction.OnDescriptionChange(it)) },
                    singleLine = false,
                )
                // Staged tasks have no category — it's a multi-step goal, not a single categorised task.
                if (state.taskType != TaskType.STAGED) {
                    TDCategoryPicker(
                        selectedKey = state.category.name,
                        options = categoryOptions(),
                        onSelected = { onAction(UiAction.OnCategoryChange(TaskCategory.fromStorage(it))) },
                    )
                    if (state.category == TaskCategory.OTHER) {
                        TDCompactOutlinedTextField(
                            value = state.customCategoryName,
                            label = stringResource(R.string.creation_custom_category_label),
                            onValueChange = { onAction(UiAction.OnCustomCategoryNameChange(it)) },
                        )
                    }
                }
                AllDayAndTime(state = state, onAction = onAction)

                val launchLocationPicker = rememberLocationPickerLauncher { name, address, lat, lng ->
                    onAction(UiAction.OnLocationPicked(name, address, lat, lng))
                }
                TDLocationPicker(
                    name = state.locationName,
                    address = state.locationAddress,
                    addLabel = stringResource(R.string.location_add_hint),
                    clearContentDescription = stringResource(R.string.location_clear),
                    onClick = launchLocationPicker,
                    onClear = { onAction(UiAction.OnLocationCleared) },
                )

                PendingPhotosRow(
                    pending = state.pendingPhotos,
                    onPick = { bytes, mime -> onAction(UiAction.OnPhotoPick(bytes, mime)) },
                    onRemoveAt = { onAction(UiAction.OnPhotoRemove(it)) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TDText(
                        text = stringResource(R.string.creation_secret_label),
                        style = TDTheme.typography.regularTextStyle,
                        color = TDTheme.colors.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    SecretCheckbox(
                        checked = state.isSecret,
                        onCheckedChange = { onAction(UiAction.OnSecretChange(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayAndTime(
    state: UiState,
    onAction: (UiAction) -> Unit,
) {
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onAction(UiAction.OnAllDayChange(!state.isAllDay)) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = stringResource(R.string.task_all_day_label),
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            TickBox(checked = state.isAllDay)
        }
        if (!state.isAllDay) {
            TimeRow(
                label = stringResource(R.string.starts),
                time = state.timeStart,
                onClick = { showStart = true },
            )
            TimeRow(
                label = stringResource(R.string.ends),
                time = state.timeEnd,
                onClick = { showEnd = true },
            )
        }
    }
    if (showStart) {
        TDWheelTimePickerDialog(
            initialTime = state.timeStart,
            onConfirm = {
                onAction(UiAction.OnTimeStartChange(it))
                showStart = false
            },
            onDismiss = { showStart = false },
        )
    }
    if (showEnd) {
        TDWheelTimePickerDialog(
            initialTime = state.timeEnd,
            onConfirm = {
                onAction(UiAction.OnTimeEndChange(it))
                showEnd = false
            },
            onDismiss = { showEnd = false },
        )
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        TDText(
            text = time?.toString() ?: "--:--",
            style = TDTheme.typography.regularTextStyle.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.darkPending,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(UiKitR.drawable.ic_clock),
            contentDescription = null,
            tint = TDTheme.colors.darkPending,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TickBox(checked: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (checked) TDTheme.colors.pendingGray else TDTheme.colors.pendingGray.copy(alpha = 0.08f),
            )
            .border(
                width = 1.5.dp,
                color = if (checked) {
                    TDTheme.colors.pendingGray
                } else {
                    TDTheme.colors.pendingGray.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(6.dp),
            ),
    ) {
        if (checked) {
            Icon(
                painter = painterResource(UiKitR.drawable.ic_check_svg),
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
