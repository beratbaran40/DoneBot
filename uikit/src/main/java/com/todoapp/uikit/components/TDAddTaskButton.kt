package com.todoapp.uikit.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.pixelSurface
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDElevationStyle
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDAddTaskButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier.size(64.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (TDTheme.style.elevationStyle) {
                // The ellipse drawable is a smooth vector disc — it cannot take a stepped corner, so
                // a hard-elevation kit paints its own block instead: accent fill, ink outline, bevel.
                TDElevationStyle.HARD ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pixelSurface(
                                fill = TDTheme.colors.primary,
                                outline = TDTheme.colors.onBackground,
                                shape = TDTheme.shapes.circle,
                                borderWidth = TDTheme.style.borderWidth,
                                shadowOffset = TDTheme.style.hardShadowOffset,
                            ),
                    )
                TDElevationStyle.SOFT ->
                    Icon(
                        modifier = Modifier.fillMaxSize(1f),
                        painter = tdPainter(R.drawable.ic_ellipse),
                        contentDescription = null,
                        tint = TDTheme.colors.pendingGray,
                    )
            }
            Icon(
                modifier = Modifier.fillMaxSize(0.57f),
                painter = tdPainter(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.cd_add_task),
                tint = when (TDTheme.style.elevationStyle) {
                    TDElevationStyle.SOFT -> TDTheme.colors.background
                    TDElevationStyle.HARD -> TDTheme.colors.onPrimary
                },
            )
        }
    }
}

@TDPreview
@Composable
fun TDAddTaskButtonPreview() {
    TDAddTaskButton(onClick = {})
}
