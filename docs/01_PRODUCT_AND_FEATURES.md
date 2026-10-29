# 01 — Product & Feature Catalog

**Name:** Lumio (placeholder) · **Tagline:** "Your music. Pure glass." · **Min SDK 26 · Target 35**
**Principles:** 100% local · zero network · zero tracking · fast on mid-range phones · gorgeous but never janky.
Priority: **P0** = ships in v1.0, **P1** = v1.0 if time allows, **P2** = v1.1 stretch.
"Code" column: ✅ scaffold already has working code, 🔧 partial, ⬜ Hermes builds from task.

## A. Onboarding & permissions
| ID | Feature | Pri | Code | Notes |
|---|---|---|---|---|
| A1 | Welcome screen with glass hero + 3 value points | P0 | ✅ | Offline, private, beautiful |
| A2 | Audio permission request (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE ≤32) | P0 | ✅ | Explain why; handle "don't ask again" → open app settings |
| A3 | Notification permission (Android 13+) | P0 | ✅ | Needed for media notification |
| A4 | Choose scan mode: whole device / pick folders | P0 | ✅ | Changeable later in Settings |
| A5 | First scan with live progress | P0 | ✅ | Count + phase text |

## B. Library & scanning (details: doc 07)
| ID | Feature | Pri | Code |
|---|---|---|---|
| B1 | MediaStore scan (title, artist, album, genre, year, track, duration, size, mime) | P0 | ✅ |
| B2 | Scan whole device OR selected folders only | P0 | ✅ |
| B3 | Add specific folders (SAF picker) — include list | P0 | ✅ |
| B4 | Exclude folders (blacklist, wins over include) | P0 | ✅ |
| B5 | Per-folder "deep scan" (reads files MediaStore missed, incl. `.nomedia` folders) | P0 | ✅ |
| B6 | Min duration filter (default 30 s) | P0 | ✅ |
| B7 | Include/exclude non-music audio (ringtones, notifications, recordings) | P0 | ✅ |
| B8 | Auto-scan on launch + auto-rescan when MediaStore changes (debounced) | P0 | 🔧 |
| B9 | Manual "Scan now" and "Full rebuild" | P0 | ✅ |
| B10 | Per-folder song counts + last-scan time | P1 | ✅ |
| B11 | Hide/unhide a song (without deleting file) | P1 | 🔧 |
| B12 | Duplicate finder (same title+artist+duration) | P2 | ⬜ |
| B13 | Multi-artist splitting by separators (`;` `/` `,`) | P1 | ⬜ |
| B14 | Album-artist grouping, "ignore The/A" sort | P1 | ⬜ |
| B15 | Folder cover art (cover.jpg/folder.jpg) fallback | P2 | ⬜ |
| B16 | CUE sheet support | P2 | ⬜ |

## C. Browsing
| ID | Feature | Pri | Code |
|---|---|---|---|
| C1 | Home: header actions, mood chips, hero artwork carousel, mixes row, recently added, albums, artists, folders | P0 | ✅ |
| C2 | Mood mixes: Feel good / Work out / Energise / Relax (genre-keyword based, editable) | P0 | 🔧 |
| C3 | Smart mixes: Favorites, Most played, Recently played, Recently added, Forgotten gems, Shuffle all | P0 | ✅ |
| C4 | Library tabs: Songs, Albums, Artists, Genres, Folders, Playlists, Favorites | P0 | ✅ |
| C5 | Sort (title, artist, album, date added, duration, plays, year) asc/desc | P0 | ✅ |
| C6 | List ↔ grid toggle, grid columns 2–4 | P0 | ✅ |
| C7 | Alphabet fast-scroller with letter bubble | P0 | ✅ |
| C8 | Multi-select (play, play next, queue, add to playlist, hide) | P0 | ✅ |
| C9 | Collection detail (album/artist/genre/folder/playlist) with Play + Shuffle | P0 | ✅ |
| C10 | Folder browser as a tree (navigate into sub-folders) | P1 | ⬜ |

## D. Search
| ID | Feature | Pri | Code |
|---|---|---|---|
| D1 | Instant local search across songs/albums/artists/genres/folders/playlists | P0 | ✅ |
| D2 | Filter chips + recent searches | P0 | ✅ |
| D3 | Diacritic-insensitive, multi-word matching | P1 | 🔧 |

## E. Playback core (details: doc 08)
| ID | Feature | Pri | Code |
|---|---|---|---|
| E1 | Media3 ExoPlayer in foreground MediaSessionService | P0 | ✅ |
| E2 | Gapless playback | P0 | ✅ |
| E3 | Shuffle, repeat off/all/one | P0 | ✅ |
| E4 | Play next / add to queue / reorder / remove / clear | P0 | ✅ |
| E5 | Resume last queue + position on launch | P0 | ✅ |
| E6 | Audio focus, duck/pause rules, pause on headphone unplug | P0 | ✅ |
| E7 | Speed 0.5–2.0×, pitch 0.5–2.0× | P0 | ✅ |
| E8 | Skip silence | P1 | ✅ |
| E9 | Fade in/out between tracks (0–12 s) | P0 | ✅ |
| E10 | True crossfade (two players overlapping) | P2 | ⬜ |
| E11 | Volume normalization (ReplayGain tags / loudness enhancer) | P2 | 🔧 |
| E12 | Previous-button restarts song if >3 s in | P0 | ✅ |
| E13 | Open-with: play audio files from other apps (VIEW intent) | P0 | ✅ |
| E14 | Formats: mp3, aac/m4a, flac, ogg, opus, wav, amr, mid | P0 | ✅ |

## F. Now Playing
| ID | Feature | Pri | Code |
|---|---|---|---|
| F1 | Expandable sheet from mini player (drag + tap), swipe down to collapse | P0 | ✅ |
| F2 | Artwork pager (swipe to change track), breathing scale on play/pause | P0 | ✅ |
| F3 | Dynamic colors + blurred artwork backdrop from current song | P0 | ✅ |
| F4 | Liquid seek bar (glass thumb squishes while dragging), time labels | P0 | ✅ |
| F5 | Controls: shuffle, prev, play/pause (morph), next, repeat | P0 | ✅ |
| F6 | Favorite heart with burst animation | P0 | ✅ |
| F7 | Lyrics view (synced .lrc, plain text) | P0 | ✅ |
| F8 | Queue sheet with drag-reorder + swipe-remove | P0 | ✅ |
| F9 | Sleep timer sheet; EQ shortcut; speed sheet; share; song info | P0 | ✅ |
| F10 | Artwork shape (rounded/square/circle-vinyl spin) | P1 | 🔧 |
| F11 | Tilt-reactive glass highlight (gyro) | P2 | ⬜ |
| F12 | Landscape + tablet layout | P1 | ⬜ |
| F13 | Car mode (huge buttons) | P2 | ⬜ |

## G. Playlists
| ID | Feature | Pri | Code |
|---|---|---|---|
| G1 | Create / rename / delete playlists | P0 | ✅ |
| G2 | Add songs from anywhere; reorder; remove | P0 | 🔧 |
| G3 | Import/Export M3U/M3U8 | P1 | ⬜ |
| G4 | Smart playlists (rules: genre, year, rating, plays, added) | P2 | ⬜ |

## H. Favorites, history, stats
| ID | Feature | Pri | Code |
|---|---|---|---|
| H1 | Favorites (survive rescans) | P0 | ✅ |
| H2 | Play count + last played (counts after 30 s or 50 %) | P0 | ✅ |
| H3 | History screen (grouped by day) | P1 | ⬜ |
| H4 | Stats screen: total time, top songs/artists/albums, this week/month/all-time | P1 | ⬜ |
| H5 | Privacy: pause history, clear history | P1 | 🔧 |

## I. Audio effects
| ID | Feature | Pri | Code |
|---|---|---|---|
| I1 | Equalizer (device bands) + presets (Flat, Bass, Treble, Vocal, Rock, Pop, Jazz, Classical, Electronic) | P0 | ✅ |
| I2 | Bass boost, Virtualizer, Loudness gain | P0 | ✅ |
| I3 | Custom preset save/delete | P1 | ⬜ |
| I4 | Graceful "not supported on this device" state | P0 | ✅ |

## J. Sleep timer & utilities
| ID | Feature | Pri | Code |
|---|---|---|---|
| J1 | Sleep timer (5–120 min, end of track, custom) with countdown chip | P0 | ✅ |
| J2 | Fade-out at timer end | P1 | ⬜ |
| J3 | Set as ringtone, delete from device, tag editor, share file | P1 | ⬜ |

## K. System integration
| ID | Feature | Pri | Code |
|---|---|---|---|
| K1 | Media notification with artwork + lockscreen controls | P0 | ✅ |
| K2 | Bluetooth/headset buttons, car metadata | P0 | ✅ |
| K3 | App shortcuts: Shuffle all, Continue playing | P1 | ⬜ |
| K4 | Quick Settings tile (play/pause) | P2 | ⬜ |
| K5 | Android Auto (MediaLibraryService + browse tree) | P2 | ⬜ |
| K6 | Themed monochrome launcher icon (Android 13+) | P0 | ✅ |
| K7 | Predictive back gesture support | P0 | ✅ |

## L. Widgets (doc 09)
| ID | Feature | Pri | Code |
|---|---|---|---|
| L1 | Responsive glass Now-Playing widget (2×2, 4×2, 4×4 layouts in one widget) | P0 | ✅ |
| L2 | Per-widget config: opacity, corner radius, background style, text color, show artwork | P0 | ✅ |
| L3 | Extra widgets: Shuffle button, Mini bar (4×1), Recently played grid | P1 | ⬜ |

## M. Settings & customization (doc 06)
| ID | Feature | Pri | Code |
|---|---|---|---|
| M1 | Appearance: theme mode, AMOLED, accent source (artwork / Material You / custom), backdrop style | P0 | ✅ |
| M2 | **Glass tuner**: blur, opacity, border, specular, refraction, corner radius — with live preview | P0 | ✅ |
| M3 | Dock style (stacked / classic echo-like), artwork shape, list density, grid columns | P0 | ✅ |
| M4 | Motion: animation speed, reduce motion, haptics | P0 | ✅ |
| M5 | Playback, Library & Scan, Lyrics, Widgets, Privacy, About screens | P0 | ✅ |
| M6 | Theme presets (Aurora, Midnight, Sunset, Mono, Forest, Sakura) + save my own | P1 | ⬜ |
| M7 | Backup/restore settings + playlists + favorites to a JSON file | P1 | ⬜ |
| M8 | "Reduce transparency" accessibility mode | P0 | ✅ |
| M9 | Language: follow system; strings externalized for translation (Hindi, Punjabi ready) | P1 | ⬜ |

## N. Quality
| ID | Feature | Pri |
|---|---|---|
| N1 | TalkBack labels on every control, 48dp touch targets, contrast ≥ 4.5:1 | P0 |
| N2 | Cold start < 1.2 s on mid-range; scroll 60 fps on 10k-song library | P0 |
| N3 | Crash-safe: DB corruption fallback, missing-file skip, no ANR | P0 |
