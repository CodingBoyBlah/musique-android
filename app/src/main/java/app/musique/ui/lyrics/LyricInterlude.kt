package app.musique.ui.lyrics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

private val DotSize = 12.dp
private val DotGap = 9.dp
private val OpenHeight = 42.dp
private const val BREATH_PERIOD_MS = 2_800.0
private const val BREATH_DEPTH = 0.08f

/**
 * An instrumental stretch drawn as three dots that fill one after another across the gap,
 * breathe while the music plays, swell over the last tenth and fold away as singing resumes.
 * Folded to zero height whenever it is not the active row. Breathing is driven by the lyric
 * clock, so it holds still while paused.
 */
@Composable
fun LyricInterlude(
    startMs: Long,
    endMs: Long,
    active: Boolean,
    clock: () -> Long,
    ink: Color,
    reduceMotion: Boolean,
    alignEnd: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val height by animateDpAsState(
        targetValue = if (active) OpenHeight else 0.dp,
        animationSpec = if (reduceMotion) tween(0) else tween(500, easing = FastOutSlowInEasing),
        label = "interludeHeight",
    )
    // In late on a soft overshoot once the space has started opening; out fast.
    val pose by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = when {
            reduceMotion -> tween(0)
            active -> spring(dampingRatio = 0.55f, stiffness = 260f)
            else -> tween(220)
        },
        label = "interludePose",
    )
    if (height <= 0.dp && pose <= 0f) {
        Canvas(modifier.fillMaxWidth().height(0.dp)) {}
        return
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        val now = clock()
        val p = interludeProgress(now, startMs, endMs)
        val dot = DotSize.toPx()
        val gap = DotGap.toPx()
        val groupWidth = dot * 3 + gap * 2
        val cy = size.height / 2f
        val originX = if (alignEnd) size.width else 0f
        val left = if (alignEnd) size.width - groupWidth else 0f

        val breath = if (reduceMotion) 1f else
            1f + BREATH_DEPTH * (0.5f - 0.5f * cos(2.0 * PI * now / BREATH_PERIOD_MS).toFloat())
        val swell = if (reduceMotion) 1f else interludeAnticipationScale(p)
        val enter = 0.5f + 0.5f * pose.coerceIn(0f, 1.2f)
        val groupScale = breath * swell * enter

        scale(groupScale, pivot = Offset(originX, cy)) {
            for (i in 0 until 3) {
                val e = interludeDotFill(p, i)
                val alpha = (0.26f + 0.74f * e) * pose.coerceIn(0f, 1f)
                val radius = dot / 2f * (0.84f + 0.16f * e)
                val center = Offset(left + dot / 2f + i * (dot + gap), cy)
                if (e > 0.01f) {
                    val glowRadius = radius * 2.2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to ink.copy(alpha = 0.4f * e * pose.coerceIn(0f, 1f)),
                            1f to Color.Transparent,
                            center = center,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = center,
                    )
                }
                drawCircle(color = ink.copy(alpha = alpha), radius = radius, center = center)
            }
        }
    }
}
