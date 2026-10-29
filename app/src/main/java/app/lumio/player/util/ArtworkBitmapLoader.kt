package app.lumio.player.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

/** Lets the Media3 notification/lockscreen show embedded art for content:// audio URIs. */
class ArtworkBitmapLoader(private val context: Context) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> { BitmapFactory.decodeByteArray(data, 0, data.size) }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> {
            ArtworkLoader.loadSync(context, uri.toString(), 512) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
}
