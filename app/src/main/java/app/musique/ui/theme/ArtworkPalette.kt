package app.musique.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Colors taken from a cover, tuned so white text stays readable on top of them.
 * Mirrors the desktop app's ambient palette: k-means pools with lightness clamped.
 */
@Immutable
data class ArtworkPalette(
    /** Cluster centers, most common first, lightness clamped for dark backgrounds. */
    val swatches: List<Color>,
    /** Top of a full-bleed artwork gradient (detail pages, Now Playing). */
    val backgroundTop: Color,
    /** Bottom of that gradient. */
    val backgroundBottom: Color,
    /** Tinted container: mini player, grouped track list card, secondary buttons. */
    val surface: Color,
    /** Light tint for a primary button on the artwork, like Cider's cream play button. */
    val accent: Color,
    /** Icon / text color on [accent]. */
    val onAccent: Color,
) {
    companion object {
        val Default = ArtworkPalette(
            swatches = listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E), Color(0xFF1C1C1E)),
            backgroundTop = Color(0xFF2C2C2E),
            backgroundBottom = Color(0xFF121212),
            surface = Color(0xFF3A3A3C),
            accent = Color(0xFFF2F2F2),
            onAccent = Color(0xFF1C1C1E),
        )
    }
}

/** Palette of whatever is playing right now; provided at the app root. */
val LocalNowPlayingPalette = staticCompositionLocalOf { ArtworkPalette.Default }

private const val SampleSize = 24
private const val Clusters = 5
private const val Iterations = 8

/** Extracts an [ArtworkPalette]. Cheap (24x24 sample) but still run it off the main thread. */
fun Bitmap.toArtworkPalette(): ArtworkPalette {
    val small = if (width == SampleSize && height == SampleSize) this else scale(SampleSize, SampleSize)
    val pixels = IntArray(SampleSize * SampleSize)
    small.getPixels(pixels, 0, SampleSize, 0, 0, SampleSize, SampleSize)

    val points = pixels.map { p ->
        floatArrayOf(((p shr 16) and 0xFF).toFloat(), ((p shr 8) and 0xFF).toFloat(), (p and 0xFF).toFloat())
    }
    // Deterministic seeds: points at evenly spaced luminance ranks, so the same cover always gives the same palette.
    val byLuma = points.sortedBy { 0.2126f * it[0] + 0.7152f * it[1] + 0.0722f * it[2] }
    val centers = Array(Clusters) { i -> byLuma[(i * (byLuma.size - 1)) / (Clusters - 1)].copyOf() }
    val assignment = IntArray(points.size)

    repeat(Iterations) {
        points.forEachIndexed { idx, pt ->
            var best = 0
            var bestDist = Float.MAX_VALUE
            centers.forEachIndexed { c, center ->
                val dr = pt[0] - center[0]; val dg = pt[1] - center[1]; val db = pt[2] - center[2]
                val d = dr * dr + dg * dg + db * db
                if (d < bestDist) { bestDist = d; best = c }
            }
            assignment[idx] = best
        }
        val sums = Array(Clusters) { FloatArray(3) }
        val counts = IntArray(Clusters)
        points.forEachIndexed { idx, pt ->
            val c = assignment[idx]
            sums[c][0] += pt[0]; sums[c][1] += pt[1]; sums[c][2] += pt[2]
            counts[c]++
        }
        for (c in 0 until Clusters) if (counts[c] > 0) {
            centers[c][0] = sums[c][0] / counts[c]
            centers[c][1] = sums[c][1] / counts[c]
            centers[c][2] = sums[c][2] / counts[c]
        }
    }

    val counts = IntArray(Clusters).also { cnt -> assignment.forEach { cnt[it]++ } }
    val ranked = (0 until Clusters).filter { counts[it] > 0 }.sortedByDescending { counts[it] }
    val hsls = ranked.map { c ->
        FloatArray(3).also { ColorUtils.RGBToHSL(centers[c][0].toInt(), centers[c][1].toInt(), centers[c][2].toInt(), it) }
    }

    // Prefer a colorful cluster for the tint when the dominant one is nearly grey (black/white covers).
    val dominant = hsls.first()
    val tintSource = hsls.take(3).maxByOrNull { it[1] * (1f - kotlin.math.abs(it[2] - 0.5f)) }
        ?.takeIf { it[1] > dominant[1] + 0.15f } ?: dominant

    fun hsl(h: FloatArray, s: Float, l: Float) =
        Color(ColorUtils.HSLToColor(floatArrayOf(h[0], s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

    return ArtworkPalette(
        swatches = hsls.map { hsl(it, it[1], it[2].coerceIn(0.07f, 0.76f)) },
        backgroundTop = hsl(dominant, dominant[1] * 0.9f, min(max(dominant[2], 0.14f), 0.22f)),
        backgroundBottom = hsl(dominant, dominant[1] * 0.85f, 0.10f),
        surface = hsl(tintSource, min(tintSource[1], 0.35f) * 0.7f, 0.25f),
        accent = hsl(tintSource, min(tintSource[1], 0.55f), 0.86f),
        onAccent = hsl(tintSource, min(tintSource[1], 0.4f), 0.10f),
    )
}

private object PaletteCache {
    val cache = LruCache<String, ArtworkPalette>(64)
}

/**
 * Loads [imageUrl] (a small cover is enough) and returns its palette, cached by URL.
 * Returns the previous palette while the next one loads; colors are animated.
 */
@Composable
fun rememberArtworkPalette(imageUrl: String?, animate: Boolean = true): ArtworkPalette {
    val context = LocalContext.current
    var target by remember { mutableStateOf(imageUrl?.let { PaletteCache.cache.get(it) } ?: ArtworkPalette.Default) }

    LaunchedEffect(imageUrl) {
        if (imageUrl == null) { target = ArtworkPalette.Default; return@LaunchedEffect }
        PaletteCache.cache.get(imageUrl)?.let { target = it; return@LaunchedEffect }
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(Size(64, 64))
                .allowHardware(false)
                .build()
        )
        val bitmap = result.image?.toBitmap() ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) { bitmap.toArtworkPalette() }
        PaletteCache.cache.put(imageUrl, palette)
        target = palette
    }

    if (!animate) return target
    return target.animated()
}

@Composable
private fun ArtworkPalette.animated(): ArtworkPalette {
    @Composable
    fun Color.anim(label: String) = animateColorAsState(this, MusiqueMotion.color(), label = label).value
    return ArtworkPalette(
        swatches = swatches,
        backgroundTop = backgroundTop.anim("bgTop"),
        backgroundBottom = backgroundBottom.anim("bgBottom"),
        surface = surface.anim("surface"),
        accent = accent.anim("accent"),
        onAccent = onAccent.anim("onAccent"),
    )
}

