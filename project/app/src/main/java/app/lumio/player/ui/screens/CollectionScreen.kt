package app.lumio.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.model.Mood
import app.lumio.player.data.scan.FolderUtils
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import app.lumio.player.util.formatTotal
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun CollectionScreen(type: CollectionType, key: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val on = MaterialTheme.colorScheme.onSurface
    val overlays = LocalOverlays.current
    val songs by remember(type, key) { c.collections.songsFor(type, key) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val player by c.player.state.collectAsStateWithLifecycle()
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val allAlbums by c.library.albums.collectAsStateWithLifecycle()
    val playlistId = key.toLongOrNull() ?: -1L
    val playlistName by remember(type, key) { if (type == CollectionType.PLAYLIST) c.playlists.nameOf(playlistId) else flowOf(null) }.collectAsStateWithLifecycle(initialValue = null)
    val sel = rememberSelection()
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    val title = when (type) {
        CollectionType.ALBUM -> songs.firstOrNull()?.album ?: key.substringBefore('|')
        CollectionType.ARTIST, CollectionType.GENRE -> key
        CollectionType.FOLDER -> FolderUtils.displayName(key)
        CollectionType.PLAYLIST -> playlistName ?: "Playlist"
        CollectionType.FAVORITES -> "Favorites"
        CollectionType.MOST_PLAYED -> "Most played"
        CollectionType.RECENT -> "Recently played"
        CollectionType.RECENTLY_ADDED -> "Recently added"
        CollectionType.FORGOTTEN -> "Forgotten gems"
        CollectionType.MOOD -> Mood.entries.firstOrNull { it.name == key }?.label ?: "Mix"
        CollectionType.ALL -> "All songs"
    }
    val total = songs.sumOf { it.durationMs }
    val subtitle = buildString {
        if (type == CollectionType.ALBUM) songs.firstOrNull()?.let { append(it.artist).append(" · ") }
        append("${songs.size} songs · ${formatTotal(total)}")
    }
    val label = title

    Column(Modifier.fillMaxSize()) {
        if (sel.active) {
            SelectionBar(sel, songs, Modifier.statusBarsPadding()) {
                if (type == CollectionType.PLAYLIST) {
                    IconButton(onClick = {
                        val ids = sel.ids.toList()
                        scope.launch { ids.forEach { c.playlists.removeSong(playlistId, it) } }
                        sel.clear()
                    }) { Icon(Icons.Rounded.Delete, "Remove from playlist", tint = on) }
                }
            }
        } else {
            ScreenHeader(title = "", onBack = onBack) {
                if (type == CollectionType.PLAYLIST) {
                    HeaderPill {
                        IconButton(onClick = { renameDialog = true }) { Icon(Icons.Rounded.Edit, "Rename", tint = on) }
                        IconButton(onClick = { deleteDialog = true }) { Icon(Icons.Rounded.Delete, "Delete playlist", tint = on) }
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = DockClearance)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArtworkImage(songs.firstOrNull()?.uri, Modifier.size(220.dp), RoundedCornerShape(28.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = on, textAlign = TextAlign.Center, maxLines = 2)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.75f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassButton("Play", { c.player.playSongs(songs, 0, source = label) }, icon = Icons.Rounded.PlayArrow, filled = true)
                        GlassButton("Shuffle", { c.player.playSongs(songs, shuffle = true, source = label) }, icon = Icons.Rounded.Shuffle)
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            if (type == CollectionType.ARTIST) {
                val theirs = allAlbums.filter { it.artist.equals(key, true) }
                if (theirs.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(theirs, key = { it.key }) { a -> CollectionCard(a.name, "${a.songCount} songs", a.artSongUri, { onNavigate(Routes.collection(CollectionType.ALBUM, a.key)) }, Modifier.width(140.dp)) }
                        }
                    }
                    item { SectionHeader("Songs") }
                }
            }
            if (songs.isEmpty()) item { EmptyState("Empty", "Nothing in this list yet.", Modifier.height(240.dp)) }
            songItems(songs, player.current?.uri, player.isPlaying, s.compactRows, sel, onPlayAt = { c.player.playSongs(songs, it, source = label) }, onMore = { overlays.songOptions = it })
        }
    }

    if (renameDialog) {
        var name by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { renameDialog = false }, title = { Text("Rename playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { scope.launch { c.playlists.rename(playlistId, name) }; renameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel") } },
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false }, title = { Text("Delete playlist?") },
            text = { Text("\"$title\" will be removed. Your songs are not deleted.") },
            confirmButton = { TextButton(onClick = { scope.launch { c.playlists.delete(playlistId) }; deleteDialog = false; onBack() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } },
        )
    }
}
