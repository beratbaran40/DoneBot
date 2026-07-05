package com.todoapp.mobile.ui.planyourday

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.deviceTimeFormatter
import com.todoapp.mobile.ui.planyourday.PlanYourDayContract.UiAction
import com.todoapp.mobile.ui.planyourday.PlanYourDayContract.UiEffect
import com.todoapp.mobile.ui.planyourday.PlanYourDayContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePicker
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

@Composable
fun PlanYourDayScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val timeFormatter = remember(context) { deviceTimeFormatter(context) }

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }

            is UiEffect.NavigateBack -> onNavigateBack()
        }
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        TDText(
            text = stringResource(R.string.when_do_you_want_to_get_notified),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(24.dp))

        TDWheelTimePicker(
            hour = uiState.selectedTime.hour,
            minute = uiState.selectedTime.minute,
            onHourChange = { onAction(UiAction.OnHourChange(it)) },
            onMinuteChange = { onAction(UiAction.OnMinuteChange(it)) },
        )

        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter =
                painterResource(
                    if (TDTheme.isDark) {
                        com.example.uikit.R.drawable.img_donebot_plan_your_day_light
                    } else {
                        com.example.uikit.R.drawable.img_donebot_plan_your_day_dark
                    },
                ),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
            )
            Spacer(Modifier.width(12.dp))
            TDText(
                text = stringResource(R.string.notify_hint),
                style = TDTheme.typography.heading7,
                color = TDTheme.colors.onBackground,
            )
        }

        Spacer(Modifier.weight(1f))

        TDButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.notify_me_at, uiState.selectedTime.format(timeFormatter)),
            type = TDButtonType.PRIMARY,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
            onClick = { onAction(UiAction.OnSave) },
        )

        Spacer(Modifier.height(12.dp))

        TDButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cancel),
            type = TDButtonType.SECONDARY,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
            onClick = { onAction(UiAction.OnCancel) },
        )

        Spacer(Modifier.height(16.dp))
    }
}

@TDPreview
@Composable
private fun PlanYourDayScreenPreview() {
    TDTheme {
        PlanYourDayScreen(
            uiState =
            UiState(
                selectedTime = LocalTime.of(9, 30),
                savedTime = LocalTime.of(9, 0),
            ),
            uiEffect = kotlinx.coroutines.flow.emptyFlow(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@TDPreview
@Composable
private fun PlanYourDayScreenNoChangePreview() {
    TDTheme {
        PlanYourDayScreen(
            uiState =
            UiState(
                selectedTime = LocalTime.of(9, 0),
                savedTime = LocalTime.of(9, 0),
            ),
            uiEffect = kotlinx.coroutines.flow.emptyFlow(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@TDPreview
@Composable
private fun PlanYourDayScreenEveningPreview() {
    TDTheme {
        PlanYourDayScreen(
            uiState =
            UiState(
                selectedTime = LocalTime.of(20, 30),
                savedTime = LocalTime.of(9, 0),
            ),
            uiEffect = kotlinx.coroutines.flow.emptyFlow(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@TDPreview
@Composable
private fun PlanYourDayScreenFirstTimePreview() {
    TDTheme {
        PlanYourDayScreen(
            uiState =
            UiState(
                selectedTime = LocalTime.of(7, 15),
                savedTime = null,
            ),
            uiEffect = kotlinx.coroutines.flow.emptyFlow(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
