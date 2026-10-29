# 05 — Animations & Motion

All durations scale by `animationSpeed` (0.5–2×); `animationsEnabled=false` or system "Remove animations" → durations 0 and springs replaced by snap.
Helpers live in `ui/theme/Motion.kt`: `LocalMotion.current.ms(base)`, `motion.spring()`.

## Spring presets
| Name | Spec | Used for |
|---|---|---|
| `Bounce` | damping 0.55, stiffness 500 | button press-release, favorite pop |
| `Smooth` | damping 0.82, stiffness 380 | sheet snap, pager settle, chips |
| `Gentle` | damping 0.9, stiffness 200 | artwork breathing, backdrop parallax |
| `Liquid` | damping 0.5, stiffness 350 | seek thumb squish |

## Catalog
| # | Trigger | Animation | Spec |
|---|---|---|---|
| 1 | Any glass button press | scale 1 → 0.94, release overshoot | `Bounce` |
| 2 | Chip select | wash fade-in + border brighten + scale 1.04→1 | 220 ms + `Smooth` |
| 3 | Song changes | Backdrop crossfade; accent color tween | 700 ms / 600 ms ease-in-out |
| 4 | Home hero pager | neighbors: scale 0.88, alpha 0.6, tiny rotationY ±6° by offset | per-frame from pager offset |
| 5 | Home sections enter | stagger fade + translateY 24→0, 60 ms apart (first load only) | 400 ms `FastOutSlowIn` |
| 6 | Mini player appears (first song) | slide up 80 dp + fade, glass "settles" (scale 0.96→1) | `Smooth` |
| 7 | Mini → Now Playing | sheet translateY with drag-follow; artwork shared-element-like grow; mini fades by progress | `Smooth`, velocity-aware |
| 8 | Now Playing artwork | play = 1.0, pause = 0.88 breathing; vinyl style = continuous rotation 14 s/rev while playing | `Gentle` |
| 9 | Artwork swipe | pager with parallax + next/prev sound-free haptic tick | `Smooth` |
| 10 | Play ↔ Pause icon | cross-morph: scale 0.6→1 + rotate −45→0 + fade | 220 ms |
| 11 | Seek drag | thumb 22→33 dp, track thickens 6→9 dp, label bubble above thumb | `Liquid` |
| 12 | Seek haptics | tick every 10 % while dragging | HapticFeedback.TextHandleMove |
| 13 | Favorite tap | heart scale 1→1.5→1 + 6 tiny particles radiating, color → accent | 450 ms |
| 14 | Playing row indicator | 3 bars random-height loop 600–900 ms | infinite |
| 15 | List item add/remove | `animateItem()` placement spring | `Smooth` |
| 16 | Selection mode | top bar cross-morph, checkmark pop | 200 ms |
| 17 | Bottom sheet open/close | scrim fade 0→0.5; panel translateY | `Smooth` |
| 18 | Screen navigation | forward: fade through + translateX 24 dp; back: reverse; shared header title | 300 ms |
| 19 | Dock item switch | active icon filled + bubble highlight slides (liquid blob) between buttons | `Smooth` |
| 20 | Scan progress | ring sweep + count ticker; finish = checkmark draw + confetti-free glow pulse | 600 ms |
| 21 | Lyrics line change | active line scale 1.0→1.08 + alpha 0.5→1; auto-scroll centered | 300 ms |
| 22 | Sleep timer chip | countdown text flip per minute; pulse at < 1 min | 300 ms |
| 23 | EQ band change | thumb glow intensity follows value | instant |
| 24 | Widget (static) | none; updates cross-fade handled by launcher | — |
| 25 | Skeleton shimmer | diagonal sweep alpha 0.08→0.2 | 1200 ms loop |
| 26 | Theme/glass slider changes | live preview updates every frame (no debounce on UI; debounce 250 ms on DataStore write) | — |
| 27 | Splash → Home | icon scale 1→1.15 + fade into UI | system splash + 300 ms |
| 28 | Predictive back | sheet/collection follow gesture progress | system |

## Implementation notes
- Prefer `animate*AsState`, `Animatable`, `updateTransition`; avoid `delay` loops except ticker.
- Heavy per-frame work belongs in `graphicsLayer { }` / `drawBehind { }` lambdas (no recomposition).
- Use `key()` and stable `@Immutable` models; never read `positionMs` high in the tree.
- Test with "Profile GPU rendering"; jank budget: < 5 % frames > 16 ms during Now-Playing open/close.
