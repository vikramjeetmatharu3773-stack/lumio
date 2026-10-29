package app.lumio.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.playback.AudioEffectsController
import app.lumio.player.playback.EqPresets
import app.lumio.player.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val on = MaterialTheme.colorScheme.onSurface
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val info by c.effects.info.collectAsStateWithLifecycle()
    val levels = remember(s.eqBands, info.bands) { AudioEffectsController.parseBands(s.eqBands, info.bands) }

    ScreenScaffold(
        "Equalizer", onBack = onBack,
        actions = { Switch(checked = s.eqEnabled, onCheckedChange = { v -> scope.launch { c.settings.update { copy(eqEnabled = v) } } }) },
    ) {
        if (!info.supported) {
            item {
                SettingsSection(null) {
                    Text("The equalizer connects when audio starts. Play any song, then come back. Some devices do not support system equalizers.",
                        color = on.copy(alpha = 0.8f), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EqPresets.curves.keys.toList()) { name -> GlassChip(name, s.eqPreset == name, { c.effects.setPreset(name) }) }
                }
            }
            item {
                SettingsSection("Bands") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
                        for (i in 0 until info.bands) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${levels[i] / 100}dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Box(Modifier.height(190.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    VerticalSlider(levels[i].toFloat(), info.minMb.toFloat()..info.maxMb.toFloat(), enabled = s.eqEnabled) { v -> c.effects.setBand(i, v.toInt()) }
                                }
                                val hz = info.centerHz.getOrNull(i) ?: 0
                                Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", style = MaterialTheme.typography.labelSmall, color = on.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsSection("Effects") {
                SettingsSlider("Bass boost", "${s.bassBoost / 10}%", s.bassBoost.toFloat(), 0f..1000f, { v -> scope.launch { c.settings.update { copy(bassBoost = v.toInt()) } } })
                SettingsSlider("Virtualizer", "${s.virtualizer / 10}%", s.virtualizer.toFloat(), 0f..1000f, { v -> scope.launch { c.settings.update { copy(virtualizer = v.toInt()) } } })
                SettingsSlider("Loudness gain", "+${s.loudnessMb / 100} dB", s.loudnessMb.toFloat(), 0f..2000f, { v -> scope.launch { c.settings.update { copy(loudnessMb = v.toInt()) } } })
                TextButton(onClick = {
                    scope.launch { c.settings.update { copy(eqPreset = "Flat", eqBands = "", bassBoost = 0, virtualizer = 0, loudnessMb = 0) } }
                }, modifier = Modifier.padding(start = 8.dp)) { Text("Reset all") }
            }
        }
    }
}

/** Material3 Slider rotated to vertical (bottom = min). */
@Composable
private fun VerticalSlider(value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Slider(
        value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, enabled = enabled,
        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
        modifier = Modifier
            .graphicsLayer { rotationZ = 270f; transformOrigin = TransformOrigin(0f, 0f) }
            .layout { measurable, constraints ->
                val p = measurable.measure(Constraints(minWidth = constraints.minHeight, maxWidth = constraints.maxHeight, minHeight = constraints.minWidth, maxHeight = constraints.maxWidth))
                layout(p.height, p.width) { p.place(-p.width, 0) }
            },
    )
}
