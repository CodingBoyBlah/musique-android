package app.musique.ui.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.SmartImage

/** Corner radius the artwork rounds in to when paused. */
private val PausedCorner = 20.dp
/** How small the artwork gets when paused. */
private const val PausedScale = 0.9f
/** Where the feather towards the backdrop starts, as a fraction of the artwork height. */
private const val FeatherStart = 0.58f

/**
 * Cider full-bleed artwork, measured once at [fullSize] and moved purely with layer transforms
 * so it never re-measures (or re-requests the image) while it animates.
 *
 * - [pausedFraction] 0 playing, 1 paused: shrinks to 0.9 around the top center, rounds the corners
 *   in and drops the feather so the paused state reads clearly.
 * - [lyricsFraction] 0 artwork mode, 1 lyrics mode: flies into a [thumbSize] rounded thumbnail at
 *   ([thumbX], [thumbY]).
 */
@Composable
internal fun NowPlayingArtwork(
    artworkUrl: String?,
    fullSize: Dp,
    pausedTopInset: Dp,
    thumbX: Dp,
    thumbY: Dp,
    pausedFraction: () -> Float,
    lyricsFraction: () -> Float,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 48.dp,
    thumbCorner: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .size(fullSize)
            .graphicsLayer {
                val paused = pausedFraction()
                val lyrics = lyricsFraction()
                val full = fullSize.toPx()

                val artScale = lerp(1f, PausedScale, paused)
                val artX = full * (1f - artScale) / 2f
                val artY = lerp(0f, pausedTopInset.toPx(), paused)

                val thumbScale = thumbSize.toPx() / full
                val s = lerp(artScale, thumbScale, lyrics)

                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = s
                scaleY = s
                translationX = lerp(artX, thumbX.toPx(), lyrics)
                translationY = lerp(artY, thumbY.toPx(), lyrics)

                val corner = lerp(lerp(0f, PausedCorner.toPx(), paused), thumbCorner.toPx(), lyrics)
                // The shape is drawn before scaling, so divide to keep the on-screen radius.
                val radius = corner / s.coerceAtLeast(0.01f)
                shape = if (radius < 0.5f) RectangleShape else RoundedCornerShape(radius)
                clip = true
                shadowElevation = 18.dp.toPx() * paused * (1f - lyrics)
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val feather = (1f - pausedFraction()) * (1f - lyricsFraction())
                if (feather > 0.01f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            FeatherStart to Color.Black,
                            1f to Color.Black.copy(alpha = 1f - feather),
                            startY = 0f,
                            endY = size.height,
                        ),
                        topLeft = Offset.Zero,
                        size = size,
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        SmartImage(
            url = artworkUrl,
            monochrome = LocalUiSettings.current.monochromePlayer,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
