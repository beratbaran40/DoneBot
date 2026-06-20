package com.todoapp.mobile.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDAddTaskButton

/**
 * The Home FAB. A single tap opens the Creation Hub ([com.todoapp.mobile.navigation.Screen.CreationHub])
 * — the old expanding menu (add-task / pomodoro / journal) was replaced by that dedicated screen.
 */
@Composable
fun HomeFabMenu(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        TDAddTaskButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp),
            onClick = onCreate,
        )
    }
}
