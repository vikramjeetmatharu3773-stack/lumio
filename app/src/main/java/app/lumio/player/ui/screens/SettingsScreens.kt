package app.lumio.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.BuildConfig
import app.lumio.player.container
import app.lumio.player.data.settings.*
import app.lumio.player.ui.components.*
import app.lumio.player.ui.nav.Routes
import kotlinx.coroutines.launch
import java.util.Locale

/** Small helper: settings state + updater used by every settings screen. */
@Composable
private fun rememberSettings(): Pair<AppSettings, ((AppSettings.() -> AppSettings) -> Unit)> {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val updater: (AppSettings.() -> AppSettings) -> Unit = { block -> scope.launch { c.settings.update(block) } }
    return s to updater
}

@Composable
fun SettingsLandingScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    ScreenScaffold("Settings", onBack = onBack) {
        item {
            SettingsSection(null) {
                SettingsNav("Appearance", "Theme, glass tuner, dock, motion", Icons.Rounded.Palette) { onNavigate(Routes.SET_APPEARANCE) }
                SettingsDivider()
                SettingsNav("Playback", "Fade, speed, resume, headphones", Icons.Rounded.PlayCircle) { onNavigate(Routes.SET_PLAYBACK) }
                SettingsDivider()
                SettingsNav("Library & scan", "Folders, filters, auto-scan", Icons.Rounded.LibraryMusic) { onNavigate(Routes.SET_LIBRARY) }
                SettingsDivider()
                SettingsNav("Lyrics", "Size and alignment", Icons.Rounded.Lyrics) { onNavigate(Routes.SET_LYRICS) }
                SettingsDivider()
                SettingsNav("Widgets", "Defaults for new widgets", Icons.Rounded.Widgets) { onNavigate(Routes.SET_WIDGETS) }
                SettingsDivider()
                SettingsNav("Privacy & data", "History and searches", Icons.Rounded.PrivacyTip) { onNavigate(Routes.SET_PRIVACY) }
                SettingsDivider()
                SettingsNav("About", "Version and licenses", Icons.Rounded.Info) { onNavigate(Routes.SET_ABOUT) }
            }
        }
    }
}

@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val (s, upd) = rememberSettings()
    ScreenScaffold("Appearance", onBack = onBack) {
        item { GlassPreview() }
        item {
            SettingsSection("Theme") {
                SettingsChoice("Mode", listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.AMOLED to "AMOLED"), s.themeMode) { upd { copy(themeMode = it) } }
                SettingsDivider()
                SettingsChoice("Accent color from", listOf(AccentSource.ARTWORK to "Album art", AccentSource.MATERIAL_YOU to "Material You", AccentSource.CUSTOM to "Custom"), s.accentSource) { upd { copy(accentSource = it) } }
                if (s.accentSource == AccentSource.CUSTOM) {
                    SettingsColors(
                        "Custom accent",
                        listOf(0xFF7C8CFF, 0xFFFF6B9D, 0xFFFFB454, 0xFF4ADE80, 0xFF22D3EE, 0xFFA78BFA, 0xFFF87171, 0xFFFACC15, 0xFF38BDF8, 0xFFF472B6, 0xFF34D399, 0xFFE5E7EB).map { it.toInt() },
                        s.customAccent,
                    ) { upd { copy(customAccent = it) } }
                }
                SettingsDivider()
                SettingsChoice("Background", listOf(BackdropStyle.ARTWORK to "Artwork blur", BackdropStyle.AURORA to "Aurora", BackdropStyle.SOLID to "Solid"), s.backdropStyle) { upd { copy(backdropStyle = it) } }
            }
        }
        item {
            SettingsSection("Glass tuner") {
                SettingsSlider("Blur", "${(s.glassBlur * 100).toInt()}%", s.glassBlur, 0f..1f, { upd { copy(glassBlur = it) } })
                SettingsSlider("Opacity", "${(s.glassOpacity * 100).toInt()}%", s.glassOpacity, 0f..1f, { upd { copy(glassOpacity = it) } })
                SettingsSlider("Border light", "${(s.glassBorder * 100).toInt()}%", s.glassBorder, 0f..1f, { upd { copy(glassBorder = it) } })
                SettingsSlider("Specular shine", "${(s.glassSpecular * 100).toInt()}%", s.glassSpecular, 0f..1f, { upd { copy(glassSpecular = it) } })
                SettingsSlider("Refraction", "${(s.glassRefraction * 100).toInt()}%", s.glassRefraction, 0f..1f, { upd { copy(glassRefraction = it) } })
                SettingsSlider("Corner radius", "${s.glassCorner} dp", s.glassCorner.toFloat(), 8f..40f, { upd { copy(glassCorner = it.toInt()) } }, steps = 31)
                SettingsSwitch("Reduce transparency", s.reduceTransparency, { upd { copy(reduceTransparency = it) } }, "Solid surfaces for maximum readability")
                TextButton(onClick = {
                    upd { val d = AppSettings(); copy(glassBlur = d.glassBlur, glassOpacity = d.glassOpacity, glassBorder = d.glassBorder, glassSpecular = d.glassSpecular, glassRefraction = d.glassRefraction, glassCorner = d.glassCorner, reduceTransparency = false) }
                }, modifier = Modifier.padding(start = 8.dp)) { Text("Reset glass to default") }
            }
        }
        item {
            SettingsSection("Layout") {
                SettingsChoice("Dock style", listOf(DockStyle.STACKED to "Stacked", DockStyle.CLASSIC to "Classic (Home · Player · Search)"), s.dockStyle) { upd { copy(dockStyle = it) } }
                SettingsDivider()
                SettingsChoice("Artwork shape", listOf(ArtworkShape.ROUNDED to "Rounded", ArtworkShape.SQUARE to "Square", ArtworkShape.CIRCLE to "Vinyl"), s.artworkShape) { upd { copy(artworkShape = it) } }
                SettingsDivider()
                SettingsChoice("Library layout", listOf(ListLayout.LIST to "List", ListLayout.GRID to "Grid"), s.libraryLayout) { upd { copy(libraryLayout = it) } }
                SettingsSlider("Grid columns", "${s.gridColumns}", s.gridColumns.toFloat(), 2f..4f, { upd { copy(gridColumns = it.toInt()) } }, steps = 1)
                SettingsSwitch("Compact song rows", s.compactRows, { upd { copy(compactRows = it) } })
            }
        }
        item {
            SettingsSection("Motion") {
                SettingsSwitch("Animations", s.animationsEnabled, { upd { copy(animationsEnabled = it) } })
                SettingsSlider("Animation speed", String.format(Locale.US, "%.1f×", s.animationSpeed), s.animationSpeed, 0.5f..2f, { upd { copy(animationSpeed = it) } })
                SettingsSwitch("Haptic feedback", s.haptics, { upd { copy(haptics = it) } })
                SettingsSwitch("Keep screen on in Now Playing", s.keepScreenOn, { upd { copy(keepScreenOn = it) } })
            }
        }
    }
}

/** Live preview: real glass components reading the current settings. */
@Composable
private fun GlassPreview() {
    val on = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip("Feel good", true, {})
            GlassChip("Work out", false, {})
        }
        GlassSurface(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.CenterStart) {
            Column(Modifier.padding(20.dp)) {
                Text("Glass card", style = MaterialTheme.typography.titleMedium, color = on)
                Text("Tune blur, opacity and shine below", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.75f))
            }
        }
        GlassSurface(shape = RoundedCornerShape(50)) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                GlassIconButton(Icons.Rounded.Home, "Home", {}, selected = true)
                GlassIconButton(Icons.Rounded.LibraryMusic, "Library", {})
                GlassIconButton(Icons.Rounded.Search, "Search", {})
            }
        }
    }
}

@Composable
fun SettingsPlaybackScreen(onBack: () -> Unit) {
    val (s, upd) = rememberSettings()
    ScreenScaffold("Playback", onBack = onBack) {
        item {
            SettingsSection("Transitions") {
                SettingsSlider("Fade between songs", if (s.fadeMs == 0) "Off" else String.format(Locale.US, "%.1f s", s.fadeMs / 1000f), s.fadeMs.toFloat(), 0f..12000f, { upd { copy(fadeMs = (it / 500f).toInt() * 500) } }, steps = 23)
                SettingsSwitch("Skip silence", s.skipSilence, { upd { copy(skipSilence = it) } }, "Trim silent gaps inside tracks")
                SettingsSwitch("Normalize volume", s.normalizeVolume, { upd { copy(normalizeVolume = it) } }, "Gentle loudness boost")
            }
        }
        item {
            SettingsSection("Defaults") {
                SettingsSlider("Default speed", String.format(Locale.US, "%.2f×", s.playbackSpeed), s.playbackSpeed, 0.5f..2f, { upd { copy(playbackSpeed = it) } })
                SettingsSlider("Default pitch", String.format(Locale.US, "%.2f×", s.playbackPitch), s.playbackPitch, 0.5f..2f, { upd { copy(playbackPitch = it) } })
                SettingsSlider("Seek step", "${s.seekStepSec} s", s.seekStepSec.toFloat(), 5f..60f, { upd { copy(seekStepSec = (it / 5f).toInt() * 5) } }, steps = 10)
            }
        }
        item {
            SettingsSection("Behavior") {
                SettingsSwitch("Resume on launch", s.resumeOnLaunch, { upd { copy(resumeOnLaunch = it) } }, "Restore your last queue and position")
                SettingsSwitch("Pause when headphones unplugged", s.pauseOnUnplug, { upd { copy(pauseOnUnplug = it) } })
                SettingsSwitch("Stop when app is removed from recents", s.stopOnTaskRemoved, { upd { copy(stopOnTaskRemoved = it) } })
                SettingsSwitch("Save listening history", s.historyEnabled, { upd { copy(historyEnabled = it) } })
            }
        }
    }
}

@Composable
fun SettingsLibraryScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val (s, upd) = rememberSettings()
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    ScreenScaffold("Library & scan", onBack = onBack) {
        item {
            SettingsSection("Where to look") {
                SettingsNav("Folders & scan manager", if (s.scanMode == ScanMode.ALL_DEVICE) "Whole device" else "Only selected folders", Icons.Rounded.FolderOpen) { onNavigate(Routes.FOLDERS) }
            }
        }
        item {
            SettingsSection("Filters") {
                SettingsSlider("Minimum length", if (s.minDurationSec == 0) "Any" else "${s.minDurationSec} s", s.minDurationSec.toFloat(), 0f..300f, { upd { copy(minDurationSec = (it / 5f).toInt() * 5) } }, steps = 59)
                SettingsSwitch("Include non-music audio", s.includeNonMusic, { upd { copy(includeNonMusic = it) } }, "Ringtones, notifications, recordings")
                SettingsSwitch("Include .nomedia folders", s.includeNoMedia, { upd { copy(includeNoMedia = it) } }, "Deep-scans your selected folders")
            }
        }
        item {
            SettingsSection("Automatic") {
                SettingsSwitch("Scan on launch", s.autoScanOnLaunch, { upd { copy(autoScanOnLaunch = it) } })
                SettingsSwitch("Rescan when files change", s.autoScanOnChange, { upd { copy(autoScanOnChange = it) } })
                SettingsSwitch("Ignore The / A / An when sorting", s.ignoreArticles, { upd { copy(ignoreArticles = it) } })
            }
        }
        item {
            SettingsSection("Scan") {
                val last = if (s.lastScanAt == 0L) "Never" else android.text.format.DateUtils.getRelativeTimeSpanString(s.lastScanAt).toString()
                Text("Last scan: $last", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(16.dp))
                if (scan.running) Text("${scan.phase} · ${scan.found} songs", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { scope.launch { c.scanner.scan() } }, enabled = !scan.running) { Text("Scan now") }
                    TextButton(onClick = { scope.launch { c.scanner.scan(rebuild = true) } }, enabled = !scan.running) { Text("Full rebuild") }
                }
            }
        }
    }
}

@Composable
fun SettingsLyricsScreen(onBack: () -> Unit) {
    val (s, upd) = rememberSettings()
    ScreenScaffold("Lyrics", onBack = onBack) {
        item {
            SettingsSection(null) {
                SettingsSlider("Text size", "${s.lyricsSizeSp} sp", s.lyricsSizeSp.toFloat(), 16f..34f, { upd { copy(lyricsSizeSp = it.toInt()) } }, steps = 17)
                SettingsSwitch("Center lines", s.lyricsCenter, { upd { copy(lyricsCenter = it) } })
                Text("Import a .lrc file from the Lyrics button in Now Playing. Synced lyrics highlight as the song plays.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun SettingsWidgetsScreen(onBack: () -> Unit) {
    val (s, upd) = rememberSettings()
    ScreenScaffold("Widgets", onBack = onBack) {
        item {
            SettingsSection("Defaults for new widgets") {
                SettingsChoice("Background", listOf(WidgetBackground.ARTWORK_GLASS to "Artwork glass", WidgetBackground.SOLID to "Solid", WidgetBackground.GRADIENT to "Gradient"), s.widgetBackground) { upd { copy(widgetBackground = it) } }
                SettingsSlider("Opacity", "${(s.widgetOpacity * 100).toInt()}%", s.widgetOpacity, 0.1f..1f, { upd { copy(widgetOpacity = it) } })
                SettingsSlider("Corner radius", "${s.widgetCorner} dp", s.widgetCorner.toFloat(), 0f..40f, { upd { copy(widgetCorner = it.toInt()) } }, steps = 39)
                Text("Add a widget from your home screen: long-press ▸ Widgets ▸ Lumio. Each widget can be customized after placing it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun SettingsPrivacyScreen(onBack: () -> Unit) {
    val (s, upd) = rememberSettings()
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("Privacy & data", onBack = onBack) {
        item {
            SettingsSection(null) {
                SettingsSwitch("Save listening history", s.historyEnabled, { upd { copy(historyEnabled = it) } })
                SettingsDivider()
                SettingsNav("Clear listening history", "Removes play history and stats", Icons.Rounded.DeleteSweep) { confirm = true }
                SettingsDivider()
                SettingsNav("Clear recent searches", null, Icons.Rounded.History) { upd { copy(recentSearches = "") } }
            }
        }
        item {
            Text("Lumio has no internet permission. Nothing ever leaves your device.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, title = { Text("Clear history?") }, text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { scope.launch { c.db.history().clear() }; confirm = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {
    val on = MaterialTheme.colorScheme.onSurface
    ScreenScaffold("About", onBack = onBack) {
        item {
            SettingsSection(null) {
                Column(Modifier.padding(20.dp)) {
                    Text("Lumio", style = MaterialTheme.typography.headlineMedium, color = on)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.75f))
                    Spacer(Modifier.height(12.dp))
                    Text("A private, offline music player with a liquid-glass design. No account, no ads, no tracking, no internet permission.", style = MaterialTheme.typography.bodyMedium, color = on)
                }
            }
        }
        item {
            SettingsSection("Open-source libraries") {
                Text("Jetpack Compose · Media3 (ExoPlayer) · Room · DataStore · Coil · Glance · Palette · Reorderable — all Apache-2.0.",
                    style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.8f), modifier = Modifier.padding(16.dp))
            }
        }
    }
}
