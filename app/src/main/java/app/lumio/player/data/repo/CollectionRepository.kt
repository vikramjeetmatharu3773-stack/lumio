package app.lumio.player.data.repo

import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.model.CollectionType.*
import app.lumio.player.data.model.Mood
import app.lumio.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Resolves (type,key) -> list of songs for the Collection screen. */
class CollectionRepository(private val library: LibraryRepository, private val playlists: PlaylistRepository) {

    fun songsFor(type: CollectionType, key: String): Flow<List<Song>> {
        val src = library.songs
        return when (type) {
            ALBUM -> src.map { l -> l.filter { it.albumKey == key }.sortedWith(compareBy({ it.track }, { it.title.lowercase() })) }
            ARTIST -> src.map { l -> l.filter { it.artist.equals(key, true) }.sortedWith(compareBy({ it.album.lowercase() }, { it.track })) }
            GENRE -> src.map { l -> l.filter { it.genre.equals(key, true) }.sortedBy { it.title.lowercase() } }
            FOLDER -> src.map { l -> l.filter { it.folderPath.equals(key, true) }.sortedBy { it.title.lowercase() } }
            PLAYLIST -> playlists.songsOf(key.toLongOrNull() ?: -1L)
            FAVORITES -> src.map { library.favorites(it) }
            MOST_PLAYED -> src.map { library.mostPlayed(it) }
            RECENT -> src.map { library.recentlyPlayed(it) }
            RECENTLY_ADDED -> src.map { library.recentlyAdded(it) }
            FORGOTTEN -> src.map { library.forgotten(it) }
            MOOD -> src.map { l -> library.moodMix(l, Mood.entries.firstOrNull { it.name == key } ?: Mood.FEEL_GOOD) }
            ALL -> src.map { it.sortedBy { s -> s.title.lowercase() } }
        }.flowOn(Dispatchers.Default)
    }
}
