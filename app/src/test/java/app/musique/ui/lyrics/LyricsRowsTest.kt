package app.musique.ui.lyrics

import app.musique.core.model.LyricLine
import app.musique.core.model.LyricRole
import app.musique.core.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRowsTest {

    private fun line(
        timeMs: Long,
        text: String,
        role: LyricRole? = null,
        agent: String? = null,
        endMs: Long? = null,
    ) = LyricLine(
        timestampMs = timeMs,
        text = text,
        endMs = endMs,
        role = role,
        agent = agent,
    )

    private fun wordLine(
        timeMs: Long,
        text: String,
        durMs: Long,
        role: LyricRole? = null,
        agent: String? = null,
    ) = LyricLine(
        timestampMs = timeMs,
        text = text,
        words = listOf(LyricWord(timeMs, timeMs + durMs, text)),
        role = role,
        agent = agent,
    )

    @Test
    fun `keeps sequential lines one per row and derives fallback endMs`() {
        val rows = buildRows(listOf(line(0, "a"), line(2000, "b"), line(4000, "c")))

        assertEquals(listOf("a", "b", "c"), rows.map { it.voices.first().text })
        assertTrue(rows.all { it.voices.size == 1 })
        assertEquals(listOf(0L, 2000L, 4000L), rows.map { it.startMs })
        assertEquals(listOf(2000L, 4000L, 8000L), rows.map { it.endMs })
    }

    @Test
    fun `groups concurrent voices within tolerance and explicit bg outside tolerance`() {
        val rows = buildRows(
            listOf(
                wordLine(1000, "lead", 2000, LyricRole.MAIN),
                wordLine(1400, "(bg)", 3600, LyricRole.BG),
                line(1040, "concurrent"),
                line(6000, "next"),
            )
        )

        assertEquals(2, rows.size)
        assertEquals(listOf("lead", "(bg)", "concurrent"), rows[0].voices.map { it.text })
        // Row end comes strictly from lead voice (1000 + 2000 = 3000), not bg (5000).
        assertEquals(3000L, rows[0].endMs)
        assertEquals("next", rows[1].voices.single().text)
    }

    @Test
    fun `voiceLayout places duets right and backing vocals on their owner side`() {
        val row = LyricRow(
            startMs = 0L,
            endMs = 1000L,
            voices = listOf(
                line(0, "lead"),
                line(0, "(lead bg)", LyricRole.BG),
                line(0, "duet", LyricRole.DUET),
                line(0, "(duet bg)", LyricRole.BG),
            ),
        )

        assertEquals(
            listOf(
                VoiceLayout(VoiceSide.LEFT, secondary = false),
                VoiceLayout(VoiceSide.LEFT, secondary = true),
                VoiceLayout(VoiceSide.RIGHT, secondary = false),
                VoiceLayout(VoiceSide.RIGHT, secondary = true),
            ),
            voiceLayout(row),
        )
    }

    @Test
    fun `concurrent unlabeled lines stay left with second marked secondary`() {
        val rows = buildRows(listOf(line(1000, "Ok Ok Ok"), line(1050, "La La La")))
        assertEquals(
            listOf(
                VoiceLayout(VoiceSide.LEFT, secondary = false),
                VoiceLayout(VoiceSide.LEFT, secondary = true),
            ),
            voiceLayout(rows.single()),
        )
    }

    @Test
    fun `overlapping timed voices stay active across later rows`() {
        val rows = buildRows(
            listOf(
                wordLine(1000, "first", 2000),
                wordLine(2000, "held bg", 5000, LyricRole.BG),
                wordLine(4000, "second", 1000),
                wordLine(6000, "third", 2000),
            )
        )
        val secondary = timedSecondaryVoices(rows)

        assertEquals(1, overlappingVoices(secondary, active = 1, timeMs = 4000L).size)
        assertEquals(1, overlappingVoices(secondary, active = 2, timeMs = 6999L).size)
        assertTrue(overlappingVoices(secondary, active = 2, timeMs = 7000L).isEmpty())
    }

    @Test
    fun `withInterludes collapses markers, adds intro, opens word silence, and drops short gaps and outros`() {
        val marked = withInterludes(
            buildRows(listOf(line(0, "a"), line(4000, "♪"), line(9000, ""), line(16000, "b")))
        )
        assertEquals(listOf("a", "~4000-16000", "b"), marked.map { if (it.isInterlude) "~${it.startMs}-${it.endMs}" else it.voices[0].text })

        val shortAndOutro = withInterludes(
            buildRows(listOf(line(0, "a"), line(4000, ""), line(5000, "b"), line(9000, "♪")))
        )
        assertEquals(listOf("a", "b"), shortAndOutro.map { it.voices[0].text })

        val intro = withInterludes(buildRows(listOf(line(12000, "a"), line(14000, "b"))))
        assertTrue(intro[0].isInterlude)
        assertEquals(0L, intro[0].startMs)
        assertEquals(12000L, intro[0].endMs)

        val wordSilence = withInterludes(buildRows(listOf(wordLine(0, "sing", 3000), wordLine(20000, "again", 1000))))
        assertEquals(3, wordSilence.size)
        assertTrue(wordSilence[1].isInterlude)
        assertEquals(3000L, wordSilence[1].startMs)
        assertEquals(20000L, wordSilence[1].endMs)

        // Line-level lyrics without marker do not invent an interlude
        val lineGap = withInterludes(buildRows(listOf(line(0, "a"), line(20000, "b"))))
        assertEquals(2, lineGap.size)
        assertFalse(lineGap.any { it.isInterlude })
    }

    @Test
    fun `withInterludes leaves unsynced rows intact`() {
        val unsynced = buildRows(
            listOf(
                LyricLine(-1L, "plain one", synced = false),
                LyricLine(-1L, "", synced = false),
                LyricLine(-1L, "plain two", synced = false),
            )
        )
        assertSame(unsynced, withInterludes(unsynced))
        assertTrue(unsynced.none { it.synced })
    }

    @Test
    fun `rowDistances ignores folded interludes unless active`() {
        val rows = withInterludes(buildRows(listOf(line(0, "a"), line(4000, ""), line(12000, "b"), line(14000, "c"))))
        assertEquals(listOf(0, 1, 1, 2), rowDistances(rows, active = 0))
        assertEquals(listOf(1, 0, 1, 2), rowDistances(rows, active = 1))
    }
}
