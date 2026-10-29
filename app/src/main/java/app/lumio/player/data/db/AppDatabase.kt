package app.lumio.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class, UserDataEntity::class, PlaylistEntity::class, PlaylistSongEntity::class,
        ScanFolderEntity::class, HistoryEntity::class, QueueItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songs(): SongDao
    abstract fun userData(): UserDataDao
    abstract fun playlists(): PlaylistDao
    abstract fun folders(): ScanFolderDao
    abstract fun history(): HistoryDao
    abstract fun queue(): QueueDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "lumio.db").build()
    }
}
