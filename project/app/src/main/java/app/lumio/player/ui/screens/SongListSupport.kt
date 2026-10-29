package app.lumio.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lumio.player.container
import app.lumio.player.data.model.Song
import app.lumio.player.ui.components.GlassSurface
import app.lumio.player.ui.components.LocalOverlays
import app.lumio.player.ui.components.SongRow
import androidx.compose.ui.platform.LocalContext

/** Multi-select state shared by Library + Collection screens. */
@Stable
class SelectionState {
    var ids by mutableStateOf(setOf<String>())
    val active: Boolean get() = ids.isNotEmpty()
    fun toggle(uri: String) { ids = if (uri in ids) ids - uri else ids + uri }
    fun clear() { ids = emptySet() }
}

@Composable
fun rememberSelection() = remember { SelectionState() }

/** Glass bar shown while selecting. */
@Composable
fun SelectionBar(sel: SelectionState, allSongs: List<Song>, modifier: Modifier = Modifier, extra: @Composable RowScope.() -> Unit = {}) {
    val c = LocalContext.current.container
    val overlays = LocalOverlays.current
    val on = MaterialTheme.colorScheme.onSurface
    BackHandler(enabled = sel.active) { sel.clear() }
    fun chosen() = allSongs.filter { it.uri in sel.ids }
    GlassSurface(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { sel.clear() }) { Icon(Icons.Rounded.Close, "Clear selection", tint = on) }
            Text("${sel.ids.size} selected", style = MaterialTheme.typography.titleSmall, color = on, modifier = Modifier.weight(1f))
            IconButton(onClick = { sel.ids = allSongs.map { it.uri }.toSet() }) { Icon(Icons.Rounded.SelectAll, "Select all", tint = on) }
            IconButton(onClick = { c.player.playSongs(chosen()); sel.clear() }) { Icon(Icons.Rounded.PlayArrow, "Play", tint = on) }
            IconButton(onClick = { c.player.playNext(chosen()); sel.clear() }) { Icon(Icons.Rounded.SkipNext, "Play next", tint = on) }
            IconButton(onClick = { c.player.addToQueue(chosen()); sel.clear() }) { Icon(Icons.Rounded.QueueMusic, "Add to queue", tint = on) }
            IconButton(onClick = { overlays.addToPlaylist = chosen(); sel.clear() }) { Icon(Icons.Rounded.PlaylistAdd, "Add to playlist", tint = on) }
            extra()
        }
    }
}

/** Standard flat song rows with selection, now-playing highlight and options. */
fun LazyListScope.songItems(
    songs: List<Song>,
    currentUri: String?,
    isPlaying: Boolean,
    compact: Boolean,
    sel: SelectionState,
    onPlayAt: (Int) -> Unit,
    onMore: (Song) -> Unit,
) {
    itemsIndexed(songs, key = { _, s -> s.uri }) { index, song ->
        SongRow(
            song = song,
            isCurrent = song.uri == currentUri,
            isPlaying = isPlaying,
            compact = compact,
            selected = song.uri in sel.ids,
            selectionMode = sel.active,
            onClick = { if (sel.active) sel.toggle(song.uri) else onPlayAt(index) },
            onLongClick = { sel.toggle(song.uri) },
            onMore = { onMore(song) },
        )
    }
}
