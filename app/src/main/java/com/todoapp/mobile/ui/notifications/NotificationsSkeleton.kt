package com.todoapp.mobile.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

private const val SKELETON_CARD_COUNT = 4

/** Loading placeholder mirroring the notifications list: a section label and a few card shells. */
@Composable
internal fun NotificationsSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TDSkeletonText(width = 80.dp, height = 14.dp)
        repeat(SKELETON_CARD_COUNT) {
            NotificationsSkeletonCard()
        }
    }
}

@Composable
private fun NotificationsSkeletonCard() {
    val shape = TDTheme.shapes.large
    val borderAlpha = if (TDTheme.isDark) 0.25f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(TDTheme.colors.settingsCard)
            .border(width = 1.dp, color = TDTheme.colors.lightGray.copy(alpha = borderAlpha), shape = shape),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(TDTheme.colors.lightGray.copy(alpha = 0.4f)),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
        ) {
            TDSkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TDSkeletonText(width = 160.dp, height = 14.dp)
                TDSkeletonText(width = 220.dp, height = 12.dp)
                TDSkeletonText(width = 48.dp, height = 10.dp)
            }
        }
    }
}

@TDPreview
@Composable
private fun NotificationsSkeletonListPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background)) {
            NotificationsSkeletonList()
        }
    }
}
