package com.todoapp.mobile.ui.common.components

import android.graphics.Bitmap

/** Largest output edge in px; larger crops are downscaled to keep upload size sane. */
internal const val CROP_MAX_OUTPUT_PX = 1024

/** Upper bound for pinch-zoom in the crop window. */
internal const val CROP_MAX_SCALE = 5f

/**
 * Inverts the on-screen display transform (ContentScale.Crop base fit + user scale/translation)
 * to map the square crop window back into source-bitmap pixels and returns the cropped (and, if
 * large, downscaled) square. Always returns a bitmap distinct from [source] so the caller can
 * recycle it without touching the source the screen still owns.
 *
 * Shared by the avatar crop screen and the task-photo crop overlay.
 */
internal fun computeCroppedBitmap(
    source: Bitmap,
    boxPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Bitmap {
    val bw = source.width.toFloat()
    val bh = source.height.toFloat()
    val base = maxOf(boxPx / bw, boxPx / bh)

    fun srcCoord(screen: Float, bDim: Float, off: Float): Float {
        val centered = boxPx / 2f + (screen - boxPx / 2f - off) / scale
        return (centered + (bDim * base - boxPx) / 2f) / base
    }

    val leftF = srcCoord(0f, bw, offsetX)
    val rightF = srcCoord(boxPx, bw, offsetX)
    val topF = srcCoord(0f, bh, offsetY)
    val bottomF = srcCoord(boxPx, bh, offsetY)

    val left = leftF.coerceIn(0f, bw - 1f).toInt()
    val top = topF.coerceIn(0f, bh - 1f).toInt()
    val width = (rightF - leftF).toInt().coerceAtLeast(1).coerceAtMost(source.width - left)
    val height = (bottomF - topF).toInt().coerceAtLeast(1).coerceAtMost(source.height - top)

    val region = Bitmap.createBitmap(source, left, top, width, height)
    // createBitmap returns the source itself when the region is the whole image — copy so a later
    // recycle of the result never frees the source.
    val cropped = if (region === source) region.copy(region.config ?: Bitmap.Config.ARGB_8888, false) else region

    val maxDim = maxOf(cropped.width, cropped.height)
    return if (maxDim > CROP_MAX_OUTPUT_PX) {
        val ratio = CROP_MAX_OUTPUT_PX.toFloat() / maxDim
        val scaled = Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * ratio).toInt().coerceAtLeast(1),
            (cropped.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== cropped) cropped.recycle()
        scaled
    } else {
        cropped
    }
}
