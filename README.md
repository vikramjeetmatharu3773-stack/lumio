# LUMIO — Local Liquid-Glass Music Player (Android) — Build Pack for Hermes

**Lumio** is a 100% offline music player for Android 8.0+ (API 26+).
No account, no ads, no tracking, **no INTERNET permission**. Liquid-glass UI inspired by the
Echo Music layout (mood chips, big artwork carousel, floating glass dock + mini player).

> "Lumio" is a placeholder name. To rename: change `app_name` in `strings.xml`,
> `rootProject.name` in `settings.gradle.kts`, and the package (task T02 explains how).

## What is in this pack

| Folder | What |
|---|---|
| `HERMES_START_HERE.md` | **Hermes reads this first.** Rules, workflow, build loop. |
| `PROGRESS.md` | Checklist Hermes updates after every task. |
| `docs/` | 13 specs: features, architecture, design system, screens, motion, settings, scanning, playback, widgets, DB, icon, QA, build env. |
| `tasks/` | 27 small, self-contained task files (T00–T26) + `INDEX.md`. One task = one focused session. |
| `agents/` | Prompts for the main agent and sub-agents. |
| `scripts/` | Termux setup, build, and pack-check scripts. |
| `project/` | **Real Android Studio / Gradle project** (Kotlin + Jetpack Compose + Media3). |
| `assets/icon/` | App icon master SVG, 512px Play Store PNG, previews. |

## Honest status of the code in `project/`

Written by hand as a strong foundation — **it has NOT been compiled yet** (no Android SDK / network
in the authoring environment). Expect a handful of compile errors on first build; Task **T00/T01**
is the "make it compile" loop.

Already written (real code — many UI strings are still hard-coded; task T23 moves them to `strings.xml`):
- Gradle setup, version catalog, manifest, resources, adaptive icon (vector), splash, themes
- Room DB (songs, user data, playlists, scan folders, history, queue)
- DataStore settings (~60 keys) — every customization knob
- MediaStore scanner + SAF folder deep-scan + include/exclude folders
- Media3 playback service + controller (queue, shuffle, repeat, speed, fade, sleep timer, resume)
- Audio effects (Equalizer, Bass Boost, Virtualizer, Loudness)
- Liquid-glass design system (backdrop, GlassSurface, buttons, liquid seek bar, dock, mini player)
- Screens: Onboarding, Home, Library (7 tabs), Collection detail, Search, Now Playing (+lyrics, queue),
  Settings (6 sub-screens), Folder & Scan manager, Equalizer
- Glance home-screen widget with glass renderer + per-widget configuration

To be built by Hermes from the tasks (specs are complete): stats/history screens, theme editor +
presets, backup/restore, tag editor/delete, smart playlists, M3U import/export, Android Auto,
tests, release signing, polish pass.

## Quick start (3 steps)

1. Copy this whole folder to Hermes' workspace (e.g. the `dropbox` folder on the SD card).
2. Tell Hermes: **"Read HERMES_START_HERE.md and begin with task T00."**
3. Get an APK either (a) on a PC with Android Studio, (b) on GitHub via the included Actions workflow
   (most reliable), or (c) best-effort inside Termux (see `docs/13_BUILD_ENVIRONMENT.md`).
