# 08 — Playback Engine

## Components
| Class | Role |
|---|---|
| `PlaybackService : MediaSessionService` | Owns `ExoPlayer` + `MediaSession`; foreground notification handled by Media3 |
| `PlayerController` | UI-side singleton; wraps `MediaController`; exposes `state: StateFlow<PlayerState>` and `position: StateFlow<Long>`; records history; persists queue |
| `AudioEffectsController` | Equalizer / BassBoost / Virtualizer / LoudnessEnhancer attached to the player's audio session |
| `SleepTimerController` | Countdown / end-of-track; emits an event the service turns into `pause()` |
| `ArtworkBitmapLoader` | Lets the notification & lockscreen show embedded album art for `content://` audio URIs |

## ExoPlayer configuration
`AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC)` with `handleAudioFocus = true` (duck on transient loss, pause on loss) · `setHandleAudioBecomingNoisy(settings.pauseOnUnplug)` · `setWakeMode(WAKE_MODE_LOCAL)` · gapless is automatic for compatible formats · `skipSilenceEnabled` from settings · default `PlaybackParameters(speed, pitch)` from settings.

## Queue model
Queue = ExoPlayer's media item list (MediaItem.mediaId = song URI). `PlayerController` maps items back to `Song` via `library.songIndex` (uri→Song). Play actions:
`playSongs(list, startIndex, shuffle)`, `playNext(song)`, `addToQueue(songs)`, `removeAt`, `move(from,to)`, `skipTo(index)`, `clear()`.
Shuffle uses ExoPlayer's shuffle mode (order preserved when turned off).

## Persistence & resume
On change (debounced 1.5 s) and on pause: write `queue_items` (uris in order) + settings `lastQueueIndex`, `lastPositionMs`, `lastSongUri`. On launch if `resumeOnLaunch` and queue empty: `setMediaItems(restored, index, position)`, `prepare()` — **do not auto-play**.

## History & play counts
`PlayerController` accumulates `listenedMs` while playing. On item change/end: if `listenedMs ≥ min(30 s, duration/2)` → `userData.recordPlay(uri, now)` + `history.insert` (if `historyEnabled`). Skips (< 5 s) increment `skipCount`.

## Fade between songs
Service loop every 200 ms while playing: `vol = min(pos/fade, remaining/fade, 1)` clamped to [0.0, 1]; `fadeMs = 0` → vol 1. This is a *fade out/in*, not an overlap. True crossfade (two ExoPlayers) = P2.

## Sleep timer
Minutes timer: countdown; at 0 → `pause()` (+ optional 5 s fade). End-of-track: on next automatic transition → `pause()`. State visible in Now Playing chip + notification-less.

## Audio effects
Created with the player's `audioSessionId`, re-created on `onAudioSessionIdChanged`. Equalizer: `numberOfBands`, `bandLevelRange` (millibels), `getCenterFreq` (milliHz). App presets are defined as 5-point dB curves and interpolated to the device's band count. Always wrap in try/catch; if any effect throws, mark unsupported and keep playing.

| Preset | dB curve (60 Hz → 14 kHz) |
|---|---|
| Flat | 0 0 0 0 0 |
| Bass | +6 +4 0 −1 −2 |
| Treble | −2 −1 0 +4 +6 |
| Vocal | −2 0 +4 +3 0 |
| Rock | +5 +2 −1 +3 +5 |
| Pop | −1 +3 +4 +2 −1 |
| Jazz | +3 +2 −2 +2 +4 |
| Classical | +4 +3 −1 +3 +4 |
| Electronic | +5 +3 0 +2 +5 |

## Lyrics
`LyricsRepository.load(song)` order: (1) app-private `lyrics/<hash(uri)>.lrc` (imported by user), (2) `.lrc` with the same base name found in the user's chosen lyrics folder (SAF) or beside the file when readable, (3) none. LRC parser supports `[mm:ss.xx]`, multiple timestamps per line, `[offset:±ms]`, ignores metadata tags. Plain text (no timestamps) shown unsynced.

## Notification / lockscreen / Bluetooth
Media3 `DefaultMediaNotificationProvider`: artwork, title/artist, prev/play/next (+ seek via system). Session activity opens `MainActivity` with the Now Playing sheet expanded (extra `open_now_playing=true`). Bluetooth AVRCP metadata is automatic from `MediaMetadata`.

## External intents
`ACTION_VIEW` with `audio/*` (content/file): play it as a single-item queue and add siblings only if resolvable in the library.

## Android Auto (P2, T22)
Convert service to `MediaLibraryService`, expose root → Playlists / Albums / Artists / Recent; add `automotive_app_desc.xml` + meta-data in manifest.

## Failure handling
`onPlayerError`: skip to next item, toast "Couldn't play <title>", after 3 consecutive errors stop. Missing file → remove from queue.
