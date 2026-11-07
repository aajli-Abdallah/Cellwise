# Cellwise v1.6.0

Offline number-puzzle game (9x9 logic grid) for flights and trips. No account, no ads, no network.
Package `com.aajli.cellwise` · Kotlin · Jetpack Compose (Material 3) · Room (KSP) · minSdk 26 · targetSdk 35.

Copyright (c) 2026 AAJLI Abdallah - ingenieur.aajli@gmail.com - github.com/aajli-Abdallah - aajli.com. All rights reserved.

## Features (v1.6.0)
- On-device generator and solver; every puzzle has exactly one solution. Levels: Easy, Medium, Hard, Expert.
- Canvas grid, large digit pad with remaining counts, pencil notes, undo, erase.
- Mistake counter and optional red highlight, same-number highlight, live timer.
- Automatic save of the game in progress (Room); resume after closing the app.
- Keep-screen-awake while playing, portrait only, dynamic Material 3 colors (Android 12+), themed adaptive icon, splash.
- Statistics screen: won / win rate / streaks, per-level table (won, best, average, mistakes), wins-per-day chart, recent games.
- Settings: theme (system/light/dark), 4 accent palettes, wallpaper colors, vibration feedback, left-handed layout; accessibility: high contrast, large numbers, reduce motion, TalkBack labels. Backup can also be saved to a chosen file on the phone.
- Daily puzzle: one puzzle per date, identical on every phone (offline, seeded by the date), level by weekday, streak, 7-day strip, daily stats.
- Own visual identity: indigo + amber palette, serif "printed puzzle" digits, living logo, new adaptive/themed icon and splash (wallpaper colors optional).
- Four levels from a technique rating: Easy (singles), Medium (locked candidates, naked pairs), Hard (hidden pairs, triples, X-Wing, XY-Wing), Expert (Swordfish or beyond: advanced logic or trial and error).
- Share a puzzle as a plain 81-digit code (📤 on the game screen); import it by pasting the shared message.
- Hint (names the technique, highlights where to look, Reveal places the digit), Check (marks wrong digits on demand), Auto notes.
- Import a grid from paper: tap-and-type or paste text, duplicate highlight, solver check (no solution / several solutions / unique), then play it.
- Backup: share a JSON backup file, import from file / WhatsApp / Files, preview (new, updated, unchanged), Merge or Replace, automatic daily local backup (last 7), weekly share reminder.
- About screen with version (BuildConfig), copyright and open-source notices.

## Releases
Release archives are stored in the [`releases/`](releases/) directory:
- [Cellwise-v1.6.0.zip](releases/Cellwise-v1.6.0.zip) (`v1.6.0`) - Expert level, 4 rated levels, puzzle code sharing (81 digits).
- [Cellwise-v1.5.0.zip](releases/Cellwise-v1.5.0.zip) (`v1.5.0`) - Themes, accent palettes, haptics, left-handed mode, accessibility.
- [Cellwise-v1.3.01.zip](releases/Cellwise-v1.3.01.zip) (`v1.3.01`) - Design refresh: midnight indigo + amber palette, serif typography, new icon & splash.
- [Cellwise-v1.3.0.zip](releases/Cellwise-v1.3.0.zip) (`v1.3.0`) - Technique engine, hints, check, auto notes, import paper grid.
- [Cellwise-v1.1.0.zip](releases/Cellwise-v1.1.0.zip) (`v1.1.0`) - Statistics screen, streaks, per-level stats, wins-per-day chart.
- [Cellwise-v1.0.0.zip](releases/Cellwise-v1.0.0.zip) (`v1.0.0`) - Initial release: bitmask solver, generator, autosave, Material 3 UI.

Detailed release notes and checksums are available in [releases/README.md](releases/README.md).

## Build
Open the folder in Android Studio (it reads `gradle/wrapper/gradle-wrapper.properties`, Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20).
Command line needs `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` copied from another project.
Unit tests: `./gradlew testDebugUnitTest` (engine, rules, time format).

## Versioning
`versionName` = `M.m.p` (corrections append two digits: 1.4.0 -> 1.4.01). `versionCode` = M*10000 + m*100 + p.
On every version update together: `app/build.gradle.kts` (versionName, versionCode), this title, `CHANGELOG.md`, the zip name, the project doc.

## Docs
- `docs/ARCHITECTURE.md` - modules, rules, data model, extension points.
- `docs/DAILY.md` - how the daily puzzle is made, streak rules, determinism caveat.
- `docs/DESIGN.md` - palette, typography, components, icon construction.
- `docs/TECHNIQUES.md` - techniques, rating, hint algorithm, grid import.
- `docs/BACKUP_FORMAT.md` - file format, versions, merge rules.
- `docs/SIGNING.md` - release keystore.
- `docs/STATS.md` - statistics definitions.
- `docs/ROADMAP.md` - planned versions and decisions.
- `CHANGELOG.md` - history, newest first.
