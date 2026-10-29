package app.lumio.player.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class AccentSource { ARTWORK, MATERIAL_YOU, CUSTOM }
enum class BackdropStyle { ARTWORK, AURORA, SOLID }
enum class DockStyle { STACKED, CLASSIC }
enum class ArtworkShape { ROUNDED, SQUARE, CIRCLE }
enum class ListLayout { LIST, GRID }
enum class ScanMode { ALL_DEVICE, SELECTED_ONLY }
enum class SortField { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, PLAY_COUNT, YEAR }
enum class WidgetBackground { ARTWORK_GLASS, SOLID, GRADIENT }

/** Single source of truth for every user setting. Doc: docs/06_SETTINGS_AND_CUSTOMIZATION.md */
data class AppSettings(
    // ---- Appearance ----
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentSource: AccentSource = AccentSource.ARTWORK,
    val customAccent: Int = 0xFF7C8CFF.toInt(),
    val backdropStyle: BackdropStyle = BackdropStyle.ARTWORK,
    val glassBlur: Float = 0.6f,
    val glassOpacity: Float = 0.5f,
    val glassBorder: Float = 0.7f,
    val glassSpecular: Float = 0.6f,
    val glassRefraction: Float = 0.5f,
    val glassCorner: Int = 28,
    val reduceTransparency: Boolean = false,
    val dockStyle: DockStyle = DockStyle.STACKED,
    val artworkShape: ArtworkShape = ArtworkShape.ROUNDED,
    val libraryLayout: ListLayout = ListLayout.LIST,
    val gridColumns: Int = 2,
    val compactRows: Boolean = false,
    val animationsEnabled: Boolean = true,
    val animationSpeed: Float = 1f,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = false,
    // ---- Playback ----
    val fadeMs: Int = 0,
    val skipSilence: Boolean = false,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val normalizeVolume: Boolean = false,
    val resumeOnLaunch: Boolean = true,
    val pauseOnUnplug: Boolean = true,
    val seekStepSec: Int = 10,
    val stopOnTaskRemoved: Boolean = false,
    val historyEnabled: Boolean = true,
    // ---- Library & scan ----
    val scanMode: ScanMode = ScanMode.ALL_DEVICE,
    val minDurationSec: Int = 30,
    val includeNonMusic: Boolean = false,
    val includeNoMedia: Boolean = false,
    val autoScanOnLaunch: Boolean = true,
    val autoScanOnChange: Boolean = true,
    val songSort: SortField = SortField.TITLE,
    val songSortAsc: Boolean = true,
    val ignoreArticles: Boolean = true,
    val lastScanAt: Long = 0L,
    // ---- Lyrics ----
    val lyricsSizeSp: Int = 22,
    val lyricsCenter: Boolean = true,
    // ---- Widget defaults ----
    val widgetOpacity: Float = 0.55f,
    val widgetCorner: Int = 28,
    val widgetBackground: WidgetBackground = WidgetBackground.ARTWORK_GLASS,
    // ---- Audio effects ----
    val eqEnabled: Boolean = false,
    val eqPreset: String = "Flat",
    val eqBands: String = "",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessMb: Int = 0,
    // ---- Hidden state ----
    val onboardingDone: Boolean = false,
    val lastSongUri: String = "",
    val lastPositionMs: Long = 0L,
    val lastQueueIndex: Int = 0,
    val recentSearches: String = "",
)
