package app.musique.ui.lyrics

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Brightness of sung text on the active line. */
private const val LIT_ALPHA = 0.99f

/** Brightness of not yet sung text on the active line. */
const val LYRIC_UNSUNG_ALPHA = 0.45f

/** Soft edge of the sweep, as a share of the font size (about one and a half glyphs wide). */
private const val SOFT_EDGE_EM = 0.8f

private val WordLift = 1.5.dp
private val WordGlow = 8.dp
private const val WordGlowAlpha = 0.35f

/** Clip margin so glyph overhang and the lifted word are never cut. */
private const val BLEED = 4_000f

/**
 * One lyric voice, always laid out from the same text at the same size, so it wraps exactly once
 * and never re-flows when it lights up. Inactive and line-synced voices draw as plain text in
 * [ink]. An active word-timed voice is redrawn line by line with a horizontal gradient whose soft
 * edge follows the sung position, plus a small lift and glow on the word being sung.
 *
 * [clock] is only read while drawing, so the sweep animates without recomposition.
 */
@Composable
fun LyricVoiceText(
    geometry: LineSweepGeometry,
    style: TextStyle,
    align: TextAlign,
    ink: Color,
    sweeping: Boolean,
    clock: () -> Long,
    modifier: Modifier = Modifier,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = geometry.fullText,
        style = style.copy(color = ink, textAlign = align),
        onTextLayout = { layout = it },
        modifier = modifier.drawWithContent {
            val result = layout
            if (!sweeping || !geometry.hasWordTiming || result == null ||
                result.layoutInput.text.text != geometry.fullText
            ) {
                drawContent()
            } else {
                drawSweep(result, geometry, geometry.cursorAt(clock()), ink, style)
            }
        },
    )
}

private fun DrawScope.drawSweep(
    layout: TextLayoutResult,
    geometry: LineSweepGeometry,
    cursor: SweepCursor,
    ink: Color,
    style: TextStyle,
) {
    val lit = ink.copy(alpha = LIT_ALPHA)
    val unsung = ink.copy(alpha = LYRIC_UNSUNG_ALPHA)
    val textLength = geometry.fullText.length
    val lineCount = layout.lineCount
    if (textLength == 0 || lineCount == 0) return

    val pos = cursor.renderCharPosition
    val sweepLine: Int
    val sweepX: Float
    val rtl: Boolean
    when {
        pos <= 0f -> {
            sweepLine = 0
            rtl = layout.getParagraphDirection(0) == ResolvedTextDirection.Rtl
            sweepX = if (rtl) layout.getLineRight(0) else layout.getLineLeft(0)
        }
        pos >= textLength -> {
            sweepLine = lineCount
            rtl = false
            sweepX = 0f
        }
        else -> {
            val index = floor(pos).toInt().coerceIn(0, textLength - 1)
            val frac = pos - index
            val box = layout.getBoundingBox(index)
            sweepLine = layout.getLineForOffset(index)
            rtl = layout.getParagraphDirection(index) == ResolvedTextDirection.Rtl
            sweepX = if (rtl) box.right - box.width * frac else box.left + box.width * frac
        }
    }

    val fontPx = if (style.fontSize.isSp) style.fontSize.toPx() else 30.dp.toPx()
    val halfEdge = fontPx * SOFT_EDGE_EM / 2f

    // The word being sung rises a touch over its first quarter and settles back.
    val token = geometry.tokens.getOrNull(cursor.activeTokenIndex)
    val liftFraction = if (token != null) activeWordLiftFraction(cursor.activeWordFraction) else 0f
    var wordLine = -1
    var wordLeft = 0f
    var wordRight = 0f
    if (token != null && liftFraction > 0.01f && token.bodyEnd > token.renderStart) {
        val first = token.renderStart.coerceIn(0, textLength - 1)
        val last = (token.bodyEnd - 1).coerceIn(0, textLength - 1)
        val line = layout.getLineForOffset(first)
        if (line == layout.getLineForOffset(last)) {
            val a = layout.getBoundingBox(first)
            val b = layout.getBoundingBox(last)
            wordLine = line
            wordLeft = min(a.left, b.left)
            wordRight = max(a.right, b.right)
        }
    }

    for (line in 0 until lineCount) {
        val top = if (line == 0) -BLEED else layout.getLineTop(line)
        val bottom = if (line == lineCount - 1) size.height + BLEED else layout.getLineBottom(line)
        val brush: Brush = when {
            line < sweepLine -> SolidColor(lit)
            line > sweepLine -> SolidColor(unsung)
            rtl -> Brush.horizontalGradient(
                0f to unsung, 1f to lit,
                startX = sweepX - halfEdge, endX = sweepX + halfEdge,
            )
            else -> Brush.horizontalGradient(
                0f to lit, 1f to unsung,
                startX = sweepX - halfEdge, endX = sweepX + halfEdge,
            )
        }
        if (line != wordLine) {
            clipRect(left = -BLEED, top = top, right = size.width + BLEED, bottom = bottom) {
                drawText(layout, brush = brush)
            }
            continue
        }
        clipRect(left = -BLEED, top = top, right = wordLeft, bottom = bottom) {
            drawText(layout, brush = brush)
        }
        clipRect(left = wordRight, top = top, right = size.width + BLEED, bottom = bottom) {
            drawText(layout, brush = brush)
        }
        val lift = WordLift.toPx() * liftFraction
        val glow = Shadow(
            color = ink.copy(alpha = WordGlowAlpha * liftFraction),
            offset = Offset.Zero,
            blurRadius = WordGlow.toPx(),
        )
        clipRect(left = wordLeft, top = top - lift, right = wordRight, bottom = bottom - lift) {
            translate(top = -lift) {
                drawText(layout, brush = brush, shadow = glow)
            }
        }
    }
}
