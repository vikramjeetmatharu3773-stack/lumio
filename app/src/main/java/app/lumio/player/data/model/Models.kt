package app.lumio.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val durationMs: Long,
    val track: Int,
    val year: Int,
    val dateAdded: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val folderPath: String,
    val displayName: String,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
) {
    val albumKey: String get() = albumKeyOf(album, artist)
}

fun albumKeyOf(album: String, artist: String) = "${album.lowercase()}|${artist.lowercase()}"

@Immutable data class Album(val key: String, val name: String, val artist: String, val songCount: Int, val year: Int, val artSongUri: String)
@Immutable data class Artist(val name: String, val songCount: Int, val albumCount: Int, val artSongUri: String)
@Immutable data class Genre(val name: String, val songCount: Int, val artSongUri: String)
@Immutable data class FolderItem(val path: String, val name: String, val songCount: Int, val artSongUri: String)
@Immutable data class PlaylistItem(val id: Long, val name: String, val songCount: Int, val artSongUri: String?)

/** Coil model: loads embedded artwork for an audio URI. */
@Immutable data class ArtworkModel(val songUri: String)

data class LyricLine(val timeMs: Long, val text: String)

enum class CollectionType(val route: String) {
    ALBUM("album"), ARTIST("artist"), GENRE("genre"), FOLDER("folder"), PLAYLIST("playlist"),
    FAVORITES("favorites"), MOST_PLAYED("mostplayed"), RECENT("recent"), RECENTLY_ADDED("recentlyadded"),
    FORGOTTEN("forgotten"), MOOD("mood"), ALL("all");

    companion object {
        fun from(route: String): CollectionType = entries.firstOrNull { it.route == route } ?: ALL
    }
}

enum class Mood(val label: String, val keywords: List<String>) {
    FEEL_GOOD("Feel good", listOf("pop", "dance", "funk", "disco", "reggae", "bhangra", "punjabi", "happy", "party", "summer", "love")),
    WORK_OUT("Work out", listOf("hip hop", "hip-hop", "rap", "edm", "electronic", "rock", "metal", "trap", "workout", "gym", "drill")),
    ENERGISE("Energise", listOf("dance", "house", "techno", "punk", "remix", "bass", "upbeat", "power", "drum")),
    RELAX("Relax", listOf("acoustic", "ambient", "chill", "lofi", "lo-fi", "classical", "jazz", "instrumental", "sufi", "ghazal", "soft", "sleep", "piano"));
}
