package app.musique.ui.lyrics

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp

const val LYRIC_SCROLL_RESPONSE_SEC = 0.6f
const val IDLE_REATTACH_MS = 3500L
const val MANUAL_BAND_REATTACH_MS = 400L

/**
 * Closed-form critically damped scroll spring (zeta = 1.0), matching desktop `scrollSpring.ts`.
 * Given the remaining scroll error on each frame, returns the scroll delta for that frame while
 * carrying velocity across line changes and layout shifts without overshoot.
 */
class LyricFollowSpring(
    responseSeconds: Float = LYRIC_SCROLL_RESPONSE_SEC,
) {
    private val omega = (2.0 * PI) / responseSeconds.toDouble()

    var velocityPxPerSec: Float = 0f
        private set

    var running: Boolean = false
        private set

    fun stop() {
        velocityPxPerSec = 0f
        running = false
    }

    fun stepDelta(remainingPx: Float, dtSeconds: Float): Float {
        if (!running && abs(remainingPx) < 1f) {
            return 0f
        }
        val dt = dtSeconds.toDouble().coerceIn(0.0, 0.064)
        if (dt <= 0.0) {
            running = true
            return 0f
        }

        val d0 = -remainingPx.toDouble()
        val v0 = velocityPxPerSec.toDouble()
        val c2 = v0 + omega * d0
        val decay = exp(-omega * dt)
        val dNext = (d0 + c2 * dt) * decay
        val vNext = (c2 - omega * (d0 + c2 * dt)) * decay

        if (abs(dNext) < 0.5 && abs(vNext) < 8.0) {
            stop()
            return remainingPx
        }

        velocityPxPerSec = vNext.toFloat()
        running = true
        return (dNext - d0).toFloat()
    }
}

/**
 * Computes the desired top Y offset (relative to container top) for the active lyric row so its
 * vertical center rests at 30% of the usable viewport height, clamped so very tall wrapped rows
 * keep their top line below 4% of the usable height.
 */
fun lyricScrollAnchorTopPx(
    usableTopPx: Float,
    usableHeightPx: Float,
    itemHeightPx: Float,
): Float {
    val safeHeight = usableHeightPx.coerceAtLeast(1f)
    val centered = safeHeight * 0.30f - itemHeightPx / 2f
    val minTop = safeHeight * 0.04f
    return usableTopPx + maxOf(centered, minTop)
}

/**
 * Returns true if the active row center lies in the middle 25%..70% band of the usable viewport,
 * used to automatically resume auto-follow after manual scrolling.
 */
fun isItemInManualReattachBand(
    itemCenterY: Float,
    usableTopPx: Float,
    usableHeightPx: Float,
): Boolean {
    val safeHeight = usableHeightPx.coerceAtLeast(1f)
    val min = usableTopPx + safeHeight * 0.25f
    val max = usableTopPx + safeHeight * 0.70f
    return itemCenterY in min..max
}

fun cascadeDelayMs(rowIndex: Int, activeIndex: Int): Long {
    if (activeIndex < 0) return 0L
    return abs(rowIndex - activeIndex).coerceAtMost(6) * 32L
}

private const val CASCADE_IDLE_GAP_NANOS = 100_000_000L
private const val ASSUMED_FRAME_NANOS = 16_000_000L

/**
 * Recent auto-follow scroll positions, so rows below the active line can trail the column by a
 * few tens of milliseconds each (Apple Music's staggered cascade) without fighting the list's
 * real scroll state. Positions are cumulative pixels scrolled by the follower.
 */
class ScrollHistory(capacity: Int = 72) {
    private val times = LongArray(capacity)
    private val values = FloatArray(capacity)
    private var start = 0
    private var count = 0

    /** Cumulative pixels the follower has scrolled. */
    var total: Float = 0f
        private set

    val isEmpty: Boolean get() = count == 0

    fun clear() {
        start = 0
        count = 0
    }

    fun record(nowNanos: Long, deltaPx: Float) {
        if (count == 0 || nowNanos - timeAt(count - 1) > CASCADE_IDLE_GAP_NANOS) {
            // Starting from rest: remember where the column sat a frame ago so the trail starts at zero.
            push(nowNanos - ASSUMED_FRAME_NANOS, total)
        }
        total += deltaPx
        push(nowNanos, total)
    }

    /** Where the column was at [nanos], interpolated, clamped to the recorded range. */
    fun valueAt(nanos: Long): Float {
        if (count == 0) return total
        if (nanos <= timeAt(0)) return valueAtIndex(0)
        if (nanos >= timeAt(count - 1)) return valueAtIndex(count - 1)
        var i = count - 1
        while (i > 0 && timeAt(i - 1) > nanos) i--
        val t0 = timeAt(i - 1)
        val t1 = timeAt(i)
        val f = if (t1 == t0) 1f else (nanos - t0).toFloat() / (t1 - t0).toFloat()
        return valueAtIndex(i - 1) + (valueAtIndex(i) - valueAtIndex(i - 1)) * f
    }

    /** How far a row delayed by [delayMs] still trails the column, in pixels (positive = below). */
    fun lagPx(nowNanos: Long, delayMs: Long): Float {
        if (count == 0 || delayMs <= 0L) return 0f
        return valueAt(nowNanos) - valueAt(nowNanos - delayMs * 1_000_000L)
    }

    /** True once every delayed row has caught up, so the cascade can stop asking for frames. */
    fun settled(nowNanos: Long, maxDelayMs: Long): Boolean =
        count == 0 || nowNanos - timeAt(count - 1) > maxDelayMs * 1_000_000L

    private fun push(t: Long, v: Float) {
        val cap = times.size
        if (count == cap) {
            start = (start + 1) % cap
            count--
        }
        val idx = (start + count) % cap
        times[idx] = t
        values[idx] = v
        count++
    }

    private fun timeAt(i: Int): Long = times[(start + i) % times.size]
    private fun valueAtIndex(i: Int): Float = values[(start + i) % values.size]
}

/** Cascade delay for a row: only rows from the active one downward trail, by 32ms per step, capped at 6 steps. */
fun cascadeTrailDelayMs(rowIndex: Int, activeIndex: Int): Long =
    if (activeIndex < 0 || rowIndex <= activeIndex) 0L else cascadeDelayMs(rowIndex, activeIndex)

const val CASCADE_MAX_DELAY_MS = 6 * 32L
