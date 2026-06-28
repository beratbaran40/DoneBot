package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun TDDatePickerDialog(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = LocalDate.now(),
    onDateSelect: (LocalDate) -> Unit,
    onDateDeselect: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var selectedMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        TDPickerField(
            title = stringResource(R.string.pick_a_date),
            value =
            selectedDate?.format(
                DateTimeFormatter.ofPattern(
                    "dd MMMM yyyy",
                ),
            ) ?: stringResource(R.string.pick_a_date),
            onClick = { isPickerOpen = true },
            emphasizeValue = selectedDate != null,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar2),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = supportingText,
        )
        if (isPickerOpen) {
            Dialog(
                onDismissRequest = { isPickerOpen = false },
            ) {
                Surface(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                ) {
                    Column(Modifier.background(TDTheme.colors.background)) {
                        TDDatePickerSingleInput(
                            selectedMonth = selectedMonth,
                            selectedDate = selectedDate,
                            onMonthBack = { selectedMonth = selectedMonth.minusMonths(1) },
                            onMonthForward = { selectedMonth = selectedMonth.plusMonths(1) },
                            onDaySelect = onDateSelect,
                            onDayDeselect = onDateDeselect,
                        )
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(TDTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TDButton(
                                text = stringResource(R.string.today),
                                type = TDButtonType.OUTLINE,
                                size = TDButtonSize.SMALL,
                                onClick = {
                                    onDateSelect(LocalDate.now())
                                    isPickerOpen = false
                                },
                            )
                            TDButton(
                                text = stringResource(R.string.ok),
                                onClick = { isPickerOpen = false },
                                size = TDButtonSize.SMALL,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@TDPreviewDialog
@Composable
fun TDDatePickerDialogPreview() {
    TDTheme {
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }

        TDDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelect = { selectedDate = it },
            onDateDeselect = { selectedDate = null },
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDDatePickerDialogEmptyPreview() {
    TDTheme {
        TDDatePickerDialog(
            selectedDate = null,
            onDateSelect = {},
            onDateDeselect = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDDatePickerDialogErrorPreview() {
    TDTheme {
        TDDatePickerDialog(
            selectedDate = null,
            onDateSelect = {},
            onDateDeselect = {},
            isError = true,
            supportingText = "Date is required",
        )
    }
}
