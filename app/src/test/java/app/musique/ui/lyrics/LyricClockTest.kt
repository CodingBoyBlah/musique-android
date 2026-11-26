package app.musique.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricClockTest {

    private fun msToNs(ms: Long) = ms * 1_000_000L

    @Test
    fun `interpolates smoothly across frames using playback speed`() {
        val tracker = LyricClockTracker()
        tracker.onSnapshot(
            PlaybackClockSnapshot(
                trackId = "t1",
                basePositionMs = 10_000L,
                durationMs = 200_000L,
                isPlaying = true,
                playbackSpeed = 1.5f,
            ),
            nowNanos = msToNs(0L),
        )

        assertEquals(10_000L, tracker.currentMs(msToNs(0L)))
        assertEquals(10_150L, tracker.currentMs(msToNs(100L)))
        assertEquals(10_375L, tracker.currentMs(msToNs(250L)))
    }

    @Test
    fun `freezes when paused or buffering`() {
        val tracker = LyricClockTracker()
        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 12_000L, 200_000L, isPlaying = false, playbackSpeed = 1f),
            nowNanos = msToNs(0L),
        )
        assertEquals(12_000L, tracker.currentMs(msToNs(5_000L)))

        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 12_000L, 200_000L, isPlaying = true, isBuffering = true, playbackSpeed = 1f),
            nowNanos = msToNs(5_000L),
        )
        assertEquals(12_000L, tracker.currentMs(msToNs(8_000L)))
    }

    @Test
    fun `ignores single stale pre-seek report right after user seek snap then snaps on confirmation`() {
        val tracker = LyricClockTracker()
        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 10_000L, 200_000L, isPlaying = true, playbackSpeed = 1f),
            nowNanos = msToNs(0L),
        )

        // User taps a line at 60_000ms
        tracker.snap(60_000L, nowNanos = msToNs(100L))
        assertEquals(60_000L, tracker.currentMs(msToNs(100L)))

        // Stale pre-seek 250ms tick arrives 150ms later saying 10_250ms -> ignored once
        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 10_250L, 200_000L, isPlaying = true, playbackSpeed = 1f),
            nowNanos = msToNs(250L),
        )
        assertEquals(60_150L, tracker.currentMs(msToNs(250L)))

        // Confirmation arrives at 60_250ms -> accepted cleanly
        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 60_250L, 200_000L, isPlaying = true, playbackSpeed = 1f),
            nowNanos = msToNs(350L),
        )
        assertTrue(tracker.currentMs(msToNs(350L)) >= 60_240L)
    }

    @Test
    fun `snaps immediately on track change`() {
        val tracker = LyricClockTracker()
        tracker.onSnapshot(
            PlaybackClockSnapshot("t1", 50_000L, 200_000L, isPlaying = true, playbackSpeed = 1f),
            nowNanos = msToNs(0L),
        )
        tracker.onSnapshot(
            PlaybackClockSnapshot("t2", 0L, 180_000L, isPlaying = true, playbackSpeed = 1f),
            nowNanos = msToNs(16L),
        )
        assertEquals(0L, tracker.currentMs(msToNs(16L)))
    }

    @Test
    fun `interlude dots fill sequentially across thirds and swell at end`() {
        val p0 = interludeProgress(0L, 0L, 3000L)
        assertEquals(0f, interludeDotFill(p0, 0), 0.001f)

        val pHalfFirst = interludeProgress(500L, 0L, 3000L)
        assertEquals(0.5f, interludeDotFill(pHalfFirst, 0), 0.001f)
        assertEquals(0f, interludeDotFill(pHalfFirst, 1), 0.001f)

        val pEnd = interludeProgress(3000L, 0L, 3000L)
        assertEquals(1f, interludeDotFill(pEnd, 2), 0.001f)
        assertEquals(1.15f, interludeAnticipationScale(pEnd), 0.001f)
    }
}
