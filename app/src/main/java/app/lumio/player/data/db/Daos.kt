package app.lumio.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

const val SONG_SELECT =
    "SELECT songs.*, user_data.isFavorite AS isFavorite, user_data.playCount AS playCount, " +
        "user_data.lastPlayed AS lastPlayed, user_data.hidden AS hidden " +
        "FROM songs LEFT JOIN user_data ON songs.uri = user_data.uri"

@Dao
interface SongDao {
    @Query(SONG_SELECT)
    fun observeAll(): Flow<List<SongRow>>

    @Query("$SONG_SELECT WHERE songs.uri IN (:uris)")
    suspend fun byUris(uris: List<String>): List<SongRow>

    @Query("SELECT uri FROM songs")
    suspend fun allUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM songs")
    suspend fun clear()

    @Query("SELECT uri FROM songs WHERE source = 1")
    suspend fun safUris(): List<String>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int
}

@Dao
abstract class UserDataDao {
    @Query("INSERT OR IGNORE INTO user_data(uri,isFavorite,playCount,lastPlayed,skipCount,hidden) VALUES(:uri,0,0,0,0,0)")
    abstract suspend fun ensure(uri: String)

    @Query("UPDATE user_data SET isFavorite = :fav WHERE uri = :uri")
    abstract suspend fun updateFavorite(uri: String, fav: Boolean)

    @Query("UPDATE user_data SET playCount = playCount + 1, lastPlayed = :now WHERE uri = :uri")
    abstract suspend fun bumpPlay(uri: String, now: Long)

    @Query("UPDATE user_data SET skipCount = skipCount + 1 WHERE uri = :uri")
    abstract suspend fun bumpSkip(uri: String)

    @Query("UPDATE user_data SET hidden = :hidden WHERE uri = :uri")
    abstract suspend fun updateHidden(uri: String, hidden: Boolean)

    @Transaction open suspend fun setFavorite(uri: String, fav: Boolean) { ensure(uri); updateFavorite(uri, fav) }
    @Transaction open suspend fun recordPlay(uri: String, now: Long) { ensure(uri); bumpPlay(uri, now) }
    @Transaction open suspend fun recordSkip(uri: String) { ensure(uri); bumpSkip(uri) }
    @Transaction open suspend fun setHidden(uri: String, hidden: Boolean) { ensure(uri); updateHidden(uri, hidden) }
}

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    abstract fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position")
    abstract fun observeAllEntries(): Flow<List<PlaylistSongEntity>>

    @Insert abstract suspend fun insert(p: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertEntries(entries: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND songUri = :uri")
    abstract suspend fun removeEntry(pid: Long, uri: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid")
    abstract suspend fun clearEntries(pid: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :pid")
    abstract suspend fun maxPosition(pid: Long): Int

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    abstract suspend fun touch(id: Long, now: Long)

    @Transaction
    open suspend fun addSongs(pid: Long, uris: List<String>, now: Long) {
        var pos = maxPosition(pid) + 1
        insertEntries(uris.map { PlaylistSongEntity(pid, it, pos++, now) })
        touch(pid, now)
    }

    @Transaction
    open suspend fun setOrder(pid: Long, uris: List<String>, now: Long) {
        clearEntries(pid)
        insertEntries(uris.mapIndexed { i, u -> PlaylistSongEntity(pid, u, i, now) })
        touch(pid, now)
    }
}

@Dao
interface ScanFolderDao {
    @Query("SELECT * FROM scan_folders ORDER BY displayPath")
    fun observeAll(): Flow<List<ScanFolderEntity>>

    @Query("SELECT * FROM scan_folders")
    suspend fun getAll(): List<ScanFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: ScanFolderEntity)

    @Query("DELETE FROM scan_folders WHERE treeUri = :uri")
    suspend fun delete(uri: String)

    @Query("UPDATE scan_folders SET lastScan = :time, songCount = :count WHERE treeUri = :uri")
    suspend fun updateStats(uri: String, time: Long, count: Int)

    @Query("UPDATE scan_folders SET mode = :mode WHERE treeUri = :uri")
    suspend fun setMode(uri: String, mode: Int)

    @Query("UPDATE scan_folders SET deepScan = :deep WHERE treeUri = :uri")
    suspend fun setDeep(uri: String, deep: Boolean)
}

@Dao
interface HistoryDao {
    @Insert suspend fun insert(h: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT songUri, COUNT(*) AS c FROM history WHERE playedAt >= :since GROUP BY songUri ORDER BY c DESC LIMIT :n")
    suspend fun topSongs(since: Long, n: Int): List<UriCount>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM history WHERE playedAt >= :since")
    suspend fun totalListened(since: Long): Long

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
abstract class QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position")
    abstract suspend fun getAll(): List<QueueItemEntity>

    @Query("DELETE FROM queue_items")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<QueueItemEntity>)

    @Transaction
    open suspend fun replaceAll(uris: List<String>) {
        clear()
        insertAll(uris.mapIndexed { i, u -> QueueItemEntity(i, u) })
    }
}
