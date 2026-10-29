package app.lumio.player.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** Tiny CPU blur (box blur x3) for 64px backdrop bitmaps. Pure Android, API 26+. */
object BitmapBlur {
    fun blurred(src: Bitmap, size: Int = 64, radius: Int = 8, saturation: Float = 1.25f): Bitmap {
        val scaled = Bitmap.createScaledBitmap(src, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
        }
        Canvas(out).drawBitmap(scaled, 0f, 0f, paint)
        blurInPlace(out, radius)
        return out
    }

    /** Aurora backdrop: soft colored blobs on a base color. */
    fun aurora(base: Int, colors: List<Int>, size: Int = 64, radius: Int = 6): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(base)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val pts = listOf(0.15f to 0.10f, 0.92f to 0.38f, 0.18f to 0.82f, 0.85f to 0.97f)
        colors.take(pts.size).forEachIndexed { i, col ->
            val (fx, fy) = pts[i]
            val strong = Color.argb(210, Color.red(col), Color.green(col), Color.blue(col))
            val clear = Color.argb(0, Color.red(col), Color.green(col), Color.blue(col))
            p.shader = RadialGradient(fx * size, fy * size, size * 0.8f, intArrayOf(strong, clear), null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        }
        blurInPlace(bmp, radius)
        return bmp
    }

    fun solid(color: Int, size: Int = 8): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }

    private fun blurInPlace(bmp: Bitmap, radius: Int) {
        val w = bmp.width
        val h = bmp.height
        var a = IntArray(w * h)
        var b = IntArray(w * h)
        bmp.getPixels(a, 0, w, 0, 0, w, h)
        repeat(3) {
            pass(a, b, w, h, radius, true)
            pass(b, a, w, h, radius, false)
        }
        bmp.setPixels(a, 0, w, 0, 0, w, h)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (line in 0 until lines) {
            for (i in 0 until len) {
                var a = 0; var rr = 0; var g = 0; var bb = 0
                for (k in -r..r) {
                    val j = (i + k).coerceIn(0, len - 1)
                    val p = src[if (horizontal) line * w + j else j * w + line]
                    a += p ushr 24; rr += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; bb += p and 0xFF
                }
                val n = 2 * r + 1
                dst[if (horizontal) line * w + i else i * w + line] =
                    ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (bb / n)
            }
        }
    }
}
