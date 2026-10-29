package app.lumio.player

import app.lumio.player.playback.AudioEffectsController
import app.lumio.player.playback.EqPresets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EqPresetsTest {
    @Test fun flatIsZero() {
        assertArrayEquals(intArrayOf(0, 0, 0, 0, 0), EqPresets.levelsMb("Flat", 5, -1500, 1500))
    }

    @Test fun interpolatesToBandCount() {
        val l = EqPresets.levelsMb("Bass", 3, -1500, 1500)
        assertEquals(3, l.size)
        assertEquals(600, l.first())   // +6 dB at the low end
        assertEquals(-200, l.last())   // -2 dB at the high end
    }

    @Test fun clampsToDeviceRange() {
        val l = EqPresets.levelsMb("Bass", 5, -300, 300)
        assertEquals(300, l[0])
    }

    @Test fun parseBandsPadsMissing() {
        assertArrayEquals(intArrayOf(100, -50, 0, 0), AudioEffectsController.parseBands("100,-50", 4))
    }
}
