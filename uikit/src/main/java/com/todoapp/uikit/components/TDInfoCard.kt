package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDInfoCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .clip(TDTheme.shapes.medium)
            .background(color = TDTheme.colors.infoCardBgColor)
            .border(1.dp, TDTheme.colors.lightGray, TDTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Icon(
            painter = tdPainter(id = R.drawable.ic_info),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
        )
        Spacer(modifier = Modifier.width(12.dp))
        TDText(text = text, color = TDTheme.colors.pendingGray)
    }
}

@TDPreview
@Composable
private fun TdInfoCardDefaultPreview() {
    TDTheme {
        TDInfoCard(
            text =
            "You'll be able to invite your family members and " +
                "assign tasks to them immediately after creating " +
                "the group.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdInfoCardShortPreview() {
    TDTheme {
        TDInfoCard(text = "Tap a task to edit it.", modifier = Modifier.padding(16.dp))
    }
}

@TDPreview
@Composable
private fun TdInfoCardLongPreview() {
    TDTheme {
        TDInfoCard(
            text =
            "Once a group is created, you can invite up to fifteen members. " +
                "Each member receives a notification and must accept the invitation " +
                "before they can see the group's shared tasks. Admins can revoke " +
                "access at any time from the group settings screen.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
