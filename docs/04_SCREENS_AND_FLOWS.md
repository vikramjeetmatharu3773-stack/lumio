# 04 — Screens & Navigation

## 4.1 Navigation map
```
Onboarding ──► AppRoot
AppRoot (always: backdrop + dock + mini player + NowPlaying sheet)
 ├─ home ─────────► history, stats, equalizer, settings, collection/*, playlists
 ├─ library?tab=  ► collection/{type}/{key}
 ├─ search ───────► collection/*, now playing
 ├─ collection/{type}/{key}   type = album|artist|genre|folder|playlist|favorites|mostplayed|recent|recentlyadded|forgotten|all|mood
 ├─ settings ─► settings/appearance | playback | library | lyrics | widgets | privacy | about
 │              └─ library ► folders (scan manager)
 ├─ equalizer · history · stats
 └─ NowPlaying (sheet, not a route) ─► queue sheet · lyrics · sleep · speed · song info
```
Dock is hidden on: onboarding, settings sub-screens' deep levels (optional), equalizer. Back from NowPlaying collapses it first.
Arguments are `Uri.encode`d strings. Bottom padding for every scrollable = dock height + mini player + 24 dp.

## 4.2 Onboarding
Full-screen aurora backdrop, floating glass orb (app icon) with slow breathing; title "Your music. Pure glass."; three glass rows (Offline & private · Any folder you choose · Hi-fi playback). Buttons: **Allow music access** → system permission(s) → **How should I find your music?** two big glass cards: *Whole device* / *Pick folders*. Then scan progress ring + count, finishing with a success burst and fade into Home. States: denied (explain + "Open settings"), no music found (suggest picking folders).

## 4.3 Home (matches the reference screenshot)
```
Lumio                [history][stats][eq][settings]     ← glass icon buttons in 1 capsule
(Feel good)(Work out)(Energise)(Relax)(Shuffle all)(Favorites) ← glass chips, horizontal
┌─prev─┐┌──────── HERO ────────┐┌─next─┐   ← pager, center card big, neighbors peek, scale/alpha by offset
│      ││  big artwork          ││      │
│      ││ Song title            ││      │
│      ││ Artists…              ││      │
└──────┘└───────────────────────┘└──────┘
[thumb] YOUR MIXES & PLAYLISTS ───────────────→   ← section header with arrow (opens Library▸Playlists)
 [Favorites][Most played][Recently added][Forgotten gems][Shuffle all][My playlist…]
Recently added  → row of CollectionCards
Albums → · Artists (circles) → · Folders →
```
Hero source priority: recently played → most played → recently added → random. Tap = play that list from that song. Long-press = options sheet. Empty library → friendly empty state with "Scan music" button.

## 4.4 Library
Top: title "Library", sort button, layout toggle, shuffle-all. Tab chips: Songs · Albums · Artists · Genres · Folders · Playlists · Favorites.
Songs: flat list + alphabet fast-scroller (right edge, bubble shows letter). Albums/Genres/Folders: grid of `CollectionCard` (columns from settings) or list. Artists: circular art. Playlists: grid + "New playlist" card. Long-press a song → selection mode (top bar morphs: count, play, next, queue, add-to-playlist, hide, select-all, close).

## 4.5 Collection detail
Large header: blurred art backdrop, artwork 220 dp (r28), title, subtitle ("12 songs · 48 min"), **Play** (filled accent glass) and **Shuffle** (glass) buttons, ⋮ menu. Then the song list. Playlist detail adds drag-reorder + swipe-to-remove. Artist detail: "Albums" row above songs.

## 4.6 Search
Glass text field with clear button, auto-focus. Chips: All · Songs · Albums · Artists · Genres · Folders · Playlists. Empty query → recent searches (clearable) + "Browse genres". Results grouped with section headers; top result highlighted. Debounce 120 ms. Keyboard "search" action hides keyboard.

## 4.7 Now Playing (sheet)
```
 ⌄            NOW PLAYING · <source>            ⋮
        ┌────────────────────────┐
        │     ARTWORK PAGER      │   swipe ← →, tap = flip to lyrics
        └────────────────────────┘
  Song title (marquee)                         ♡
  Artist · Album
  ───────●━━━━━━━━━━━━━━  1:23        −2:41   ← LiquidSeekBar
     🔀   ⏮    ( ▶ )    ⏭    🔁                ← glass controls, play = 76 dp
   [lyrics] [queue] [sleep] [EQ] [speed] [share]  ← small glass icon row
```
Gestures: drag down = collapse; artwork swipe; double-tap artwork = favorite; long-press title = song info. Backdrop = current artwork blur + scrim. Landscape: artwork left, controls right.

## 4.8 Queue sheet
Header "Up next · N songs · 18 min", buttons Clear, Save as playlist. Rows: drag handle, art, title/artist, remove ✕. Current row highlighted; auto-scroll to current. Swipe left = remove; "Play next" moves to position after current.

## 4.9 Settings
Landing: glass cards (icon + title + one-line summary) → Appearance · Playback · Library & Scan · Lyrics · Widgets · Privacy & Data · About. Appearance has a **live preview** card at top (mini dock + chip + card rendered with current glass values) above the sliders. Details: doc 06.

## 4.10 Folders / Scan manager (Settings ▸ Library & Scan ▸ Folders)
Segmented: *Whole device* / *Only selected folders*. List of folders: path, badge Include/Exclude, deep-scan toggle, song count, last scan, ⋮ remove / rescan. FAB-style glass button "+ Add folder" (system folder picker). Footer: **Scan now**, **Full rebuild**, progress card (phase + count). Warning card when "Only selected" and list is empty.

## 4.11 Equalizer
Enable switch; preset chips; vertical band sliders (glass thumbs) with center frequency labels and dB readout; Bass boost, Virtualizer, Loudness gain knobs/sliders; Reset. Unsupported device → explanatory card.

## 4.12 History & Stats (task T20)
History: grouped by day (Today/Yesterday/date), tap to replay, clear all. Stats: period chips (Week/Month/Year/All), big "hours listened" ring, top songs/artists/albums lists, listening-by-hour bar chart.

## 4.13 Empty / loading / error states
Every list has: skeleton shimmer (glass, 3 rows) while loading, empty-state illustration (glass orb + text + CTA), and permission-lost state routing back to onboarding.
