package app.lumio.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Size
import androidx.compose.ui.graphics.drawScope.drawRoundRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.data.model.Song
import app.lumio.player.data.settings.DockStyle
import app.lumio.player.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sin

/** Floating glass dock. STACKED = mini player above a 3-button capsule. CLASSIC = Home | mini player | Search (Echo-like). */
@Composable
fun GlassDock(
    currentRoute: String?,
    dockStyle: DockStyle,
    song: Song?,
    isPlaying: Boolean,
    position: StateFlow<Long>,
    durationMs: Long,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onSearch: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val lastSong = rememberLastNonNull(song)
    val route = currentRoute.orEmpty()
    val mini: @Composable (Modifier) -> Unit = { m ->
        if (lastSong != null) MiniPlayer(lastSong, isPlaying, position, durationMs, onOpenPlayer, onToggle, onNext, onPrevious, m)
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dockStyle == DockStyle.STACKED) {
            AnimatedVisibility(
                visible = song != null,
                enter = slideInVertically(motion.smooth()) { it / 2 } + fadeIn(),
                exit = slideOutVertically(motion.smooth()) { it / 2 } + fadeOut(),
            ) { mini(Modifier.fillMaxWidth()) }
            GlassSurface(shape = RoundedCornerShape(50)) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DockItem(Icons.Rounded.Home, Icons.Outlined.Home, "Home", route.startsWith("home"), onHome)
                    DockItem(Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "Library", route.startsWith("library"), onLibrary)
                    DockItem(Icons.Rounded.Search, Icons.Outlined.Search, "Search", route.startsWith("search"), onSearch)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.Home, "Home", onHome, size = 50.dp, selected = route.startsWith("home"))
                Box(Modifier.weight(1f)) { if (song != null) mini(Modifier.fillMaxWidth()) }
                GlassIconButton(Icons.Rounded.Search, "Search", onSearch, size = 50.dp, selected = route.startsWith("search"))
            }
        }
    }
}

@Composable
private fun DockItem(filled: ImageVector, outlined: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val wash by animateColorAsState(if (selected) accent.copy(alpha = 0.32f) else Color.Transparent, LocalMotion.current.tween<Color>(220), label = "dock")
    Box(
        Modifier.size(56.dp).bounceClick(onClick = onClick).background(wash, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (selected) filled else outlined, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    position: StateFlow<Long>,
    durationMs: Long,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = MaterialTheme.colorScheme.onSurface
    GlassSurface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(50),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { if (acc < -100f) onNext() else if (acc > 100f) onPrevious() },
                        onDragCancel = {},
                        onHorizontalDrag = { _, d -> acc += d },
                    )
                }
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(song.uri, Modifier.size(44.dp), CircleShape)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, color = on, maxLines = 1, modifier = Modifier.basicMarquee())
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1)
            }
            IconButton(onClick = onToggle) { PlayPauseIcon(isPlaying, on, 30.dp) }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Next", tint = on) }
        }
        MiniProgress(position, durationMs, Modifier.align(Alignment.BottomCenter).padding(horizontal = 26.dp))
        if (isPlaying) MusicBeatLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = 26.dp).height(24.dp), isPlaying)
    }
}

@Composable
private fun MusicBeatLine(modifier: Modifier = Modifier, playing: Boolean = true) {
    val t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                withFrameNanos { _: Long -> t += 0.08f }
                delay(16)
            }
        }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val accent = MaterialTheme.colorScheme.primary
        val barW = 2f
        val barGap = 3f
        val count = (w / (barW + barGap)).toInt().coerceAtLeast(4).coerceAtMost(12)
        repeat(count) { i ->
            val x = i.toFloat() * (barW + barGap) + barGap / 2
            val beat = abs(sin(t + i * 0.7f)).coerceIn(0.3f, 1f)
            val barH = h * beat
            val y = (h - barH) / 2f
            drawRoundRect(
                accent.copy(alpha = 0.85f),
                size = Size(barW, barH),
                topLeft = Offset(x, y),
                cornerRadius = CornerRadius(barW / 2, barW / 2),
            )
        }
    }
}

@Composable
private fun MiniProgress(position: StateFlow<Long>, durationMs: Long, modifier: Modifier) {
    val pos by position.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val f = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height)
        drawLine(accent, Offset(0f, size.height / 2), Offset(size.width * f, size.height / 2), strokeWidth = size.height)
    }
}
