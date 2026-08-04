package com.todoapp.uikit.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDOverlayPermissionItem(
    context: Context,
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TDTheme.colors.onBackground.copy(alpha = 0.2f),
                shape = TDTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TDText(
                text = stringResource(R.string.overlay_permission),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            TDText(
                text = stringResource(R.string.allows_the_app_to_display_content_over_other_applications),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TDButton(
                isEnable = true,
                text = stringResource(R.string.grant_permission),
                onClick = {
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri(),
                        )
                    context.startActivity(intent)
                },
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_close),
                contentDescription = "Close",
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@TDPreview
@Composable
private fun TDOverlayPermissionItemPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDOverlayPermissionItem(context = LocalContext.current)
        }
    }
}
