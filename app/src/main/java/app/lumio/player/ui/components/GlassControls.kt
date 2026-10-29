package app.lumio.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lumio.player.ui.theme.LocalMotion

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    selected: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    GlassSurface(
        modifier = modifier.size(size),
        shape = CircleShape,
        tint = if (selected) accent.copy(alpha = 0.30f) else Color.Unspecified,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val wash by animateColorAsState(
        if (selected) accent.copy(alpha = 0.30f) else Color.Transparent,
        LocalMotion.current.tween<Color>(220),
        label = "chip",
    )
    GlassSurface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(50),
        tint = wash,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    GlassSurface(
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(50),
        tint = if (filled) accent.copy(alpha = 0.88f) else Color.Unspecified,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = fg)
            Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (onClick != null) Icon(Icons.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

/** Play/Pause icon with a cross-morph (docs/05 row 10). */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, tint: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(motion.ms(180))) + scaleIn(initialScale = 0.6f)) togetherWith
                (fadeOut(tween(motion.ms(120))) + scaleOut(targetScale = 0.6f))
        },
        modifier = modifier,
        label = "playpause",
    ) { playing ->
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}
