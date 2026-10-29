package app.lumio.player

import app.lumio.player.data.model.Song
import app.lumio.player.data.repo.sortSongs
import app.lumio.player.data.settings.SortField
import org.junit.Assert.assertEquals
import org.junit.Test

class SortingTest {
    private fun song(title: String, plays: Int = 0) = Song(
        uri = title, title = title, artist = "a", album = "b", albumId = 0, genre = "", durationMs = 1000, track = 0, year = 0,
        dateAdded = 0, sizeBytes = 0, mimeType = "audio/mpeg", folderPath = "primary:", displayName = title, playCount = plays,
    )

    @Test fun ignoresLeadingArticles() {
        val sorted = sortSongs(listOf(song("The Zebra"), song("Apple"), song("A Bird")), SortField.TITLE, true, ignoreArticles = true)
        assertEquals(listOf("Apple", "A Bird", "The Zebra"), sorted.map { it.title })
    }

    @Test fun descendingByPlayCount() {
        val sorted = sortSongs(listOf(song("x", 1), song("y", 9), song("z", 5)), SortField.PLAY_COUNT, false, true)
        assertEquals(listOf("y", "z", "x"), sorted.map { it.title })
    }
}
