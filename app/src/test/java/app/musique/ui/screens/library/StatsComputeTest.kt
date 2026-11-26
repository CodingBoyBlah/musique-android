package app.musique.ui.screens.library

import app.musique.core.history.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsComputeTest {
    private val day = 86_400_000L
    private val now = System.currentTimeMillis()

    private fun play(id: String, artist: String, daysAgo: Int, heard: Long = 180_000) =
        PlayRecord(t = now - daysAgo * day, id = id, name = id, artist = artist, artistUri = "a:$artist", heardMs = heard, durationMs = 200_000)

    @Test
    fun `ranks by plays and filters by range`() {
        val records = listOf(play("x", "A", 0), play("x", "A", 1), play("y", "B", 2), play("z", "B", 40))
        val month = StatsViewModel.compute(records, StatsRange.Month, now)
        assertEquals(3, month.plays)
        assertEquals("x", month.topTracks.first().key)
        assertEquals(2, month.distinctArtists)
        assertEquals(4, StatsViewModel.compute(records, StatsRange.All, now).plays)
        assertEquals(540_000L, month.totalMs)
    }

    @Test
    fun `counts the current streak through yesterday`() {
        val records = listOf(play("x", "A", 1), play("x", "A", 2), play("x", "A", 3), play("x", "A", 7))
        val stats = StatsViewModel.compute(records, StatsRange.All, now)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }
}
