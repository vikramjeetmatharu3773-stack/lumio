package app.lumio.player.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.lumio.player.MainActivity
import app.lumio.player.R
import app.lumio.player.container
import app.lumio.player.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Responsive glass Now-Playing widget (2x2 / 4x2 / 4x4 in one provider). Spec: docs/09 */
class NowPlayingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val cfg = WidgetConfigStore(context).load(widgetId, c.settings.current())
        val snap = loadWidgetSnapshot(context)
        val density = context.resources.displayMetrics.density
        val small = WidgetGlassRenderer.render(snap.art, 400, 400, cfg, snap.accent, 400f / 110f)
        val medium = WidgetGlassRenderer.render(snap.art, 800, 360, cfg, snap.accent, 800f / 250f)
        val large = WidgetGlassRenderer.render(snap.art, 800, 800, cfg, snap.accent, 800f / 250f)
        val roundArt = if (cfg.showArt) snap.art?.let { WidgetGlassRenderer.roundedArt(it, 320, 56f) } else null
        provideContent {
            WidgetBody(snap.song, snap.playing, cfg, small, medium, large, roundArt, snap.accent)
        }
    }
}

@Composable
private fun WidgetBody(
    song: Song?, playing: Boolean, cfg: WidgetConfig,
    small: Bitmap, medium: Bitmap, large: Bitmap, art: Bitmap?, accent: Int,
) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val bg = when {
        size.width >= 200.dp && size.height >= 200.dp -> large
        size.width >= 200.dp -> medium
        else -> small
    }
    val fg = if (cfg.text == WidgetText.DARK) Color(0xFF14141C) else Color.White
    val fgProv = ColorProvider(fg)
    val open = actionStartActivity(Intent(ctx, MainActivity::class.java).putExtra("open_now_playing", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val title = song?.title ?: "Tap to play music"
    val artist = song?.artist ?: "Lumio"

    Box(GlanceModifier.fillMaxSize().background(ImageProvider(bg)).clickable(open)) {
        when {
            size.width >= 200.dp && size.height >= 200.dp -> Column(
                GlanceModifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (art != null) { Image(ImageProvider(art), null, GlanceModifier.size(170.dp), contentScale = ContentScale.Fit); Spacer(GlanceModifier.height(10.dp)) }
                Text(title, style = TextStyle(color = fgProv, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(artist, style = TextStyle(color = fgProv, fontSize = 13.sp), maxLines = 1)
                if (cfg.showControls) {
                    Spacer(GlanceModifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Btn(R.drawable.ic_w_shuffle, "Shuffle", 40.dp, fg, actionRunCallback<ShuffleAction>()); Spacer(GlanceModifier.width(10.dp))
                        Btn(R.drawable.ic_w_prev, "Previous", 44.dp, fg, actionRunCallback<PrevAction>()); Spacer(GlanceModifier.width(10.dp))
                        Btn(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", 56.dp, fg, actionRunCallback<PlayPauseAction>()); Spacer(GlanceModifier.width(10.dp))
                        Btn(R.drawable.ic_w_next, "Next", 44.dp, fg, actionRunCallback<NextAction>())
                    }
                }
            }
            size.width >= 200.dp -> Row(GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (art != null) { Image(ImageProvider(art), null, GlanceModifier.size(84.dp), contentScale = ContentScale.Fit); Spacer(GlanceModifier.width(12.dp)) }
                Column(GlanceModifier.defaultWeight()) {
                    Text(title, style = TextStyle(color = fgProv, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(artist, style = TextStyle(color = fgProv, fontSize = 12.sp), maxLines = 1)
                    if (cfg.showControls) {
                        Spacer(GlanceModifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Btn(R.drawable.ic_w_prev, "Previous", 38.dp, fg, actionRunCallback<PrevAction>()); Spacer(GlanceModifier.width(8.dp))
                            Btn(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", 44.dp, fg, actionRunCallback<PlayPauseAction>()); Spacer(GlanceModifier.width(8.dp))
                            Btn(R.drawable.ic_w_next, "Next", 38.dp, fg, actionRunCallback<NextAction>())
                        }
                    }
                }
            }
            else -> Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (art != null) Image(ImageProvider(art), null, GlanceModifier.size(52.dp), contentScale = ContentScale.Fit) else Image(ImageProvider(R.drawable.ic_w_note), null, GlanceModifier.size(40.dp), colorFilter = ColorFilter.tint(fgProv))
                    Spacer(GlanceModifier.defaultWeight())
                    if (cfg.showControls) Btn(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", 44.dp, fg, actionRunCallback<PlayPauseAction>())
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(title, style = TextStyle(color = fgProv, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(artist, style = TextStyle(color = fgProv, fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Btn(icon: Int, desc: String, size: androidx.compose.ui.unit.Dp, tint: Color, action: androidx.glance.action.Action) {
    Box(GlanceModifier.size(size).background(ImageProvider(R.drawable.w_btn_bg)).clickable(action), contentAlignment = Alignment.Center) {
        Image(ImageProvider(icon), desc, GlanceModifier.size(size * 0.55f), colorFilter = ColorFilter.tint(ColorProvider(tint)))
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { context.container.player.togglePlay() }
}
class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { context.container.player.next() }
}
class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { context.container.player.previous() }
}
class ShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = context.container
        c.player.playSongs(c.library.songs.value, shuffle = true, source = "Shuffle all")
    }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = WidgetConfigStore(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch { appWidgetIds.forEach { store.delete(it) } }
    }
}
