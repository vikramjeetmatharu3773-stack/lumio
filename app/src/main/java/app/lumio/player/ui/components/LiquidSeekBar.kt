package app.lumio.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.ui.theme.LocalMotion
import app.lumio.player.util.formatDuration
import kotlinx.coroutines.flow.StateFlow

/**
 * Seek bar + time labels. Collects the position flow HERE so only this small composable recomposes 4x/s.
 * Glass thumb squishes while dragging (docs/05 rows 11-12).
 */
@Composable
fun PlayerSeekSection(
    position: StateFlow<Long>,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pos by position.collectAsStateWithLifecycle()
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val dur = durationMs.coerceAtLeast(1L)
    val fraction = if (dragFraction >= 0f) dragFraction else (pos.toFloat() / dur).coerceIn(0f, 1f)
    val shownMs = (fraction * dur).toLong()
    Column(modifier) {
        LiquidSeekBar(
            fraction = fraction,
            onDrag = { dragFraction = it },
            onCommit = { f -> dragFraction = -1f; onSeek((f * dur).toLong()) },
            onCancel = { dragFraction = -1f },
            bubbleText = formatDuration(shownMs),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shownMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            Text("-" + formatDuration((dur - shownMs).coerceAtLeast(0)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        }
    }
}

@Composable
fun LiquidSeekBar(
    fraction: Float,
    onDrag: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    onCancel: () -> Unit,
    bubbleText: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val motion = LocalMotion.current
    val haptic = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    var lastBucket by remember { mutableIntStateOf(-1) }
    val grow by animateFloatAsState(if (dragging) 1f else 0f, motion.liquid(), label = "grow")
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> onCommit((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                var current = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        current = (o.x / size.width).coerceIn(0f, 1f)
                        onDrag(current)
                    },
                    onDragEnd = { dragging = false; lastBucket = -1; onCommit(current) },
                    onDragCancel = { dragging = false; lastBucket = -1; onCancel() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        current = (change.position.x / size.width).coerceIn(0f, 1f)
                        onDrag(current)
                        val bucket = (current * 10).toInt()
                        if (bucket != lastBucket) {
                            lastBucket = bucket
                            if (motion.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackH = (6f + 3f * grow).dp.toPx()
            val cy = size.height / 2f
            val x = size.width * fraction
            drawRoundRect(onSurface.copy(alpha = 0.20f), Offset(0f, cy - trackH / 2), Size(size.width, trackH), CornerRadius(trackH / 2))
            drawRoundRect(
                Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent)),
                Offset(0f, cy - trackH / 2), Size(x.coerceAtLeast(0f), trackH), CornerRadius(trackH / 2),
            )
            val s = 1f + 0.5f * grow
            val tw = 22.dp.toPx() * s
            val th = 22.dp.toPx() * (1f + 0.2f * grow)
            val cx = x.coerceIn(tw / 2, size.width - tw / 2)
            val tl = Offset(cx - tw / 2, cy - th / 2)
            drawRoundRect(Color.Black.copy(alpha = 0.25f), tl + Offset(0f, 2.dp.toPx()), Size(tw, th), CornerRadius(th / 2))
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White, Color(0xFFDCE3FF)), startY = tl.y, endY = tl.y + th),
                tl, Size(tw, th), CornerRadius(th / 2),
            )
            drawRoundRect(Color.White.copy(alpha = 0.9f), tl, Size(tw, th), CornerRadius(th / 2), style = Stroke(1.dp.toPx()))
        }
    }
}
