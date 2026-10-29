package app.lumio.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lumio.player.data.model.ArtworkModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ArtPlaceholder(modifier: Modifier = Modifier) {
    val a = MaterialTheme.colorScheme.primary
    Box(
        modifier.background(Brush.linearGradient(listOf(a.copy(alpha = 0.55f), a.copy(alpha = 0.15f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxSize(0.4f))
    }
}

/** Placeholder is drawn underneath, so a failed/missing image simply leaves it visible (cheap in lists). */
@Composable
fun ArtworkImage(
    songUri: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val ctx = LocalContext.current
    Box(modifier.clip(shape)) {
        ArtPlaceholder(Modifier.fillMaxSize())
        if (songUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(ArtworkModel(songUri)).build(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun EqualizerBars(playing: Boolean, color: Color, modifier: Modifier = Modifier.size(18.dp)) {
    val t = rememberInfiniteTransition(label = "bars")
    val h1 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse), label = "b1")
    val h2 by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(860, easing = LinearEasing), RepeatMode.Reverse), label = "b2")
    val h3 by t.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(740, easing = LinearEasing), RepeatMode.Reverse), label = "b3")
    Canvas(modifier) {
        val w = size.width / 5f
        listOf(h1, h2, h3).forEachIndexed { i, h ->
            val hh = if (playing) h else 0.25f
            drawRoundRect(
                color,
                topLeft = Offset(i * 2 * w, size.height * (1 - hh)),
                size = Size(w, size.height * hh),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}

fun artShape(shape: app.lumio.player.data.settings.ArtworkShape, radius: Dp = 24.dp): Shape = when (shape) {
    app.lumio.player.data.settings.ArtworkShape.ROUNDED -> RoundedCornerShape(radius)
    app.lumio.player.data.settings.ArtworkShape.SQUARE -> RoundedCornerShape(4.dp)
    app.lumio.player.data.settings.ArtworkShape.CIRCLE -> androidx.compose.foundation.shape.CircleShape
}
