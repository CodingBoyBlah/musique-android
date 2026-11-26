package app.musique.core.lastfm

import android.content.Context
import android.util.Log
import app.musique.core.model.PlayableAudio
import app.musique.playback.PlaybackStateHolder
import app.musique.playback.model.PlaybackOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LastFm"
private const val API = "https://ws.audioscrobbler.com/2.0/"

data class LastFmStatus(
    val configured: Boolean = false,
    val connected: Boolean = false,
    val username: String? = null,
    val awaitingAuth: Boolean = false,
)

/**
 * Last.fm, the same way the desktop app does it: the user brings an API key and secret, authorizes
 * a request token in the browser, and we keep the permanent session key. Every authed call is md5
 * signed with the shared secret. Everything lives in app private storage.
 */
@Singleton
class LastFmClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("lastfm", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<LastFmStatus> = _status.asStateFlow()

    val apiKey: String get() = prefs.getString("api_key", "").orEmpty()
    val secret: String get() = prefs.getString("secret", "").orEmpty()

    private fun readStatus() = LastFmStatus(
        configured = !prefs.getString("api_key", null).isNullOrBlank() && !prefs.getString("secret", null).isNullOrBlank(),
        connected = !prefs.getString("session", null).isNullOrBlank(),
        username = prefs.getString("username", null),
        awaitingAuth = !prefs.getString("token", null).isNullOrBlank(),
    )

    private fun refresh() {
        _status.value = readStatus()
    }

    fun saveApi(apiKey: String, secret: String) {
        prefs.edit().putString("api_key", apiKey.trim()).putString("secret", secret.trim()).apply()
        refresh()
    }

    /** Gets a request token and returns the page where the user approves it. */
    suspend fun startAuth(): Result<String> = runCatching {
        val json = call("auth.getToken", emptyMap(), write = false, signed = true)
        val token = json.getString("token")
        prefs.edit().putString("token", token).apply()
        refresh()
        "https://www.last.fm/api/auth/?api_key=$apiKey&token=$token"
    }

    /** Trades the approved token for a session key. Fails until the user has approved it. */
    suspend fun finishAuth(): Result<String> = runCatching {
        val token = prefs.getString("token", null) ?: error("Start connecting first")
        val json = call("auth.getSession", mapOf("token" to token), write = false, signed = true)
        val session = json.getJSONObject("session")
        val name = session.getString("name")
        prefs.edit()
            .putString("session", session.getString("key"))
            .putString("username", name)
            .remove("token")
            .apply()
        refresh()
        name
    }

    fun disconnect() {
        prefs.edit().remove("session").remove("username").remove("token").apply()
        refresh()
    }

    suspend fun nowPlaying(artist: String, track: String, album: String, durationSec: Long) {
        val session = prefs.getString("session", null) ?: return
        runCatching {
            call(
                "track.updateNowPlaying",
                buildMap {
                    put("artist", artist); put("track", track); put("sk", session)
                    if (album.isNotEmpty()) put("album", album)
                    if (durationSec > 0) put("duration", durationSec.toString())
                },
                write = true,
                signed = true,
            )
        }.onFailure { Log.w(TAG, "now playing failed: ${it.message}") }
    }

    suspend fun scrobble(artist: String, track: String, album: String, timestamp: Long, durationSec: Long) {
        val session = prefs.getString("session", null) ?: return
        runCatching {
            call(
                "track.scrobble",
                buildMap {
                    put("artist", artist); put("track", track); put("timestamp", timestamp.toString()); put("sk", session)
                    if (album.isNotEmpty()) put("album", album)
                    if (durationSec > 0) put("duration", durationSec.toString())
                },
                write = true,
                signed = true,
            )
        }.onFailure { Log.w(TAG, "scrobble failed: ${it.message}") }
    }

    private suspend fun call(method: String, extra: Map<String, String>, write: Boolean, signed: Boolean): JSONObject =
        withContext(Dispatchers.IO) {
            val key = apiKey.ifBlank { error("Add your Last.fm API key first") }
            val params = sortedMapOf("method" to method, "api_key" to key)
            params.putAll(extra)
            if (signed) params["api_sig"] = sign(params, secret)
            params["format"] = "json"

            val request = if (write) {
                val body = FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
                Request.Builder().url(API).post(body)
            } else {
                val url = API.toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
                Request.Builder().url(url).get()
            }.header("User-Agent", "Musique-Android").build()

            http.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
                if (json.has("error")) error(json.optString("message", "Last.fm error ${json.optInt("error")}"))
                if (!response.isSuccessful) error("HTTP ${response.code}")
                json
            }
        }

    private fun sign(params: Map<String, String>, secret: String): String {
        val raw = buildString {
            params.toSortedMap().forEach { (k, v) -> if (k != "format" && k != "callback") append(k).append(v) }
            append(secret)
        }
        return MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

/**
 * Sends now playing and scrobbles for music played on this phone. Like the desktop app: tracks of
 * 30 s or more, scrobbled once half of it (or 4 minutes) has actually been heard. Playback on another
 * Connect device is left to that device so nothing is scrobbled twice.
 */
@Singleton
class LastFmScrobbler @Inject constructor(
    private val client: LastFmClient,
    private val stateHolder: PlaybackStateHolder,
) {
    // one thread, so the track bookkeeping below never races itself
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var started = false

    private var current: PlayableAudio? = null
    private var startedAtSec = 0L
    private var heardMs = 0L
    private var scrobbled = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            stateHolder.state
                .map { it.currentAudio?.takeIf { audio -> audio.isTrack() && it.owner == PlaybackOwner.LOCAL } }
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collect { audio -> onTrack(audio) }
        }
        scope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(1_000)
                val now = System.currentTimeMillis()
                val s = stateHolder.state.value
                if (s.isPlaying && !s.isBuffering && s.owner == PlaybackOwner.LOCAL) heardMs += now - last
                last = now
                maybeScrobble()
            }
        }
    }

    private suspend fun onTrack(audio: PlayableAudio?) {
        current = audio
        heardMs = 0L
        scrobbled = false
        startedAtSec = System.currentTimeMillis() / 1000
        if (audio == null || !client.status.value.connected) return
        client.nowPlaying(audio.artistName(), audio.name, audio.albumName(), audio.duration / 1000)
    }

    private suspend fun maybeScrobble() {
        val audio = current ?: return
        if (scrobbled || audio.duration < 30_000 || !client.status.value.connected) return
        if (heardMs < minOf(audio.duration / 2, 240_000L)) return
        scrobbled = true
        client.scrobble(audio.artistName(), audio.name, audio.albumName(), startedAtSec, audio.duration / 1000)
    }

    private fun PlayableAudio.artistName() = artists?.firstOrNull()?.name.orEmpty()
    private fun PlayableAudio.albumName() = sourceTrack?.album?.name.orEmpty()
}
