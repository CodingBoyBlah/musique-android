package app.musique.data.remote

import android.util.Log
import app.musique.BuildConfig
import app.musique.core.model.LyricLine
import app.musique.core.model.LyricWord
import app.musique.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "LrcLibClient"
private const val LRCLIB_HOST = "lrclib.net"
private const val CALL_TIMEOUT_SECONDS = 6L

/** Candidates whose duration differs more than this from the track are discarded. */
internal const val LRCLIB_DURATION_TOLERANCE_SECONDS = 5

/**
 * One row of the LRCLIB API (https://lrclib.net/docs).
 */
@Serializable
data class LrcLibTrack(
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val syncedLyrics: String? = null,
)

/**
 * Fallback lyrics provider used when Spotify has no lyrics for a track.
 *
 * Lookup order: exact match on `/api/get` (title, artist, album, duration), then a
 * `/api/search` by title and artist filtered by duration. Only synced lyrics are returned.
 */
@Singleton
class LrcLibClient @Inject constructor(
    private val json: Json,
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val userAgent = "Musique/${BuildConfig.VERSION_NAME} (https://github.com/CodingBoyBlah/musique-android)"

    suspend fun getLyrics(track: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        val title = track.name.trim()
        val artist = track.artists.firstOrNull()?.name?.trim().orEmpty()
        if (title.isEmpty()) return@withContext emptyList()

        val durationSeconds = (track.duration / 1000.0).roundToInt()

        try {
            val exact = fetchExact(title, artist, track.album?.name, durationSeconds)
            resolveLrcLib(exact, durationSeconds) { search(title, artist) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB lookup failed", e)
            emptyList()
        }
    }

    /** `null` means the API answered 404 (no exact match). Network failures throw. */
    private fun fetchExact(title: String, artist: String, album: String?, durationSeconds: Int): LrcLibTrack? {
        val url = baseUrl("api/get")
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .apply { if (!album.isNullOrBlank()) addQueryParameter("album_name", album) }
            .addQueryParameter("duration", durationSeconds.toString())
            .build()

        client.newCall(request(url)).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IOException("LRCLIB get returned HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("LRCLIB get returned no body")
            return json.decodeFromString<LrcLibTrack>(body)
        }
    }

    private fun search(title: String, artist: String): List<LrcLibTrack> {
        val url = baseUrl("api/search")
            .addQueryParameter("track_name", title)
            .apply { if (artist.isNotEmpty()) addQueryParameter("artist_name", artist) }
            .build()

        client.newCall(request(url)).execute().use { response ->
            if (response.code == 404) return emptyList()
            if (!response.isSuccessful) throw IOException("LRCLIB search returned HTTP ${response.code}")
            val body = response.body?.string() ?: return emptyList()
            return json.decodeFromString<List<LrcLibTrack>>(body)
        }
    }

    private fun baseUrl(path: String): HttpUrl.Builder =
        HttpUrl.Builder()
            .scheme("https")
            .host(LRCLIB_HOST)
            .addPathSegments(path)

    private fun request(url: HttpUrl): Request =
        Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .get()
            .build()
}

/**
 * Picks the search result with usable synced lyrics closest in duration, within
 * [LRCLIB_DURATION_TOLERANCE_SECONDS]. Instrumental and unsynced rows are skipped.
 */
internal fun pickBestMatch(candidates: List<LrcLibTrack>, durationSeconds: Int): LrcLibTrack? =
    candidates
        .asSequence()
        .filter { it.syncedLines().isNotEmpty() }
        .map { it to abs((it.duration ?: 0.0).roundToInt() - durationSeconds) }
        .filter { (_, diff) -> diff <= LRCLIB_DURATION_TOLERANCE_SECONDS }
        .minByOrNull { (_, diff) -> diff }
        ?.first

/** Uses the exact match when it has synced lyrics, otherwise searches once. */
internal suspend fun resolveLrcLib(
    exact: LrcLibTrack?,
    durationSeconds: Int,
    search: suspend () -> List<LrcLibTrack>,
): List<LyricLine> {
    if (exact?.instrumental == true) return emptyList()

    val lines = exact?.syncedLines().orEmpty()
    if (lines.isNotEmpty()) return lines

    return pickBestMatch(search(), durationSeconds)?.syncedLines().orEmpty()
}

internal fun LrcLibTrack.syncedLines(): List<LyricLine> {
    if (instrumental) return emptyList()
    return syncedLyrics?.takeIf { it.isNotBlank() }?.let(LrcParser::parseSynced).orEmpty()
}

/**
 * Parses standard LRC (`[mm:ss.xx]` or `[mm:ss.xxx]`, several per line allowed)
 * and enhanced LRC (`<mm:ss.xx>` inline word/syllable timestamps).
 */
object LrcParser {
    private val timestamp = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTimestamp = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private const val DEFAULT_WORD_DURATION_MS = 400L

    private data class EnhancedBody(
        val text: String,
        val words: List<LyricWord>,
        val endMs: Long?,
    )

    fun parseSynced(text: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        text.lineSequence().forEach { rawLine ->
            val stamps = timestamp.findAll(rawLine).toList()
            if (stamps.isEmpty()) return@forEach

            val body = rawLine.substring(stamps.last().range.last + 1).trim()
            if (body.isEmpty()) return@forEach

            val stampTimes = stamps.map(::parseStampMs)
            val wordMatches = wordTimestamp.findAll(body).toList()

            if (wordMatches.isEmpty()) {
                stampTimes.forEach { ts ->
                    lines += LyricLine(
                        timestampMs = ts,
                        text = body,
                        synced = true,
                    )
                }
                return@forEach
            }

            val baseStamp = stampTimes.first()
            val enhanced = parseEnhancedBody(body, baseStamp, wordMatches) ?: return@forEach

            stampTimes.forEach { ts ->
                val shift = ts - baseStamp
                val shiftedWords = enhanced.words.map { word ->
                    val start = (word.startMs + shift).coerceAtLeast(0L)
                    val end = (word.endMs + shift).coerceAtLeast(start)
                    LyricWord(startMs = start, endMs = end, text = word.text)
                }
                val shiftedEnd = enhanced.endMs?.let { (it + shift).coerceAtLeast(ts) }
                lines += LyricLine(
                    timestampMs = ts,
                    text = enhanced.text,
                    endMs = shiftedEnd,
                    words = shiftedWords,
                    synced = true,
                )
            }
        }

        val sorted = lines.sortedBy { it.timestampMs }
        return sorted.mapIndexed { idx, line ->
            val words = line.words
            if (words.isNullOrEmpty() || line.endMs != null) {
                line
            } else {
                val last = words.last()
                val nextLineStart = sorted.getOrNull(idx + 1)?.timestampMs
                val resolvedEnd = if (nextLineStart != null && nextLineStart > last.startMs) {
                    minOf(nextLineStart, last.startMs + DEFAULT_WORD_DURATION_MS)
                } else {
                    last.startMs + DEFAULT_WORD_DURATION_MS
                }
                if (resolvedEnd == last.endMs) {
                    line
                } else {
                    val updated = words.toMutableList()
                    updated[updated.lastIndex] = last.copy(endMs = resolvedEnd)
                    line.copy(words = updated)
                }
            }
        }
    }

    private fun parseEnhancedBody(
        body: String,
        baseStampMs: Long,
        matches: List<MatchResult>,
    ): EnhancedBody? {
        val rawSegments = mutableListOf<Pair<Long, String>>()
        val prefix = body.substring(0, matches.first().range.first)
        if (prefix.isNotBlank()) {
            rawSegments += baseStampMs to prefix
        }

        var trailingEndMs: Long? = null
        for (i in matches.indices) {
            val startMs = parseStampMs(matches[i])
            val segStart = matches[i].range.last + 1
            val segEnd = if (i + 1 < matches.size) matches[i + 1].range.first else body.length
            val slice = body.substring(segStart, segEnd)
            if (slice.isEmpty() || slice.isBlank()) {
                if (i == matches.lastIndex) {
                    trailingEndMs = startMs
                } else if (rawSegments.isNotEmpty() && slice.isNotEmpty()) {
                    // Preserve a single separating space if a blank slice sat between tags.
                    val (prevStart, prevText) = rawSegments.last()
                    if (!prevText.last().isWhitespace()) {
                        rawSegments[rawSegments.lastIndex] = prevStart to "$prevText "
                    }
                }
            } else {
                rawSegments += startMs to slice
            }
        }

        if (rawSegments.isEmpty()) return null

        val words = ArrayList<LyricWord>(rawSegments.size)
        for (i in rawSegments.indices) {
            val (startMs, rawText) = rawSegments[i]
            var text = rawText
            if (i == 0) {
                text = text.trimStart()
            } else if (words.isNotEmpty() && words.last().text.lastOrNull()?.isWhitespace() == true) {
                text = text.trimStart()
            }
            if (i == rawSegments.lastIndex) {
                text = text.trimEnd()
            }
            if (text.isEmpty()) continue

            val nextStart = rawSegments.getOrNull(i + 1)?.first ?: trailingEndMs
            val endMs = if (nextStart != null) {
                maxOf(startMs, nextStart)
            } else {
                startMs + DEFAULT_WORD_DURATION_MS
            }
            words += LyricWord(startMs = startMs, endMs = endMs, text = text)
        }

        if (words.isEmpty()) return null
        val cleanText = words.joinToString("") { it.text }.trim()
        if (cleanText.isEmpty()) return null

        val lineEndMs = trailingEndMs?.let { maxOf(it, words.last().endMs) }
        return EnhancedBody(text = cleanText, words = words, endMs = lineEndMs)
    }

    private fun parseStampMs(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3]
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }
}
