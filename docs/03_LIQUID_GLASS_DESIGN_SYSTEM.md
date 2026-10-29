# 03 — Liquid Glass Design System

## 3.1 Look in one sentence
A deep, softly-lit world whose colors come from the current album art, with **frosted, edge-lit glass
capsules** (dock, mini player, chips, cards, sheets) floating above it and reacting with springs.

## 3.2 Tokens
**Spacing:** 4 · 8 · 12 · 16 · 20 · 24 · 32 · 48 dp. Screen horizontal padding 20 dp.
**Radius:** chip = pill · small card 20 · card 28 (default, user-tunable 8–40) · hero 36 · sheet top 32 · artwork 24.
**Touch target:** ≥ 48 dp. Dock buttons 60 dp circles. Mini player 64 dp tall pill.
**Type (system sans, weights carry the style):**
| Role | Size/Weight |
|---|---|
| Display (app title) | 34 sp / Bold, tracking −0.5 |
| Title (screen) | 26 sp / Bold |
| Heading (section) | 20 sp / SemiBold |
| Body | 15 sp / Medium |
| Secondary | 13 sp / Regular, onSurface @ 70 % |
| Caption | 11 sp / Medium |
Optional: bundle "Outfit" or "Plus Jakarta Sans" in `res/font` (task T02).
**Base colors:**
| Token | Dark | Light | AMOLED |
|---|---|---|---|
| background | #07070D | #F3F4FA | #000000 |
| onSurface | #F2F2F7 | #14141C | #FFFFFF |
| scrim (over backdrop) | black 35 % | white 25 % | black 45 % |
| accent default | #7C8CFF | #4F5BFF | #7C8CFF |
Accent comes from artwork Palette (vibrant → muted → dominant fallback), animated 600 ms.

## 3.3 The glass recipe (what `GlassSurface` draws, bottom to top)
1. **Backdrop sample** — the *same* pre-blurred artwork bitmap used by the screen background, drawn
   offset by the panel's window position (so the glass shows the exact pixels behind it) and magnified by
   `1 + 0.05 × refraction` around the panel center (lens feel).
2. **Frost tint** — dark: white @ (0.06 + 0.20 × opacity); light: white @ (0.30 + 0.45 × opacity); plus black @ 0.12 in dark.
3. **Specular wash** — diagonal gradient white @ (0.22 × specular) top-left → transparent bottom-right.
4. **Edge light** — 1 dp border, vertical gradient: top white @ 0.60×border → middle 0.08×border → bottom 0.28×border.
5. **Content** on top. Text on glass always uses `onSurface` and must reach 4.5:1 against the tinted result.

Tunables (live in Settings ▸ Appearance ▸ Glass): **Blur** (backdrop bitmap blur radius 2–14 px at 64 px), **Opacity**, **Border**, **Specular**, **Refraction**, **Corner radius**. Defaults: 0.6 / 0.5 / 0.7 / 0.6 / 0.5 / 28 dp.

**Reduce transparency mode:** replaces steps 1–3 with a solid surface `background @ 0.92` + the border. Also auto-enabled when the system "Remove animations / High contrast" is on.

**Why a shared bitmap instead of real blur?** Compose has no backdrop-blur primitive. Re-using one small blurred bitmap is cheap (one 64×64 bitmap), works on API 26+, and keeps scrolling smooth. Limitation: list rows scrolling *under* a glass panel are hidden, not frosted. Upgrade path (P2, task T10): the Haze library for true live blur on API 31+.

## 3.4 Backdrop generation
`BackdropFactory`: artwork → scale to 64×64 → 3-pass box blur (radius from setting) → boost saturation ×1.25 → vignette. No artwork → 4 radial blobs from accent/hue-shifted colors on `background` (**Aurora** style). Styles (setting): `ARTWORK`, `AURORA`, `SOLID`. Crossfade old→new 700 ms.

## 3.5 Component catalog
| Component | Spec |
|---|---|
| `GlassSurface` | Base. Params: shape, style, onClick (bounce), content |
| `GlassIconButton` | 48 dp circle default; icon 24; press scale 0.94 spring(0.55, 500) |
| `GlassChip` | Pill, 40 dp high, 16 dp h-padding; selected = accent @ 28 % wash + bright border |
| `GlassDock` | Stacked: 3 circular buttons (Home, Library, Search) inside a floating capsule; mini player above. Classic: Home ○ · mini-player pill · Search ○ in one row (matches Echo screenshot) |
| `MiniPlayer` | 64 dp pill: artwork 44 (r14), title/artist (marquee), play/pause, next; thin progress hairline along bottom edge; swipe left/right = next/prev; tap/drag up = expand |
| `LiquidSeekBar` | 6 dp track, accent gradient fill, 22 dp glass thumb → 33 dp while dragging (spring), haptic tick every 10 % |
| `SongRow` | Flat (no glass): 52 dp art r14, title, "artist · album", duration, ⋮; playing row shows animated bars + accent title |
| `CollectionCard` | Square art r24, title, subtitle; press → scale 0.96 |
| `HeroCard` | Tall (aspect 0.78) art r36, bottom gradient scrim, title 28 sp bold + artists 16 sp (as in screenshot) |
| `GlassBottomSheet` | In-composition (not a Dialog, so glass stays aligned): scrim + panel r32 top, drag-to-dismiss |
| `SettingsRows` | Switch, Slider, Choice, Color, Nav — flat rows grouped inside one glass card per section |
| `EqualizerBars` | 3–4 animated bars, accent color |

## 3.6 Iconography
Material Symbols Rounded via `material-icons-extended` (Filled for active, Outlined for inactive). 24 dp, 2 dp optical stroke.

## 3.7 Light theme
Glass becomes milky-white; backdrop is a lighter, desaturated artwork blur; shadows soft (blur 24, alpha 0.12, tinted accent).

## 3.8 Hard rules
- No glass inside `LazyColumn` items. Rows are flat.
- Max ≈ 6 simultaneous `GlassSurface` on screen; header buttons share one capsule when possible.
- Never animate `blur`/backdrop bitmap size per frame — only alpha/translation/scale.
