package app.lumio.player.data.repo

import app.lumio.player.data.db.AppDatabase
import app.lumio.player.data.db.PlaylistEntity
import app.lumio.player.data.model.PlaylistItem
import app.lumio.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class PlaylistRepository(db: AppDatabase, private val library: LibraryRepository) {
    private val dao = db.playlists()

    val playlists: Flow<List<PlaylistItem>> =
        combine(dao.observePlaylists(), dao.observeAllEntries(), library.songIndex) { pls, entries, idx ->
            val byPl = entries.groupBy { it.playlistId }
            pls.map { p ->
                val valid = byPl[p.id].orEmpty().sortedBy { it.position }.mapNotNull { idx[it.songUri] }
                PlaylistItem(p.id, p.name, valid.size, valid.firstOrNull()?.uri)
            }
        }.flowOn(Dispatchers.Default)

    fun songsOf(id: Long): Flow<List<Song>> =
        combine(dao.observeAllEntries(), library.songIndex) { entries, idx ->
            entries.filter { it.playlistId == id }.sortedBy { it.position }.mapNotNull { idx[it.songUri] }
        }.flowOn(Dispatchers.Default)

    fun nameOf(id: Long): Flow<String?> = dao.observePlaylists().map { l -> l.firstOrNull { it.id == id }?.name }

    suspend fun create(name: String, uris: List<String> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = dao.insert(PlaylistEntity(name = name.trim().ifEmpty { "New playlist" }, createdAt = now, updatedAt = now))
        if (uris.isNotEmpty()) dao.addSongs(id, uris, now)
        return id
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim(), System.currentTimeMillis())
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun addSongs(id: Long, uris: List<String>) = dao.addSongs(id, uris, System.currentTimeMillis())
    suspend fun removeSong(id: Long, uri: String) = dao.removeEntry(id, uri)
    suspend fun reorder(id: Long, uris: List<String>) = dao.setOrder(id, uris, System.currentTimeMillis())
}
