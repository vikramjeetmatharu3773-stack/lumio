# 10 — Database Schema (Room, version 1, `lumio.db`)

| Table | Columns | Notes |
|---|---|---|
| `songs` | **uri** PK, title, artist, album, albumId, genre, durationMs, track, year, dateAdded, sizeBytes, mimeType, folderPath, displayName, source(0=MediaStore,1=SAF) | Replaced freely by scans. Idx: folderPath, album, artist |
| `user_data` | **uri** PK, isFavorite, playCount, lastPlayed, skipCount, hidden | Never deleted by scans |
| `playlists` | **id** PK auto, name, createdAt, updatedAt | |
| `playlist_songs` | **(playlistId, songUri)** PK, position, addedAt | FK → playlists CASCADE; idx playlistId; no FK to songs (survives rescans) |
| `scan_folders` | **treeUri** PK, displayPath, volume, relativePath, mode(0 include/1 exclude), deepScan, lastScan, songCount | |
| `history` | **id** PK auto, songUri, playedAt, listenedMs | idx playedAt |
| `queue_items` | **position** PK, songUri | Saved queue |

## Query patterns
- Library list: `songs LEFT JOIN user_data` → `SongRow(@Embedded SongEntity + isFavorite, playCount, lastPlayed, hidden)`.
- Top songs: `SELECT songUri, COUNT(*) c FROM history WHERE playedAt >= :since GROUP BY songUri ORDER BY c DESC LIMIT :n`.
- Total listening: `SELECT COALESCE(SUM(listenedMs),0) FROM history WHERE playedAt >= :since`.
- All derived views (albums, artists, genres, folders, search) are computed **in memory** from the `songs` flow.

## Migrations
Schema JSON exported to `app/schemas/` (KSP arg). Every schema change = bump version + `Migration` + test. Never use destructive migration in release.

## Backup JSON format (T18)
```json
{ "app":"lumio", "version":1, "settings":{...}, "favorites":[{"t":"title","a":"artist","d":123000}],
  "playlists":[{"name":"x","songs":[{"t":"..","a":"..","d":0}]}], "playCounts":[{"t":"..","a":"..","d":0,"n":12}] }
```
Match on lowercase title + artist + duration ±2 s.
