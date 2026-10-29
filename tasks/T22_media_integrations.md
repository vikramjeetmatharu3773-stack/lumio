# T22 — Media integrations

**Read ONLY:** docs/08 (Notification, Android Auto)

**Files you will touch:**
- `AndroidManifest.xml`
- `res/xml/shortcuts.xml`
- `playback/* `

**Steps:**
1. App shortcuts (Shuffle all, Continue).
2. Quick Settings tile.
3. Android Auto: MediaLibraryService + browse tree (P2).

**Acceptance (all must be true):**
- [ ] Shortcuts visible on long-press; DHU shows browse tree

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T22: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
