package app.lumio.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.repo.SearchResults
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FILTERS = listOf("All", "Songs", "Albums", "Artists", "Genres", "Folders")

@Composable
fun SearchScreen(onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    val on = MaterialTheme.colorScheme.onSurface
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    val overlays = LocalOverlays.current
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val player by c.player.state.collectAsStateWithLifecycle()
    val genres by c.library.genres.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val sel = rememberSelection()
    val recents = remember(s.recentSearches) { s.recentSearches.split('\n').filter { it.isNotBlank() } }

    val results by produceState(SearchResults(), query) {
        if (query.isBlank()) { value = SearchResults(); return@produceState }
        delay(120)
        value = withContext(Dispatchers.Default) { c.library.search(query) }
    }

    fun saveRecent(q: String) {
        if (q.trim().length < 2) return
        scope.launch { c.settings.update { copy(recentSearches = (listOf(q.trim()) + recentSearches.split('\n').filter { it.isNotBlank() && it != q.trim() }).take(10).joinToString("\n")) } }
    }

    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp), shape = RoundedCornerShape(50), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, "Search", tint = on)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search songs, albums, artists…", color = on.copy(alpha = 0.55f), style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = on),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { saveRecent(query); focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    )
                }
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear", tint = on) }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FILTERS.size) { i -> GlassChip(FILTERS[i], filter == i, { filter = i }) }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = DockClearance)) {
            if (query.isBlank()) {
                if (recents.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent searches", style = MaterialTheme.typography.titleMedium, color = on)
                            Text("Clear", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { scope.launch { c.settings.update { copy(recentSearches = "") } } })
                        }
                    }
                    items(recents) { r ->
                        Row(Modifier.fillMaxWidth().clickable { query = r }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, null, tint = on.copy(alpha = 0.6f)); Spacer(Modifier.width(14.dp)); Text(r, color = on, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (genres.isNotEmpty()) {
                    item { SectionHeader("Browse genres") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genres.take(24), key = { it.name }) { g -> GlassChip(g.name, false, { onNavigate(Routes.collection(CollectionType.GENRE, g.name)) }) }
                        }
                    }
                }
            } else if (results.isEmpty) {
                item { EmptyState("No results", "Try a different spelling or fewer words.", Modifier.height(300.dp)) }
            } else {
                if ((filter == 0 || filter == 1) && results.songs.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    songItems(results.songs.take(if (filter == 0) 8 else 200), player.current?.uri, player.isPlaying, s.compactRows, sel,
                        onPlayAt = { saveRecent(query); c.player.playSongs(results.songs, it, source = "Search") }, onMore = { overlays.songOptions = it })
                }
                if ((filter == 0 || filter == 2) && results.albums.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    items(results.albums, key = { "a" + it.key }) { a -> ResultRow(a.name, a.artist, a.artSongUri, false) { saveRecent(query); onNavigate(Routes.collection(CollectionType.ALBUM, a.key)) } }
                }
                if ((filter == 0 || filter == 3) && results.artists.isNotEmpty()) {
                    item { SectionHeader("Artists") }
                    items(results.artists, key = { "r" + it.name }) { a -> ResultRow(a.name, "${a.songCount} songs", a.artSongUri, true) { saveRecent(query); onNavigate(Routes.collection(CollectionType.ARTIST, a.name)) } }
                }
                if ((filter == 0 || filter == 4) && results.genres.isNotEmpty()) {
                    item { SectionHeader("Genres") }
                    items(results.genres, key = { "g" + it.name }) { g -> ResultRow(g.name, "${g.songCount} songs", g.artSongUri, false) { onNavigate(Routes.collection(CollectionType.GENRE, g.name)) } }
                }
                if ((filter == 0 || filter == 5) && results.folders.isNotEmpty()) {
                    item { SectionHeader("Folders") }
                    items(results.folders, key = { "f" + it.path }) { f -> ResultRow(f.name, "${f.songCount} songs", f.artSongUri, false) { onNavigate(Routes.collection(CollectionType.FOLDER, f.path)) } }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, art: String?, circle: Boolean, onClick: () -> Unit) {
    val on = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtworkImage(art, Modifier.size(52.dp), if (circle) CircleShape else RoundedCornerShape(14.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = on, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}
