package app.lumio.player.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import app.lumio.player.container
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.WidgetBackground
import app.lumio.player.ui.components.*
import app.lumio.player.ui.theme.BackdropState
import app.lumio.player.ui.theme.LumioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Shown when a widget is placed or reconfigured. Live preview uses the real glass renderer. */
class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val c = applicationContext.container
        val store = WidgetConfigStore(applicationContext)

        setContent {
            val backdrop = remember { BackdropState() }
            LumioTheme(AppSettings(), null, backdrop) {
                var cfg by remember { mutableStateOf<WidgetConfig?>(null) }
                var snap by remember { mutableStateOf<WidgetSnapshot?>(null) }
                LaunchedEffect(Unit) {
                    cfg = store.load(widgetId, c.settings.current())
                    snap = loadWidgetSnapshot(applicationContext)
                }
                val current = cfg
                val s = snap
                val preview by produceState<Bitmap?>(null, current, s) {
                    if (current != null && s != null) value = withContext(Dispatchers.Default) {
                        WidgetGlassRenderer.render(s.art, 800, 360, current, s.accent, 800f / 250f)
                    }
                }
                Column(
                    Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                ) {
                    ScreenHeader("Customize widget")
                    preview?.let { Image(it.asImageBitmap(), "Widget preview", Modifier.fillMaxWidth().padding(16.dp)) }
                    if (current != null) {
                        SettingsSection("Look") {
                            SettingsChoice("Background", listOf(WidgetBackground.ARTWORK_GLASS to "Artwork glass", WidgetBackground.SOLID to "Solid", WidgetBackground.GRADIENT to "Gradient"), current.bg) { cfg = current.copy(bg = it) }
                            SettingsSlider("Opacity", "${(current.opacity * 100).toInt()}%", current.opacity, 0.1f..1f, { cfg = current.copy(opacity = it) })
                            SettingsSlider("Blur", "${(current.blur * 100).toInt()}%", current.blur, 0f..1f, { cfg = current.copy(blur = it) })
                            SettingsSlider("Corner radius", "${current.corner} dp", current.corner.toFloat(), 0f..40f, { cfg = current.copy(corner = it.toInt()) })
                            SettingsChoice("Text color", listOf(WidgetText.AUTO to "Auto", WidgetText.LIGHT to "Light", WidgetText.DARK to "Dark"), current.text) { cfg = current.copy(text = it) }
                        }
                        SettingsSection("Content") {
                            SettingsSwitch("Show artwork", current.showArt, { cfg = current.copy(showArt = it) })
                            SettingsSwitch("Show controls", current.showControls, { cfg = current.copy(showControls = it) })
                        }
                        SettingsSection("Buttons") {
                            SettingsSlider("Button size", "${current.buttonSize} dp", current.buttonSize.toFloat(), 32f..80f, { cfg = current.copy(buttonSize = it.toInt()) })
                            SettingsSlider("Button opacity", "${(current.buttonOpacity * 100).toInt()}%", current.buttonOpacity, 0.3f..1f, { cfg = current.copy(buttonOpacity = it) })
                        }
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            GlassButton("Done", {
                                lifecycleScope.launch {
                                    store.save(widgetId, current)
                                    NowPlayingWidget().updateAll(applicationContext)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                    finish()
                                }
                            }, filled = true)
                        }
                    }
                }
            }
        }
    }
}
