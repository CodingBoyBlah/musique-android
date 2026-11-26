package app.musique.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.theme.ArtworkPalette
import app.musique.ui.theme.MusiqueMotion
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Ambient drifting color pools sampled from the playing track's [ArtworkPalette].
 * Animates only while [isPlaying] and [animateDrift] are true; freezes cleanly in place when paused.
 */
@Composable
fun NowPlayingBackdrop(
    palette: ArtworkPalette,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    animateDrift: Boolean = true,
    monochrome: Boolean = LocalUiSettings.current.monochromePlayer,
    scrimAlpha: Float = 0.22f,
) {
    fun Color.maybeMonochrome(): Color {
        if (!monochrome) return this
        val l = luminance().coerceIn(0.06f, 0.70f)
        return Color(l, l, l, alpha)
    }

    val targetTop = palette.backgroundTop.maybeMonochrome()
    val targetBottom = palette.backgroundBottom.maybeMonochrome()
    val swatches = palette.swatches

    val bgTop by animateColorAsState(targetTop, MusiqueMotion.color(), label = "backdropTop")
    val bgBottom by animateColorAsState(targetBottom, MusiqueMotion.color(), label = "backdropBottom")

    val pool0 by animateColorAsState(
        (swatches.getOrNull(0) ?: palette.backgroundTop).maybeMonochrome(),
        MusiqueMotion.color(),
        label = "pool0",
    )
    val pool1 by animateColorAsState(
        (swatches.getOrNull(1) ?: swatches.getOrNull(0) ?: palette.surface).maybeMonochrome(),
        MusiqueMotion.color(),
        label = "pool1",
    )
    val pool2 by animateColorAsState(
        (swatches.getOrNull(2) ?: swatches.getOrNull(0) ?: palette.backgroundTop).maybeMonochrome(),
        MusiqueMotion.color(),
        label = "pool2",
    )
    val pool3 by animateColorAsState(
        (swatches.getOrNull(3) ?: swatches.getOrNull(1) ?: palette.surface).maybeMonochrome(),
        MusiqueMotion.color(),
        label = "pool3",
    )

    val phase = remember { Animatable(0f) }
    val shouldRun = isPlaying && animateDrift

    LaunchedEffect(shouldRun) {
        if (!shouldRun) return@LaunchedEffect
        val fullCycle = (2.0 * PI).toFloat()
        while (isActive) {
            val start = (phase.value % fullCycle).let { if (it < 0f) it + fullCycle else it }
            phase.snapTo(start)
            phase.animateTo(
                targetValue = start + fullCycle,
                animationSpec = tween(durationMillis = 26_000, easing = LinearEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(bgTop, bgBottom),
                    ),
                )

                val t = phase.value
                val maxDim = max(w, h)

                fun drawPool(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
                    if (radius <= 0f) return
                    val center = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }

                drawPool(
                    color = pool0,
                    cx = w * (0.24f + 0.14f * cos(t)),
                    cy = h * (0.26f + 0.11f * sin(t)),
                    radius = maxDim * 0.78f,
                    alpha = 0.52f,
                )
                drawPool(
                    color = pool1,
                    cx = w * (0.78f + 0.14f * sin(t * 1.1f + 1.4f)),
                    cy = h * (0.42f + 0.13f * cos(t * 0.9f)),
                    radius = maxDim * 0.72f,
                    alpha = 0.45f,
                )
                drawPool(
                    color = pool2,
                    cx = w * (0.28f + 0.15f * sin(t * 0.8f + 2.7f)),
                    cy = h * (0.76f + 0.12f * cos(t * 1.2f + 0.8f)),
                    radius = maxDim * 0.80f,
                    alpha = 0.44f,
                )
                drawPool(
                    color = pool3,
                    cx = w * (0.76f + 0.12f * cos(t * 1.3f + 4.1f)),
                    cy = h * (0.86f + 0.10f * sin(t * 0.7f)),
                    radius = maxDim * 0.65f,
                    alpha = 0.36f,
                )

                // Contrast guard so white titles and lyric lines remain legible over bright artwork.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = scrimAlpha * 0.45f),
                        0.55f to Color.Black.copy(alpha = scrimAlpha),
                        1f to Color.Black.copy(alpha = (scrimAlpha + 0.14f).coerceAtMost(0.55f)),
                    ),
                )
            },
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NowPlayingBackdropPreview() {
    val samplePalette = ArtworkPalette(
        swatches = listOf(
            Color(0xFF8E3B16),
            Color(0xFF4F2411),
            Color(0xFFB35D2E),
            Color(0xFF2B1810),
        ),
        backgroundTop = Color(0xFF542205),
        backgroundBottom = Color(0xFF281002),
        surface = Color(0xFF4C2410),
        accent = Color(0xFFF3D5BE),
        onAccent = Color(0xFF1F1108),
    )
    NowPlayingBackdrop(palette = samplePalette, isPlaying = false)
}
