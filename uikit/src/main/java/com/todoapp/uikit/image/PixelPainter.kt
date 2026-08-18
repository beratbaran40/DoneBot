package com.todoapp.uikit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.roundToInt

/** Target edge, in px, of one output block. Six reads as clearly 8-bit without erasing the subject. */
internal const val DEFAULT_BLOCK_SIZE = 6

/**
 * Renders [painter] once into a small offscreen bitmap and returns a painter that magnifies it with
 * [FilterQuality.None] — nearest-neighbour, so the blocks stay hard-edged. The effect is a
 * downsample-then-upscale, which is exactly how a photoreal render becomes pixel art.
 *
 * **Returns [painter] itself for every kit that is not pixel-cornered**, so call sites can wrap
 * unconditionally and the rounded kits keep their original artwork untouched.
 *
 * The source aspect ratio is preserved: the offscreen is sized proportionally rather than squashed
 * to a square, so the caller's `ContentScale` (usually `Crop`) behaves exactly as it did before.
 *
 * Memory is negligible — a 180dp illustration at [blockSize] 6 on a 3.5x screen is ~105x105 px
 * (~44 KB) rather than the full-resolution decode.
 *
 * Two constraints worth knowing:
 * - The returned painter owns a bitmap that is recycled when this composable leaves the composition.
 *   Do **not** hoist it above the composable that created it; a surviving reference would draw a
 *   recycled bitmap and crash.
 * - Intended for raster artwork ([painterResource] on a webp/png). A `VectorPainter` drawn from a
 *   bare [CanvasDrawScope] relies on a composition-scoped cache, and a 24dp icon at this block size
 *   collapses to a handful of cells anyway — give icons a pixel drawable variant instead.
 */
@Composable
internal fun rememberPixelPainter(
    painter: Painter,
    size: Dp,
    blockSize: Int = DEFAULT_BLOCK_SIZE,
): Painter {
    val pixelate = TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val sizePx = with(density) { size.roundToPx() }
    val blocks = blockSize.coerceAtLeast(1)

    val bitmap: ImageBitmap? =
        remember(pixelate, painter, sizePx, blocks, density, layoutDirection) {
            if (!pixelate || sizePx <= 0) {
                return@remember null
            }
            val cells = (sizePx / blocks).coerceAtLeast(1)
            val intrinsic = painter.intrinsicSize
            val aspect =
                if (intrinsic.isSpecified && intrinsic.height > 0f) {
                    intrinsic.width / intrinsic.height
                } else {
                    1f
                }
            val width = if (aspect >= 1f) cells else (cells * aspect).roundToInt().coerceAtLeast(1)
            val height = if (aspect >= 1f) (cells / aspect).roundToInt().coerceAtLeast(1) else cells
            val target = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
            val cellSize = Size(width.toFloat(), height.toFloat())
            CanvasDrawScope().draw(density, layoutDirection, Canvas(target), cellSize) {
                with(painter) { draw(size = cellSize) }
            }
            target
        }

    // CLAUDE.md bitmap rule: key on the bitmap and close over the LOCAL reference. Re-reading the
    // state inside onDispose would recycle the NEW bitmap after a size or palette change and leak
    // the old one.
    DisposableEffect(bitmap) {
        val captured = bitmap
        onDispose { captured?.asAndroidBitmap()?.recycle() }
    }

    return remember(bitmap, painter) {
        bitmap?.let { BitmapPainter(it, filterQuality = FilterQuality.None) } ?: painter
    }
}

/** Avatars are small; a 3px block keeps a 36dp face recognisable while still reading as pixel art. */
private const val AVATAR_BLOCK_SIZE = 3

/**
 * Coil model for artwork that should read as pixel art in a pixel kit. Wraps [data] in a request
 * decoded at a deliberately tiny resolution; paired with [tdPixelFilterQuality] the result is
 * nearest-neighbour magnified, so Coil does the downsample-upscale and owns the native memory.
 *
 * Returns [data] untouched for every other kit. Use for BRANDED artwork and avatars only — never for
 * user photos, which must stay faithful in every kit.
 */
@Composable
fun rememberPixelImageModel(
    data: Any?,
    size: Dp,
    blockSize: Int = AVATAR_BLOCK_SIZE,
): Any? {
    val pixelate = TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL
    val context = LocalContext.current
    val density = LocalDensity.current
    val cells = with(density) { (size.roundToPx() / blockSize.coerceAtLeast(1)).coerceAtLeast(1) }
    return remember(pixelate, data, cells, context) {
        if (!pixelate || data == null) {
            data
        } else {
            ImageRequest.Builder(context).data(data).size(cells).build()
        }
    }
}

/** [FilterQuality.None] in a pixel kit — nearest-neighbour keeps magnified blocks hard-edged. */
@Composable
fun tdPixelFilterQuality(): FilterQuality = if (TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL) {
    FilterQuality.None
} else {
    DrawScope.DefaultFilterQuality
}

/** Cell count along the longest edge for a downsampled [ImageBitmap]; 24 keeps a small logo legible. */
private const val BITMAP_CELLS = 24

/**
 * Downsampled copy of [source] for a pixel kit, to be drawn back with [FilterQuality.None].
 *
 * The [Painter]-based [rememberPixelPainter] cannot help where the drawing goes through
 * `DrawScope.drawImage` on a raw [ImageBitmap] (the Polaroid camera's branding sticker). Note that a
 * `drawImage` that merely shrinks with nearest-neighbour aliases rather than blocks — the blocks come
 * from downsample-*then*-upscale, which is why the small copy has to be produced here and remembered
 * rather than computed per frame.
 *
 * Returns [source] untouched for every other kit. The copy is recycled when the caller leaves the
 * composition, so do not hoist it.
 */
@Composable
fun rememberPixelBitmap(
    source: ImageBitmap,
    cells: Int = BITMAP_CELLS,
): ImageBitmap {
    val pixelate = TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val small: ImageBitmap? = remember(pixelate, source, cells, density, layoutDirection) {
        if (!pixelate || source.width <= 0 || source.height <= 0) {
            return@remember null
        }
        val aspect = source.width.toFloat() / source.height.toFloat()
        val w = (if (aspect >= 1f) cells else (cells * aspect).roundToInt()).coerceAtLeast(1)
        val h = (if (aspect >= 1f) (cells / aspect).roundToInt() else cells).coerceAtLeast(1)
        val target = ImageBitmap(w, h, ImageBitmapConfig.Argb8888)
        val size = Size(w.toFloat(), h.toFloat())
        CanvasDrawScope().draw(density, layoutDirection, Canvas(target), size) {
            with(BitmapPainter(source)) { draw(size = size) }
        }
        target
    }

    DisposableEffect(small) {
        val captured = small
        onDispose { captured?.asAndroidBitmap()?.recycle() }
    }
    return small ?: source
}
