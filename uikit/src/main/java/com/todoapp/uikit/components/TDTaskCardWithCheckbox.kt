package com.todoapp.uikit.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreviewForm
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDTaskCardWithCheckbox(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    taskText: String,
    taskDescription: String?,
    onCheckBoxClick: (Boolean) -> Unit,
    isDragging: Boolean = false,
    isAnyDragging: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    categoryLabel: String? = null,
    @androidx.annotation.DrawableRes categoryIcon: Int? = null,
    categoryStripeColor: Color? = null,
    locationLabel: String? = null,
    onLocationClick: (() -> Unit)? = null,
    isOverdue: Boolean = false,
    overdueLabel: String? = null,
) {
    var showConfetti by remember { mutableStateOf(false) }
    var prevChecked by remember { mutableStateOf(isChecked) }
    LaunchedEffect(isChecked) {
        if (isChecked && !prevChecked) showConfetti = true
        prevChecked = isChecked
    }

    val cardBg by animateColorAsState(
        targetValue = if (isChecked) TDTheme.colors.lightGreen else TDTheme.colors.lightPending,
        animationSpec = tween(300),
        label = "cardBg",
    )
    val idleBorderColor by animateColorAsState(
        targetValue =
        if (isChecked) {
            TDTheme.colors.mediumGreen.copy(alpha = 0.3f)
        } else {
            TDTheme.colors.pendingGray.copy(alpha = 0.25f)
        },
        animationSpec = tween(300),
        label = "borderColor",
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "dragScale",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isAnyDragging && !isDragging) 0.72f else 1f,
        animationSpec = tween(200),
        label = "cardAlpha",
    )
    val effectiveBorderColor by animateColorAsState(
        targetValue = if (isDragging) TDTheme.colors.pendingGray else idleBorderColor,
        animationSpec = tween(150),
        label = "effectiveBorderColor",
    )
    val effectiveBorderWidth by animateDpAsState(
        targetValue = if (isDragging) 2.dp else 1.dp,
        animationSpec = tween(150),
        label = "effectiveBorderWidth",
    )
    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = effectiveBorderWidth,
                    color = effectiveBorderColor,
                    shape = shape,
                ).graphicsLayer {
                    scaleX = dragScale
                    scaleY = dragScale
                    alpha = cardAlpha
                    shadowElevation = if (isDragging) with(density) { 12.dp.toPx() } else 0f
                }.background(
                    color = cardBg,
                    shape = shape,
                ).drawBehind {
                    if (categoryStripeColor != null) {
                        drawRect(
                            color = categoryStripeColor,
                            topLeft = Offset.Zero,
                            size = Size(width = 4.dp.toPx(), height = size.height),
                        )
                    }
                }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDTaskCheckBox(
                isChecked = isChecked,
                onToggle = { onCheckBoxClick(!isChecked) },
            )
            Spacer(Modifier.width(10.dp))
            Column {
                TDText(
                    text = taskText,
                    color = TDTheme.colors.onBackground,
                    style =
                    TDTheme.typography.regularTextStyle.copy(
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                if (!taskDescription.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    TDText(
                        text = taskDescription,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                        overflow = TextOverflow.Ellipsis,
                        style =
                        TDTheme.typography.subheading1.copy(
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                    )
                }
                val showOverdueChip = isOverdue && !isChecked && !overdueLabel.isNullOrBlank()
                if (showOverdueChip || !categoryLabel.isNullOrBlank() || !locationLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showOverdueChip) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = TDTheme.colors.crossRed,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_warning),
                                    contentDescription = null,
                                    tint = TDTheme.colors.background,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                TDText(
                                    text = overdueLabel!!,
                                    color = TDTheme.colors.background,
                                    style = TDTheme.typography.subheading2,
                                )
                            }
                            if (!categoryLabel.isNullOrBlank() || !locationLabel.isNullOrBlank()) {
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        if (!categoryLabel.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = if (isChecked) TDTheme.colors.darkGreen else TDTheme.colors.darkPending,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                if (categoryIcon != null) {
                                    Icon(
                                        painter = painterResource(categoryIcon),
                                        contentDescription = null,
                                        tint = TDTheme.colors.background,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                TDText(
                                    text = categoryLabel,
                                    color = TDTheme.colors.background,
                                    style = TDTheme.typography.subheading2,
                                )
                            }
                            if (!locationLabel.isNullOrBlank()) Spacer(Modifier.width(6.dp))
                        }
                        if (!locationLabel.isNullOrBlank()) {
                            val pillModifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) TDTheme.colors.darkGreen else TDTheme.colors.darkPending)
                                .let { base -> if (onLocationClick != null) base.clickable(onClick = onLocationClick) else base }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                            Row(modifier = pillModifier, verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pin),
                                    contentDescription = null,
                                    tint = TDTheme.colors.background,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                TDText(
                                    text = locationLabel,
                                    color = TDTheme.colors.background,
                                    style = TDTheme.typography.subheading2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showConfetti) {
            ConfettiEffect(
                modifier =
                Modifier
                    .matchParentSize()
                    .clip(shape),
                onAnimFinished = { showConfetti = false },
            )
        }
    }
}

@Composable
private fun ConfettiEffect(
    modifier: Modifier = Modifier,
    onAnimFinished: () -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
    )

    LaunchedEffect(progress) {
        if (progress == 1f) {
            onAnimFinished()
        }
    }
}

@Composable
private fun TDTaskCheckBox(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)

    val checkboxBg by animateColorAsState(
        targetValue = if (isChecked) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray.copy(alpha = 0.08f),
        animationSpec = tween(250),
        label = "checkboxBg",
    )
    val checkboxBorder by animateColorAsState(
        targetValue = if (isChecked) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "checkboxBorder",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "checkScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        modifier
            .size(28.dp)
            .clip(shape)
            .background(checkboxBg, shape)
            .border(1.5.dp, checkboxBorder, shape)
            .clickable(onClick = onToggle),
    ) {
        if (isChecked) {
            Icon(
                modifier =
                Modifier
                    .size(16.dp)
                    .scale(checkScale),
                painter = painterResource(R.drawable.ic_check_svg),
                contentDescription = null,
                tint = TDTheme.colors.white,
            )
        } else {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(R.drawable.ic_sand_clock),
                contentDescription = null,
                tint = TDTheme.colors.pendingGray,
            )
        }
    }
}

@TDPreviewForm
@Composable
private fun TDTaskCardOverduePreview() {
    TDTaskCardWithCheckbox(
        isChecked = false,
        taskText = "Buy a cat food",
        taskDescription = "1kg",
        onCheckBoxClick = {},
        isOverdue = true,
        overdueLabel = "Gecikti",
        categoryLabel = "Shopping",
    )
}

@TDPreviewForm
@Composable
private fun TDTaskCardCheckedPreview() {
    TDTaskCardWithCheckbox(
        isChecked = true,
        taskText = "Buy a cat food",
        taskDescription = "1kg",
        onCheckBoxClick = {},
    )
}

@TDPreviewForm
@Composable
private fun TDTaskCardUncheckedPreview() {
    TDTaskCardWithCheckbox(
        isChecked = false,
        taskText = "Buy a cat food",
        taskDescription = "1kg",
        onCheckBoxClick = {},
    )
}

@TDPreviewForm
@Composable
private fun TDTaskCard_ListPreview() {
    Column(
        modifier =
        Modifier
            .background(TDTheme.colors.background)
            .padding(16.dp),
    ) {
        TDTaskCardWithCheckbox(
            isChecked = true,
            taskText = "Buy a cat food",
            taskDescription = "1kg",
            onCheckBoxClick = {},
        )
        Spacer(Modifier.size(8.dp))
        TDTaskCardWithCheckbox(
            isChecked = false,
            taskText = "Walk the dog",
            taskDescription = "15 min",
            onCheckBoxClick = {},
        )
        Spacer(Modifier.size(8.dp))
        TDTaskCardWithCheckbox(
            isChecked = true,
            taskText = "Clean the house",
            taskDescription = null,
            onCheckBoxClick = {},
        )
    }
}

@TDPreviewForm
@Composable
private fun TDTaskCheckBoxCheckedPreview() {
    TDTaskCheckBox(isChecked = true, onToggle = {})
}

@TDPreviewForm
@Composable
private fun TDTaskCheckBoxUncheckedPreview() {
    TDTaskCheckBox(isChecked = false, onToggle = {})
}
