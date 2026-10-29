package app.lumio.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.lumio.player.MainActivity
import app.lumio.player.container
import app.lumio.player.util.ArtworkBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeMs = 0
    private var stopOnRemoved = false

    override fun onCreate() {
        super.onCreate()
        val c = applicationContext.container

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        c.effects.attach(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { c.effects.attach(audioSessionId) }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && c.sleepTimer.consumeEndOfTrack()) player.pause()
            }
        })

        val open = Intent(this, MainActivity::class.java).apply {
            putExtra("open_now_playing", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        session = MediaSession.Builder(this, player)
            .setBitmapLoader(ArtworkBitmapLoader(this))
            .setSessionActivity(sessionActivity)
            .build()

        scope.launch {
            val first = c.settings.current()
            player.playbackParameters = PlaybackParameters(first.playbackSpeed, first.playbackPitch)
            c.settings.flow.collect { s ->
                player.skipSilenceEnabled = s.skipSilence
                player.setHandleAudioBecomingNoisy(s.pauseOnUnplug)
                fadeMs = s.fadeMs
                stopOnRemoved = s.stopOnTaskRemoved
            }
        }
        scope.launch { c.sleepTimer.events.collect { player.pause() } }
        scope.launch { fadeLoop() }
    }

    /** Simple fade-in/out near track boundaries (not an overlap crossfade). */
    private suspend fun fadeLoop() {
        while (scope.isActive) {
            val fade = fadeMs
            if (fade <= 0) {
                if (player.volume != 1f) player.volume = 1f
            } else if (player.isPlaying) {
                val dur = player.duration
                if (dur > 0) {
                    val pos = player.currentPosition
                    val fin = (pos.toFloat() / fade).coerceIn(0f, 1f)
                    val fout = ((dur - pos).toFloat() / fade).coerceIn(0f, 1f)
                    player.volume = minOf(fin, fout).coerceAtLeast(0.02f)
                }
            }
            delay(200)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (stopOnRemoved || !player.playWhenReady || player.mediaItemCount == 0) pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        applicationContext.container.effects.releaseAll()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
