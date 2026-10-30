package app.lumio.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.lumio.player.container
import app.lumio.player.data.model.LyricLine
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ArtworkShape
import app.lumio.player.playback.PlayerState
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import app.lumio.player.ui.theme.LocalGlass
import app.lumio.player.ui.theme.LocalMotion
import app.lumio.player.util.formatDuration
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Expandable full-screen player. progress: 0 = hidden (mini player visible), 1 = fully open. docs/04 4.7 */
@Composable
fun NowPlayingSheet(progress: Animatable<Float, AnimationVector1D>, onNavigate: (String) -> Unit) {
    val visible by remember { derivedStateOf { progress.value > 0f } }
    val scope = rememberCoroutineScope()
    val motion = LocalMotion.current
    BackHandler(enabled = visible) { scope.launch { progress.animateTo(0f, motion.smooth()) } }
    if (!visible) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val hPx = constraints.maxHeight.toFloat()
        val drag = rememberDraggableState { d -> scope.launch { progress.snapTo((progress.value - d / hPx).coerceIn(0f, 1f)) } }
        val dragModifier = Modifier.draggable(
            orientation = Orientation.Vertical,
            state = drag,
            onDragStopped = { v ->
                scope.launch {
                    val target = if (v > 1200f || progress.value < 0.6f) 0f else 1f
                    progress.animateTo(target, motion.smooth())
                }
            },
        )
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = (1f - progress.value) * hPx
                val r = ((1f - progress.value).coerceAtLeast(0f)) * 28.dp.toPx()
                shape = RoundedCornerShape(topStart = r.coerceAtLeast(0f), topEnd = r.coerceAtLeast(0f), bottomEnd = 0f, bottomStart = 0f)
                clip = true
            }
        ) {
            GlassSurface(
                Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                style = LocalGlass.current.copy(opacity = 0.05f, border = 0f, specular = 0f, refraction = 0f),
            ) {
                NowPlayingContent(dragModifier, onCollapse = { scope.launch { progress.animateTo(0f, motion.smooth()) } }, onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun NowPlayingContent(dragModifier: Modifier, onCollapse: () -> Unit, onNavigate: (String) -> Unit) {
    val c = LocalContext.current.container
    val st by c.player.state.collectAsStateWithLifecycle()
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val sleep by c.sleepTimer.state.collectAsStateWithLifecycle()
    val overlays = LocalOverlays.current
    val scope = rememberCoroutineScope()
    val song = st.current
    val on = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary

    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var lyricsTick by remember { mutableIntStateOf(0) }
    val lyrics by produceState<List<LyricLine>?>(null, song?.uri, lyricsTick) {
        value = if (song != null) c.lyrics.load(song) else null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && song != null) scope.launch { if (c.lyrics.import(song, uri)) lyricsTick++ }
    }

    if (song == null) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Collapse", onCollapse)
            EmptyState("Nothing playing", "Pick a song to start.")
        }
        return
    }

    @Composable fun Header() {
        Row(dragModifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Collapse", onCollapse)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = on.copy(alpha = 0.7f))
                Text(st.sourceLabel.ifBlank { song.album }, style = MaterialTheme.typography.labelMedium, color = on, maxLines = 1)
                if (sleep.active) {
                    Text(
                        if (sleep.endOfTrack) "Sleep: after this song" else "Sleep in ${formatDuration(sleep.remainingMs)}",
                        style = MaterialTheme.typography.labelSmall, color = accent,
                    )
                }
            }
            IconButton(onClick = { overlays.songOptions = song }) { Icon(Icons.Rounded.MoreVert, "More options", tint = on) }
        }
    }

    @Composable fun ArtOrLyrics(modifier: Modifier) {
        if (showLyrics) {
            LyricsView(
                lines = lyrics, position = c.player.position, sizeSp = s.lyricsSizeSp, center = s.lyricsCenter,
                onSeek = { c.player.seekTo(it) }, onImport = { importLauncher.launch(arrayOf("*/*")) },
                modifier = modifier,
            )
        } else {
            BoxWithConstraints(modifier.then(dragModifier), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight)
                ArtworkPager(
                    st = st, shape = s.artworkShape, modifier = Modifier.size(side),
                    onSkipTo = { c.player.skipTo(it) },
                    onDoubleTap = { c.player.toggleFavorite(song) },
                    onTap = { showLyrics = true },
                )
            }
        }
    }

    @Composable fun Info() {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.headlineSmall, color = on, maxLines = 1, modifier = Modifier.basicMarquee())
                Text("${song.artist} · ${song.album}", style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.75f), maxLines = 1)
            }
            FavoriteButton(song.uri, song.isFavorite) { c.player.toggleFavorite(song) }
        }
        PlayerSeekSection(c.player.position, st.durationMs, onSeek = { c.player.seekTo(it) }, modifier = Modifier.padding(top = 4.dp))
    }

    @Composable fun Transport() {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { c.player.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (st.shuffle) accent else on.copy(alpha = 0.8f)) }
            GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", { c.player.previous() }, size = 56.dp)
            GlassSurface(Modifier.size(76.dp), shape = CircleShape, tint = accent.copy(alpha = 0.35f), onClick = { c.player.togglePlay() }, contentAlignment = Alignment.Center) {
                PlayPauseIcon(st.isPlaying, on, 40.dp)
            }
            GlassIconButton(Icons.Rounded.SkipNext, "Next", { c.player.next() }, size = 56.dp)
            IconButton(onClick = { c.player.cycleRepeat() }) {
                Icon(
                    if (st.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
                    tint = if (st.repeat != Player.REPEAT_MODE_OFF) accent else on.copy(alpha = 0.8f),
                )
            }
        }
    }

    @Composable fun Actions() {
        GlassSurface(Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(50)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { showLyrics = !showLyrics }) { Icon(Icons.Rounded.Lyrics, "Lyrics", tint = if (showLyrics) accent else on) }
                IconButton(onClick = { overlays.queue = true }) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = on) }
                IconButton(onClick = { overlays.sleepTimer = true }) { Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (sleep.active) accent else on) }
                IconButton(onClick = { onNavigate(Routes.EQUALIZER) }) { Icon(Icons.Rounded.Equalizer, "Equalizer", tint = on) }
                IconButton(onClick = { overlays.speed = true }) { Icon(Icons.Rounded.Speed, "Speed", tint = if (st.speed != 1f || st.pitch != 1f) accent else on) }
                IconButton(onClick = { shareSong(c.app, song) }) { Icon(Icons.Rounded.Share, "Share", tint = on) }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ArtOrLyrics(Modifier.weight(0.9f).fillMaxHeight())
                Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Header(); Info(); Transport(); Actions()
                }
            }
        } else {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 4.dp)) {
                Header()
                ArtOrLyrics(Modifier.weight(1f).fillMaxWidth())
                Info(); Transport(); Actions()
            }
        }
    }
}

@Composable
private fun ArtworkPager(
    st: PlayerState,
    shape: ArtworkShape,
    modifier: Modifier,
    onSkipTo: (Int) -> Unit,
    onDoubleTap: () -> Unit,
    onTap: () -> Unit,
) {
    val motion = LocalMotion.current
    val accent = MaterialTheme.colorScheme.primary
    val queue = st.queue
    val pager = rememberPagerState(initialPage = st.index.coerceAtLeast(0)) { queue.size.coerceAtLeast(1) }
    val indexNow by rememberUpdatedState(st.index)
    LaunchedEffect(st.index) {
        if (st.index >= 0 && st.index < queue.size && st.index != pager.settledPage && !pager.isScrollInProgress) pager.animateScrollToPage(st.index)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { p -> if (p != indexNow && p in queue.indices) onSkipTo(p) }
    }
    val scale by animateFloatAsState(if (st.isPlaying) 1f else 0.88f, motion.gentle(), label = "artScale")
    val circle = shape == ArtworkShape.CIRCLE
    val rot = remember { Animatable(0f) }
    LaunchedEffect(st.isPlaying, circle) {
        if (st.isPlaying && circle) {
            while (true) {
                rot.snapTo(rot.value % 360f)
                rot.animateTo(rot.value + 360f, tween(14000, easing = LinearEasing))
            }
        }
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), center = center, radius = size.minDimension * 0.8f),
                    radius = size.minDimension * 0.8f,
                )
            },
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), pageSpacing = 16.dp, beyondViewportPageCount = 1) { page ->
            val item = queue.getOrNull(page)
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { if (circle) rotationZ = rot.value }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }, onTap = { onTap() }) }
            ) {
                ArtworkImage(item?.uri, Modifier.fillMaxSize(), artShape(shape, 28.dp))
            }
        }
    }
}

@Composable
private fun FavoriteButton(songUri: String, isFav: Boolean, onToggle: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val on = MaterialTheme.colorScheme.onSurface
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    var prev by remember(songUri) { mutableStateOf(isFav) }
    LaunchedEffect(isFav, songUri) {
        if (isFav && !prev) {
            launch { scale.animateTo(1.5f, tween(120)); scale.animateTo(1f, spring(0.4f, 400f)) }
            burst.snapTo(0f)
            burst.animateTo(1f, tween(450))
        }
        prev = isFav
    }
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val b = burst.value
            if (b > 0.01f && b < 0.99f) {
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0).toFloat()
                    val r = size.minDimension * (0.30f + 0.20f * b)
                    drawCircle(accent.copy(alpha = 1f - b), radius = 3.dp.toPx(), center = center + Offset(cos(a) * r, sin(a) * r))
                }
            }
        }
        Icon(
            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite",
            tint = if (isFav) accent else on,
            modifier = Modifier.size(30.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
    }
}
