package com.todoapp.mobile.ui.journal.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.theme.TDTheme
import java.io.File
import kotlin.math.abs

/**
 * Photo rendered as a polaroid taped to the notebook page: white frame with bottom slack,
 * drop shadow, stable rotation derived from the source path, and a washi-tape stripe on top.
 */
@Composable
internal fun PolaroidPhoto(
    path: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationAngle = remember(path) { stableAngle(path, rangeDegrees = ROTATION_RANGE) }
    val tapeRotation = remember(path) { stableAngle(path + "_tape", rangeDegrees = TAPE_ROTATION_RANGE) }

    Box(
        modifier = modifier
            .padding(top = 20.dp, bottom = 8.dp, start = 6.dp, end = 6.dp)
            .rotate(rotationAngle),
    ) {
        // Polaroid frame: white card with bottom slack, the image sits inside.
        Box(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 22.dp)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(IMAGE_SIZE.dp),
            )
        }

        // Washi tape across the top, slightly tilted.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .rotate(tapeRotation)
                .zIndex(1f)
                .shadow(elevation = 1.dp)
                .background(TAPE_COLOR.copy(alpha = TAPE_ALPHA))
                .size(width = TAPE_WIDTH.dp, height = TAPE_HEIGHT.dp),
        )

        // Remove X button — kept from the prior strip so the user can still delete a photo
        // directly without opening fullscreen.
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(24.dp)
                .background(TDTheme.colors.crossRed, CircleShape)
                .zIndex(2f),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(string.journal_entry_remove_photo_cd),
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

private fun stableAngle(seed: String, rangeDegrees: Int): Float {
    // Deterministic 0..rangeDegrees value from the seed, centred so we get -range/2..+range/2.
    val hashed = abs(seed.hashCode())
    val mod = hashed % (rangeDegrees * 2 + 1)
    return (mod - rangeDegrees).toFloat()
}

private const val IMAGE_SIZE = 104
private const val TAPE_WIDTH = 56
private const val TAPE_HEIGHT = 14
private const val ROTATION_RANGE = 6
private const val TAPE_ROTATION_RANGE = 4
private const val TAPE_ALPHA = 0.85f
private val TAPE_COLOR = Color(0xFFFFE082)
