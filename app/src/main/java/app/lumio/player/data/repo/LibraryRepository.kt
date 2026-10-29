package app.lumio.player.data.repo

import app.lumio.player.data.db.AppDatabase
import app.lumio.player.data.db.toSong
import app.lumio.player.data.model.Album
import app.lumio.player.data.model.Artist
import app.lumio.player.data.model.FolderItem
import app.lumio.player.data.model.Genre
import app.lumio.player.data.model.Mood
import app.lumio.player.data.model.Song
import app.lumio.player.data.scan.FolderUtils
import app.lumio.player.data.settings.SettingsRepository
import app.lumio.player.util.normalizeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.Random

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
) {
    val isEmpty get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && genres.isEmpty() && folders.isEmpty()
}

/** In-memory library derived from the songs table. Fine up to ~50k songs. */
class LibraryRepository(db: AppDatabase, val settings: SettingsRepository, scope: CoroutineScope) {
    private val loadedFlag = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = loadedFlag.asStateFlow()

    val songs: StateFlow<List<Song>> = db.songs().observeAll()
        .map { rows -> rows.filter { it.hidden != true }.map { it.toSong() } }
        .flowOn(Dispatchers.Default)
        .onEach { loadedFlag.value = true }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val songIndex: StateFlow<Map<String, Song>> = songs
        .map { l -> l.associateBy { it.uri } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val albums: StateFlow<List<Album>> = songs.map { list ->
        list.groupBy { it.albumKey }.map { (k, v) ->
            Album(k, v.first().album, v.first().artist, v.size, v.maxOf { it.year }, v.first().uri)
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val artists: StateFlow<List<Artist>> = songs.map { list ->
        list.groupBy { it.artist }.map { (name, v) ->
            Artist(name, v.size, v.map { it.albumKey }.distinct().size, v.first().uri)
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val genres: StateFlow<List<Genre>> = songs.map { list ->
        list.filter { it.genre.isNotBlank() }.groupBy { it.genre }.map { (name, v) ->
            Genre(name, v.size, v.first().uri)
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<FolderItem>> = songs.map { list ->
        list.groupBy { it.folderPath }.map { (path, v) ->
            FolderItem(path, FolderUtils.displayName(path), v.size, v.first().uri)
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val searchIndex: StateFlow<List<Pair<Song, String>>> = songs.map { l ->
        l.map { it to normalizeText("${it.title} ${it.artist} ${it.album}") }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ---------- smart mixes (pure functions of the song list) ----------
    fun recentlyAdded(l: List<Song>) = l.sortedByDescending { it.dateAdded }.take(100)
    fun mostPlayed(l: List<Song>) = l.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(100)
    fun recentlyPlayed(l: List<Song>) = l.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(100)
    fun favorites(l: List<Song>) = l.filter { it.isFavorite }
    fun forgotten(l: List<Song>): List<Song> {
        val cutoff = System.currentTimeMillis() - 60L * 24 * 3600 * 1000
        val old = l.filter { it.playCount > 0 && it.lastPlayed < cutoff }.sortedByDescending { it.playCount }
        if (old.isNotEmpty()) return old.take(100)
        return l.filter { it.playCount == 0 }.shuffled(Random(daySeed())).take(60)
    }

    fun moodMix(l: List<Song>, mood: Mood): List<Song> {
        val hits = l.filter { s ->
            val hay = "${s.genre} ${s.title} ${s.album}".lowercase()
            mood.keywords.any { hay.contains(it) }
        }
        val pick = if (hits.size >= 5) hits else l
        return pick.shuffled(Random(daySeed() + mood.ordinal)).take(100)
    }

    private fun daySeed() = System.currentTimeMillis() / (24L * 3600 * 1000)

    // ---------- search ----------
    fun search(query: String): SearchResults {
        val words = normalizeText(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return SearchResults()
        val songHits = searchIndex.value.asSequence().filter { (_, hay) -> words.all { hay.contains(it) } }.map { it.first }.take(200).toList()
        fun m(s: String) = normalizeText(s).let { n -> words.all { n.contains(it) } }
        return SearchResults(
            songs = songHits,
            albums = albums.value.filter { m("${it.name} ${it.artist}") }.take(40),
            artists = artists.value.filter { m(it.name) }.take(40),
            genres = genres.value.filter { m(it.name) }.take(20),
            folders = folders.value.filter { m(it.name) }.take(20),
        )
    }
}
