package app.lumio.player.util

import android.graphics.drawable.BitmapDrawable
import app.lumio.player.data.model.ArtworkModel
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coil fetcher for [ArtworkModel]: embedded artwork of an audio file. */
class ArtworkFetcher(private val data: ArtworkModel, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val bmp = ArtworkLoader.loadSync(options.context, data.songUri, 600) ?: return@withContext null
        DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bmp),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<ArtworkModel> {
        override fun create(data: ArtworkModel, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(data, options)
    }
}

class ArtworkKeyer : Keyer<ArtworkModel> {
    override fun key(data: ArtworkModel, options: Options): String = "art:${data.songUri}"
}
