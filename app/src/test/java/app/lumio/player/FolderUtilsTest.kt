package app.lumio.player

import app.lumio.player.data.scan.FolderUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderUtilsTest {
    @Test fun relativePathPrimary() {
        assertEquals("primary:Music/Rock/", FolderUtils.keyFromRelative("external_primary", "Music/Rock/"))
        assertEquals("primary:", FolderUtils.keyFromRelative(null, ""))
    }

    @Test fun relativePathSdCardIsLowercased() {
        assertEquals("1a2b-3c4d:Songs/", FolderUtils.keyFromRelative("1A2B-3C4D", "Songs"))
    }

    @Test fun dataPathLegacy() {
        assertEquals("primary:Music/Rock/", FolderUtils.keyFromData("/storage/emulated/0/Music/Rock/a.mp3"))
        assertEquals("1a2b-3c4d:Songs/", FolderUtils.keyFromData("/storage/1A2B-3C4D/Songs/a.mp3"))
        assertEquals("primary:", FolderUtils.keyFromData("/storage/emulated/0/a.mp3"))
    }

    @Test fun dirDocId() {
        assertEquals("primary:Music/Rock/", FolderUtils.keyFromDirDocId("primary:Music/Rock"))
        assertEquals("primary:", FolderUtils.keyFromDirDocId("primary:"))
    }

    @Test fun insideIsPrefixAndCaseInsensitive() {
        assertTrue(FolderUtils.isInside("primary:Music/Rock/Live/", "primary:Music/Rock/"))
        assertTrue(FolderUtils.isInside("PRIMARY:music/rock/", "primary:Music/Rock/"))
        assertFalse(FolderUtils.isInside("primary:Music/Rockabilly/", "primary:Music/Rock/"))
    }
}
