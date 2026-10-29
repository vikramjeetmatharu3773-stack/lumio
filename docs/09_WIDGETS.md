# 09 — Widgets (Glance, "liquid glass" rendered)

Launchers cannot blur what is behind a widget, so the **glass is painted by the app** into a bitmap
(`WidgetGlassRenderer`): blurred artwork + frost tint + specular wash + edge light + rounded corners, then used as the widget background via `ImageProvider`. Result looks like a frosted card on every launcher and API level.

## Widget 1 — Now Playing (responsive, one provider, three layouts)
| Size (cells) | Layout |
|---|---|
| 2×2 (≈110 dp) | Artwork fills card (rounded), glass pill with play/pause bottom-right, title overlay bottom-left |
| 4×2 (≈250×110) | Artwork 88 dp left; title, artist; row prev · play · next; thin glass tint |
| 4×4 (≈250×250) | Large artwork top, title/artist, 5-button row (shuffle, prev, play, next, repeat), heart |
Empty state: "Tap to play music" with app icon, opens app.

## Per-widget configuration (opens when placed; re-openable by long-press → reconfigure)
| Option | Key | Default | Range |
|---|---|---|---|
| Background style | `bg` | ARTWORK_GLASS | ARTWORK_GLASS, SOLID, GRADIENT |
| Glass opacity | `opacity` | 0.55 | 0.1–1.0 |
| Blur | `blur` | 0.6 | 0–1 |
| Corner radius | `corner` | 28 | 0–40 dp |
| Text color | `text` | AUTO | AUTO, LIGHT, DARK |
| Show artwork | `art` | true | |
| Show controls | `controls` | true | |
| Accent color for buttons | `accent` | from artwork | |
Stored in a separate DataStore `lumio_widgets`, keys `w<id>_<name>`. Config activity shows a live preview using the same renderer.

## Extra widgets (P1, T19)
- **Shuffle** 1×1 button (starts shuffle-all)
- **Mini bar** 4×1: art + title + play
- **Recently played** 4×2/4×4 grid of 4/8 albums, tap = play

## Behavior
- Updates via `NowPlayingWidget().updateAll(context)` when current song / isPlaying changes (debounced 300 ms) and when config changes.
- Button callbacks are `ActionCallback`s calling `container.player` (connects to service if needed).
- Bitmap budget: render ≤ 3 bitmaps per update (small 400², medium 800×360, large 800²); recycle old.
- Cold start: if player not connected, show last played song from settings (`lastSongUri`).
- Never do network. Never block `provideGlance` > 2 s (artwork load has a 1.5 s timeout).

## Acceptance
Place all three sizes; change opacity/corner in config → visible change; play/pause/next from widget works with app closed; widget survives device reboot.
