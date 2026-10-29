package app.lumio.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.lumio.player.ui.theme.BackdropState
import app.lumio.player.ui.theme.GlassStyle
import app.lumio.player.ui.theme.LocalBackdrop
import app.lumio.player.ui.theme.LocalGlass
import app.lumio.player.ui.theme.LocalIsDark
import app.lumio.player.ui.theme.LocalMotion
import app.lumio.player.ui.theme.drawBackdrop

/**
 * The ONE place glass is drawn. Recipe (docs/03 3.3): backdrop sample -> frost tint -> specular wash -> edge light.
 * Do NOT use inside lazy list items.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalGlass.current.corner),
    style: GlassStyle = LocalGlass.current,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalBackdrop.current
    val dark = LocalIsDark.current
    val base = MaterialTheme.colorScheme.background
    var origin by remember { mutableStateOf(Offset.Zero) }
    val clickMod = if (onClick != null) Modifier.bounceClick(onLongClick = onLongClick, onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .then(clickMod)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
            .drawBehind { drawGlass(backdrop, origin, style, dark, base, tint) }
            .border(1.dp, glassBorderBrush(style, dark), shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

private fun DrawScope.drawGlass(
    backdrop: BackdropState,
    origin: Offset,
    s: GlassStyle,
    dark: Boolean,
    base: Color,
    tint: Color,
) {
    if (s.reduceTransparency) {
        drawRect(base.copy(alpha = 0.92f))
    } else {
        drawRect(base)
        val k = 1f + 0.05f * s.refraction
        val pivot = Offset(origin.x + size.width / 2f, origin.y + size.height / 2f)
        withTransform({
            translate(-origin.x, -origin.y)
            scale(k, k, pivot)
        }) { drawBackdrop(backdrop) }
        drawRect(
            if (dark) Color.White.copy(alpha = 0.06f + 0.20f * s.opacity)
            else Color.White.copy(alpha = 0.30f + 0.45f * s.opacity)
        )
        if (dark) drawRect(Color.Black.copy(alpha = 0.12f))
        if (s.specular > 0f) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.22f * s.specular), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
            )
        }
    }
    if (tint != Color.Unspecified) drawRect(tint)
}

private fun glassBorderBrush(s: GlassStyle, dark: Boolean): Brush {
    val k = s.border
    val hi = if (dark) 0.60f else 0.90f
    val lo = if (dark) 0.28f else 0.50f
    return Brush.verticalGradient(
        listOf(Color.White.copy(alpha = hi * k), Color.White.copy(alpha = 0.08f * k), Color.White.copy(alpha = lo * k))
    )
}

/** Press-scale with spring overshoot + optional long press (with haptic). */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotion.current
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) pressedScale else 1f,
        animationSpec = motion.bounce(),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick?.let { lc ->
                {
                    if (motion.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    lc()
                }
            },
            onClick = onClick,
        )
}
