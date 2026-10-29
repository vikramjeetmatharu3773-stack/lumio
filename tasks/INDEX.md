# Task Index (do in order)

| ID | Title | Parallel-safe with |
|---|---|---|
| T00 | Environment, Gradle wrapper, first build attempt | — |
| T01 | Compile-fix loop: scaffold -> BUILD SUCCESSFUL | — |
| T02 | Branding: name, package, icon, splash, strings, fonts | — |
| T03 | Database verification, schema export, DAO tests | — |
| T04 | Settings repository verification + tests | — |
| T05 | Permissions + MediaStore scanner | T10 |
| T06 | Folder scanning: include/exclude, deep scan, .nomedia, auto-rescan | — |
| T07 | Playback service + controller | — |
| T08 | Audio effects + presets | T11 |
| T09 | Sleep timer, fade, speed/pitch, skip silence | — |
| T10 | Glass design system hardening | T05 |
| T11 | Home screen | T08 |
| T12 | Library screens | T14 |
| T13 | Collection detail + song options | — |
| T14 | Search | T12 |
| T15 | Now Playing | — |
| T16 | Playlists | — |
| T17 | Settings (all sub-screens) | T19 |
| T18 | Customization: presets, theme editor, backup/restore | — |
| T19 | Widgets | T17 |
| T20 | Stats, History, Favorites polish | T21 |
| T21 | Lyrics import, tag editor, delete, ringtone | T20 |
| T22 | Media integrations | — |
| T23 | Performance, accessibility, localization | — |
| T24 | Automated tests | — |
| T25 | Release build, signing, R8, Play assets | — |
| T26 | Final animation + polish pass | — |

**Dependencies:** T00→T01 first. T02–T04 any order after T01. T05→T06→T07. T07 before T08/T09/T15. T10 before T11–T15. T13 before T16. Everything before T23–T26.
