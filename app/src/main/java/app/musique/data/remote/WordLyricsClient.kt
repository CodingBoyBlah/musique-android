package app.musique.data.remote

import android.util.Log
import app.musique.core.model.LyricLine
import app.musique.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private const val TAG = "WordLyrics"

/** Community database of hand aligned, word level TTML keyed by Spotify track id. */
private const val AMLL_BASE = "https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/spotify-lyrics"

/** Apple Music TTML relay, matched on title, artist, album and duration (whole seconds). */
private const val BETTERLYRICS_API = "https://lyrics-api.boidu.dev/getLyrics"

private const val UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

/** A match is only the same recording when its duration is this close. */
private const val DURATION_TOLERANCE_MS = 1500L

/**
 * The word by word lyric sources the desktop app uses that work without an account: the AMLL TTML
 * database and BetterLyrics (Apple Music documents). Both are best effort; any failure is "no lyrics".
 */
@Singleton
class WordLyricsClient @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun amll(track: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        val id = track.id
        if (id.isEmpty() || !id.all { it.isLetterOrDigit() && it.code < 128 }) return@withContext emptyList()
        fetch("$AMLL_BASE/$id.ttml")?.let(TtmlParser::parse).orEmpty()
    }

    suspend fun betterLyrics(track: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        val title = cleanTitle(track.name).ifEmpty { track.name }.trim()
        val artist = track.artists.firstOrNull()?.name?.trim().orEmpty()
        if (title.isEmpty() || artist.isEmpty()) return@withContext emptyList()
        val album = track.album?.name?.trim().orEmpty()
        val response = (if (album.isNotEmpty()) query(title, artist, album, track.duration) else null)
            ?: query(title, artist, null, track.duration)
            ?: return@withContext emptyList()
        if (!matches(response, track)) return@withContext emptyList()
        TtmlParser.parse(response.optString("ttml"))
    }

    private fun query(title: String, artist: String, album: String?, durationMs: Long): JSONObject? {
        val url = BETTERLYRICS_API.toHttpUrl().newBuilder()
            .addQueryParameter("s", title)
            .addQueryParameter("a", artist)
            .addQueryParameter("d", ((durationMs + 500) / 1000).toString())
            .apply { if (album != null) addQueryParameter("al", album) }
            .build()
            .toString()
        val body = fetch(url) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()?.takeIf { it.optString("ttml").isNotBlank() }
    }

    private fun matches(response: JSONObject, track: Track): Boolean {
        if (response.has("duration")) {
            val d = response.optDouble("duration", 0.0)
            val ms = if (d > 10_000) d.toLong() else (d * 1000).toLong()
            if (ms > 0 && abs(ms - track.duration) > DURATION_TOLERANCE_MS) return false
        }
        val title = response.optString("title").ifEmpty { response.optString("trackName") }
        if (title.isNotEmpty() && !textMatches(title, track.name) && !textMatches(title, cleanTitle(track.name))) {
            return false
        }
        return true
    }

    private fun fetch(url: String): String? = try {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "lyrics fetch failed: ${e.message}")
        null
    }

    companion object {
        /** Drops "- Remastered 2011", "(feat. X)" and similar so title searches hit. */
        fun cleanTitle(name: String): String =
            name.replace(Regex("""\s+-\s+.*$"""), "")
                .replace(Regex("""\s*[(\[](feat\.?|ft\.?|with|remaster|live|radio edit)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
                .trim()

        fun textMatches(a: String, b: String): Boolean {
            fun norm(s: String) = s.filter { it.isLetterOrDigit() }.lowercase()
            val x = norm(a)
            val y = norm(b)
            return x.isNotEmpty() && y.isNotEmpty() && (x.contains(y) || y.contains(x))
        }
    }
}
