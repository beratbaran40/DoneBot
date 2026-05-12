package com.todoapp.mobile.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun OverdueBanner(
    count: Int,
    onView: () -> Unit,
) {
    val countText =
        if (count == 1) {
            stringResource(com.todoapp.mobile.R.string.overdue_summary_count_one, count)
        } else {
            stringResource(com.todoapp.mobile.R.string.overdue_summary_count_other, count)
        }
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(TDTheme.colors.lightRed, RoundedCornerShape(12.dp))
            .clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = TDTheme.colors.crossRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = countText,
                style = TDTheme.typography.subheading4,
                color = TDTheme.colors.crossRed,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.overdue_summary_view),
            style = TDTheme.typography.subheading4,
            color = TDTheme.colors.crossRed,
        )
    }
}

@TDPreview
@Composable
private fun OverdueBannerSinglePreview() {
    TDTheme {
        OverdueBanner(count = 1, onView = {})
    }
}

@TDPreview
@Composable
private fun OverdueBannerManyPreview() {
    TDTheme {
        OverdueBanner(count = 4, onView = {})
    }
}
