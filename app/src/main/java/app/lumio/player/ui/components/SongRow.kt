package app.lumio.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lumio.player.data.model.Song
import app.lumio.player.util.formatDuration

/** Flat list row (no glass, for scroll performance). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    compact: Boolean = false,
    onLongClick: () -> Unit = {},
    onMore: () -> Unit = {},
) {
    val accent = MaterialTheme.colorScheme.primary
    val on = MaterialTheme.colorScheme.onSurface
    val art = if (compact) 44.dp else 52.dp
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    selected -> accent.copy(alpha = 0.22f)
                    isCurrent -> accent.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 8.dp, top = if (compact) 4.dp else 6.dp, bottom = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(art)) {
            ArtworkImage(song.uri, Modifier.fillMaxSize())
            if (selectionMode) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = if (selected) accent else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(26.dp))
                }
            } else if (isCurrent) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    EqualizerBars(isPlaying, accent)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = if (isCurrent) accent else on, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} · ${song.album}", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelMedium, color = on.copy(alpha = 0.6f))
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "More options", tint = on.copy(alpha = 0.75f)) }
    }
}

@Composable
fun CollectionCard(
    title: String,
    subtitle: String,
    artUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onLongClick: (() -> Unit)? = null,
) {
    val on = MaterialTheme.colorScheme.onSurface
    Column(modifier.bounceClick(pressedScale = 0.96f, onLongClick = onLongClick, onClick = onClick)) {
        ArtworkImage(artUri, Modifier.fillMaxWidth().aspectRatio(1f), shape)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = on, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Square tile with icon + title over artwork (smart mixes). */
@Composable
fun MixCard(
    title: String,
    subtitle: String,
    artUri: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.aspectRatio(1f).bounceClick(pressedScale = 0.96f, onClick = onClick).clip(RoundedCornerShape(24.dp))) {
        ArtworkImage(artUri, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.72f)))))
        Box(Modifier.padding(12.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}
