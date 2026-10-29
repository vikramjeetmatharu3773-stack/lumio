# T10 — Glass design system hardening

**Read ONLY:** docs/03_LIQUID_GLASS_DESIGN_SYSTEM.md; docs/05_ANIMATIONS_AND_MOTION.md rows 1-3,17,19

**Files you will touch:**
- `ui/theme/Glass.kt`
- `ui/theme/Theme.kt`
- `ui/components/GlassSurface.kt`
- `util/BitmapBlur.kt`

**Steps:**
1. Verify backdrop alignment: glass shows exact pixels behind it while scrolling, on rotation, inside the Now Playing sheet.
2. Implement Reduce-transparency path + auto-enable on system 'remove animations'.
3. Light-theme tuning pass.
4. Profile: with 6 glass surfaces scrolling Home stays > 55 fps on a mid device. If not, cache panel backdrop in `drawWithCache`.
5. (P2 optional) Evaluate Haze library for real blur on API 31+ behind a feature flag.

**Acceptance (all must be true):**
- [ ] Visual parity with doc 03 recipe
- [ ] No jank in profile
- [ ] Reduce transparency works everywhere

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T10: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
