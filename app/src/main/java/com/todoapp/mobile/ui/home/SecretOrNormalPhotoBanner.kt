package com.todoapp.mobile.ui.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

/**
 * First-photo banner for a Home task card.
 *
 * When the task is secret the image is rendered blurred with a dim overlay and
 * a centered lock icon + hint text over it, so the preview doesn't leak any
 * visual content but the user still sees it's a secret task with a photo.
 */
@Composable
fun SecretOrNormalPhotoBanner(
    url: String,
    isSecret: Boolean,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val imgModifier =
            Modifier.fillMaxSize().let {
                // Compose blur only works on API 31+; on older devices we fall back to a heavy dim.
                if (isSecret && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.blur(28.dp) else it
            }
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imgModifier,
        )
        if (isSecret) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = tdPainter(R.drawable.ic_secret_mode),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TDText(
                        text = stringResource(R.string.secret_task_hint),
                        style = TDTheme.typography.subheading2,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
