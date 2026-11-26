package app.musique.ui.lyrics

import app.musique.core.model.LyricLine
import app.musique.core.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSweepTest {

    @Test
    fun `weldPunctuation attaches comma and opening parenthesis cleanly`() {
        val comma = weldPunctuation(
            listOf(
                RenderWord("I lied to you", 0L, 900L),
                RenderWord(",", 900L, 950L),
                RenderWord(" I lied", 950L, 1600L),
            )
        )
        assertTrue(comma.any { it.text == "you, " })

        val bracket = weldPunctuation(
            listOf(
                RenderWord("again   (", 0L, 520L),
                RenderWord("Baby,", 520L, 900L),
            )
        )
        assertTrue(bracket.any { it.text == "(Baby," })
        assertEquals("again (Baby,", bracket.joinToString("") { it.text })
    }

    @Test
    fun `weldPunctuation spreads phrase span across split words and enforces monotonic times`() {
        val phrase = weldPunctuation(listOf(RenderWord("Put myself to sleep", 0L, 2000L)))
        assertEquals(4, phrase.size)
        assertEquals(0L, phrase.first().startMs)
        assertEquals(2000L, phrase.last().endMs)
        phrase.forEachIndexed { idx, tok ->
            assertTrue(tok.endMs > tok.startMs)
            if (idx > 0) assertTrue(tok.startMs >= phrase[idx - 1].endMs)
        }

        val unordered = weldPunctuation(
            listOf(
                RenderWord("one ", 500L, 900L),
                RenderWord("two ", 200L, 400L),
                RenderWord("three", 800L, 600L),
            )
        )
        unordered.forEachIndexed { idx, tok ->
            assertTrue(tok.endMs >= tok.startMs)
            if (idx > 0) assertTrue(tok.startMs >= unordered[idx - 1].endMs)
        }
    }

    @Test
    fun `lyricWords never fabricates word timings for line-synced lines`() {
        val lineOnly = LyricLine(timestampMs = 5000L, text = "Hello world", endMs = 9000L, words = null)
        val words = lyricWords(lineOnly, rowStartMs = 5000L)
        assertEquals(1, words.size)
        assertEquals(5000L, words.single().startMs)
        assertEquals(5000L, words.single().endMs)

        val geom = buildSweepGeometry(words)
        assertFalse(geom.hasWordTiming)
        assertEquals(0f, geom.cursorAt(4999L).charPosition, 0.001f)
        assertEquals(geom.totalChars.toFloat(), geom.cursorAt(5000L).charPosition, 0.001f)
    }

    @Test
    fun `cursorAt interpolates smoothly across word timings and holds in gaps`() {
        val words = lyricWords(
            LyricLine(
                timestampMs = 1000L,
                text = "Hello world",
                words = listOf(
                    LyricWord(1000L, 2000L, "Hello "),
                    LyricWord(2500L, 3500L, "world"),
                ),
            ),
            rowStartMs = 1000L,
        )
        val geom = buildSweepGeometry(words)
        assertTrue(geom.hasWordTiming)

        val midFirst = geom.cursorAt(1500L)
        assertEquals(2.5f, midFirst.charPosition, 0.05f)
        assertEquals(0, midFirst.activeTokenIndex)
        assertEquals(0.5f, midFirst.activeWordFraction, 0.01f)

        // Gap between 2000ms and 2500ms holds at token 1 start
        val inGap = geom.cursorAt(2200L)
        assertEquals(5.0f, inGap.charPosition, 0.05f)
        assertEquals(-1, inGap.activeTokenIndex)

        val midSecond = geom.cursorAt(3000L)
        assertEquals(7.5f, midSecond.charPosition, 0.05f)
        assertEquals(1, midSecond.activeTokenIndex)
    }

    @Test
    fun `lyricTone keeps distance 1 sharp and clamps blur and high contrast alpha`() {
        assertEquals(Tone(0f, 1f), lyricTone(0, past = false))

        val next = lyricTone(1, past = false)
        assertEquals(0f, next.blurDp, 0.001f)
        assertEquals(0.54f, next.alpha, 0.001f)

        val prev = lyricTone(1, past = true)
        assertEquals(0f, prev.blurDp, 0.001f)
        assertEquals(0.44f, prev.alpha, 0.001f)

        val far = lyricTone(10, past = true)
        assertEquals(6.5f, far.blurDp, 0.001f)
        assertEquals(0.10f, far.alpha, 0.001f)

        val highContrastFar = lyricTone(10, past = true, highContrast = true)
        assertEquals(0f, highContrastFar.blurDp, 0.001f)
        assertEquals(0.50f, highContrastFar.alpha, 0.001f)
    }

    @Test
    fun `lyricSeekTarget adjusts by offset and clamps at zero`() {
        assertEquals(30_237L, lyricSeekTarget(30_237L, 0L))
        assertEquals(30_037L, lyricSeekTarget(30_237L, 200L))
        assertEquals(0L, lyricSeekTarget(100L, 400L))
    }
}
