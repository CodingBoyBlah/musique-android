package app.musique.core.history

import android.content.Context
import android.util.Log
import app.musique.core.lastfm.LastFmClient
import app.musique.core.model.CoverSize
import app.musique.core.model.PlayableAudio
import app.musique.core.model.getCover
import app.musique.playback.PlaybackStateHolder
import app.musique.playback.model.PlaybackOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayHistory"

/** A song that was actually listened to on this phone. */
@Serializable
data class PlayRecord(
    /** When it started, epoch ms. */
    val t: Long,
    val id: String,
    val name: String,
    val artist: String,
    val artistUri: String? = null,
    val album: String? = null,
    val albumUri: String? = null,
    val cover: String? = null,
    val heardMs: Long,
    val durationMs: Long,
)

/**
 * Local listening history, the source for Stats, kept the way the desktop app keeps it: on the
 * device, never sent anywhere. One JSON object per line, appended, so a crash can lose at most the
 * song that was playing and there is no database schema to migrate.
 */
@Singleton
class PlayHistory @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val file = File(context.filesDir, "play_history.jsonl")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    suspend fun record(play: PlayRecord) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            runCatching { file.appendText(json.encodeToString(PlayRecord.serializer(), play) + "\n") }
                .onFailure { Log.w(TAG, "could not save a play", it) }
        }
    }

    suspend fun all(): List<PlayRecord> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (!file.exists()) return@synchronized emptyList()
            file.useLines { lines ->
                lines.mapNotNull { line ->
                    if (line.isBlank()) null
                    else runCatching { json.decodeFromString(PlayRecord.serializer(), line) }.getOrNull()
                }.toList()
            }
        }
    }
}

/**
 * Follows what plays on this phone: records each song into [PlayHistory] when it ends and drives
 * Last.fm (now playing, then a scrobble once half the song or 4 minutes has been heard, the
 * desktop rule). Only time actually heard counts. Songs playing on another Connect device are left
 * to that device so nothing is counted twice.
 */
@Singleton
class ListeningTracker @Inject constructor(
    private val stateHolder: PlaybackStateHolder,
    private val history: PlayHistory,
    private val lastFm: LastFmClient,
) {
    // one thread, so the bookkeeping below never races itself
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var started = false

    private var current: PlayableAudio? = null
    private var startedAtMs = 0L
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
                if (s.isPlaying && !s.isBuffering && s.owner == PlaybackOwner.LOCAL && current != null) {
                    heardMs += now - last
                }
                last = now
                maybeScrobble()
            }
        }
    }

    private suspend fun onTrack(audio: PlayableAudio?) {
        current?.let { finished -> if (heardMs >= 30_000 || (finished.duration in 1..60_000 && heardMs * 2 >= finished.duration)) save(finished) }
        current = audio
        heardMs = 0L
        scrobbled = false
        startedAtMs = System.currentTimeMillis()
        if (audio != null && lastFm.status.value.connected) {
            lastFm.nowPlaying(audio.artistName(), audio.name, audio.albumName(), audio.duration / 1000)
        }
    }

    private suspend fun save(audio: PlayableAudio) {
        val artist = audio.artists?.firstOrNull()
        val album = audio.sourceTrack?.album
        history.record(
            PlayRecord(
                t = startedAtMs,
                id = audio.id,
                name = audio.name,
                artist = artist?.name.orEmpty(),
                artistUri = artist?.uri,
                album = album?.name,
                albumUri = album?.uri,
                cover = audio.getCover(CoverSize.SMALL)?.uri,
                heardMs = heardMs,
                durationMs = audio.duration,
            )
        )
    }

    private suspend fun maybeScrobble() {
        val audio = current ?: return
        if (scrobbled || audio.duration < 30_000 || !lastFm.status.value.connected) return
        if (heardMs < minOf(audio.duration / 2, 240_000L)) return
        scrobbled = true
        lastFm.scrobble(audio.artistName(), audio.name, audio.albumName(), startedAtMs / 1000, audio.duration / 1000)
    }

    private fun PlayableAudio.artistName() = artists?.firstOrNull()?.name.orEmpty()
    private fun PlayableAudio.albumName() = sourceTrack?.album?.name.orEmpty()
}
