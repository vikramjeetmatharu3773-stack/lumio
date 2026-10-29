package app.lumio.player

import android.app.Application
import android.content.Context
import app.lumio.player.util.ArtworkFetcher
import app.lumio.player.util.ArtworkKeyer
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache

class LumioApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(ArtworkKeyer())
            add(ArtworkFetcher.Factory())
        }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .allowHardware(false)
        .crossfade(false)
        .build()
}

val Context.container: AppContainer
    get() = (applicationContext as LumioApp).container
