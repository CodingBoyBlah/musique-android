package app.musique.data.repository

import app.musique.core.SpClient
import app.musique.core.model.LyricLine
import app.musique.core.model.LyricWord
import app.musique.core.model.LyricsResponse
import app.musique.core.model.RawLyricSyllable
import app.musique.core.model.Track
import app.musique.data.remote.LrcLibClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val DEFAULT_SYLLABLE_DURATION_MS = 400L

class PlayerRepository @Inject constructor(
    private val spClient: SpClient,
    private val json: Json,
    private val settingsRepository: SettingsRepository,
    private val lrcLibClient: LrcLibClient,
) {
    suspend fun getLyrics(track: Track?, timeoutMs: Long = 2000L): List<LyricLine> {
        val spotifyLyrics = getSpotifyLyrics(track, timeoutMs)
        if (track == null) return spotifyLyrics
        if (spotifyLyrics.isNotEmpty() && spotifyLyrics.any { it.synced }) return spotifyLyrics
        if (!settingsRepository.lyricsFallbackEnabled.first()) return spotifyLyrics

        val fallback = lrcLibClient.getLyrics(track)
        return if (fallback.isNotEmpty()) fallback else spotifyLyrics
    }

    private suspend fun getSpotifyLyrics(track: Track?, timeoutMs: Long): List<LyricLine> =
        withContext(Dispatchers.IO) {
            val id = track?.id ?: return@withContext emptyList()

            val raw: String = try {
                withTimeout(timeoutMs) {
                    spClient.getLyrics(id) ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }

            return@withContext parseSpotifyLyricsResponse(raw, json)
        }
}

internal fun parseSpotifyLyricsResponse(raw: String, json: Json): List<LyricLine> {
    if (raw.isBlank()) return emptyList()
    return try {
        val response: LyricsResponse = json.decodeFromString(raw)
        val unsynced = response.lyrics.syncType.equals("UNSYNCED", ignoreCase = true)
        if (unsynced) {
            response.lyrics.lines
                .filter { it.words.isNotBlank() }
                .map {
                    LyricLine(
                        timestampMs = -1L,
                        text = it.words,
                        synced = false,
                    )
                }
        } else {
            val parsed = response.lyrics.lines.mapNotNull { line ->
                val startMs = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                val endMs = line.endTimeMs?.toLongOrNull()?.takeIf { it > startMs }
                val words = parseSpotifySyllables(line.words, line.syllables, endMs)
                LyricLine(
                    timestampMs = startMs,
                    text = line.words,
                    endMs = endMs,
                    words = words,
                    synced = true,
                )
            }
            if (parsed.any { it.text.isNotBlank() }) parsed else emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun parseSpotifySyllables(
    text: String,
    syllables: List<RawLyricSyllable>?,
    lineEndMs: Long?,
): List<LyricWord>? {
    if (syllables.isNullOrEmpty() || text.isEmpty()) return null
    val out = ArrayList<LyricWord>(syllables.size)
    var cursor = 0
    for (i in syllables.indices) {
        if (cursor >= text.length) break
        val syllable = syllables[i]
        val start = syllable.startTimeMs.toLongOrNull() ?: return null
        val count = syllable.numChars.coerceAtLeast(0)
        val endCursor = (cursor + count).coerceAtMost(text.length)
        val slice = text.substring(cursor, endCursor)
        cursor = endCursor
        val nextStart = syllables.getOrNull(i + 1)?.startTimeMs?.toLongOrNull()
        val end = when {
            nextStart != null && nextStart >= start -> nextStart
            lineEndMs != null && lineEndMs >= start -> lineEndMs
            else -> start + DEFAULT_SYLLABLE_DURATION_MS
        }
        if (slice.isNotEmpty()) {
            out += LyricWord(startMs = start, endMs = end, text = slice)
        }
    }
    if (cursor < text.length && out.isNotEmpty()) {
        val last = out.last()
        out[out.lastIndex] = last.copy(text = last.text + text.substring(cursor))
    }
    return out.takeIf { it.isNotEmpty() }
}
