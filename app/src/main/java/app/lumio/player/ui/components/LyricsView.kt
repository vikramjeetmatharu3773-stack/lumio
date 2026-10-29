package app.lumio.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.data.model.LyricLine
import app.lumio.player.ui.theme.LocalMotion
import kotlinx.coroutines.flow.StateFlow

/** Synced (.lrc) or plain lyrics. Empty -> import prompt. docs/04 4.7, docs/05 row 21. */
@Composable
fun LyricsView(
    lines: List<LyricLine>?,
    position: StateFlow<Long>,
    sizeSp: Int,
    center: Boolean,
    onSeek: (Long) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = MaterialTheme.colorScheme.onSurface
    if (lines.isNullOrEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No lyrics for this song", style = MaterialTheme.typography.titleMedium, color = on)
                GlassButton("Import .lrc file", onImport)
            }
        }
        return
    }
    val synced = lines.first().timeMs >= 0
    val posState = position.collectAsStateWithLifecycle()
    val active by remember(lines) {
        derivedStateOf { if (synced) lines.indexOfLast { it.timeMs <= posState.value }.coerceAtLeast(0) else -1 }
    }
    val listState = rememberLazyListState()
    val motion = LocalMotion.current
    LaunchedEffect(active) { if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0)) }
    LazyColumn(
        modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(lines) { i, line ->
            val isActive = i == active || !synced
            val color by animateColorAsState(if (isActive) on else on.copy(alpha = 0.45f), motion.tween<Color>(300), label = "lyric")
            val scale by animateFloatAsState(if (i == active) 1.06f else 1f, tween(motion.ms(300)), label = "lyricScale")
            Text(
                line.text, color = color, fontSize = sizeSp.sp, lineHeight = (sizeSp * 1.3f).sp,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(if (center) 0.5f else 0f, 0.5f) }
                    .then(if (synced) Modifier.clickable { onSeek(line.timeMs) } else Modifier),
            )
        }
    }
}
