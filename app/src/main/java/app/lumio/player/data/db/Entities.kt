package app.lumio.player.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lumio.player.data.model.Song

@Entity(tableName = "songs", indices = [Index("folderPath"), Index("album"), Index("artist")])
data class SongEntity(
    @PrimaryKey val uri: String,
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
    val source: Int = 0, // 0 = MediaStore, 1 = SAF deep scan
)

@Entity(tableName = "user_data")
data class UserDataEntity(
    @PrimaryKey val uri: String,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val skipCount: Int = 0,
    val hidden: Boolean = false,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [ForeignKey(PlaylistEntity::class, ["id"], ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId")],
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songUri: String,
    val position: Int,
    val addedAt: Long,
)

@Entity(tableName = "scan_folders")
data class ScanFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayPath: String,
    val volume: String,
    val relativePath: String,
    val mode: Int, // 0 include, 1 exclude
    val deepScan: Boolean,
    val lastScan: Long,
    val songCount: Int,
) {
    /** Same format as SongEntity.folderPath, e.g. "primary:Music/Rock/" */
    val key: String get() = "${volume.lowercase()}:$relativePath"
    companion object { const val INCLUDE = 0; const val EXCLUDE = 1 }
}

@Entity(tableName = "history", indices = [Index("playedAt")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songUri: String,
    val playedAt: Long,
    val listenedMs: Long,
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val songUri: String,
)

/** Join result: song + user data. */
data class SongRow(
    @Embedded val song: SongEntity,
    val isFavorite: Boolean?,
    val playCount: Int?,
    val lastPlayed: Long?,
    val hidden: Boolean?,
)

fun SongRow.toSong() = Song(
    uri = song.uri, title = song.title, artist = song.artist, album = song.album, albumId = song.albumId,
    genre = song.genre, durationMs = song.durationMs, track = song.track, year = song.year,
    dateAdded = song.dateAdded, sizeBytes = song.sizeBytes, mimeType = song.mimeType,
    folderPath = song.folderPath, displayName = song.displayName,
    isFavorite = isFavorite ?: false, playCount = playCount ?: 0, lastPlayed = lastPlayed ?: 0L,
)

data class UriCount(val songUri: String, val c: Int)
