package com.todoapp.mobile.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.components.TDAddTaskButton
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun HomeFabMenu(
    onAddTask: () -> Unit,
    onPomodoro: () -> Unit,
    onJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFabMenuOpen by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabMenuOpen) 45f else 0f,
        label = "fabRotation",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (isFabMenuOpen) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { isFabMenuOpen = false },
            )
        }

        Column(
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(
                visible = isFabMenuOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                        Modifier
                            .background(TDTheme.colors.pendingGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        TDText(
                            text = stringResource(com.todoapp.mobile.R.string.fab_label_journal),
                            style = TDTheme.typography.subheading4,
                            color = TDTheme.colors.background,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        modifier =
                        Modifier
                            .size(48.dp)
                            .background(TDTheme.colors.pendingGray, CircleShape),
                        onClick = {
                            isFabMenuOpen = false
                            onJournal()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_journal),
                            contentDescription = stringResource(com.todoapp.mobile.R.string.fab_label_journal),
                            tint = TDTheme.colors.background,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFabMenuOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                        Modifier
                            .background(TDTheme.colors.pendingGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        TDText(
                            text = stringResource(com.todoapp.mobile.R.string.fab_label_pomodoro),
                            style = TDTheme.typography.subheading4,
                            color = TDTheme.colors.background,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        modifier =
                        Modifier
                            .size(48.dp)
                            .background(TDTheme.colors.pendingGray, CircleShape),
                        onClick = {
                            isFabMenuOpen = false
                            onPomodoro()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pomodoro),
                            contentDescription = stringResource(com.todoapp.mobile.R.string.fab_label_pomodoro),
                            tint = TDTheme.colors.background,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFabMenuOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                        Modifier
                            .background(TDTheme.colors.pendingGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        TDText(
                            text = stringResource(com.todoapp.mobile.R.string.fab_label_add_task),
                            style = TDTheme.typography.subheading4,
                            color = TDTheme.colors.background,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        modifier =
                        Modifier
                            .size(48.dp)
                            .background(TDTheme.colors.pendingGray, CircleShape),
                        onClick = {
                            isFabMenuOpen = false
                            onAddTask()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_task),
                            contentDescription = stringResource(com.todoapp.mobile.R.string.fab_label_add_task),
                            tint = TDTheme.colors.background,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            TDAddTaskButton(
                modifier =
                Modifier
                    .size(56.dp)
                    .graphicsLayer { rotationZ = fabRotation },
                onClick = { isFabMenuOpen = !isFabMenuOpen },
            )
        }
    }
}
