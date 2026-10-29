package app.lumio.player

import android.app.Application
import app.lumio.player.data.db.AppDatabase
import app.lumio.player.data.repo.CollectionRepository
import app.lumio.player.data.repo.LibraryRepository
import app.lumio.player.data.repo.LyricsRepository
import app.lumio.player.data.repo.PlaylistRepository
import app.lumio.player.data.scan.MediaScanner
import app.lumio.player.data.settings.SettingsRepository
import app.lumio.player.playback.AudioEffectsController
import app.lumio.player.playback.PlayerController
import app.lumio.player.playback.SleepTimerController
import app.lumio.player.widget.NowPlayingWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Manual DI: one instance per process, created in LumioApp. Spec: docs/02_ARCHITECTURE.md */
class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: AppDatabase by lazy { AppDatabase.build(app) }
    val settings: SettingsRepository by lazy { SettingsRepository(app) }
    val library: LibraryRepository by lazy { LibraryRepository(db, settings, scope) }
    val playlists: PlaylistRepository by lazy { PlaylistRepository(db, library) }
    val collections: CollectionRepository by lazy { CollectionRepository(library, playlists) }
    val scanner: MediaScanner by lazy { MediaScanner(app, db, settings) }
    val lyrics: LyricsRepository by lazy { LyricsRepository(app) }
    val effects: AudioEffectsController by lazy { AudioEffectsController(settings, scope) }
    val sleepTimer: SleepTimerController by lazy { SleepTimerController(scope) }
    val player: PlayerController by lazy { PlayerController(app, scope, library, db, settings) }

    @OptIn(FlowPreview::class)
    fun start() {
        scanner.startObserving(scope)
        // keep home-screen widgets in sync with playback
        scope.launch {
            player.state
                .map { Triple(it.current?.uri, it.isPlaying, it.shuffle) }
                .distinctUntilChanged()
                .debounce(300)
                .collect {
                    try { NowPlayingWidget().updateAll(app) } catch (_: Exception) {}
                }
        }
    }
}
