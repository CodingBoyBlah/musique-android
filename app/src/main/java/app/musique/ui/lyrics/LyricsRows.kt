package app.musique.ui.lyrics

import androidx.compose.runtime.Immutable
import app.musique.core.model.LyricLine
import app.musique.core.model.LyricRole
import kotlin.math.abs

const val CONCURRENT_TOL_MS = 60L
const val INTERLUDE_MIN_MS = 2500L
const val INTRO_MIN_MS = 3000L
const val SILENCE_MIN_MS = 7000L
const val DEFAULT_LINE_DURATION_MS = 4000L

enum class VoiceSide {
    LEFT,
    RIGHT,
}

@Immutable
data class VoiceLayout(
    val side: VoiceSide,
    val secondary: Boolean,
)

data class OverlappingVoice(
    val row: Int,
    val voice: Int,
    val startMs: Long,
    val endMs: Long,
)

@Immutable
data class LyricRow(
    val startMs: Long,
    val endMs: Long,
    val voices: List<LyricLine>,
    val isInterlude: Boolean = false,
    val synced: Boolean = startMs >= 0L,
)

data class ActiveLyricsState(
    val activeRow: Int,
    val overlapping: List<OverlappingVoice>,
    val singing: List<OverlappingVoice>,
)

internal fun cleanSpaces(s: String): String =
    s.replace(Regex("""[\u00A0\u200B\u202F\uFEFF]"""), " ")

private fun LyricLine.tidy(): LyricLine = copy(
    text = cleanSpaces(text),
    words = words?.map { it.copy(text = cleanSpaces(it.text)) },
    translation = translation?.let(::cleanSpaces),
    roman = roman?.let(::cleanSpaces),
)

private fun leadLineEnd(lead: LyricLine, fallbackNextStart: Long): Long {
    val words = lead.words
    if (!words.isNullOrEmpty()) {
        val maxEnd = words.maxOf { it.endMs }
        if (maxEnd > lead.timestampMs) return maxEnd
    }
    val explicitEnd = lead.endMs
    if (explicitEnd != null && explicitEnd > lead.timestampMs) return explicitEnd
    return fallbackNextStart
}

/**
 * Groups sequential lyric lines into display rows:
 * - Concurrent lines within [CONCURRENT_TOL_MS] and explicit background vocals share a row.
 * - Unsynced lines remain ungrouped static rows.
 * - Row [LyricRow.endMs] comes strictly from the lead voice so trailing ad-libs do not linger the row.
 */
fun buildRows(lines: List<LyricLine>): List<LyricRow> {
    if (lines.isEmpty()) return emptyList()

    if (lines.none { it.synced && it.timestampMs >= 0L }) {
        return lines.map { line ->
            LyricRow(
                startMs = -1L,
                endMs = -1L,
                voices = listOf(line.tidy().copy(synced = false)),
                isInterlude = false,
                synced = false,
            )
        }
    }

    val mutableRows = mutableListOf<MutableList<LyricLine>>()
    val rowStarts = mutableListOf<Long>()

    for (rawLine in lines) {
        val l = rawLine.tidy()
        val lastVoices = mutableRows.lastOrNull()
        val lastStart = rowStarts.lastOrNull()

        if (l.effectiveIsBackground && lastVoices != null) {
            lastVoices += l
            continue
        }

        if (
            lastVoices != null &&
            lastStart != null &&
            lastStart >= 0L &&
            l.synced &&
            l.timestampMs >= 0L &&
            abs(l.timestampMs - lastStart) <= CONCURRENT_TOL_MS
        ) {
            lastVoices += l
            continue
        }

        mutableRows += mutableListOf(l)
        rowStarts += if (l.synced) l.timestampMs else -1L
    }

    return mutableRows.mapIndexed { i, voices ->
        val startMs = rowStarts[i]
        val endMs = if (startMs >= 0L) {
            val fallbackNext = if (i + 1 < mutableRows.size && rowStarts[i + 1] >= 0L) {
                rowStarts[i + 1]
            } else {
                startMs + DEFAULT_LINE_DURATION_MS
            }
            leadLineEnd(voices.first(), fallbackNext)
        } else {
            -1L
        }
        LyricRow(
            startMs = startMs,
            endMs = endMs,
            voices = voices.toList(),
            isInterlude = false,
            synced = startMs >= 0L,
        )
    }
}

/**
 * Determines each voice's horizontal alignment side and whether it renders as a subordinate
 * backing vocal. Backing vocals inherit the alignment side of the primary singer before them.
 */
fun voiceLayout(row: LyricRow): List<VoiceLayout> {
    var owner = VoiceSide.LEFT
    return row.voices.mapIndexed { i, voice ->
        val primary = !voice.effectiveIsBackground && (
            i == 0 ||
                voice.role == LyricRole.DUET ||
                voice.role == LyricRole.MAIN ||
                !voice.agent.isNullOrEmpty()
            )
        val secondary = voice.effectiveIsBackground || !primary
        if (secondary) {
            VoiceLayout(side = owner, secondary = true)
        } else {
            owner = if (voice.role == LyricRole.DUET) VoiceSide.RIGHT else VoiceSide.LEFT
            VoiceLayout(side = owner, secondary = false)
        }
    }
}

fun timedVoices(rows: List<LyricRow>): List<OverlappingVoice> =
    rows.flatMapIndexed { ri, row ->
        row.voices.mapIndexedNotNull { vi, voice ->
            if (!voice.synced || voice.timestampMs < 0L) return@mapIndexedNotNull null
            val words = voice.words
            val hasWords = !words.isNullOrEmpty()
            val explicitEnd = voice.endMs
            if (!hasWords && explicitEnd == null) return@mapIndexedNotNull null
            val endMs = if (hasWords) {
                words!!.mapIndexed { wi, w ->
                    when {
                        w.endMs > w.startMs -> w.endMs
                        wi + 1 < words.size -> words[wi + 1].startMs
                        else -> w.startMs + 400L
                    }
                }.maxOrNull() ?: voice.timestampMs
            } else {
                explicitEnd!!
            }
            OverlappingVoice(row = ri, voice = vi, startMs = voice.timestampMs, endMs = endMs)
        }
    }

fun timedSecondaryVoices(rows: List<LyricRow>): List<OverlappingVoice> {
    val layouts = rows.map(::voiceLayout)
    return timedVoices(rows).filter { layouts[it.row][it.voice].secondary }
}

fun overlappingVoices(
    voices: List<OverlappingVoice>,
    active: Int,
    timeMs: Long,
): List<OverlappingVoice> =
    voices.filter { it.row < active && timeMs in it.startMs until it.endMs }

private fun isMarkerText(text: String): Boolean {
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val charCount = Character.charCount(cp)
        val isMarkerChar = Character.isWhitespace(cp) ||
            cp == 0x266A || // ♪
            cp == 0x266B || // ♫
            cp == 0x266C || // ♬
            cp == 0x1F3B5 || // 🎵
            cp == 0x1F3B6    // 🎶
        if (!isMarkerChar) return false
        i += charCount
    }
    return true
}

private fun isMarkerRow(row: LyricRow): Boolean =
    row.voices.all { v ->
        isMarkerText(v.text) && v.words.orEmpty().all { w -> isMarkerText(w.text) }
    }

private fun sungEnd(row: LyricRow): Long? {
    var end: Long? = null
    for (v in row.voices) {
        val words = v.words.orEmpty()
        for (w in words) {
            val wMax = maxOf(w.startMs, w.endMs)
            end = if (end == null) wMax else maxOf(end, wMax)
        }
    }
    return end
}

private fun gapRow(startMs: Long): LyricRow = LyricRow(
    startMs = startMs,
    endMs = startMs,
    voices = listOf(LyricLine(timestampMs = startMs, text = "")),
    isInterlude = true,
    synced = true,
)

/**
 * Converts instrumental marker lines, long word-timed vocal silences, and long song intros
 * into interlude rows, dropping short blips (< [INTERLUDE_MIN_MS]) and trailing outros.
 */
fun withInterludes(rows: List<LyricRow>): List<LyricRow> {
    if (rows.isEmpty() || !rows[0].synced || rows[0].startMs < 0L) return rows

    val out = mutableListOf<LyricRow>()
    for (row in rows) {
        val last = out.lastOrNull()
        if (isMarkerRow(row)) {
            if (last?.isInterlude != true) {
                out += gapRow(row.startMs)
            }
            continue
        }
        if (last != null && !last.isInterlude) {
            val end = sungEnd(last)
            if (end != null && row.startMs - end >= SILENCE_MIN_MS) {
                out += gapRow(end)
            }
        }
        out += row
    }

    if (out.isNotEmpty()) {
        if (out[0].isInterlude) {
            out[0] = gapRow(0L)
        } else if (out[0].startMs >= INTRO_MIN_MS) {
            out.add(0, gapRow(0L))
        }
    }

    val result = mutableListOf<LyricRow>()
    for (i in out.indices) {
        val row = out[i]
        if (!row.isInterlude) {
            result += row
            continue
        }
        val next = out.getOrNull(i + 1) ?: continue // skip outro
        if (next.startMs - row.startMs >= INTERLUDE_MIN_MS) {
            result += row.copy(endMs = next.startMs)
        }
    }
    return result
}

/**
 * Visual distance from the active row, skipping folded interludes so inactive gaps do not
 * push surrounding lyric lines an extra step out of focus.
 */
fun rowDistances(rows: List<LyricRow>, active: Int): List<Int> {
    if (rows.isEmpty()) return emptyList()
    val positions = IntArray(rows.size)
    var p = 0
    for (i in rows.indices) {
        positions[i] = p
        if (!rows[i].isInterlude || i == active) {
            p++
        }
    }
    val at = if (active < 0 || active >= rows.size) -1 else positions[active]
    return positions.map { abs(it - at) }
}

/** Binary search for the latest row whose [LyricRow.startMs] <= [clockMs]. */
fun activeRowIndex(rows: List<LyricRow>, clockMs: Long, synced: Boolean): Int {
    if (!synced || rows.isEmpty()) return -1
    var low = 0
    var high = rows.lastIndex
    var idx = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (rows[mid].startMs <= clockMs) {
            idx = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return idx
}

fun activeLyricsAt(
    rows: List<LyricRow>,
    voices: List<OverlappingVoice>,
    clockMs: Long,
    synced: Boolean,
): ActiveLyricsState {
    if (!synced || rows.isEmpty()) {
        return ActiveLyricsState(activeRow = -1, overlapping = emptyList(), singing = emptyList())
    }
    val active = activeRowIndex(rows, clockMs, synced = true)
    return ActiveLyricsState(
        activeRow = active,
        overlapping = overlappingVoices(voices, active, clockMs),
        singing = voices.filter { clockMs in it.startMs until it.endMs },
    )
}
