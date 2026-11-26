package app.musique.ui.lyrics

import androidx.compose.runtime.Immutable
import app.musique.core.model.LyricLine
import kotlin.math.max
import kotlin.math.min

@Immutable
data class RenderWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@Immutable
data class Tone(
    val blurDp: Float,
    val alpha: Float,
)

@Immutable
data class SweepTokenRange(
    val wordIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val renderStart: Int,
    val bodyEnd: Int,
    val renderEnd: Int,
    val charStart: Int,
    val charLength: Int,
)

@Immutable
data class LineSweepGeometry(
    val fullText: String,
    val hasWordTiming: Boolean,
    val tokens: List<SweepTokenRange>,
    val totalChars: Int,
)

data class SweepCursor(
    val charPosition: Float,
    val activeTokenIndex: Int,
    val activeWordFraction: Float,
    val renderCharPosition: Float,
)

fun lyricSeekTarget(lineStartMs: Long, offsetMs: Long = 0L): Long =
    max(0L, lineStartMs - offsetMs)

fun lyricTone(distance: Int, past: Boolean, highContrast: Boolean = false): Tone {
    if (distance <= 0) return Tone(blurDp = 0f, alpha = 1f)
    val d = distance - 1
    if (highContrast) {
        val base = if (past) 0.62f else 0.72f
        return Tone(blurDp = 0f, alpha = max(0.50f, base - d * 0.06f))
    }
    val base = if (past) 0.44f else 0.54f
    return Tone(
        blurDp = min(6.5f, d * 2.2f),
        alpha = max(0.10f, base - d * 0.12f),
    )
}

fun mapWords(line: LyricLine): List<RenderWord> {
    val ws = line.words.orEmpty()
    return ws.mapIndexed { i, w ->
        val end = when {
            w.endMs > w.startMs -> w.endMs
            i + 1 < ws.size -> max(w.startMs, ws[i + 1].startMs)
            else -> w.startMs + 400L
        }
        RenderWord(
            text = cleanSpaces(w.text),
            startMs = w.startMs,
            endMs = end,
        )
    }
}

private val TRAILING_PUNCT = Regex("""^[,.!?;:…)\]}»”’"'\-–—、。！？」』]+$""")
private val LEADING_PUNCT = Regex("""^[(\[{«“‘¿¡「『]+$""")
private val TRAILING_WHITESPACE = Regex("""\s+$""")

/**
 * Re-splits provider word tokens on whitespace so punctuation never breaks onto its own line,
 * collapses multi-space runs, distributes phrase durations proportionally across characters,
 * and ensures monotonic timestamps.
 */
fun weldPunctuation(words: List<RenderWord>): List<RenderWord> {
    if (words.isEmpty()) return emptyList()

    val flatText = StringBuilder()
    val cStart = mutableListOf<Double>()
    val cEnd = mutableListOf<Double>()

    for (w in words) {
        val len = w.text.length
        if (len == 0) continue
        val span = (w.endMs - w.startMs).toDouble()
        for (i in 0 until len) {
            cStart += w.startMs + (span * i) / len
            cEnd += w.startMs + (span * (i + 1)) / len
        }
        flatText.append(w.text)
    }

    val text = flatText.toString()
    if (text.isEmpty()) return emptyList()

    data class MutableWord(var text: String, var startMs: Double, var endMs: Double)

    val split = mutableListOf<MutableWord>()
    var i = 0
    while (i < text.length) {
        if (text[i].isWhitespace()) {
            val from = i
            while (i < text.length && text[i].isWhitespace()) i++
            val prev = split.lastOrNull()
            if (prev != null) {
                prev.text += " "
                prev.endMs = max(prev.endMs, cEnd.getOrElse(i - 1) { cEnd[from] })
            }
            continue
        }
        val from = i
        while (i < text.length && !text[i].isWhitespace()) i++
        split += MutableWord(
            text = text.substring(from, i),
            startMs = cStart[from],
            endMs = cEnd[i - 1],
        )
    }

    val out = mutableListOf<MutableWord>()
    var lead = ""
    var leadStart = 0.0

    for (w in split) {
        val bare = w.text.trim()
        if (bare.isEmpty()) continue

        if (LEADING_PUNCT.matches(bare)) {
            if (lead.isEmpty()) leadStart = w.startMs
            lead += bare
            continue
        }

        val prev = out.lastOrNull()
        if (TRAILING_PUNCT.matches(bare) && prev != null && lead.isEmpty()) {
            val hadTrailingSpace = w.text.lastOrNull()?.isWhitespace() == true
            prev.text = prev.text.replace(TRAILING_WHITESPACE, "") + bare + if (hadTrailingSpace) " " else ""
            prev.endMs = max(prev.endMs, w.endMs)
            continue
        }

        out += MutableWord(
            text = lead + w.text,
            startMs = if (lead.isNotEmpty()) min(leadStart, w.startMs) else w.startMs,
            endMs = w.endMs,
        )
        lead = ""
    }

    if (lead.isNotEmpty()) {
        val prev = out.lastOrNull()
        if (prev != null) {
            prev.text = prev.text.replace(TRAILING_WHITESPACE, "") + lead
        } else {
            out += MutableWord(text = lead, startMs = leadStart, endMs = leadStart)
        }
    }

    val finalized = ArrayList<RenderWord>(out.size)
    for (k in out.indices) {
        var s = out[k].startMs.toLong()
        if (k > 0) {
            s = max(s, finalized[k - 1].endMs)
        }
        val e = max(out[k].endMs.toLong(), s)
        finalized += RenderWord(text = out[k].text, startMs = s, endMs = e)
    }
    return finalized
}

/**
 * Returns real word timings when present, or a single line token so line-synced lyrics
 * light instantaneously as a whole without faking interior timing.
 */
fun lyricWords(line: LyricLine, rowStartMs: Long): List<RenderWord> {
    val effectiveStart = if (line.timestampMs >= 0L) line.timestampMs else rowStartMs
    if (!line.words.isNullOrEmpty()) {
        val welded = weldPunctuation(mapWords(line))
        if (welded.isNotEmpty()) return welded
    }
    return listOf(
        RenderWord(
            text = cleanSpaces(line.text),
            startMs = effectiveStart,
            endMs = effectiveStart,
        )
    )
}

fun buildSweepGeometry(words: List<RenderWord>): LineSweepGeometry {
    val safeWords = if (words.isEmpty()) listOf(RenderWord("♪", 0L, 0L)) else words
    val sb = StringBuilder()
    val ranges = ArrayList<SweepTokenRange>(safeWords.size)
    var charAt = 0

    safeWords.forEachIndexed { idx, w ->
        val renderStart = sb.length
        sb.append(w.text)
        val renderEnd = sb.length
        val trimmedLen = w.text.trimEnd().length
        val bodyEnd = renderStart + trimmedLen
        val n = max(w.text.trim().length, 1)
        ranges += SweepTokenRange(
            wordIndex = idx,
            startMs = w.startMs,
            endMs = w.endMs,
            renderStart = renderStart,
            bodyEnd = bodyEnd,
            renderEnd = renderEnd,
            charStart = charAt,
            charLength = n,
        )
        charAt += n
    }

    val hasWordTiming = safeWords.size > 1 ||
        (safeWords.size == 1 && safeWords[0].endMs > safeWords[0].startMs)

    return LineSweepGeometry(
        fullText = sb.toString(),
        hasWordTiming = hasWordTiming,
        tokens = ranges,
        totalChars = max(charAt, 1),
    )
}

fun LineSweepGeometry.cursorAt(clockMs: Long): SweepCursor {
    if (tokens.isEmpty()) return SweepCursor(0f, -1, 0f, 0f)
    if (!hasWordTiming) {
        val lit = clockMs >= tokens[0].startMs
        return if (lit) {
            SweepCursor(
                charPosition = totalChars.toFloat(),
                activeTokenIndex = -1,
                activeWordFraction = 0f,
                renderCharPosition = fullText.length.toFloat(),
            )
        } else {
            SweepCursor(0f, -1, 0f, 0f)
        }
    }

    if (clockMs <= tokens[0].startMs) {
        return SweepCursor(0f, -1, 0f, 0f)
    }

    for (i in tokens.indices) {
        val tok = tokens[i]
        if (clockMs < tok.startMs) {
            return SweepCursor(
                charPosition = tok.charStart.toFloat(),
                activeTokenIndex = -1,
                activeWordFraction = 0f,
                renderCharPosition = tok.renderStart.toFloat(),
            )
        }
        if (clockMs <= tok.endMs) {
            val span = max(tok.endMs - tok.startMs, 1L).toFloat()
            val frac = ((clockMs - tok.startMs).toFloat() / span).coerceIn(0f, 1f)
            val charPos = tok.charStart + tok.charLength * frac
            val renderLen = (tok.renderEnd - tok.renderStart).coerceAtLeast(1)
            val renderPos = tok.renderStart + renderLen * frac
            return SweepCursor(
                charPosition = charPos,
                activeTokenIndex = tok.wordIndex,
                activeWordFraction = frac,
                renderCharPosition = renderPos,
            )
        }
    }

    return SweepCursor(
        charPosition = totalChars.toFloat(),
        activeTokenIndex = -1,
        activeWordFraction = 0f,
        renderCharPosition = fullText.length.toFloat(),
    )
}

/**
 * Computes normalized vertical lift (0..1) for the active word: rises quickly over the first 25%
 * of the word's duration and settles gently down over the remainder.
 */
fun activeWordLiftFraction(wordProgress: Float): Float {
    val p = wordProgress.coerceIn(0f, 1f)
    if (p <= 0f || p >= 1f) return 0f
    return if (p < 0.25f) {
        val t = p / 0.25f
        t * (2f - t)
    } else {
        val t = (p - 0.25f) / 0.75f
        val inv = 1f - t
        inv * inv
    }
}
