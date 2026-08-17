package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import com.todoapp.uikit.theme.timePickerColors
import com.todoapp.uikit.util.deviceUses24HourClock
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TDPlanTimePickerField(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    is24Hour: Boolean = deviceUses24HourClock(LocalContext.current),
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    val (initialHour, initialMinute) = remember(time) { time.hour to time.minute }
    val timeFormatter = remember(is24Hour) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier =
            Modifier
                .clip(TDTheme.shapes.large)
                .background(TDTheme.colors.background)
                .clickable { isDialogOpen = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.onBackground.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier =
                Modifier
                    .clip(tdCorner(999.dp))
                    .background(TDTheme.colors.pendingGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = tdPainter(R.drawable.ic_clock),
                    contentDescription = null,
                    tint = TDTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = time.format(timeFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    color = TDTheme.colors.onBackground,
                )
            }
        }

        if (isDialogOpen) {
            Dialog(onDismissRequest = { isDialogOpen = false }) {
                key(initialHour, initialMinute) {
                    val timePickerState =
                        rememberTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            is24Hour = is24Hour,
                        )

                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = TDTheme.shapes.large,
                        tonalElevation = 8.dp,
                        contentColor = TDTheme.colors.onBackground,
                        color = TDTheme.colors.background,
                    ) {
                        Column(
                            modifier =
                            Modifier
                                .widthIn(max = 320.dp)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            TimeInput(
                                state = timePickerState,
                                colors = timePickerColors(),
                            )

                            PlanTimePickerActions(
                                onCancel = { isDialogOpen = false },
                                onConfirm = {
                                    onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                                    isDialogOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@TDPreview
@Composable
fun TDPlanTimePicker_DefaultPreview() {
    TDTheme {
        val timeState = remember { mutableStateOf(LocalTime.of(9, 0)) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TDPlanTimePickerField(
                title = "Plan your day",
                subtitle = "When do you want to start your day?",
                time = timeState.value,
                onTimeChange = { timeState.value = it },
            )
        }
    }
}

/**
 * Extracted so it can be previewed — the dialog it lives in opens off internal state, so the only
 * way to see this row at 320dp was to run the app.
 *
 * Weighted, not left to TDButton's own 140dp floor: the dialog Column is capped at 320dp and spends
 * 48 of it on padding, so two SMALL buttons want 140 + 12 + 140 = 292dp of the 272dp there is. A
 * min-width child in an over-full Row is coerced into what is left rather than overflowing, so the
 * second button silently rendered 20dp narrower than the first at every screen size.
 */
@Composable
private fun PlanTimePickerActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            size = TDButtonSize.SMALL,
            type = TDButtonType.SECONDARY,
        )

        TDButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.ok),
            onClick = onConfirm,
            size = TDButtonSize.SMALL,
        )
    }
}

/** The dialog footer inside the real 320dp box it lives in. Both buttons must be the same width. */
@TDPreviewNarrow
@Composable
private fun TDPlanTimePickerActionsNarrowPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(24.dp),
        ) {
            PlanTimePickerActions(onCancel = {}, onConfirm = {})
        }
    }
}

@TDPreview
@Composable
fun TDPlanTimePicker_SelectedPreview() {
    TDTheme {
        val timeState = remember { mutableStateOf(LocalTime.of(14, 30)) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TDPlanTimePickerField(
                title = "Plan your day",
                subtitle = "When do you want to start your day?",
                time = timeState.value,
                onTimeChange = { timeState.value = it },
            )
        }
    }
}
