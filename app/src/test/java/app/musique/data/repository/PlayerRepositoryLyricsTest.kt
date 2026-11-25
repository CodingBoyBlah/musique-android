package app.musique.data.repository

import app.musique.core.model.LyricWord
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRepositoryLyricsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `line synced response preserves timestamps and leaves words null`() {
        val raw = """
            {
              "lyrics": {
                "syncType": "LINE_SYNCED",
                "lines": [
                  {"startTimeMs": "1200", "words": "First line", "endTimeMs": "0"},
                  {"startTimeMs": "4500", "words": "♪", "endTimeMs": "0"},
                  {"startTimeMs": "9000", "words": "Second line", "endTimeMs": "12000"}
                ]
              }
            }
        """.trimIndent()

        val lines = parseSpotifyLyricsResponse(raw, json)
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.synced })
        assertEquals(1200L, lines[0].timestampMs)
        assertNull(lines[0].endMs)
        assertNull(lines[0].words)
        assertEquals("♪", lines[1].text)
        assertEquals(12000L, lines[2].endMs)
    }

    @Test
    fun `unsynced response clears timings and sets synced false`() {
        val raw = """
            {
              "lyrics": {
                "syncType": "UNSYNCED",
                "lines": [
                  {"startTimeMs": "0", "words": "Static verse one", "endTimeMs": "0"},
                  {"startTimeMs": "0", "words": "", "endTimeMs": "0"},
                  {"startTimeMs": "0", "words": "Static verse two", "endTimeMs": "0"}
                ]
              }
            }
        """.trimIndent()

        val lines = parseSpotifyLyricsResponse(raw, json)
        assertEquals(2, lines.size)
        assertTrue(lines.none { it.synced })
        assertTrue(lines.all { it.timestampMs == -1L })
        assertFalse(lines[0].synced)
        assertEquals("Static verse one", lines[0].text)
        assertEquals("Static verse two", lines[1].text)
    }

    @Test
    fun `syllable array slices line text into timed words when present`() {
        val raw = """
            {
              "lyrics": {
                "syncType": "LINE_SYNCED",
                "lines": [
                  {
                    "startTimeMs": "1000",
                    "words": "Hello world",
                    "endTimeMs": "2200",
                    "syllables": [
                      {"startTimeMs": "1000", "numChars": 6},
                      {"startTimeMs": "1500", "numChars": 5}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val lines = parseSpotifyLyricsResponse(raw, json)
        assertEquals(1, lines.size)
        assertEquals(
            listOf(
                LyricWord(1000L, 1500L, "Hello "),
                LyricWord(1500L, 2200L, "world"),
            ),
            lines[0].words
        )
    }

    @Test
    fun `all blank synced lines return empty so fallback can run`() {
        val raw = """
            {
              "lyrics": {
                "syncType": "LINE_SYNCED",
                "lines": [
                  {"startTimeMs": "1000", "words": "   ", "endTimeMs": "0"}
                ]
              }
            }
        """.trimIndent()

        assertTrue(parseSpotifyLyricsResponse(raw, json).isEmpty())
    }
}
