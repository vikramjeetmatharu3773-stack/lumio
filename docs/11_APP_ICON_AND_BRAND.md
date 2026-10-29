# 11 — App Icon & Brand

## Concept: "the glass orb"
A deep indigo-to-violet field with a cyan glow bottom-right and a magenta glow top-left. In the center a
**frosted glass orb** (translucent white fill, bright rim, curved specular highlight) holds a bold white
**beamed eighth-note pair** — music inside glass. Reads at 48 px, looks premium at 512 px.

## Palette
| Name | Hex |
|---|---|
| Midnight | #0B1030 |
| Indigo | #2E1A7A |
| Violet | #6A3DFF |
| Cyan glow | #00E5FF (alpha) |
| Magenta glow | #FF4FD8 (alpha) |
| Glass white | #FFFFFF (alpha 0.10–0.9) |

## Files
| File | Purpose |
|---|---|
| `res/drawable/ic_launcher_background.xml` | adaptive background (gradient + glows) |
| `res/drawable/ic_launcher_foreground.xml` | adaptive foreground (orb + note), 66 dp safe zone |
| `res/drawable/ic_launcher_monochrome.xml` | Android 13 themed icon |
| `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` | adaptive icon definitions |
| `assets/icon/lumio_icon.svg` | master vector |
| `assets/icon/lumio_play_store_512.png` | Play Store 512×512 (no transparency) |
| `assets/icon/lumio_icon_preview_round.png` | preview in circle mask |
| `assets/icon/feature_graphic_spec.md` | Play feature graphic 1024×500 brief |

## Rules
Keep the glyph inside the 66 dp circle (adaptive safe zone). No text in the icon. Monochrome version = orb ring + note only.
Wordmark: "Lumio" in Bold, tracking −0.5, white on glass; used on onboarding only.

## Play Store listing kit (T25)
Short description (80): "Beautiful offline music player with liquid glass design. No ads, no internet."
Screenshots (8): Home, Now Playing, Library, Folders/Scan, Equalizer, Widgets, Theme tuner, Lyrics. 1080×1920, captured with the real app in dark theme with colorful album art.
Feature graphic: dark indigo gradient, orb at left, headline "Your music. Pure glass." at right.
