package app.musique.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Springs tuned to Apple's response / damping pairs. Never use linear curves for movement. */
object MusiqueMotion {
    /** Sheets opening and closing, shared bounds. Fast, no wobble. */
    fun <T> sheet() = spring<T>(dampingRatio = 0.88f, stiffness = 360f)

    /** Button presses and toggles. A little bounce. */
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = 450f)

    /** Now Playing artwork shrinking on pause and growing on play. */
    fun <T> artwork() = spring<T>(dampingRatio = 0.72f, stiffness = 280f)

    /** Lyrics following the active line: critically damped so fast lines keep their momentum. */
    fun <T> lyricScroll() = spring<T>(dampingRatio = 1f, stiffness = 180f)

    /** Layout changes such as the tab bar pill expanding. */
    fun <T> layout() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f)

    /** Palette changes between tracks glide rather than snap. */
    fun <T> color() = tween<T>(durationMillis = 600, easing = FastOutSlowInEasing)
}

/**
 * Shrinks the element while pressed and springs back on release, like Apple's button style.
 * Pass the same [interactionSource] to the clickable so the press is seen.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
    pressedAlpha: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MusiqueMotion.press(),
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) pressedAlpha else 1f,
        animationSpec = MusiqueMotion.press(),
        label = "pressAlpha",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
