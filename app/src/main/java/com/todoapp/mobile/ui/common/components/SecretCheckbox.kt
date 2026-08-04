package com.todoapp.mobile.ui.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Secret-mode checkbox: an empty box that, on check, springs the `ic_secret_mode` icon in with a bouncy
 * scale + fade. Shared by the add-task sheet and the Creation Hub "Detaylar" panel.
 */
@Composable
fun SecretCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = TDTheme.shapes.tiny
    val bgColor by animateColorAsState(
        targetValue = if (checked) TDTheme.colors.pendingGray else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "secretCheckboxBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) {
            TDTheme.colors.pendingGray
        } else {
            TDTheme.colors.onBackground.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "secretCheckboxBorder",
    )

    val iconScale = remember { Animatable(if (checked) 1f else 0f) }
    val iconAlpha = remember { Animatable(if (checked) 1f else 0f) }
    var prevChecked by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked && !prevChecked) {
            iconAlpha.snapTo(0f)
            iconScale.snapTo(0.4f)
            iconAlpha.animateTo(1f, animationSpec = tween(durationMillis = 150))
            iconScale.animateTo(
                targetValue = 1.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            iconScale.animateTo(1f, animationSpec = tween(durationMillis = 150))
        } else if (!checked && prevChecked) {
            iconAlpha.animateTo(0f, animationSpec = tween(durationMillis = 120))
            iconScale.snapTo(0f)
        }
        prevChecked = checked
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = tdPainter(R.drawable.ic_secret_mode),
            contentDescription = null,
            colorFilter = ColorFilter.tint(TDTheme.colors.white),
            modifier = Modifier
                .size(16.dp)
                .scale(iconScale.value)
                .alpha(iconAlpha.value),
        )
    }
}

@TDPreview
@Composable
private fun SecretCheckboxPreview() {
    TDTheme {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            SecretCheckbox(checked = false, onCheckedChange = {})
            SecretCheckbox(checked = true, onCheckedChange = {})
        }
    }
}
