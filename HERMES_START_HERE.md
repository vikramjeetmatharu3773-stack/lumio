# HERMES — START HERE

You are building **Lumio**, a local Android music player. Everything you need is in this folder.
Do not panic: the project skeleton already exists in `project/`. Your job is to make it compile,
then finish the remaining tasks one by one. Work small. Work in order.

## 1. Golden rules
1. **Read only what the current task names.** Never load all docs into context.
2. **One task at a time**, in the order of `tasks/INDEX.md`. Finish → build → update `PROGRESS.md` → `git commit`.
3. **Never add** the INTERNET permission, analytics, ads, crash-reporting SDKs, or any network library. The app is offline by design.
4. **Do not change library versions** in `project/gradle/libs.versions.toml` unless a task says so.
5. **Small edits, not rewrites.** Keep every Kotlin file under ~400 lines; split big files.
6. **Do not invent APIs.** If unsure about a Compose/Media3/Room call, write the simplest known-good version and note it in `BLOCKERS.md`.
7. New user-visible strings go into `res/values/strings.xml`.
8. Package root is `app.lumio.player`. Folder map is in `docs/02_ARCHITECTURE.md`.
9. Glass UI is only drawn with `GlassSurface` and friends (`ui/components`). Do not hand-roll translucent boxes.
10. Performance rule: no glass surface inside list items (rows are flat). Max ~6 glass surfaces on screen at once.

## 2. Build loop (use this every time)
```
cd project
./gradlew :app:assembleDebug 2>&1 | tee build.log | grep -E "^e: |error:|FAILED|BUILD" | head -20
```
- Fix the **first** error only, rebuild. Repeat.
- Max **5 attempts per error**. Then write the error + what you tried in `BLOCKERS.md`, skip to the next independent task.
- If `gradlew` is missing, run `gradle wrapper --gradle-version 8.10.2` once (the wrapper JAR could not be shipped).
- Termux/low RAM: see `docs/13_BUILD_ENVIRONMENT.md`. If local building is impossible, push to GitHub and let `.github/workflows/android.yml` build the APK; read the log from there.

## 3. Multi-agent split (suggested for your local models)
| Role | Model idea | Does |
|---|---|---|
| Main / planner / reviewer | llama3.1:8b | Picks next task from INDEX, hands one task file to a sub-agent, reviews diff, updates PROGRESS.md |
| Coder-Kotlin | llama3.1:8b | Data, playback, scanner, settings tasks |
| Coder-UI | llama3.1:8b | Compose screens/components tasks |
| Boilerplate / docs / tests | mistral:7b | Strings, XML, unit tests, README edits |

Parallel-safe pairs (touch different files): T05+T10, T08+T11, T12+T14, T17+T19, T20+T21.
Never run two agents on the same task or the same file.
Prompts are in `agents/`.

## 4. Per-task Definition of Done
- [ ] Code compiles (`assembleDebug` = BUILD SUCCESSFUL)
- [ ] Task's acceptance checklist is satisfied (each task file has one)
- [ ] No new warnings about missing permissions / manifest
- [ ] `PROGRESS.md` ticked, one-line note added
- [ ] `git add -A && git commit -m "T<nn>: <title>"`

## 5. Definition of Done for the whole app
All `PROGRESS.md` boxes ticked, debug APK installs on API 26, 31 and 34+ devices/emulators,
manual QA in `docs/12_TESTING_QA_RELEASE.md` passes, release APK/AAB builds and is signed.

## 6. Where things are
```
docs/        specs (read the one the task names)
tasks/       T00..T26 + INDEX.md
project/     the Gradle project  (open THIS folder in Android Studio)
assets/icon/ icon master files
```
## 7. If you get stuck
Write it in `BLOCKERS.md` (task id, error, tried), move on. The human will review blockers.
