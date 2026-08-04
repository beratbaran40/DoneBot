package com.todoapp.mobile.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

@Composable
internal fun DetailsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 56.dp, corner = 12.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 56.dp, corner = 12.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonAllDayRow()
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonTimeRow()
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 80.dp, corner = 16.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 80.dp, corner = 16.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 80.dp, corner = 16.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 120.dp, corner = 12.dp)
            Spacer(Modifier.height(16.dp))
            DetailsSkeletonField(height = 56.dp, corner = 12.dp)
            Spacer(Modifier.height(24.dp))
        }
        DetailsSkeletonBottomBar()
    }
}

@Composable
private fun DetailsSkeletonField(
    height: Dp,
    corner: Dp,
) {
    TDSkeletonBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(corner),
    )
}

@Composable
private fun DetailsSkeletonAllDayRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonText(width = 100.dp, height = 14.dp)
        Spacer(Modifier.weight(1f))
        TDSkeletonBox(
            modifier = Modifier.size(width = 52.dp, height = 32.dp),
            shape = TDTheme.shapes.large,
        )
    }
}

@Composable
private fun DetailsSkeletonTimeRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = TDTheme.shapes.medium,
        )
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = TDTheme.shapes.medium,
        )
    }
}

@Composable
private fun DetailsSkeletonBottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = tdCorner(24.dp),
        )
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = tdCorner(24.dp),
        )
    }
}

@TDPreview
@Composable
private fun DetailsSkeletonPreview() {
    TDTheme {
        DetailsSkeleton()
    }
}
