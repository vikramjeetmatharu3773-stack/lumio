# 02 — Architecture

## Stack (pinned — do not upgrade without a task)
Kotlin 2.0.21 · AGP 8.7.3 · Gradle 8.10.2 · JDK 17 · compileSdk/targetSdk 35 · minSdk 26
Jetpack Compose (BOM 2024.11.00, Material3) · Navigation-Compose 2.8.4 · Lifecycle 2.8.7
Media3 1.4.1 (exoplayer + session) · Room 2.6.1 (KSP) · DataStore Preferences 1.1.1
Coil 2.7.0 · Glance 1.1.1 (widgets) · Palette 1.0.0 · WorkManager 2.10.0 · Reorderable 2.4.0
Manual DI (`AppContainer`) — no Hilt, fewer moving parts for the build agent.

## Layers
```
UI (Compose screens)                    ui/
   │  collects StateFlow, calls fun
Domain-ish repositories                  data/repo/  playback/PlayerController
   │
Data: Room (db/) · DataStore (settings/) · MediaStore+SAF scanner (scan/)
Playback: PlaybackService (ExoPlayer + MediaSession) ←IPC→ MediaController in PlayerController
```
Everything lives in ONE process. `LumioApp.container` (an `AppContainer`) owns singletons:
`db, settings, scanner, library, playlists, lyrics, effects, sleepTimer, player`.
Both the `PlaybackService` and the UI read the same container (service gets it via `applicationContext.container`).

## Package map (`app.lumio.player`)
```
LumioApp.kt            Application + Coil ImageLoader factory
AppContainer.kt        singletons
MainActivity.kt        edge-to-edge, splash, permission gate, intents
data/model/            Song, Album, Artist, Genre, FolderItem, PlaylistItem, enums
data/db/               Entities, DAOs, AppDatabase
data/settings/         AppSettings (data class + enums), SettingsRepository (DataStore)
data/scan/             MediaScanner (MediaStore + SAF), FolderUtils
data/repo/             LibraryRepository, PlaylistRepository, LyricsRepository
playback/              PlaybackService, PlayerController, AudioEffectsController, SleepTimerController
util/                  ArtworkLoader, ArtworkFetcher (Coil), ArtworkBitmapLoader, BitmapBlur, Format
ui/theme/              Theme, Glass (style + backdrop), Type, Motion
ui/components/         GlassSurface, GlassButtons, LiquidSeekBar, SongRow, Artwork, Dock, MiniPlayer, Sheets, SettingsRows
ui/nav/                Routes, AppRoot (NavHost + dock + now-playing sheet)
ui/screens/            Onboarding, Home, Library, Collection, Search, NowPlaying, Queue, Settings*, Folders, Equalizer
widget/                NowPlayingWidget, receiver, actions, glass renderer, config activity
```

## State handling (important)
Repositories expose **hot, app-scoped `StateFlow`s** (`library.songs`, `player.state`, …) so screens just `collectAsStateWithLifecycle()` them.
The scaffold therefore uses **no ViewModels**; small UI state uses `remember` / `rememberSaveable`. If a screen grows heavy logic, add a ViewModel
for that screen only (factory via `viewModelFactory { initializer { Vm(container) } }`).

## Key data flows
1. **Scan:** `MediaScanner.scan()` → rows into `songs` table → `LibraryRepository.songs` (Flow→StateFlow) → derived albums/artists/genres/folders (in memory, ≤50k songs OK) → UI.
2. **Play:** UI calls `player.playSongs(list, index)` → `MediaController` → `PlaybackService` ExoPlayer → listener → `PlayerController.state` → UI + widget + notification.
3. **User data** (favorite, plays, hidden) lives in `user_data`, **separate from `songs`**, so rescans never erase it.
4. **Backdrop:** current song artwork → Palette + 64px blurred bitmap → `BackdropState` → drawn by root background AND by every `GlassSurface` (aligned by window position). See doc 03.
5. **Settings:** `SettingsRepository.flow` (DataStore) → `LumioTheme` → CompositionLocals (glass, motion) → whole UI reacts live.

## Threading
Scanner/DB/IO: `Dispatchers.IO`. Library derivations: `Dispatchers.Default`. Player & UI: Main. App-scope coroutines live in `AppContainer.scope`.

## Permissions (manifest)
`READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` (maxSdk 32), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`. **No INTERNET.**

## API-level behavior matrix
| Feature | 26–28 | 29–30 | 31+ | 33+ |
|---|---|---|---|---|
| Folder path | `DATA` column | `RELATIVE_PATH` + `VOLUME_NAME` | same | same |
| Genre column | tag-less (empty) | 30+: `GENRE` | ✓ | ✓ |
| Artwork | MediaMetadataRetriever | `loadThumbnail` | ✓ | ✓ |
| Delete file | `contentResolver.delete` | 30+: `createDeleteRequest` | ✓ | ✓ |
| Audio permission | READ_EXTERNAL | READ_EXTERNAL | READ_EXTERNAL (≤32) | READ_MEDIA_AUDIO |
| Notifications | auto | auto | auto | runtime permission |
| Material You colors | ✗ | ✗ | ✓ | ✓ |

## Error handling rules
- Any `contentResolver.query` returning null → treat as empty, never crash.
- A song whose file is missing: skip in queue, show toast once, remove on next scan.
- All `catch` blocks in scanner/artwork loaders swallow and continue (log with `Log.w`).

## Module split (optional later)
`:core:data`, `:core:playback`, `:core:ui`, `:feature:*`. Not needed for v1.
