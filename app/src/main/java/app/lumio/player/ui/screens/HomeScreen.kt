package app.lumio.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.model.Mood
import app.lumio.player.data.model.Song
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.DockStyle
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import kotlin.math.absoluteValue

private data class MixItem(val title: String, val subtitle: String, val art: String?, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    val lib = c.library
    val songs by lib.songs.collectAsStateWithLifecycle()
    val loaded by lib.isLoaded.collectAsStateWithLifecycle()
    val albums by lib.albums.collectAsStateWithLifecycle()
    val artists by lib.artists.collectAsStateWithLifecycle()
    val folders by lib.folders.collectAsStateWithLifecycle()
    val playlists by c.playlists.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val overlays = LocalOverlays.current

    // Hero list is computed once per visit so it doesn't reshuffle while you listen.
    var hero by remember { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(songs.isEmpty()) {
        hero = lib.recentlyPlayed(songs).take(12).ifEmpty { lib.mostPlayed(songs).take(12) }
            .ifEmpty { lib.recentlyAdded(songs).take(12) }.ifEmpty { songs.shuffled().take(12) }
    }

    val mixes = remember(songs, playlists) {
        buildList {
            val fav = lib.favorites(songs)
            add(MixItem("Favorites", "${fav.size} songs", fav.firstOrNull()?.uri, Icons.Rounded.Favorite) { onNavigate(Routes.collection(CollectionType.FAVORITES)) })
            val mp = lib.mostPlayed(songs)
            add(MixItem("Most played", "${mp.size} songs", mp.firstOrNull()?.uri, Icons.Rounded.TrendingUp) { onNavigate(Routes.collection(CollectionType.MOST_PLAYED)) })
            add(MixItem("Recently added", "New in library", lib.recentlyAdded(songs).firstOrNull()?.uri, Icons.Rounded.NewReleases) { onNavigate(Routes.collection(CollectionType.RECENTLY_ADDED)) })
            add(MixItem("Forgotten gems", "Replay the old ones", lib.forgotten(songs).firstOrNull()?.uri, Icons.Rounded.AutoAwesome) { onNavigate(Routes.collection(CollectionType.FORGOTTEN)) })
            add(MixItem("Shuffle all", "${songs.size} songs", songs.randomOrNull()?.uri, Icons.Rounded.Shuffle) { c.player.playSongs(songs, shuffle = true, source = "Shuffle all") })
            playlists.forEach { p -> add(MixItem(p.name, "${p.songCount} songs", p.artSongUri, Icons.Rounded.QueueMusic) { onNavigate(Routes.collection(CollectionType.PLAYLIST, p.id.toString())) }) }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = DockClearance)) {
        item { HomeHeader(s.dockStyle == DockStyle.CLASSIC, onNavigate) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Mood.entries.toList()) { m -> GlassChip(m.label, false, { onNavigate(Routes.collection(CollectionType.MOOD, m.name)) }) }
                item { GlassChip("Favorites", false, { onNavigate(Routes.collection(CollectionType.FAVORITES)) }, icon = Icons.Rounded.Favorite) }
                item { GlassChip("Shuffle all", false, { c.player.playSongs(songs, shuffle = true, source = "Shuffle all") }, icon = Icons.Rounded.Shuffle) }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (loaded && songs.isEmpty()) {
            item {
                EmptyState("No music yet", "Pick folders or scan your device to get started.", Modifier.height(380.dp)) {
                    GlassButton("Open scan settings", { onNavigate(Routes.FOLDERS) }, filled = true)
                }
            }
        } else {
            if (hero.isNotEmpty()) item { HeroPager(hero, onPlay = { i -> c.player.playSongs(hero, i, source = "Quick picks") }, onLong = { overlays.songOptions = it }) }
            item { SectionHeader("Your mixes & playlists", onClick = { onNavigate(Routes.library(5)) }) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(mixes) { m -> MixCard(m.title, m.subtitle, m.art, m.icon, m.onClick, Modifier.width(150.dp)) }
                }
            }
            val recent = lib.recentlyAdded(songs).take(20)
            if (recent.isNotEmpty()) {
                item { SectionHeader("Recently added", onClick = { onNavigate(Routes.collection(CollectionType.RECENTLY_ADDED)) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recent, key = { it.uri }) { sg ->
                            CollectionCard(sg.title, sg.artist, sg.uri, { c.player.playSongs(recent, recent.indexOf(sg), source = "Recently added") },
                                Modifier.width(140.dp), onLongClick = { overlays.songOptions = sg })
                        }
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("Albums", onClick = { onNavigate(Routes.library(1)) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(albums.take(30), key = { it.key }) { a -> CollectionCard(a.name, a.artist, a.artSongUri, { onNavigate(Routes.collection(CollectionType.ALBUM, a.key)) }, Modifier.width(140.dp)) }
                    }
                }
            }
            if (artists.isNotEmpty()) {
                item { SectionHeader("Artists", onClick = { onNavigate(Routes.library(2)) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(artists.take(30), key = { it.name }) { a -> CollectionCard(a.name, "${a.songCount} songs", a.artSongUri, { onNavigate(Routes.collection(CollectionType.ARTIST, a.name)) }, Modifier.width(120.dp), CircleShape) }
                    }
                }
            }
            if (folders.isNotEmpty()) {
                item { SectionHeader("Folders", onClick = { onNavigate(Routes.library(4)) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(folders.take(30), key = { it.path }) { f -> CollectionCard(f.name, "${f.songCount} songs", f.artSongUri, { onNavigate(Routes.collection(CollectionType.FOLDER, f.path)) }, Modifier.width(140.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(showLibrary: Boolean, onNavigate: (String) -> Unit) {
    val on = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Lumio", style = MaterialTheme.typography.displayLarge, color = on, modifier = Modifier.weight(1f))
        GlassSurface(shape = RoundedCornerShape(50)) {
            Row(Modifier.padding(horizontal = 4.dp)) {
                if (showLibrary) IconButton(onClick = { onNavigate(Routes.library()) }) { Icon(Icons.Rounded.LibraryMusic, "Library", tint = on) }
                IconButton(onClick = { onNavigate(Routes.HISTORY) }) { Icon(Icons.Rounded.History, "History", tint = on) }
                IconButton(onClick = { onNavigate(Routes.STATS) }) { Icon(Icons.Rounded.QueryStats, "Stats", tint = on) }
                IconButton(onClick = { onNavigate(Routes.EQUALIZER) }) { Icon(Icons.Rounded.Equalizer, "Equalizer", tint = on) }
                IconButton(onClick = { onNavigate(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Settings", tint = on) }
            }
        }
    }
}

@Composable
private fun HeroPager(items: List<Song>, onPlay: (Int) -> Unit, onLong: (Song) -> Unit) {
    val pager = rememberPagerState(initialPage = 0) { items.size }
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 52.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth().height(430.dp),
    ) { page ->
        val signed = (pager.currentPage - page) + pager.currentPageOffsetFraction
        val off = signed.absoluteValue.coerceIn(0f, 1f)
        val song = items.getOrNull(page) ?: return@HorizontalPager
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val sc = lerp(0.88f, 1f, 1f - off)
                scaleX = sc; scaleY = sc
                alpha = lerp(0.6f, 1f, 1f - off)
                rotationY = signed.coerceIn(-1f, 1f) * -6f
            }
        ) {
            HeroCard(song, onClick = { onPlay(page) }, onLong = { onLong(song) })
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun HeroCard(song: Song, onClick: () -> Unit, onLong: () -> Unit) {
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)).bounceClick(pressedScale = 0.97f, onLongClick = onLong, onClick = onClick),
    ) {
        ArtworkImage(song.uri, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(song.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
