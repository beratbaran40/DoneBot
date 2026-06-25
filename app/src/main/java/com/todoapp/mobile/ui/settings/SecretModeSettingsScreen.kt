package com.todoapp.mobile.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.security.SecretModeReopenOption
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun SecretModeSettingsScreen(
    uiState: SettingsContract.UiState,
    onAction: (UiAction) -> Unit,
    uiEffect: Flow<SettingsContract.UiEffect> = emptyFlow(),
) {
    val context = LocalContext.current
    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is SettingsContract.UiEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            SettingsContract.UiEffect.RecreateActivity -> Unit
            is SettingsContract.UiEffect.ApplyLocale -> Unit
            is SettingsContract.UiEffect.SaveDataExport -> Unit
        }
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SecretModeStatusCard(
            isActive = uiState.isSecretModeActive,
            statusMessage = uiState.remainedSecretModeTime,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TDText(
                text = stringResource(R.string.auto_disable_after),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            SecretModeOptionList(
                selected = uiState.selectedSecretMode,
                onSelect = { onAction(UiAction.OnSelectedSecretModeChange(it)) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TDButton(
            text = stringResource(R.string.save_preference),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onAction(UiAction.OnSettingsSave) },
        )

        TDButton(
            text = stringResource(R.string.disable_secret_mode),
            type = TDButtonType.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
            isEnable = uiState.isSecretModeActive,
            onClick = { onAction(UiAction.OnDisableSecretModeTap) },
        )
    }
}

@Composable
private fun SecretModeStatusCard(
    isActive: Boolean,
    statusMessage: String,
) {
    val transition = updateTransition(targetState = isActive, label = "secretModeStatus")
    val borderColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "borderColor",
    ) { active -> if (active) TDTheme.colors.pendingGray else TDTheme.colors.onBackground.copy(alpha = 0.2f) }
    val backgroundColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "backgroundColor",
    ) { active ->
        if (active) {
            TDTheme.colors.pendingGray.copy(
                alpha = 0.12f,
            )
        } else {
            TDTheme.colors.onBackground.copy(alpha = 0.06f)
        }
    }
    val iconTint by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "iconTint",
    ) { active -> if (active) TDTheme.colors.pendingGray else TDTheme.colors.onBackground.copy(alpha = 0.4f) }
    val titleColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "titleColor",
    ) { active -> if (active) TDTheme.colors.pendingGray else TDTheme.colors.onBackground }
    val iconScale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) },
        label = "iconScale",
    ) { active -> if (active) 1f else 0.85f }

    val showSubtitle =
        isActive &&
            statusMessage.isNotBlank() &&
            statusMessage != "Secret mode is closed."

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_secret_mode),
                contentDescription = null,
                tint = iconTint,
                modifier =
                Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedContent(
                    targetState = isActive,
                    transitionSpec = {
                        fadeIn(tween(300)) + slideInVertically { -it / 3 } togetherWith
                            fadeOut(tween(200)) + slideOutVertically { it / 3 }
                    },
                    label = "titleText",
                ) { active ->
                    TDText(
                        text =
                        if (active) {
                            stringResource(R.string.secret_mode_active)
                        } else {
                            stringResource(R.string.secret_mode_inactive)
                        },
                        style = TDTheme.typography.heading5,
                        color = titleColor,
                    )
                }
                AnimatedVisibility(
                    visible = showSubtitle,
                    enter = fadeIn(tween(300)) + expandVertically(),
                    exit = fadeOut(tween(200)) + shrinkVertically(),
                ) {
                    TDText(
                        text = statusMessage,
                        style = TDTheme.typography.subheading3,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretModeOptionList(
    selected: SecretModeReopenOption,
    onSelect: (SecretModeReopenOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SecretModeReopenOptions.all.forEach { option ->
            val isSelected = option.id == selected.id
            SecretModeOptionRow(
                label = option.label,
                isSelected = isSelected,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun SecretModeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) TDTheme.colors.pendingGray.copy(alpha = 0.12f) else Color.Transparent
    val labelColor = if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.onBackground

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioIndicator(isSelected = isSelected)
            TDText(
                text = label,
                style = TDTheme.typography.heading7,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun RadioIndicator(isSelected: Boolean) {
    if (isSelected) {
        Box(
            modifier =
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(
                    Modifier.border(2.dp, TDTheme.colors.pendingGray, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .then(
                        Modifier.border(5.dp, TDTheme.colors.pendingGray, CircleShape),
                    ),
            )
        }
    } else {
        Box(
            modifier =
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, TDTheme.colors.onBackground.copy(alpha = 0.3f), CircleShape),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SecretModeSettingsScreenPreview_Inactive() {
    TDTheme {
        SecretModeSettingsScreen(
            uiState =
            SettingsContract.UiState(
                selectedSecretMode = SecretModeReopenOptions.Minutes5,
                isSecretModeActive = false,
                remainedSecretModeTime = "Secret mode is closed.",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SecretModeSettingsScreenPreview_Active() {
    TDTheme {
        SecretModeSettingsScreen(
            uiState =
            SettingsContract.UiState(
                selectedSecretMode = SecretModeReopenOptions.Minutes5,
                isSecretModeActive = true,
                remainedSecretModeTime = "Secret mode will be open for 03:50.",
            ),
            onAction = {},
        )
    }
}
