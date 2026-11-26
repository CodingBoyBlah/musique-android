package app.musique.ui.lyrics

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.min

private const val SLEW_LIMIT_MS = 250.0
private const val CORRECT_OVER_MS = 1_000.0
private const val MAX_CORRECTION = 0.1
private const val STRAY_WINDOW_NS = 1_000_000_000L

@Immutable
data class PlaybackClockSnapshot(
    val trackId: String? = null,
    val basePositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
)

/**
 * Smooth frame-level lyric clock interpolating between 250ms player position reports,
 * matching desktop `playClock.ts` slew and stray-seek protection.
 */
class LyricClockTracker {
    private var trackId: String? = null
    private var durationMs: Long = 0L
    private var anchorPosMs: Double = 0.0
    private var anchorNanos: Long = 0L
    private var playing: Boolean = false
    private var speed: Double = 1.0
    private var correction: Double = 1.0
    private var lastJumpNanos: Long = Long.MIN_VALUE / 4
    private var strays: Int = 0
    private var initialized: Boolean = false

    fun currentMs(nowNanos: Long): Long {
        if (!initialized) return 0L
        val raw = if (!playing) {
            anchorPosMs
        } else {
            val elapsedMs = ((nowNanos - anchorNanos).coerceAtLeast(0L)) / 1_000_000.0
            anchorPosMs +
                elapsedMs * speed +
                min(elapsedMs, CORRECT_OVER_MS) * speed * (correction - 1.0)
        }
        val clamped = if (durationMs > 0L) raw.coerceIn(0.0, durationMs.toDouble()) else raw.coerceAtLeast(0.0)
        return clamped.toLong()
    }

    /** Snaps the clock immediately when the user taps a lyric line to seek. */
    fun snap(positionMs: Long, nowNanos: Long) {
        val clamped = if (durationMs > 0L) positionMs.coerceIn(0L, durationMs) else positionMs.coerceAtLeast(0L)
        anchorPosMs = clamped.toDouble()
        anchorNanos = nowNanos
        correction = 1.0
        lastJumpNanos = nowNanos
        strays = 0
        initialized = true
    }

    fun onSnapshot(snapshot: PlaybackClockSnapshot, nowNanos: Long) {
        val effectivePlaying = snapshot.isPlaying && !snapshot.isBuffering
        val effectiveSpeed = if (snapshot.playbackSpeed > 0f) snapshot.playbackSpeed.toDouble() else 1.0
        val trackChanged = snapshot.trackId != trackId
        durationMs = snapshot.durationMs

        if (!initialized || trackChanged) {
            trackId = snapshot.trackId
            playing = effectivePlaying
            speed = effectiveSpeed
            anchorPosMs = snapshot.basePositionMs.coerceAtLeast(0L).toDouble()
            anchorNanos = nowNanos
            correction = 1.0
            lastJumpNanos = nowNanos
            strays = 0
            initialized = true
            return
        }

        if (effectivePlaying && playing) {
            val predicted = currentMs(nowNanos).toDouble()
            val drift = snapshot.basePositionMs.toDouble() - predicted
            if (abs(drift) < SLEW_LIMIT_MS) {
                strays = 0
                anchorPosMs = predicted
                anchorNanos = nowNanos
                speed = effectiveSpeed
                val wanted = 1.0 + drift / (CORRECT_OVER_MS * speed)
                correction = wanted.coerceIn(1.0 - MAX_CORRECTION, 1.0 + MAX_CORRECTION)
                return
            }
            if ((nowNanos - lastJumpNanos) < STRAY_WINDOW_NS && strays == 0) {
                // First far-off report immediately after an optimistic seek is usually stale pre-seek state.
                strays = 1
                return
            }
        }

        playing = effectivePlaying
        speed = effectiveSpeed
        snap(snapshot.basePositionMs, nowNanos)
    }
}

fun interludeProgress(clockMs: Long, startMs: Long, endMs: Long): Float {
    val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
    return ((clockMs - startMs).toFloat() / span).coerceIn(0f, 1f)
}

fun interludeDotFill(progress: Float, dotIndex: Int): Float {
    val f = (progress * 3f - dotIndex).coerceIn(0f, 1f)
    return f * f * (3f - 2f * f)
}

fun interludeAnticipationScale(progress: Float): Float {
    val t = ((progress - 0.9f) * 10f).coerceIn(0f, 1f)
    return 1f + 0.15f * t
}
