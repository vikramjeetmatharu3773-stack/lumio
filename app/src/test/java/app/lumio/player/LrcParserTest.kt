package app.lumio.player

import app.lumio.player.data.repo.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test fun parsesSyncedLines() {
        val l = LrcParser.parse("[ar:Artist]\n[00:12.50]Hello\n[01:02.00]World")
        assertEquals(2, l.size)
        assertEquals(12_500L, l[0].timeMs)
        assertEquals("World", l[1].text)
        assertEquals(62_000L, l[1].timeMs)
    }

    @Test fun supportsMultipleStampsAndOffset() {
        val l = LrcParser.parse("[offset:500]\n[00:10.00][00:20.00]Chorus")
        assertEquals(2, l.size)
        assertEquals(9_500L, l[0].timeMs)
        assertEquals(19_500L, l[1].timeMs)
    }

    @Test fun plainTextHasNegativeTime() {
        val l = LrcParser.parse("just words\nmore words")
        assertTrue(l.all { it.timeMs == -1L })
        assertEquals(2, l.size)
    }
}
