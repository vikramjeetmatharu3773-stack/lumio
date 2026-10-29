package app.lumio.player.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** User-tunable glass parameters (all 0..1 except corner). Spec: docs/03 section 3.3 */
@Immutable
data class GlassStyle(
    val blur: Float = 0.6f,
    val opacity: Float = 0.5f,
    val border: Float = 0.7f,
    val specular: Float = 0.6f,
    val refraction: Float = 0.5f,
    val corner: Dp = 28.dp,
    val reduceTransparency: Boolean = false,
)

val LocalGlass = compositionLocalOf { GlassStyle() }
val LocalIsDark = compositionLocalOf { true }

/**
 * One small blurred bitmap shared by the screen background AND every GlassSurface.
 * Each glass panel draws the same bitmap offset by its own window position, so it shows exactly what is "behind" it.
 */
@Stable
class BackdropState {
    var current by mutableStateOf<ImageBitmap?>(null)
    var previous by mutableStateOf<ImageBitmap?>(null)
    var progress by mutableFloatStateOf(1f)
    var rootSize by mutableStateOf(IntSize.Zero)

    fun update(bmp: ImageBitmap) {
        previous = current
        current = bmp
        progress = 0f
    }
}

val LocalBackdrop = compositionLocalOf { BackdropState() }

/** Draw the backdrop scaled to the full root size at (0,0) of the current coordinate space. */
fun DrawScope.drawBackdrop(state: BackdropState) {
    val dst = state.rootSize
    if (dst.width == 0 || dst.height == 0) return
    state.previous?.let {
        drawImage(it, dstSize = dst, filterQuality = FilterQuality.Medium)
    }
    state.current?.let {
        drawImage(it, dstSize = dst, alpha = state.progress, filterQuality = FilterQuality.Medium)
    }
}

/** Crossfades to a new backdrop bitmap whenever it changes. */
@Composable
fun BackdropEffect(state: BackdropState, bitmap: ImageBitmap?) {
    val motion = LocalMotion.current
    LaunchedEffect(bitmap) {
        if (bitmap == null) return@LaunchedEffect
        state.update(bitmap)
        if (!motion.enabled) {
            state.progress = 1f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, motion.tween(700)) { state.progress = value }
    }
}
