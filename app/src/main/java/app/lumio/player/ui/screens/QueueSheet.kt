package app.lumio.player.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.AppContainer
import app.lumio.player.ui.components.ArtworkImage
import app.lumio.player.ui.components.GlassBottomSheet
import app.lumio.player.ui.components.OverlayController
import app.lumio.player.util.formatTotal
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Up-next queue with drag-reorder (Reorderable lib). Swipe-to-remove = task T15. */
@Composable
fun QueueSheet(o: OverlayController, c: AppContainer) {
    val st by c.player.state.collectAsStateWithLifecycle()
    var items by remember { mutableStateOf(st.queue) }
    LaunchedEffect(st.queue) { items = st.queue }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
        c.player.moveItem(from.index, to.index)
    }
    val on = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val keys = remember(items) {
        val counts = HashMap<String, Int>()
        items.map { s -> val n = counts[s.uri] ?: 0; counts[s.uri] = n + 1; "${s.uri}#$n" }
    }
    LaunchedEffect(o.queue) { if (o.queue && st.index > 0) runCatching { listState.scrollToItem(st.index) } }

    GlassBottomSheet(visible = o.queue, onDismiss = { o.queue = false }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Up next", style = MaterialTheme.typography.titleLarge, color = on)
                Text("${items.size} songs · ${formatTotal(items.sumOf { it.durationMs })}", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f))
            }
            TextButton(onClick = { o.newPlaylist = items; o.queue = false }) { Text("Save") }
            TextButton(onClick = { c.player.clearQueue(); o.queue = false }) { Text("Clear") }
        }
        LazyColumn(Modifier.weight(1f, fill = false), state = listState) {
            itemsIndexed(items, key = { i, _ -> keys[i] }) { index, song ->
                ReorderableItem(reorder, key = keys[index]) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
                    Row(
                        Modifier.fillMaxWidth().shadow(elevation)
                            .background(if (index == st.index) accent.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { c.player.skipTo(index) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.DragHandle, "Reorder", tint = on.copy(alpha = 0.6f), modifier = Modifier.draggableHandle().padding(8.dp).size(28.dp))
                        ArtworkImage(song.uri, Modifier.size(44.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = if (index == st.index) accent else on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { c.player.removeAt(index) }) { Icon(Icons.Rounded.Close, "Remove from queue", tint = on.copy(alpha = 0.7f)) }
                    }
                }
            }
        }
    }
}
