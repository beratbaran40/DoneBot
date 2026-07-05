package com.todoapp.mobile.ui.common.taskform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.uikit.components.TDChoiceChip
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDDatePickerDialog
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import com.example.uikit.R as UiKitR

/**
 * Type-specific task-form sections, shared by the Creation Hub (create) and the task detail screen
 * (edit) so both surfaces stay visually identical and in sync. Each composable is contract-agnostic:
 * it takes primitives / domain types + callbacks, and the caller maps its own MVI state into them.
 */

private data class ReminderOption(val minutes: Long?, val labelRes: Int)

private val REMINDER_OPTIONS = listOf(
    ReminderOption(null, R.string.creation_reminder_off),
    ReminderOption(0L, R.string.creation_reminder_ontime),
    ReminderOption(MINUTES_15, R.string.creation_reminder_15m),
    ReminderOption(MINUTES_1_HOUR, R.string.creation_reminder_1h),
    ReminderOption(MINUTES_1_DAY, R.string.creation_reminder_1d),
)

private data class FrequencyOption(
    val recurrence: Recurrence,
    val labelRes: Int,
    val iconRes: Int,
)

private val FREQUENCY_OPTIONS = listOf(
    FrequencyOption(Recurrence.DAILY, R.string.creation_freq_daily, UiKitR.drawable.ic_sun),
    FrequencyOption(Recurrence.WEEKLY, R.string.creation_freq_weekly, R.drawable.ic_calendar),
    FrequencyOption(Recurrence.MONTHLY, R.string.creation_freq_monthly, UiKitR.drawable.ic_moon),
    FrequencyOption(Recurrence.YEARLY, R.string.creation_freq_yearly, UiKitR.drawable.ic_globe),
)

/** Read-only type indicator (medallion + name + subtitle). Tür değiştirici YOK. */
@Composable
fun TaskTypeHeader(
    icon: Painter,
    name: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        TDText(
            text = name,
            style = TDTheme.typography.heading4.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        TDText(
            text = subtitle,
            style = TDTheme.typography.subheading1.copy(textAlign = TextAlign.Center),
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun TaskFormDateField(
    date: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onDeselect: () -> Unit = {},
) {
    TDDatePickerDialog(
        modifier = modifier,
        selectedDate = date,
        onDateSelect = onSelect,
        onDateDeselect = onDeselect,
    )
}

@Composable
fun TaskReminderChips(
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.reminder_label))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            REMINDER_OPTIONS.forEach { option ->
                TDChoiceChip(
                    label = stringResource(option.labelRes),
                    selected = selected == option.minutes,
                    onClick = { onSelect(option.minutes) },
                    selectedContainerColor = TDTheme.colors.pendingGray,
                    selectedContentColor = TDTheme.colors.white,
                )
            }
        }
    }
}

@Composable
fun TaskFrequencyChips(
    selected: Recurrence,
    onSelect: (Recurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_frequency_label))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FREQUENCY_OPTIONS.forEach { option ->
                TDChoiceChip(
                    label = stringResource(option.labelRes),
                    selected = selected == option.recurrence,
                    onClick = { onSelect(option.recurrence) },
                    leadingIcon = painterResource(option.iconRes),
                    selectedContainerColor = TDTheme.colors.pendingGray,
                    selectedContentColor = TDTheme.colors.white,
                )
            }
        }
    }
}

@Composable
fun TaskSubtaskEditor(
    drafts: List<String>,
    onChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    stepPlaceholder: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val connectorColor = TDTheme.colors.pendingGray.copy(alpha = 0.5f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_steps_label))
        Column {
            drafts.forEachIndexed { index, draft ->
                // Dotted connector linking each step to the previous one (sequence feel).
                if (index > 0) DashedConnector(connectorColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TDCompactOutlinedTextField(
                        value = draft,
                        placeholder = stepPlaceholder(index),
                        onValueChange = { onChange(index, it) },
                        modifier = Modifier.weight(1f),
                    )
                    // The trailing empty row is the "add" affordance and has nothing to remove.
                    if (index != drafts.lastIndex) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                painter = painterResource(UiKitR.drawable.ic_delete),
                                contentDescription = stringResource(R.string.creation_remove_step_cd),
                                tint = TDTheme.colors.crossRed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskFormSectionLabel(text: String) {
    TDText(
        text = text,
        style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.SemiBold),
        color = TDTheme.colors.onBackground,
        isHeading = true,
    )
}

@Composable
private fun DashedConnector(color: Color) {
    Canvas(
        modifier = Modifier
            .padding(start = 16.dp)
            .width(2.dp)
            .height(14.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
        )
    }
}

private const val MINUTES_15 = 15L
private const val MINUTES_1_HOUR = 60L
private const val MINUTES_1_DAY = 1440L
private const val DASH_ON = 4f
private const val DASH_OFF = 6f
