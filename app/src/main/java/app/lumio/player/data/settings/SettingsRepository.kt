package app.lumio.player.data.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(name = "lumio_settings")

class SettingsRepository(private val context: Context) {

    val flow: Flow<AppSettings> = context.settingsStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = flow.first()

    /** Usage: settings.update { copy(glassBlur = 0.8f) } */
    suspend fun update(transform: AppSettings.() -> AppSettings) {
        context.settingsStore.edit { prefs -> prefs.write(prefs.toSettings().transform()) }
    }

    private object K {
        val themeMode = stringPreferencesKey("theme_mode")
        val accentSource = stringPreferencesKey("accent_source")
        val customAccent = intPreferencesKey("custom_accent")
        val backdropStyle = stringPreferencesKey("backdrop_style")
        val glassBlur = floatPreferencesKey("glass_blur")
        val glassOpacity = floatPreferencesKey("glass_opacity")
        val glassBorder = floatPreferencesKey("glass_border")
        val glassSpecular = floatPreferencesKey("glass_specular")
        val glassRefraction = floatPreferencesKey("glass_refraction")
        val glassCorner = intPreferencesKey("glass_corner")
        val reduceTransparency = booleanPreferencesKey("reduce_transparency")
        val dockStyle = stringPreferencesKey("dock_style")
        val artworkShape = stringPreferencesKey("artwork_shape")
        val libraryLayout = stringPreferencesKey("library_layout")
        val gridColumns = intPreferencesKey("grid_columns")
        val compactRows = booleanPreferencesKey("compact_rows")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        val animationSpeed = floatPreferencesKey("animation_speed")
        val haptics = booleanPreferencesKey("haptics")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val fadeMs = intPreferencesKey("fade_ms")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val playbackSpeed = floatPreferencesKey("playback_speed")
        val playbackPitch = floatPreferencesKey("playback_pitch")
        val normalizeVolume = booleanPreferencesKey("normalize_volume")
        val resumeOnLaunch = booleanPreferencesKey("resume_on_launch")
        val pauseOnUnplug = booleanPreferencesKey("pause_on_unplug")
        val seekStepSec = intPreferencesKey("seek_step_sec")
        val stopOnTaskRemoved = booleanPreferencesKey("stop_on_task_removed")
        val historyEnabled = booleanPreferencesKey("history_enabled")
        val scanMode = stringPreferencesKey("scan_mode")
        val minDurationSec = intPreferencesKey("min_duration_sec")
        val includeNonMusic = booleanPreferencesKey("include_non_music")
        val includeNoMedia = booleanPreferencesKey("include_nomedia")
        val autoScanOnLaunch = booleanPreferencesKey("auto_scan_launch")
        val autoScanOnChange = booleanPreferencesKey("auto_scan_change")
        val songSort = stringPreferencesKey("song_sort")
        val songSortAsc = booleanPreferencesKey("song_sort_asc")
        val ignoreArticles = booleanPreferencesKey("ignore_articles")
        val lastScanAt = longPreferencesKey("last_scan_at")
        val lyricsSizeSp = intPreferencesKey("lyrics_size_sp")
        val lyricsCenter = booleanPreferencesKey("lyrics_center")
        val widgetOpacity = floatPreferencesKey("widget_opacity")
        val widgetCorner = intPreferencesKey("widget_corner")
        val widgetBackground = stringPreferencesKey("widget_background")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val bassBoost = intPreferencesKey("bass_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val loudnessMb = intPreferencesKey("loudness_mb")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val lastSongUri = stringPreferencesKey("last_song_uri")
        val lastPositionMs = longPreferencesKey("last_position_ms")
        val lastQueueIndex = intPreferencesKey("last_queue_index")
        val recentSearches = stringPreferencesKey("recent_searches")
    }

    private inline fun <reified E : Enum<E>> Preferences.enumOf(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOf(K.themeMode, d.themeMode),
            accentSource = enumOf(K.accentSource, d.accentSource),
            customAccent = this[K.customAccent] ?: d.customAccent,
            backdropStyle = enumOf(K.backdropStyle, d.backdropStyle),
            glassBlur = this[K.glassBlur] ?: d.glassBlur,
            glassOpacity = this[K.glassOpacity] ?: d.glassOpacity,
            glassBorder = this[K.glassBorder] ?: d.glassBorder,
            glassSpecular = this[K.glassSpecular] ?: d.glassSpecular,
            glassRefraction = this[K.glassRefraction] ?: d.glassRefraction,
            glassCorner = this[K.glassCorner] ?: d.glassCorner,
            reduceTransparency = this[K.reduceTransparency] ?: d.reduceTransparency,
            dockStyle = enumOf(K.dockStyle, d.dockStyle),
            artworkShape = enumOf(K.artworkShape, d.artworkShape),
            libraryLayout = enumOf(K.libraryLayout, d.libraryLayout),
            gridColumns = this[K.gridColumns] ?: d.gridColumns,
            compactRows = this[K.compactRows] ?: d.compactRows,
            animationsEnabled = this[K.animationsEnabled] ?: d.animationsEnabled,
            animationSpeed = this[K.animationSpeed] ?: d.animationSpeed,
            haptics = this[K.haptics] ?: d.haptics,
            keepScreenOn = this[K.keepScreenOn] ?: d.keepScreenOn,
            fadeMs = this[K.fadeMs] ?: d.fadeMs,
            skipSilence = this[K.skipSilence] ?: d.skipSilence,
            playbackSpeed = this[K.playbackSpeed] ?: d.playbackSpeed,
            playbackPitch = this[K.playbackPitch] ?: d.playbackPitch,
            normalizeVolume = this[K.normalizeVolume] ?: d.normalizeVolume,
            resumeOnLaunch = this[K.resumeOnLaunch] ?: d.resumeOnLaunch,
            pauseOnUnplug = this[K.pauseOnUnplug] ?: d.pauseOnUnplug,
            seekStepSec = this[K.seekStepSec] ?: d.seekStepSec,
            stopOnTaskRemoved = this[K.stopOnTaskRemoved] ?: d.stopOnTaskRemoved,
            historyEnabled = this[K.historyEnabled] ?: d.historyEnabled,
            scanMode = enumOf(K.scanMode, d.scanMode),
            minDurationSec = this[K.minDurationSec] ?: d.minDurationSec,
            includeNonMusic = this[K.includeNonMusic] ?: d.includeNonMusic,
            includeNoMedia = this[K.includeNoMedia] ?: d.includeNoMedia,
            autoScanOnLaunch = this[K.autoScanOnLaunch] ?: d.autoScanOnLaunch,
            autoScanOnChange = this[K.autoScanOnChange] ?: d.autoScanOnChange,
            songSort = enumOf(K.songSort, d.songSort),
            songSortAsc = this[K.songSortAsc] ?: d.songSortAsc,
            ignoreArticles = this[K.ignoreArticles] ?: d.ignoreArticles,
            lastScanAt = this[K.lastScanAt] ?: d.lastScanAt,
            lyricsSizeSp = this[K.lyricsSizeSp] ?: d.lyricsSizeSp,
            lyricsCenter = this[K.lyricsCenter] ?: d.lyricsCenter,
            widgetOpacity = this[K.widgetOpacity] ?: d.widgetOpacity,
            widgetCorner = this[K.widgetCorner] ?: d.widgetCorner,
            widgetBackground = enumOf(K.widgetBackground, d.widgetBackground),
            eqEnabled = this[K.eqEnabled] ?: d.eqEnabled,
            eqPreset = this[K.eqPreset] ?: d.eqPreset,
            eqBands = this[K.eqBands] ?: d.eqBands,
            bassBoost = this[K.bassBoost] ?: d.bassBoost,
            virtualizer = this[K.virtualizer] ?: d.virtualizer,
            loudnessMb = this[K.loudnessMb] ?: d.loudnessMb,
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            lastSongUri = this[K.lastSongUri] ?: d.lastSongUri,
            lastPositionMs = this[K.lastPositionMs] ?: d.lastPositionMs,
            lastQueueIndex = this[K.lastQueueIndex] ?: d.lastQueueIndex,
            recentSearches = this[K.recentSearches] ?: d.recentSearches,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.themeMode] = s.themeMode.name
        this[K.accentSource] = s.accentSource.name
        this[K.customAccent] = s.customAccent
        this[K.backdropStyle] = s.backdropStyle.name
        this[K.glassBlur] = s.glassBlur
        this[K.glassOpacity] = s.glassOpacity
        this[K.glassBorder] = s.glassBorder
        this[K.glassSpecular] = s.glassSpecular
        this[K.glassRefraction] = s.glassRefraction
        this[K.glassCorner] = s.glassCorner
        this[K.reduceTransparency] = s.reduceTransparency
        this[K.dockStyle] = s.dockStyle.name
        this[K.artworkShape] = s.artworkShape.name
        this[K.libraryLayout] = s.libraryLayout.name
        this[K.gridColumns] = s.gridColumns
        this[K.compactRows] = s.compactRows
        this[K.animationsEnabled] = s.animationsEnabled
        this[K.animationSpeed] = s.animationSpeed
        this[K.haptics] = s.haptics
        this[K.keepScreenOn] = s.keepScreenOn
        this[K.fadeMs] = s.fadeMs
        this[K.skipSilence] = s.skipSilence
        this[K.playbackSpeed] = s.playbackSpeed
        this[K.playbackPitch] = s.playbackPitch
        this[K.normalizeVolume] = s.normalizeVolume
        this[K.resumeOnLaunch] = s.resumeOnLaunch
        this[K.pauseOnUnplug] = s.pauseOnUnplug
        this[K.seekStepSec] = s.seekStepSec
        this[K.stopOnTaskRemoved] = s.stopOnTaskRemoved
        this[K.historyEnabled] = s.historyEnabled
        this[K.scanMode] = s.scanMode.name
        this[K.minDurationSec] = s.minDurationSec
        this[K.includeNonMusic] = s.includeNonMusic
        this[K.includeNoMedia] = s.includeNoMedia
        this[K.autoScanOnLaunch] = s.autoScanOnLaunch
        this[K.autoScanOnChange] = s.autoScanOnChange
        this[K.songSort] = s.songSort.name
        this[K.songSortAsc] = s.songSortAsc
        this[K.ignoreArticles] = s.ignoreArticles
        this[K.lastScanAt] = s.lastScanAt
        this[K.lyricsSizeSp] = s.lyricsSizeSp
        this[K.lyricsCenter] = s.lyricsCenter
        this[K.widgetOpacity] = s.widgetOpacity
        this[K.widgetCorner] = s.widgetCorner
        this[K.widgetBackground] = s.widgetBackground.name
        this[K.eqEnabled] = s.eqEnabled
        this[K.eqPreset] = s.eqPreset
        this[K.eqBands] = s.eqBands
        this[K.bassBoost] = s.bassBoost
        this[K.virtualizer] = s.virtualizer
        this[K.loudnessMb] = s.loudnessMb
        this[K.onboardingDone] = s.onboardingDone
        this[K.lastSongUri] = s.lastSongUri
        this[K.lastPositionMs] = s.lastPositionMs
        this[K.lastQueueIndex] = s.lastQueueIndex
        this[K.recentSearches] = s.recentSearches
    }
}
