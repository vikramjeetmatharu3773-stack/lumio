# 06 — Settings & Customization (exact keys = `AppSettings` fields in code)

Storage: Preferences DataStore `lumio_settings`. Every row below exists in `AppSettings.kt` and `SettingsRepository.kt`. UI writes through `settings.update { copy(field = x) }`.

## Appearance
| Setting | Field | Type | Default | Range / options |
|---|---|---|---|---|
| Theme | `themeMode` | enum | SYSTEM | SYSTEM, LIGHT, DARK, AMOLED |
| Accent source | `accentSource` | enum | ARTWORK | ARTWORK, MATERIAL_YOU (31+), CUSTOM |
| Custom accent | `customAccent` | Int ARGB | 0xFF7C8CFF | color picker (12 swatches + hue/sat sliders) |
| Backdrop | `backdropStyle` | enum | ARTWORK | ARTWORK, AURORA, SOLID |
| Glass blur | `glassBlur` | Float | 0.6 | 0–1 |
| Glass opacity | `glassOpacity` | Float | 0.5 | 0–1 |
| Glass border | `glassBorder` | Float | 0.7 | 0–1 |
| Glass specular | `glassSpecular` | Float | 0.6 | 0–1 |
| Glass refraction | `glassRefraction` | Float | 0.5 | 0–1 |
| Corner radius | `glassCorner` | Int dp | 28 | 8–40 |
| Reduce transparency | `reduceTransparency` | Boolean | false | |
| Dock style | `dockStyle` | enum | STACKED | STACKED, CLASSIC |
| Artwork shape | `artworkShape` | enum | ROUNDED | ROUNDED, SQUARE, CIRCLE |
| Library layout | `libraryLayout` | enum | LIST | LIST, GRID |
| Grid columns | `gridColumns` | Int | 2 | 2–4 |
| Compact rows | `compactRows` | Boolean | false | |
| Animations | `animationsEnabled` | Boolean | true | |
| Animation speed | `animationSpeed` | Float | 1.0 | 0.5–2.0 |
| Haptics | `haptics` | Boolean | true | |
| Keep screen on (Now Playing) | `keepScreenOn` | Boolean | false | |

## Playback
| Setting | Field | Type | Default | Range |
|---|---|---|---|---|
| Fade between songs | `fadeMs` | Int ms | 0 | 0–12000 |
| Skip silence | `skipSilence` | Boolean | false | |
| Default speed | `playbackSpeed` | Float | 1.0 | 0.5–2.0 |
| Default pitch | `playbackPitch` | Float | 1.0 | 0.5–2.0 |
| Normalize volume | `normalizeVolume` | Boolean | false | (loudness enhancer) |
| Resume on launch | `resumeOnLaunch` | Boolean | true | |
| Pause when headphones unplugged | `pauseOnUnplug` | Boolean | true | |
| Seek step (notification/widget skip) | `seekStepSec` | Int | 10 | 5–60 |
| Stop when app removed from recents | `stopOnTaskRemoved` | Boolean | false | |
| Save listening history | `historyEnabled` | Boolean | true | |

## Library & Scan
| Setting | Field | Type | Default | Range |
|---|---|---|---|---|
| Scan mode | `scanMode` | enum | ALL_DEVICE | ALL_DEVICE, SELECTED_ONLY |
| Minimum length | `minDurationSec` | Int | 30 | 0–300 |
| Include non-music audio | `includeNonMusic` | Boolean | false | |
| Include `.nomedia` folders (deep scan) | `includeNoMedia` | Boolean | false | |
| Scan on launch | `autoScanOnLaunch` | Boolean | true | |
| Rescan when files change | `autoScanOnChange` | Boolean | true | |
| Default song sort | `songSort` / `songSortAsc` | enum/Boolean | TITLE / true | TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, PLAY_COUNT, YEAR |
| Ignore "The/A/An" when sorting | `ignoreArticles` | Boolean | true | |
| Last scan time | `lastScanAt` | Long | 0 | read-only |
Folders list lives in DB table `scan_folders` (not DataStore).

## Lyrics
`lyricsSizeSp` Int 22 (16–34) · `lyricsCenter` Boolean true.

## Widgets (defaults for newly added widgets)
`widgetOpacity` Float 0.55 · `widgetCorner` Int 28 · `widgetBackground` enum ARTWORK_GLASS (ARTWORK_GLASS, SOLID, GRADIENT). Each placed widget then stores its own copy (doc 09).

## Audio effects (Equalizer screen)
`eqEnabled` false · `eqPreset` "Flat" · `eqBands` CSV millibels · `bassBoost` 0–1000 · `virtualizer` 0–1000 · `loudnessMb` 0–2000.

## Hidden/state keys (not shown in UI)
`onboardingDone`, `lastSongUri`, `lastPositionMs`, `lastQueueIndex`, `recentSearches` (newline-joined, max 10).

## Settings screens layout
Each screen = scrollable column of **section glass cards**, rows 56 dp, 16 dp padding, dividers hairline. Components: `SettingsSwitch`, `SettingsSlider(label, valueText)`, `SettingsChoice(options)`, `SettingsColor`, `SettingsNav`. Every slider shows live value text. Reset-to-default icon on each section header.

## Customization ideas Hermes should implement in T18
1. **Presets** (one tap sets all glass + accent + backdrop fields): Aurora (default), Midnight (AMOLED, low glass opacity), Sunset (warm custom accent, high refraction), Mono (grayscale accent, border 1.0), Forest, Sakura, **Crystal** (opacity 0.15, blur 1.0, specular 1.0), **Frosted** (opacity 0.9, blur 0.3).
2. **My presets:** save current look under a name (stored as JSON in DataStore key `custom_presets`).
3. **Live preview card** reusing real components.
4. **Export/Import look** as small JSON (share with friends).
5. **Backup/Restore** all settings + playlists + favorites (+play counts) to a `.lumio.json` via SAF. Never include file paths of songs outside what's needed to re-match (match by title+artist+duration).
