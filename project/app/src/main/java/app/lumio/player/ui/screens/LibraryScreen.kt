package app.lumio.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.model.Song
import app.lumio.player.data.repo.sortSongs
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ListLayout
import app.lumio.player.data.settings.SortField
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import app.lumio.player.util.firstLetter
import app.lumio.player.util.sortKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TABS = listOf("Songs", "Albums", "Artists", "Genres", "Folders", "Playlists", "Favorites")

@Composable
internal fun HeaderPill(content: @Composable RowScope.() -> Unit) {
    GlassSurface(shape = RoundedCornerShape(50)) { Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, content = content) }
}

@Composable
fun LibraryScreen(initialTab: Int, onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val on = MaterialTheme.colorScheme.onSurface
    val overlays = LocalOverlays.current
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val songs by c.library.songs.collectAsStateWithLifecycle()
    val albums by c.library.albums.collectAsStateWithLifecycle()
    val artists by c.library.artists.collectAsStateWithLifecycle()
    val genres by c.library.genres.collectAsStateWithLifecycle()
    val folders by c.library.folders.collectAsStateWithLifecycle()
    val playlists by c.playlists.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val player by c.player.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, TABS.lastIndex)) }
    var field by remember(s.songSort) { mutableStateOf(s.songSort) }
    var asc by remember(s.songSortAsc) { mutableStateOf(s.songSortAsc) }
    val sel = rememberSelection()

    val sorted by produceState(songs, songs, field, asc, s.ignoreArticles) {
        value = withContext(Dispatchers.Default) { sortSongs(songs, field, asc, s.ignoreArticles) }
    }
    val favorites = remember(songs) { songs.filter { it.isFavorite }.sortedBy { it.title.lowercase() } }

    Column(Modifier.fillMaxSize()) {
        if (sel.active) {
            SelectionBar(sel, if (tab == 6) favorites else sorted, Modifier.statusBarsPadding())
        } else {
            ScreenHeader("Library") {
                HeaderPill {
                    if (tab == 0) IconButton(onClick = {
                        overlays.sort = SortRequest(field, asc) { f, a ->
                            field = f; asc = a
                            scope.launch { c.settings.update { copy(songSort = f, songSortAsc = a) } }
                        }
                    }) { Icon(Icons.Rounded.Sort, "Sort", tint = on) }
                    if (tab != 0 && tab != 6) IconButton(onClick = {
                        scope.launch { c.settings.update { copy(libraryLayout = if (libraryLayout == ListLayout.GRID) ListLayout.LIST else ListLayout.GRID) } }
                    }) { Icon(if (s.libraryLayout == ListLayout.GRID) Icons.Rounded.ViewList else Icons.Rounded.GridView, "Change layout", tint = on) }
                    IconButton(onClick = { c.player.playSongs(songs, shuffle = true, source = "Library") }) { Icon(Icons.Rounded.Shuffle, "Shuffle all", tint = on) }
                }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(TABS.size) { i -> GlassChip(TABS[i], tab == i, { tab = i; sel.clear() }) }
        }
        Spacer(Modifier.height(12.dp))

        val grid = s.libraryLayout == ListLayout.GRID
        when (tab) {
            0 -> SongsList(sorted, field, asc, s, sel, player.current?.uri, player.isPlaying, "Songs", onMore = { overlays.songOptions = it })
            6 -> SongsList(favorites, SortField.TITLE, true, s, sel, player.current?.uri, player.isPlaying, "Favorites", showScroller = false, onMore = { overlays.songOptions = it })
            1 -> CollectionList(albums, grid, s.gridColumns, { it.key }, { it.name }, { "${it.artist} · ${it.songCount}" }, { it.artSongUri }, RoundedCornerShape(24.dp)) { onNavigate(Routes.collection(CollectionType.ALBUM, it.key)) }
            2 -> CollectionList(artists, grid, s.gridColumns, { it.name }, { it.name }, { "${it.albumCount} albums · ${it.songCount} songs" }, { it.artSongUri }, CircleShape) { onNavigate(Routes.collection(CollectionType.ARTIST, it.name)) }
            3 -> CollectionList(genres, grid, s.gridColumns, { it.name }, { it.name }, { "${it.songCount} songs" }, { it.artSongUri }, RoundedCornerShape(24.dp)) { onNavigate(Routes.collection(CollectionType.GENRE, it.name)) }
            4 -> CollectionList(folders, grid, s.gridColumns, { it.path }, { it.name }, { "${it.songCount} songs" }, { it.artSongUri }, RoundedCornerShape(24.dp)) { onNavigate(Routes.collection(CollectionType.FOLDER, it.path)) }
            5 -> {
                LazyVerticalGrid(
                    GridCells.Fixed(s.gridColumns.coerceIn(2, 4)),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = DockClearance),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        GlassSurface(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(24.dp), onClick = { overlays.newPlaylist = emptyList() }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Add, null, tint = on, modifier = Modifier.size(40.dp))
                                Text("New playlist", style = MaterialTheme.typography.titleSmall, color = on)
                            }
                        }
                    }
                    items(playlists, key = { it.id }) { p ->
                        CollectionCard(p.name, "${p.songCount} songs", p.artSongUri, { onNavigate(Routes.collection(CollectionType.PLAYLIST, p.id.toString())) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsList(
    songs: List<Song>,
    field: SortField,
    asc: Boolean,
    s: AppSettings,
    sel: SelectionState,
    currentUri: String?,
    isPlaying: Boolean,
    source: String,
    showScroller: Boolean = true,
    onMore: (Song) -> Unit,
) {
    val c = LocalContext.current.container
    val listState = rememberLazyListState()
    val letters = remember(songs, field, asc, s.ignoreArticles) {
        if (!asc || field !in listOf(SortField.TITLE, SortField.ARTIST, SortField.ALBUM)) null
        else {
            val m = HashMap<Char, Int>()
            songs.forEachIndexed { i, sg ->
                val text = when (field) { SortField.TITLE -> sg.title; SortField.ARTIST -> sg.artist; else -> sg.album }
                m.putIfAbsent(firstLetter(sortKey(text, s.ignoreArticles)), i)
            }
            m
        }
    }
    if (songs.isEmpty()) {
        EmptyState("Nothing here yet", "Songs will appear here once your library is scanned.")
        return
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = DockClearance)) {
            songItems(songs, currentUri, isPlaying, s.compactRows, sel, onPlayAt = { c.player.playSongs(songs, it, source = source) }, onMore = onMore)
        }
        if (showScroller && letters != null && songs.size > 40) FastScroller(letters, listState, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun <T> CollectionList(
    items: List<T>,
    grid: Boolean,
    columns: Int,
    key: (T) -> Any,
    title: (T) -> String,
    subtitle: (T) -> String,
    art: (T) -> String?,
    shape: Shape,
    onClick: (T) -> Unit,
) {
    if (items.isEmpty()) { EmptyState("Nothing here yet", "Scan your library to fill this tab."); return }
    if (grid) {
        LazyVerticalGrid(
            GridCells.Fixed(columns.coerceIn(2, 4)),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = DockClearance),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(items, key = key) { it -> CollectionCard(title(it), subtitle(it), art(it), { onClick(it) }, shape = shape) }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = DockClearance)) {
            items(items, key = key) { it ->
                val on = MaterialTheme.colorScheme.onSurface
                Row(
                    Modifier.fillMaxWidth().clickable { onClick(it) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(art(it), Modifier.size(56.dp), shape = if (shape == CircleShape) CircleShape else RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title(it), style = MaterialTheme.typography.bodyLarge, color = on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle(it), style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
