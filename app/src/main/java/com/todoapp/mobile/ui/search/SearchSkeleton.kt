package com.todoapp.mobile.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SearchSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDSkeletonBox(
                modifier = Modifier.size(width = 110.dp, height = 32.dp),
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        repeat(SEARCH_SKELETON_TASK_COUNT) { index ->
            SearchSkeletonTaskCard(variantIndex = index)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private const val SEARCH_SKELETON_TASK_COUNT = 4

private val searchTitleWidths = listOf(220.dp, 180.dp, 240.dp, 200.dp)
private val searchSubtitleWidths = listOf(140.dp, 100.dp, 160.dp, 120.dp)

@Composable
private fun SearchSkeletonTaskCard(variantIndex: Int) {
    val titleWidth: Dp = searchTitleWidths[variantIndex % searchTitleWidths.size]
    val subtitleWidth: Dp = searchSubtitleWidths[variantIndex % searchSubtitleWidths.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonBox(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TDSkeletonText(width = titleWidth, height = 16.dp)
            TDSkeletonText(width = subtitleWidth, height = 12.dp)
        }
    }
}

@TDPreview
@Composable
private fun SearchSkeletonPreview() {
    TDTheme {
        SearchSkeleton()
    }
}
