package app.lumio.player.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.AppContainer
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.model.Song
import app.lumio.player.data.settings.SortField
import app.lumio.player.ui.nav.Routes
import app.lumio.player.ui.screens.QueueSheet
import app.lumio.player.ui.theme.LocalMotion
import app.lumio.player.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------- overlay state

@Stable
class OverlayController {
    var songOptions by mutableStateOf<Song?>(null)
    var addToPlaylist by mutableStateOf<List<Song>?>(null)
    var newPlaylist by mutableStateOf<List<Song>?>(null)
    var songInfo by mutableStateOf<Song?>(null)
    var sleepTimer by mutableStateOf(false)
    var speed by mutableStateOf(false)
    var queue by mutableStateOf(false)
    var sort by mutableStateOf<SortRequest?>(null)
}

class SortRequest(val field: SortField, val asc: Boolean, val onChange: (SortField, Boolean) -> Unit)

val LocalOverlays = compositionLocalOf { OverlayController() }

private class Holder<T>(var v: T? = null)

@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val h = remember { Holder<T>() }
    if (value != null) h.v = value
    return h.v
}

// ---------------------------------------------------------------- generic glass sheet

@Composable
fun GlassBottomSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val motion = LocalMotion.current
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    var drag by remember { mutableFloatStateOf(0f) }
    val on = MaterialTheme.colorScheme.onSurface
    BackHandler(enabled = visible) { onDismiss() }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(motion.ms(200))), exit = fadeOut(tween(motion.ms(200)))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pointerInput(Unit) { detectTapGestures { onDismiss() } })
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(motion.smooth()) { it } + fadeIn(),
            exit = slideOutVertically(motion.smooth()) { it } + fadeOut(),
        ) {
            GlassSurface(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .offset { IntOffset(0, drag.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { d -> drag = (drag + d).coerceAtLeast(0f) },
                        onDragStopped = { v -> if (drag > 220f || v > 1800f) onDismiss(); drag = 0f },
                    ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
                    Box(
                        Modifier.padding(top = 10.dp, bottom = 8.dp).align(Alignment.CenterHorizontally)
                            .size(40.dp, 4.dp).clip(CircleShape).background(on.copy(alpha = 0.35f))
                    )
                    content()
                }
            }
        }
    }
}

@Composable
fun SheetAction(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun SheetHeader(song: Song) {
    val on = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtworkImage(song.uri, Modifier.size(56.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium, color = on, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- host

@Composable
fun OverlayHost(o: OverlayController, onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    SongOptionsSheet(o, c, onNavigate)
    AddToPlaylistSheet(o, c)
    SleepTimerSheet(o, c)
    SpeedSheet(o, c)
    QueueSheet(o, c)
    SortSheet(o)
    SongInfoDialog(o)
    NewPlaylistDialog(o, c)
}

@Composable
private fun SongOptionsSheet(o: OverlayController, c: AppContainer, onNavigate: (String) -> Unit) {
    val song = rememberLastNonNull(o.songOptions)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val idx by c.library.songIndex.collectAsStateWithLifecycle()
    GlassBottomSheet(visible = o.songOptions != null, onDismiss = { o.songOptions = null }) {
        if (song != null) {
            val fresh = idx[song.uri] ?: song
            SheetHeader(fresh)
            SheetAction(Icons.Rounded.SkipNext, "Play next") { c.player.playNext(listOf(fresh)); o.songOptions = null }
            SheetAction(Icons.Rounded.QueueMusic, "Add to queue") { c.player.addToQueue(listOf(fresh)); o.songOptions = null }
            SheetAction(Icons.Rounded.PlaylistAdd, "Add to playlist") { o.addToPlaylist = listOf(fresh); o.songOptions = null }
            SheetAction(if (fresh.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (fresh.isFavorite) "Remove from favorites" else "Add to favorites") {
                c.player.toggleFavorite(fresh); o.songOptions = null
            }
            SheetAction(Icons.Rounded.Album, "Go to album") { o.songOptions = null; onNavigate(Routes.collection(CollectionType.ALBUM, fresh.albumKey)) }
            SheetAction(Icons.Rounded.Person, "Go to artist") { o.songOptions = null; onNavigate(Routes.collection(CollectionType.ARTIST, fresh.artist)) }
            SheetAction(Icons.Rounded.Info, "Song info") { o.songInfo = fresh; o.songOptions = null }
            SheetAction(Icons.Rounded.Share, "Share") { shareSong(ctx, fresh); o.songOptions = null }
            SheetAction(Icons.Rounded.VisibilityOff, "Hide from library") {
                scope.launch(Dispatchers.IO) { c.db.userData().setHidden(fresh.uri, true) }
                o.songOptions = null
            }
        }
    }
}

fun shareSong(ctx: Context, song: Song) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = song.mimeType.ifBlank { "audio/*" }
        putExtra(Intent.EXTRA_STREAM, Uri.parse(song.uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(i, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun AddToPlaylistSheet(o: OverlayController, c: AppContainer) {
    val songs = rememberLastNonNull(o.addToPlaylist)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by c.playlists.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    GlassBottomSheet(visible = o.addToPlaylist != null, onDismiss = { o.addToPlaylist = null }) {
        Text("Add to playlist", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        SheetAction(Icons.Rounded.Add, "New playlist") { o.newPlaylist = songs; o.addToPlaylist = null }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(playlists, key = { it.id }) { p ->
                SheetAction(Icons.Rounded.QueueMusic, "${p.name}  ·  ${p.songCount}") {
                    scope.launch { c.playlists.addSongs(p.id, songs.orEmpty().map { it.uri }) }
                    Toast.makeText(ctx, "Added to ${p.name}", Toast.LENGTH_SHORT).show()
                    o.addToPlaylist = null
                }
            }
        }
    }
}

@Composable
private fun NewPlaylistDialog(o: OverlayController, c: AppContainer) {
    val songs = o.newPlaylist ?: return
    var name by remember(songs) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { o.newPlaylist = null },
        title = { Text("New playlist") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = {
                c.scope.launch { c.playlists.create(name, songs.map { it.uri }) }
                o.newPlaylist = null
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { o.newPlaylist = null }) { Text("Cancel") } },
    )
}

@Composable
private fun SleepTimerSheet(o: OverlayController, c: AppContainer) {
    val st by c.sleepTimer.state.collectAsStateWithLifecycle()
    GlassBottomSheet(visible = o.sleepTimer, onDismiss = { o.sleepTimer = false }) {
        Text("Sleep timer", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        if (st.active) {
            val label = if (st.endOfTrack) "Stopping after this song" else "Stopping in ${formatDuration(st.remainingMs)}"
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            SheetAction(Icons.Rounded.Timer, "Turn off timer") { c.sleepTimer.cancel(); o.sleepTimer = false }
        }
        LazyColumn {
            items(listOf(5, 10, 15, 30, 45, 60, 90, 120)) { m ->
                SheetAction(Icons.Rounded.Timer, "$m minutes") { c.sleepTimer.start(m); o.sleepTimer = false }
            }
            item { SheetAction(Icons.Rounded.Timer, "End of current song") { c.sleepTimer.startEndOfTrack(); o.sleepTimer = false } }
        }
    }
}

@Composable
private fun SpeedSheet(o: OverlayController, c: AppContainer) {
    val state by c.player.state.collectAsStateWithLifecycle()
    var speed by remember(state.speed) { mutableFloatStateOf(state.speed) }
    var pitch by remember(state.pitch) { mutableFloatStateOf(state.pitch) }
    val on = MaterialTheme.colorScheme.onSurface
    val colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = on.copy(alpha = 0.25f))
    GlassBottomSheet(visible = o.speed, onDismiss = { o.speed = false }) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Playback speed  ${"%.2f".format(speed)}×", style = MaterialTheme.typography.titleMedium, color = on)
            Slider(value = speed, onValueChange = { speed = it; c.player.setSpeed(speed, pitch) }, valueRange = 0.5f..2f, colors = colors)
            Text("Pitch  ${"%.2f".format(pitch)}×", style = MaterialTheme.typography.titleMedium, color = on)
            Slider(value = pitch, onValueChange = { pitch = it; c.player.setSpeed(speed, pitch) }, valueRange = 0.5f..2f, colors = colors)
            TextButton(onClick = { speed = 1f; pitch = 1f; c.player.setSpeed(1f, 1f) }) { Text("Reset") }
        }
    }
}

@Composable
private fun SongInfoDialog(o: OverlayController) {
    val s = o.songInfo ?: return
    AlertDialog(
        onDismissRequest = { o.songInfo = null },
        title = { Text(s.title) },
        text = {
            Column {
                listOf(
                    "Artist" to s.artist, "Album" to s.album, "Genre" to s.genre.ifBlank { "—" },
                    "Year" to (if (s.year > 0) s.year.toString() else "—"), "Duration" to formatDuration(s.durationMs),
                    "Size" to "%.1f MB".format(s.sizeBytes / 1_048_576.0), "Format" to s.mimeType, "Folder" to s.folderPath,
                ).forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { o.songInfo = null }) { Text("Close") } },
    )
}

@Composable
private fun SortSheet(o: OverlayController) {
    val req = rememberLastNonNull(o.sort)
    GlassBottomSheet(visible = o.sort != null, onDismiss = { o.sort = null }) {
        if (req != null) {
            Text("Sort by", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            val labels = mapOf(
                SortField.TITLE to "Title", SortField.ARTIST to "Artist", SortField.ALBUM to "Album",
                SortField.DATE_ADDED to "Date added", SortField.DURATION to "Duration", SortField.PLAY_COUNT to "Play count", SortField.YEAR to "Year",
            )
            LazyColumn {
                items(SortField.entries.toList()) { f ->
                    val sel = f == req.field
                    SheetAction(
                        if (sel) Icons.Rounded.Check else Icons.Rounded.SwapVert, labels[f] ?: f.name,
                        onClick = { req.onChange(f, req.asc); o.sort = if (f == req.field) req else SortRequest(f, req.asc, req.onChange) },
                        tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                item {
                    SheetAction(Icons.Rounded.SwapVert, if (req.asc) "Ascending  (tap for descending)" else "Descending  (tap for ascending)", onClick = {
                        req.onChange(req.field, !req.asc); o.sort = SortRequest(req.field, !req.asc, req.onChange)
                    })
                }
            }
        }
    }
}
