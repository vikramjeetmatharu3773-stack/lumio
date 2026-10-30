package app.lumio.player.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.palette.graphics.Palette
import app.lumio.player.container
import app.lumio.player.data.db.toSong
import app.lumio.player.data.model.Song
import app.lumio.player.data.settings.WidgetBackground
import app.lumio.player.util.ArtworkLoader
import app.lumio.player.util.ArtworkThemeBuilder
import app.lumio.player.util.BitmapBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class WidgetSnapshot(val song: Song?, val playing: Boolean, val art: Bitmap?, val accent: Int)

/** Current/last song + artwork + accent, shared by the widget and its config screen. */
suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val c = context.container
    val st = c.player.state.value
    var song = st.current
    if (song == null) {
        val last = c.settings.current().lastSongUri
        if (last.isNotEmpty()) song = c.db.songs().byUris(listOf(last)).firstOrNull()?.toSong()
    }
    val art = song?.let { s -> withTimeoutOrNull(1500) { withContext(Dispatchers.IO) { ArtworkLoader.loadSync(context, s.uri, 400) } } }
    val accent = art?.let { b ->
        val p = Palette.from(b).maximumColorCount(12).generate()
        ArtworkThemeBuilder.brighten(p.getVibrantColor(p.getMutedColor(0xFF7C8CFF.toInt())))
    } ?: 0xFF7C8CFF.toInt()
    return WidgetSnapshot(song, st.isPlaying, art, accent)
}

/** Paints the "liquid glass" card into a bitmap because launchers cannot blur behind widgets. */
object WidgetGlassRenderer {
    fun render(art: Bitmap?, w: Int, h: Int, cfg: WidgetConfig, accent: Int, pxPerDp: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val r = (cfg.corner * pxPerDp).coerceAtLeast(0f)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val path = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
        c.clipPath(path)
        val strength = cfg.opacity.coerceIn(0.1f, 1f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (cfg.bg) {
            WidgetBackground.ARTWORK_GLASS -> {
                val radius = (2 + 12 * cfg.blur).toInt()
                val base = if (art != null) BitmapBlur.blurred(art, 48, radius)
                else BitmapBlur.aurora(0xFF0B0B14.toInt(), listOf(accent, Color.rgb(90, 60, 200), Color.rgb(30, 160, 200), Color.rgb(200, 60, 160)), 48, radius)
                paint.alpha = (255 * (0.35f + 0.65f * strength)).toInt()
                c.drawBitmap(base, coverMatrix(base.width, base.height, w, h), paint)
                paint.alpha = 255
                val frost = if (cfg.text == WidgetText.DARK) 0.55f + 0.3f * strength else 0.06f + 0.20f * strength
                c.drawColor(Color.argb((frost * 255).toInt(), 255, 255, 255))
                if (cfg.text != WidgetText.DARK) c.drawColor(Color.argb(40, 0, 0, 0))
            }
            WidgetBackground.SOLID -> c.drawColor(Color.argb((strength * 255).toInt(), 20, 20, 30))
            WidgetBackground.GRADIENT -> {
                paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(accent, 0xFF0B1030.toInt()), null, Shader.TileMode.CLAMP)
                paint.alpha = (255 * strength).toInt()
                c.drawRect(rect, paint)
                paint.shader = null
            }
        }
        // specular wash
        paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), Color.argb(56, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.alpha = 255
        c.drawRect(rect, paint)
        // edge light
        val stroke = 1.4f * pxPerDp
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(Color.argb(150, 255, 255, 255), Color.argb(20, 255, 255, 255), Color.argb(70, 255, 255, 255)), null, Shader.TileMode.CLAMP)
        }
        val inset = stroke / 2
        c.drawRoundRect(RectF(inset, inset, w - inset, h - inset), r.coerceAtLeast(0f), r.coerceAtLeast(0f), border)
        return out
    }

    fun roundedArt(art: Bitmap, sizePx: Int, radiusPx: Float): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radiusPx.coerceAtLeast(0f), radiusPx.coerceAtLeast(0f), Path.Direction.CW) })
        c.drawBitmap(art, coverMatrix(art.width, art.height, sizePx, sizePx), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun coverMatrix(bw: Int, bh: Int, w: Int, h: Int): Matrix {
        val s = maxOf(w.toFloat() / bw, h.toFloat() / bh)
        return Matrix().apply {
            setScale(s, s)
            postTranslate((w - bw * s) / 2f, (h - bh * s) / 2f)
        }
    }
}
