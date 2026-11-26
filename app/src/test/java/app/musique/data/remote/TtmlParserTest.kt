package app.musique.data.remote

import app.musique.core.model.LyricRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtmlParserTest {

    private val doc = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
          <body><div>
            <p begin="00:12.000" end="00:14.500" ttm:agent="v1">
              <span begin="00:12.000" end="00:12.400">I</span> <span begin="00:12.400" end="00:13.100">said</span>
              <span ttm:role="x-bg" begin="00:13.200" end="00:14.400"><span begin="00:13.200" end="00:13.800">(ooh</span> <span begin="00:13.800" end="00:14.400">yeah)</span></span>
              <span ttm:role="x-translation">Dije</span>
            </p>
            <p begin="1:02.5" end="1:04" ttm:agent="v2"><span begin="62.5s" end="63.2s">Tom &amp; Jerry</span></p>
            <p begin="00:05.000" end="00:06.000">Intro line</p>
          </div></body>
        </tt>
    """.trimIndent()

    @Test
    fun `parses words, backing vocals, duets and translations`() {
        val lines = TtmlParser.parse(doc)
        assertEquals(4, lines.size)

        assertEquals("Intro line", lines[0].text)
        assertNull(lines[0].words)

        val lead = lines[1]
        assertEquals(12_000L, lead.timestampMs)
        assertEquals("I said", lead.text)
        assertEquals(listOf("I ", "said"), lead.words!!.map { it.text })
        assertEquals(LyricRole.MAIN, lead.role)
        assertEquals("Dije", lead.translation)

        val bg = lines[2]
        assertTrue(bg.effectiveIsBackground)
        assertEquals("ooh yeah", bg.text)
        assertEquals(13_200L, bg.words!!.first().startMs)

        val duet = lines[3]
        assertEquals(LyricRole.DUET, duet.role)
        assertEquals(62_500L, duet.timestampMs)
        assertEquals("Tom & Jerry", duet.text)
    }

    @Test
    fun `reads every clock shape`() {
        assertEquals(3_723_400L, TtmlParser.parseTime("1:02:03.4"))
        assertEquals(62_050L, TtmlParser.parseTime("01:02.05"))
        assertEquals(1_500L, TtmlParser.parseTime("1.5s"))
        assertEquals(250L, TtmlParser.parseTime("250ms"))
        assertNull(TtmlParser.parseTime("12f"))
    }
}
