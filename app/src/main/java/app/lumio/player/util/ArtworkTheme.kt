package app.lumio.player.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import app.lumio.player.data.settings.BackdropStyle

/** Result of analysing the current song's artwork. */
class ArtworkTheme(val accent: Int, val colors: List<Int>, val backdrop: Bitmap)

object ArtworkThemeBuilder {
    /** blur 0..1 -> radius 2..14. Blocking: call on Default/IO. */
    fun build(
        context: Context,
        songUri: String?,
        blur: Float,
        style: BackdropStyle,
        fallbackAccent: Int,
        baseBg: Int,
    ): ArtworkTheme {
        val radius = (2 + 12 * blur.coerceIn(0f, 1f)).toInt()
        val art = if (songUri != null) ArtworkLoader.loadSync(context, songUri, 256) else null

        var accent: Int
        val colors: List<Int>
        if (art == null) {
            val hsv = FloatArray(3)
            Color.colorToHSV(fallbackAccent, hsv)
            accent = fallbackAccent
            colors = listOf(
                fallbackAccent,
                Color.HSVToColor(floatArrayOf((hsv[0] + 70f) % 360f, 0.65f, 0.75f)),
                Color.HSVToColor(floatArrayOf((hsv[0] + 200f) % 360f, 0.60f, 0.60f)),
                Color.HSVToColor(floatArrayOf((hsv[0] + 300f) % 360f, 0.55f, 0.55f)),
            )
        } else {
            try {
                val palette = Palette.from(art).maximumColorCount(16).generate()
                val dominant = palette.getDominantColor(fallbackAccent)
                accent = brighten(palette.getVibrantColor(palette.getLightVibrantColor(palette.getMutedColor(dominant))))
                colors = listOf(
                    accent,
                    palette.getDarkVibrantColor(dominant),
                    palette.getMutedColor(dominant),
                    palette.getLightMutedColor(dominant),
                )
            } catch (_: Exception) {
                val hsv = FloatArray(3)
                Color.colorToHSV(fallbackAccent, hsv)
                accent = fallbackAccent
                colors = listOf(
                    fallbackAccent,
                    Color.HSVToColor(floatArrayOf((hsv[0] + 70f) % 360f, 0.65f, 0.75f)),
                    Color.HSVToColor(floatArrayOf((hsv[0] + 200f) % 360f, 0.60f, 0.60f)),
                    Color.HSVToColor(floatArrayOf((hsv[0] + 300f) % 360f, 0.55f, 0.55f)),
                )
            }
        }
        val backdrop = when (style) {
            BackdropStyle.SOLID -> BitmapBlur.solid(baseBg)
            BackdropStyle.AURORA -> BitmapBlur.aurora(baseBg, colors, radius = radius)
            BackdropStyle.ARTWORK ->
                if (art != null) BitmapBlur.blurred(art, radius = radius) else BitmapBlur.aurora(baseBg, colors, radius = radius)
        }
        return ArtworkTheme(accent, colors, backdrop)
    }

    /** Make sure accent is readable on dark surfaces. */
    fun brighten(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceIn(0.35f, 0.95f)
        hsv[2] = hsv[2].coerceIn(0.78f, 1f)
        return Color.HSVToColor(hsv)
    }
}
