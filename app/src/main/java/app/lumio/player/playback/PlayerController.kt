package app.lumio.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.lumio.player.data.db.AppDatabase
import app.lumio.player.data.db.HistoryEntity
import app.lumio.player.data.model.Song
import app.lumio.player.data.repo.LibraryRepository
import app.lumio.player.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class PlayerState(
    val connected: Boolean = false,
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val index: Int = -1,
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val sourceLabel: String = "",
)

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(uri)
    .setUri(Uri.parse(uri))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title).setArtist(artist).setAlbumTitle(album)
            .setArtworkUri(Uri.parse(uri)) // ArtworkBitmapLoader resolves embedded art from the audio URI
            .setDurationMs(durationMs)
            .build()
    ).build()

/** UI-side playback facade. All player calls are marshalled to the main thread. Spec: docs/08. */
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayDeque<(MediaController) -> Unit>()
    private val raw = MutableStateFlow(PlayerState())

    /** Current song is refreshed from the library so favorite/play-count changes show instantly. */
    val state: StateFlow<PlayerState> = combine(raw, library.songIndex) { s, idx ->
        s.copy(current = s.current?.let { idx[it.uri] ?: it })
    }.stateIn(scope, SharingStarted.Eagerly, PlayerState())

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private var listenedMs = 0L
    private var currentUri: String? = null
    private var currentDuration = 0L
    private var persistJob: Job? = null
    private var consecutiveErrors = 0
    private var sourceLabel = ""

    // ------------------------------------------------------------------ connection
    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val c = MediaController.Builder(context, token).buildAsync().await()
                controller = c
                connecting = false
                c.addListener(listener)
                currentUri = c.currentMediaItem?.mediaId
                refresh(c, full = true)
                restoreQueueIfNeeded(c)
                while (pending.isNotEmpty()) pending.removeFirst().invoke(c)
                startTicker()
            } catch (e: Exception) {
                connecting = false
                Log.e(TAG, "connect failed", e)
            }
        }
    }

    private fun whenReady(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) block(c)
            else scope.launch(Dispatchers.Main.immediate) { block(c) }
        } else {
            scope.launch(Dispatchers.Main.immediate) { pending.addLast(block) }
            connect()
        }
    }

    // ------------------------------------------------------------------ commands
    fun playSongs(list: List<Song>, startIndex: Int = 0, shuffle: Boolean = false, source: String = "") = whenReady { c ->
        if (list.isEmpty()) return@whenReady
        val start = if (shuffle) Random.nextInt(list.size) else startIndex.coerceIn(0, list.lastIndex)
        sourceLabel = source
        c.setMediaItems(list.map { it.toMediaItem() }, start, 0L)
        c.shuffleModeEnabled = shuffle
        c.prepare()
        c.play()
    }

    fun playUri(uri: Uri) = whenReady { c ->
        c.setMediaItem(MediaItem.Builder().setMediaId(uri.toString()).setUri(uri).build())
        c.prepare(); c.play()
    }

    fun togglePlay() = whenReady { c -> if (c.isPlaying) c.pause() else { if (c.playbackState == Player.STATE_IDLE) c.prepare(); c.play() } }
    fun play() = whenReady { it.play() }
    fun pause() = whenReady { it.pause() }
    fun next() = whenReady { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previous() = whenReady { it.seekToPrevious() } // restarts song if > 3 s in (Media3 default)
    fun seekTo(ms: Long) = whenReady { _position.value = ms; it.seekTo(ms) }
    fun seekBy(deltaMs: Long) = whenReady { c -> c.seekTo((c.currentPosition + deltaMs).coerceAtLeast(0)) }
    fun toggleShuffle() = whenReady { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = whenReady {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun skipTo(index: Int) = whenReady { c -> if (index in 0 until c.mediaItemCount) { c.seekToDefaultPosition(index); c.play() } }

    fun playNext(songs: List<Song>) = whenReady { c ->
        if (c.mediaItemCount == 0) playSongs(songs)
        else c.addMediaItems(c.currentMediaItemIndex + 1, songs.map { it.toMediaItem() })
    }
    fun addToQueue(songs: List<Song>) = whenReady { c ->
        if (c.mediaItemCount == 0) playSongs(songs) else c.addMediaItems(songs.map { it.toMediaItem() })
    }
    fun removeAt(index: Int) = whenReady { c -> if (index in 0 until c.mediaItemCount) c.removeMediaItem(index) }
    fun moveItem(from: Int, to: Int) = whenReady { c -> if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to) }
    fun clearQueue() = whenReady { it.clearMediaItems() }
    fun setSpeed(speed: Float, pitch: Float) = whenReady { it.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2f), pitch.coerceIn(0.5f, 2f)) }

    fun toggleFavorite(song: Song) {
        scope.launch(Dispatchers.IO) { db.userData().setFavorite(song.uri, !song.isFavorite) }
    }

    // ------------------------------------------------------------------ listener / state
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val structural = events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) onTrackChanged(player)
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) consecutiveErrors = 0
            refresh(player, structural)
            if (structural || (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && !player.isPlaying)) schedulePersist()
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            consecutiveErrors++
            Toast.makeText(context, "Couldn't play ${c.currentMediaItem?.mediaMetadata?.title ?: "this song"}", Toast.LENGTH_SHORT).show()
            if (consecutiveErrors >= 3) { c.stop(); consecutiveErrors = 0; return }
            if (c.hasNextMediaItem()) { c.seekToNextMediaItem(); c.prepare(); c.play() }
        }
    }

    private fun refresh(p: Player, full: Boolean) {
        val idx = library.songIndex.value
        val old = raw.value
        val queue = if (full || (old.queue.isEmpty() && p.mediaItemCount > 0)) {
            List(p.mediaItemCount) { i -> p.getMediaItemAt(i).let { idx[it.mediaId] ?: fallbackSong(it) } }
        } else old.queue
        val index = p.currentMediaItemIndex
        val current = queue.getOrNull(index)
        raw.value = PlayerState(
            connected = true,
            current = current,
            isPlaying = p.isPlaying,
            durationMs = p.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L,
            shuffle = p.shuffleModeEnabled,
            repeat = p.repeatMode,
            queue = queue,
            index = index,
            speed = p.playbackParameters.speed,
            pitch = p.playbackParameters.pitch,
            sourceLabel = sourceLabel,
        )
    }

    private fun fallbackSong(item: MediaItem): Song {
        val md = item.mediaMetadata
        return Song(
            uri = item.mediaId, title = md.title?.toString() ?: "Unknown", artist = md.artist?.toString() ?: "Unknown artist",
            album = md.albumTitle?.toString() ?: "Unknown album", albumId = 0, genre = "", durationMs = md.durationMs ?: 0L,
            track = 0, year = 0, dateAdded = 0, sizeBytes = 0, mimeType = "audio/*", folderPath = "", displayName = "",
        )
    }

    private fun startTicker() {
        scope.launch(Dispatchers.Main.immediate) {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                val c = controller
                val now = SystemClock.elapsedRealtime()
                var playing = false
                if (c != null) {
                    playing = c.isPlaying
                    if (playing) {
                        listenedMs += now - last
                        val d = c.duration
                        if (d > 0) currentDuration = d
                    }
                    _position.value = c.currentPosition
                }
                last = now
                delay(if (playing) 250 else 700)
            }
        }
    }

    // ------------------------------------------------------------------ history & play counts
    private fun onTrackChanged(p: Player) {
        finalizeListen()
        currentUri = p.currentMediaItem?.mediaId
        currentDuration = p.duration.takeIf { it > 0 } ?: 0L
    }

    private fun finalizeListen() {
        val uri = currentUri ?: return
        val listened = listenedMs
        val dur = currentDuration
        listenedMs = 0L
        scope.launch(Dispatchers.IO) {
            val s = settings.current()
            val now = System.currentTimeMillis()
            val threshold = if (dur > 0) minOf(30_000L, dur / 2) else 30_000L
            if (listened >= threshold) {
                db.userData().recordPlay(uri, now)
                if (s.historyEnabled) db.history().insert(HistoryEntity(songUri = uri, playedAt = now, listenedMs = listened))
            } else if (listened in 1..4_999) {
                db.userData().recordSkip(uri)
            }
        }
    }

    // ------------------------------------------------------------------ queue persistence
    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(1500)
            persistNow()
        }
    }

    private suspend fun persistNow() {
        data class Snap(val uris: List<String>, val index: Int, val pos: Long)
        val snap = withContext(Dispatchers.Main.immediate) {
            val c = controller ?: return@withContext null
            Snap(List(c.mediaItemCount) { c.getMediaItemAt(it).mediaId }, c.currentMediaItemIndex, c.currentPosition)
        } ?: return
        try {
            db.queue().replaceAll(snap.uris)
            settings.update { copy(lastQueueIndex = snap.index, lastPositionMs = snap.pos, lastSongUri = snap.uris.getOrNull(snap.index) ?: "") }
        } catch (e: Exception) {
            Log.w(TAG, "persist failed", e)
        }
    }

    private suspend fun restoreQueueIfNeeded(c: MediaController) {
        if (c.mediaItemCount > 0) return
        val s = settings.current()
        if (!s.resumeOnLaunch) return
        val saved = db.queue().getAll()
        if (saved.isEmpty()) return
        library.isLoaded.first { it }
        val idx = library.songIndex.value
        val songs = saved.mapNotNull { idx[it.songUri] }
        if (songs.isEmpty()) return
        val startIdx = songs.indexOfFirst { it.uri == s.lastSongUri }.coerceAtLeast(0)
        val pos = if (songs[startIdx].uri == s.lastSongUri) s.lastPositionMs else 0L
        c.setMediaItems(songs.map { it.toMediaItem() }, startIdx, pos)
        c.prepare() // do NOT auto-play
    }

    companion object { private const val TAG = "PlayerController" }
}
