package com.todoapp.mobile.ui.journal.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R.string
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.theme.TDTheme

/**
 * Bottom control zone. It shows the white shutter while framing and swaps to the retake / save pair
 * once a print has developed, keeping a fixed height either way so the camera body above it never
 * shifts when the state changes.
 */
@Composable
internal fun PolaroidCameraControlBar(
    photoState: PhotoState,
    onShutterPressChange: (Boolean) -> Unit,
    onShutterClick: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(SHUTTER_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        if (photoState == PhotoState.Done) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TDButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(string.polaroid_retake),
                    type = TDButtonType.CANCEL,
                    size = TDButtonSize.SMALL,
                    fullWidth = true,
                    onClick = onRetake,
                )
                TDButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(string.polaroid_save_to_journal),
                    type = TDButtonType.PRIMARY,
                    size = TDButtonSize.SMALL,
                    fullWidth = true,
                    onClick = onSave,
                )
            }
        } else {
            PolaroidShutterButton(
                enabled = photoState == PhotoState.Idle,
                onPressChange = onShutterPressChange,
                onClick = onShutterClick,
            )
        }
    }
}

/**
 * The white shutter: a ring with a filled disc that shrinks under the finger. It reports its press
 * state through [onPressChange] so the red button drawn on the camera body depresses with it — the
 * two are the same control, one obvious and one skeuomorphic.
 *
 * Colors come from the Polaroid palette rather than the theme: this is a physical camera surface,
 * and it must not change with the app's colour kit.
 */
@Composable
internal fun PolaroidShutterButton(
    enabled: Boolean,
    onPressChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paper = TDTheme.colors.polaroid.printPaper
    val label = stringResource(string.polaroid_shutter_content_description)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val discScale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        label = "polaroidShutterDisc",
    )

    LaunchedEffect(isPressed) { onPressChange(isPressed) }

    Box(
        modifier = modifier
            .size(SHUTTER_SIZE)
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStroke = RING_STROKE.toPx()
            val outerRadius = size.minDimension / 2f
            drawCircle(
                color = paper.copy(alpha = 0.9f),
                radius = outerRadius - ringStroke / 2f,
                style = Stroke(ringStroke),
            )
            drawCircle(
                color = paper,
                radius = (outerRadius - ringStroke - RING_GAP.toPx()) * discScale,
            )
        }
    }
}

private val SHUTTER_SIZE = 76.dp
private val RING_STROKE = 3.dp
private val RING_GAP = 5.dp
private const val PRESSED_SCALE = 0.86f
private const val DISABLED_ALPHA = 0.4f
