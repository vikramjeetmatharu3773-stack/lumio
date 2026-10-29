# 07 — Library & Folder Scanning

## Goals
Find every song the user wants, fast, without ever losing favorites/play counts, with full control over **where** to look.

## Folder key (the one concept to understand)
Every song gets `folderPath = "<volume>:<relativePath>/"`, e.g. `primary:Music/Rock/` or `1A2B-3C4D:Songs/`.
- `volume`: `primary` for internal storage, otherwise the SD/USB volume id (compared **case-insensitive**).
- API 29+: from `RELATIVE_PATH` + `VOLUME_NAME` (`external_primary` → `primary`). API 26–28: parse `DATA` (`/storage/emulated/0/...` → `primary`, `/storage/XXXX-XXXX/...` → `XXXX-XXXX`).
- A scan-folder entry has the same key. A song is "inside" a folder if `song.folderPath.startsWith(folder.key, ignoreCase = true)`.

## Scan modes
| Mode | Behavior |
|---|---|
| `ALL_DEVICE` | Take everything MediaStore knows, then remove songs inside any **Exclude** folder. Include-folders only matter for *deep scan*. |
| `SELECTED_ONLY` | Keep only songs inside an **Include** folder (minus Exclude folders). Empty include list → empty library + warning. |
Precedence: **Exclude always wins** over Include.

## Pipeline (`MediaScanner.scan()`), runs on IO dispatcher
1. Guard: if already running return. Publish `ScanState(running, phase="Reading library")`.
2. Load settings + `scan_folders`.
3. **Stage 1 — MediaStore query** (fast, thousands/sec). Filters: `DURATION >= minDuration`, `IS_MUSIC != 0` unless `includeNonMusic`.
4. Build `SongEntity` list; apply mode + include/exclude filter. Publish found count every 250 rows.
5. **Stage 2 — Deep scan (SAF)** for each Include folder with `deepScan = true` (or all includes when `includeNoMedia` on): walk the tree with `DocumentsContract` child queries (not `DocumentFile`, it is 10× slower), for each audio file *not already found* (dedupe key = `folderPath + displayName`, lowercase) read tags with `MediaMetadataRetriever` and add a `SongEntity` with `source = 1` and the document URI. Skip Exclude subtrees. Publish phase "Deep scanning <folder>".
6. **Diff & write:** `existing = db.songs.allUris()`; delete `existing − new`; upsert new in chunks of 500.
7. Update per-folder `songCount`, `lastScan`; settings `lastScanAt`.
8. Publish `ScanState(finished, summary = "1,240 songs · 3 folders · 4.2 s")`.
User data is untouched (separate table).

## Triggers
| Trigger | Condition |
|---|---|
| First run | after permission |
| App launch | `autoScanOnLaunch` — incremental: skip deep scan unless folders changed |
| MediaStore change | `ContentObserver` on audio URI, debounced 3 s, only if `autoScanOnChange` and app process alive |
| Manual | Settings ▸ Scan now / pull-to-refresh on Library |
| Full rebuild | clear `songs` table first, then scan (user data still kept) |
| Folder added/removed/mode changed | immediate scan |

## SAF details
- Picker: `ActivityResultContracts.OpenDocumentTree()`. Immediately `takePersistableUriPermission(READ)`; store tree URI string.
- `DocumentsContract.getTreeDocumentId(uri)` → `"primary:Music/Rock"` → volume=`primary`, rel=`Music/Rock/`.
- Playing a SAF-only song: ExoPlayer reads the document URI through `ContentDataSource` — permission persisted, works after restart.
- If a persisted permission disappears (user revoked / folder deleted) → mark folder "Needs access" with a re-pick button; don't delete its songs until a rescan confirms.

## Audio extensions accepted in deep scan
`mp3 m4a aac flac ogg oga opus wav wave amr 3gp mid midi` and any MIME starting with `audio/`. Ignore files < 8 KB.

## Metadata cleanup
`<unknown>` or blank → `Unknown artist` / `Unknown album`. Track number `1002` → 2 (disc*1000+track). Year 0 → blank. Title blank → filename without extension.

## Large-library behavior
- Insert in chunks; never hold >1 `Cursor` open; never decode artwork during scan.
- `LibraryRepository` derives collections lazily (stateIn) so scanning 50k songs does not freeze UI.
- Progress UI updates ≤ 4×/s.

## Edge cases checklist
SD card removed mid-scan · file renamed · duplicate files in two folders (kept, duplicate finder later) · very long names · emoji/RTL titles · songs with no tags · `.nomedia` in music folder · hidden dot-folders · Android 14 partial media access ("Select photos and videos" does not apply to audio, but handle revoked permission) · MediaStore returning 0 rows on a fresh install (offer deep scan).

## Acceptance (task T05/T06)
Add `/Music/Test` as include with deep scan → files inside a `.nomedia` subfolder appear. Add `/Music/Podcasts` as exclude → none of its songs appear in any tab or search. Switch to SELECTED_ONLY → library shrinks to included folders only. Favorites survive Full rebuild.
