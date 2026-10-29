package app.lumio.player.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.WidgetBackground
import kotlinx.coroutines.flow.first

enum class WidgetText { AUTO, LIGHT, DARK }

/** Per-widget customization. Spec: docs/09_WIDGETS.md */
data class WidgetConfig(
    val bg: WidgetBackground = WidgetBackground.ARTWORK_GLASS,
    val opacity: Float = 0.55f,
    val blur: Float = 0.6f,
    val corner: Int = 28,
    val text: WidgetText = WidgetText.AUTO,
    val showArt: Boolean = true,
    val showControls: Boolean = true,
    val buttonSize: Int = 56,
    val buttonOpacity: Float = 0.85f,
) {
    companion object {
        fun defaults(s: AppSettings) = WidgetConfig(bg = s.widgetBackground, opacity = s.widgetOpacity, corner = s.widgetCorner)
    }
}

private val Context.widgetStore by preferencesDataStore(name = "lumio_widgets")

class WidgetConfigStore(private val context: Context) {
    suspend fun load(id: Int, defaults: AppSettings): WidgetConfig {
        val p = context.widgetStore.data.first()
        val d = WidgetConfig.defaults(defaults)
        return WidgetConfig(
            bg = p[stringPreferencesKey("w${id}_bg")]?.let { v -> WidgetBackground.entries.firstOrNull { it.name == v } } ?: d.bg,
            opacity = p[floatPreferencesKey("w${id}_opacity")] ?: d.opacity,
            blur = p[floatPreferencesKey("w${id}_blur")] ?: d.blur,
            corner = p[intPreferencesKey("w${id}_corner")] ?: d.corner,
            text = p[stringPreferencesKey("w${id}_text")]?.let { v -> WidgetText.entries.firstOrNull { it.name == v } } ?: d.text,
            showArt = p[booleanPreferencesKey("w${id}_art")] ?: d.showArt,
            showControls = p[booleanPreferencesKey("w${id}_controls")] ?: d.showControls,
            buttonSize = p[intPreferencesKey("w${id}_btnSize")] ?: d.buttonSize,
            buttonOpacity = p[floatPreferencesKey("w${id}_btnOpacity")] ?: d.buttonOpacity,
        )
    }

    suspend fun save(id: Int, c: WidgetConfig) {
        context.widgetStore.edit { p ->
            p[stringPreferencesKey("w${id}_bg")] = c.bg.name
            p[floatPreferencesKey("w${id}_opacity")] = c.opacity
            p[floatPreferencesKey("w${id}_blur")] = c.blur
            p[intPreferencesKey("w${id}_corner")] = c.corner
            p[stringPreferencesKey("w${id}_text")] = c.text.name
            p[booleanPreferencesKey("w${id}_art")] = c.showArt
            p[booleanPreferencesKey("w${id}_controls")] = c.showControls
            p[intPreferencesKey("w${id}_btnSize")] = c.buttonSize
            p[floatPreferencesKey("w${id}_btnOpacity")] = c.buttonOpacity
        }
    }

    suspend fun delete(id: Int) {
        context.widgetStore.edit { p ->
            listOf("bg", "opacity", "blur", "corner", "text", "art", "controls").forEach { k ->
                p.remove(stringPreferencesKey("w${id}_$k")); p.remove(floatPreferencesKey("w${id}_$k"))
                p.remove(intPreferencesKey("w${id}_$k")); p.remove(booleanPreferencesKey("w${id}_$k"))
            }
        }
    }
}
