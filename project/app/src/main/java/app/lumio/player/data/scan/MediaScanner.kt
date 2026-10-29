package app.lumio.player.data.scan

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import app.lumio.player.data.db.AppDatabase
import app.lumio.player.data.db.ScanFolderEntity
import app.lumio.player.data.db.SongEntity
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ScanMode
import app.lumio.player.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.Locale

/** MediaStore scan + optional SAF deep scan. Spec: docs/07_LIBRARY_AND_FOLDER_SCANNING.md */
class MediaScanner(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    data class ScanState(val running: Boolean = false, val phase: String = "", val found: Int = 0, val summary: String = "")

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private val mutex = Mutex()
    private var observeJob: Job? = null

    suspend fun scan(rebuild: Boolean = false, deep: Boolean = true) = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext
        val t0 = SystemClock.elapsedRealtime()
        try {
            _state.value = ScanState(true, "Reading library")
            val s = settings.current()
            val folders = db.folders().getAll()
            val includes = folders.filter { it.mode == ScanFolderEntity.INCLUDE }
            val excludes = folders.filter { it.mode == ScanFolderEntity.EXCLUDE }

            fun allowed(folderPath: String): Boolean {
                if (excludes.any { FolderUtils.isInside(folderPath, it.key) }) return false
                if (s.scanMode == ScanMode.SELECTED_ONLY) return includes.any { FolderUtils.isInside(folderPath, it.key) }
                return true
            }

            val found = LinkedHashMap<String, SongEntity>()
            queryMediaStore(s) { e ->
                if (allowed(e.folderPath)) {
                    found[e.uri] = e
                    if (found.size % 250 == 0) _state.update { it.copy(found = found.size) }
                }
            }

            if (deep) {
                val known = HashSet<String>(found.size * 2)
                found.values.forEach { known.add(dedupeKey(it.folderPath, it.displayName)) }
                for (f in includes) {
                    if (!(f.deepScan || s.includeNoMedia)) continue
                    _state.update { it.copy(phase = "Deep scanning ${f.displayPath}") }
                    try {
                        deepScanTree(f, known, excludes, s) { e ->
                            found[e.uri] = e
                            if (found.size % 50 == 0) _state.update { st -> st.copy(found = found.size) }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "deep scan failed for ${f.displayPath}", e)
                    }
                }
            }

            _state.update { it.copy(phase = "Saving", found = found.size) }
            if (rebuild) db.songs().clear()
            val existing = db.songs().allUris().toSet()
            val keepSaf = if (deep) emptySet() else db.songs().safUris().toSet()
            val toDelete = existing - found.keys - keepSaf
            toDelete.chunked(500).forEach { db.songs().deleteByUris(it) }
            found.values.toList().chunked(500).forEach { db.songs().upsertAll(it) }

            val now = System.currentTimeMillis()
            for (f in folders) {
                val n = found.values.count { FolderUtils.isInside(it.folderPath, f.key) }
                db.folders().updateStats(f.treeUri, now, n)
            }
            settings.update { copy(lastScanAt = now) }
            val secs = (SystemClock.elapsedRealtime() - t0) / 1000.0
            _state.value = ScanState(false, "Done", found.size, String.format(Locale.US, "%d songs · %.1f s", found.size, secs))
        } catch (e: Exception) {
            Log.e(TAG, "scan failed", e)
            _state.value = ScanState(false, "Failed", 0, "Scan failed: ${e.message}")
        } finally {
            mutex.unlock()
        }
    }

    // ---------------- Folders ----------------

    suspend fun addFolder(treeUri: Uri, mode: Int, deep: Boolean): ScanFolderEntity = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w(TAG, "persist permission failed", e)
        }
        val p = FolderUtils.parseTree(treeUri)
        val entity = ScanFolderEntity(treeUri.toString(), p.display, p.volume, p.relativePath, mode, deep, 0L, 0)
        db.folders().upsert(entity)
        entity
    }

    suspend fun removeFolder(treeUri: String) = withContext(Dispatchers.IO) {
        db.folders().delete(treeUri)
        try {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(treeUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
    }

    // ---------------- Auto rescan ----------------

    fun startObserving(scope: CoroutineScope) {
        if (observeJob != null) return
        try {
            val handler = Handler(Looper.getMainLooper())
            var pending: Job? = null
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    pending?.cancel()
                    pending = scope.launch {
                        delay(3000)
                        try {
                            val s = settings.current()
                            if (s.autoScanOnChange && s.onboardingDone) scan(deep = false)
                        } catch (_: Exception) {}
                    }
                }
            }
            context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
            observeJob = Job()
        } catch (_: Exception) {}
    }

    // ---------------- Stage 1: MediaStore ----------------

    @Suppress("DEPRECATION")
    private fun queryMediaStore(s: AppSettings, onSong: (SongEntity) -> Unit) {
        val media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else media
        val cols = ArrayList<String>().apply {
            add(MediaStore.Audio.Media._ID); add(MediaStore.Audio.Media.TITLE); add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM); add(MediaStore.Audio.Media.ALBUM_ID); add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK); add(MediaStore.Audio.Media.YEAR); add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.SIZE); add(MediaStore.Audio.Media.MIME_TYPE); add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= 29) { add(MediaStore.Audio.Media.RELATIVE_PATH); add(MediaStore.Audio.Media.VOLUME_NAME) }
            if (Build.VERSION.SDK_INT >= 30) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()
        val selection = buildString {
            append("${MediaStore.Audio.Media.DURATION} >= ?")
            if (!s.includeNonMusic) append(" AND ${MediaStore.Audio.Media.IS_MUSIC} != 0")
        }
        val args = arrayOf((s.minDurationSec * 1000L).toString())
        val cursor = context.contentResolver.query(collection, cols, selection, args, null) ?: return
        cursor.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val iData = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val iRel = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val iVol = c.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
            val iGenre = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(iId)
                    val display = c.getString(iName) ?: ""
                    val vol = if (iVol >= 0) c.getString(iVol) else null
                    val rel = if (iRel >= 0) c.getString(iRel) else null
                    val folder = if (vol != null && rel != null) FolderUtils.keyFromRelative(vol, rel)
                    else FolderUtils.keyFromData(if (iData >= 0) c.getString(iData) else null)
                    val titleRaw = c.getString(iTitle)
                    val artistRaw = c.getString(iArtist)
                    val albumRaw = c.getString(iAlbum)
                    val genreRaw = if (iGenre >= 0) c.getString(iGenre) else null
                    val dur = if (!c.isNull(iDur)) c.getLong(iDur) else 0L
                    val track = if (!c.isNull(iTrack)) c.getInt(iTrack) % 1000 else 0
                    val year = if (!c.isNull(iYear)) c.getInt(iYear).coerceAtLeast(0) else 0
                    val added = if (!c.isNull(iAdded)) c.getLong(iAdded) * 1000L else 0L
                    val size = if (!c.isNull(iSize)) c.getLong(iSize) else 0L
                    val mime = if (!c.isNull(iMime)) c.getString(iMime) else "audio/*"
                    onSong(
                        SongEntity(
                            uri = ContentUris.withAppendedId(media, id).toString(),
                            title = clean(titleRaw, display.substringBeforeLast('.')),
                            artist = clean(artistRaw, "Unknown artist"),
                            album = clean(albumRaw, "Unknown album"),
                            albumId = if (!c.isNull(iAlbumId)) c.getLong(iAlbumId) else 0L,
                            genre = clean(genreRaw, ""),
                            durationMs = dur,
                            track = track,
                            year = year,
                            dateAdded = added,
                            sizeBytes = size,
                            mimeType = mime,
                            folderPath = folder,
                            displayName = display,
                            source = 0,
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "bad row skipped", e)
                }
            }
        }
    }

    // ---------------- Stage 2: SAF deep scan ----------------

    private fun deepScanTree(
        folder: ScanFolderEntity,
        known: MutableSet<String>,
        excludes: List<ScanFolderEntity>,
        s: AppSettings,
        onSong: (SongEntity) -> Unit,
    ) {
        val tree = Uri.parse(folder.treeUri)
        val stack = ArrayDeque<String>()
        stack.add(DocumentsContract.getTreeDocumentId(tree))
        val proj = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        while (stack.isNotEmpty()) {
            val parentId = stack.removeLast()
            val parentKey = FolderUtils.keyFromDirDocId(parentId)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            val cursor = try { context.contentResolver.query(children, proj, null, null, null) } catch (e: Exception) { null } ?: continue
            cursor.use { c ->
                while (c.moveToNext()) {
                    val docId = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val dirKey = FolderUtils.keyFromDirDocId(docId)
                        if (name.startsWith(".") && !s.includeNoMedia) continue
                        if (excludes.any { FolderUtils.isInside(dirKey, it.key) }) continue
                        stack.add(docId)
                        continue
                    }
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (!(mime.startsWith("audio/") || ext in AUDIO_EXT)) continue
                    if (c.getLong(3) in 1 until 8 * 1024) continue
                    if (excludes.any { FolderUtils.isInside(parentKey, it.key) }) continue
                    val dk = dedupeKey(parentKey, name)
                    if (!known.add(dk)) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                    readTags(docUri, name, parentKey, c.getLong(4), c.getLong(3), mime, s)?.let(onSong)
                }
            }
        }
    }

    private fun readTags(uri: Uri, name: String, folderKey: String, modified: Long, size: Long, mime: String, s: AppSettings): SongEntity? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (dur < s.minDurationSec * 1000L) return null
            val year = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull() ?: 0
            val track = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull() ?: 0
            return SongEntity(
                uri = uri.toString(),
                title = clean(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), name.substringBeforeLast('.')),
                artist = clean(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST), "Unknown artist"),
                album = clean(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), "Unknown album"),
                albumId = 0L,
                genre = clean(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE), ""),
                durationMs = dur, track = track % 1000, year = year,
                dateAdded = modified, sizeBytes = size, mimeType = mime.ifBlank { "audio/*" },
                folderPath = folderKey, displayName = name, source = 1,
            )
        } catch (e: Exception) {
            Log.w(TAG, "tag read failed: $name", e)
            return null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    private fun clean(s: String?, fallback: String): String =
        s?.trim()?.takeIf { it.isNotEmpty() && it != "<unknown>" } ?: fallback

    private fun dedupeKey(folderKey: String, name: String) = (folderKey + name).lowercase()

    companion object {
        private const val TAG = "MediaScanner"
        private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wave", "amr", "3gp", "mid", "midi")
    }
}
