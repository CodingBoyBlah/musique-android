package app.musique.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollSpringTest {

    @Test
    fun `critically damped spring glides monotonically without overshoot and stops cleanly`() {
        val spring = LyricFollowSpring()
        var remaining = 160f
        val dt = 1f / 60f
        var steps = 0

        // First step kicks off motion toward 0 without overshooting.
        while (steps < 180) {
            val delta = spring.stepDelta(remaining, dt)
            remaining -= delta
            assertTrue("delta should move toward target", delta >= 0f)
            assertTrue("critically damped spring must not overshoot", remaining >= -0.01f)
            steps++
            if (!spring.running) break
        }

        assertFalse(spring.running)
        assertEquals(0f, remaining, 0.5f)
    }

    @Test
    fun `preserves momentum when target shifts mid-flight`() {
        val spring = LyricFollowSpring()
        var remaining = 160f
        val dt = 1f / 60f

        repeat(10) {
            remaining -= spring.stepDelta(remaining, dt)
        }
        val velBeforeShift = spring.velocityPxPerSec
        assertTrue(velBeforeShift > 50f)

        // New line triggers while already moving: velocity stays intact.
        remaining += 120f
        val delta = spring.stepDelta(remaining, dt)
        assertTrue(delta > 0f)
        assertTrue(spring.velocityPxPerSec > 50f)
    }

    @Test
    fun `stop immediately clears velocity and running state`() {
        val spring = LyricFollowSpring()
        spring.stepDelta(300f, 1f / 60f)
        assertTrue(spring.running)
        assertTrue(spring.velocityPxPerSec > 0f)

        spring.stop()
        assertFalse(spring.running)
        assertEquals(0f, spring.velocityPxPerSec, 0.001f)
    }

    @Test
    fun `lyricScrollAnchorTopPx targets 30 percent center and clamps tall lines to 4 percent top`() {
        // Normal 60px line inside 1000px viewport -> center at 300px -> top at 270px.
        assertEquals(270f, lyricScrollAnchorTopPx(usableTopPx = 0f, usableHeightPx = 1000f, itemHeightPx = 60f), 0.1f)

        // Tall 700px line inside 1000px viewport -> top clamped at 4% (40px).
        assertEquals(40f, lyricScrollAnchorTopPx(usableTopPx = 0f, usableHeightPx = 1000f, itemHeightPx = 700f), 0.1f)

        // Respects top inset.
        assertEquals(370f, lyricScrollAnchorTopPx(usableTopPx = 100f, usableHeightPx = 1000f, itemHeightPx = 60f), 0.1f)
    }

    @Test
    fun `manual reattach band checks 25 to 70 percent of usable viewport`() {
        assertFalse(isItemInManualReattachBand(200f, usableTopPx = 0f, usableHeightPx = 1000f))
        assertTrue(isItemInManualReattachBand(300f, usableTopPx = 0f, usableHeightPx = 1000f))
        assertTrue(isItemInManualReattachBand(690f, usableTopPx = 0f, usableHeightPx = 1000f))
        assertFalse(isItemInManualReattachBand(750f, usableTopPx = 0f, usableHeightPx = 1000f))
    }

    @Test
    fun `cascadeDelayMs scales by row distance and caps at 6 steps`() {
        assertEquals(0L, cascadeDelayMs(5, 5))
        assertEquals(64L, cascadeDelayMs(7, 5))
        assertEquals(192L, cascadeDelayMs(20, 5))
    }

    @Test
    fun `scroll history trails delayed rows and settles to zero`() {
        val history = ScrollHistory()
        val frame = 16_000_000L
        var now = 1_000_000_000L
        repeat(10) {
            history.record(now, 10f)
            now += frame
        }
        val last = now - frame
        assertEquals(100f, history.total, 0.001f)
        assertEquals(0f, history.lagPx(last, 0L), 0.001f)
        // 32ms ago the column was two frames (20px) behind.
        assertEquals(20f, history.lagPx(last, 32L), 0.5f)
        // Before motion started the column sat at zero, so a long delay trails the whole move.
        assertEquals(100f, history.lagPx(last, 1_000L), 0.5f)
        assertFalse(history.settled(last, CASCADE_MAX_DELAY_MS))
        // Once the column has been still longer than the longest delay, nobody trails.
        val later = last + 300_000_000L
        assertTrue(history.settled(later, CASCADE_MAX_DELAY_MS))
        assertEquals(0f, history.lagPx(later, CASCADE_MAX_DELAY_MS), 0.001f)
    }

    @Test
    fun `only rows below the active line trail`() {
        assertEquals(0L, cascadeTrailDelayMs(3, 5))
        assertEquals(0L, cascadeTrailDelayMs(5, 5))
        assertEquals(32L, cascadeTrailDelayMs(6, 5))
        assertEquals(CASCADE_MAX_DELAY_MS, cascadeTrailDelayMs(30, 5))
    }
}
