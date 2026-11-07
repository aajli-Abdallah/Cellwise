# Cellwise

<p align="center">
  <strong>Offline number-puzzle game (9x9 logic grid) for flights and trips. No account, no ads, no network.</strong>
</p>

<p align="center">
  <a href="https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.6.0"><img src="https://img.shields.io/badge/Release-v1.6.0-indigo.svg" alt="Version 1.6.0" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%E2%80%9335)-blue.svg" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet.svg" alt="Jetpack Compose & Material 3" />
  <img src="https://img.shields.io/badge/Internet-0%20Permissions%20(100%25%20Private)-success.svg" alt="No Internet Required" />
  <img src="https://img.shields.io/badge/License-Copyright%20%C2%A9%202026-amber.svg" alt="Copyright AAJLI Abdallah" />
</p>

---

Cellwise is a distraction-free, 100% offline 9x9 logic grid puzzle game crafted for flights, commutes, and daily mental workouts. It requires zero accounts, has no advertisements, requires zero internet permissions, and runs completely self-contained on your device.

Unlike casual puzzle apps that rely on guesswork, pre-packaged grid databases, or online servers, Cellwise features an **on-device pure Kotlin solver and generator**. Every puzzle is mathematically validated to have **exactly one unique solution** and is graded across four distinct difficulty levels based on real deduction techniques.

---

## 📖 Table of Contents
- [Highlights & Principles](#-highlights--principles)
- [Key Features](#-key-features)
  - [Technique-Rated Difficulty](#-technique-rated-difficulty)
  - [Daily Puzzle & Offline Streaks](#-daily-puzzle--offline-streaks)
  - [Newspaper & Paper Grid Import](#-newspaper--paper-grid-import)
  - [Smart Assist: Hints, Error Check & Auto-Notes](#-smart-assist-hints-error-check--auto-notes)
  - [Statistics & Analytical Charts](#-statistics--analytical-charts)
  - [81-Digit Code Sharing & Portable Backups](#-81-digit-code-sharing--portable-backups)
- [Design & User Experience](#-design--user-experience)
- [Privacy & Offline First](#-privacy--offline-first)
- [Technical Architecture](#-technical-architecture)
- [Documentation & Blueprint](#-documentation--blueprint)
- [Changelog & Releases](#-changelog--releases)
- [Roadmap](#-roadmap)
- [Author & License](#-author--license)

---

## 💡 Highlights & Principles

- **100% Offline & Private:** Zero internet permissions requested in `AndroidManifest.xml`. No analytics, no ads, no trackers, and no external calls.
- **On-Device Generation & Solving:** Fast bitmask MRV solver and SplitMix64 PRNG create and verify fresh grids with unique solutions instantly on your device.
- **Deductive Technique Rating:** Difficulty is not just clue count; it is determined by the minimum logical technique required to solve the grid without guessing.
- **Preserved Play State:** Automatic state persistence via Room database. Close the app mid-flight and resume right where you left off.
- **Custom Visual Identity:** High-readability serif display digits, midnight indigo and amber palette, living breathing logo, and themed adaptive icon.

---

## 🎯 Key Features

### 🧩 Technique-Rated Difficulty
Cellwise rates puzzles into four progressive tiers based on solving techniques:
- **Easy:** Solvable using only Naked Singles and Hidden Singles.
- **Medium:** Requires Locked Candidates (Pointing Pairs, Box/Line Reduction) and Naked Pairs.
- **Hard:** Introduces Hidden Pairs, Naked/Hidden Triples, X-Wing, and XY-Wing (always solvable with pure logic).
- **Expert:** Advanced logic requiring Swordfish or beyond, testing mastery on complex constraints.

### 📅 Daily Puzzle & Offline Streaks
- **One Puzzle Per Date:** Completely deterministic and identical on every phone without internet, seeded offline by the phone's epoch calendar date.
- **Scheduled Level Progression:** Mon–Tue Easy, Wed–Fri Medium, Sat–Sun Hard.
- **Active Streak Tracking:** Tracks current streak, best streak, total solved days, and displays an interactive 7-day strip on the Home dashboard.

### 📰 Newspaper & Paper Grid Import
- **Play Printed Puzzles:** Transcribe puzzles seen in morning papers, magazines, or books.
- **Dual Input Modes:** Tap-and-type with auto-advancing cursor or paste an entire grid / 81-character string (`.` or `0` for empty cells).
- **Instant Validation:** Real-time duplicate highlighting in rows, columns, and 3x3 boxes.
- **Solver Check:** Automatically validates whether the grid has no solution, multiple solutions, or a unique playable solution before starting.

### 💡 Smart Assist: Hints, Error Check & Auto-Notes
- **Technique-Naming Hints:** The hint button names the exact technique to use and highlights the affected unit/cells. Reveal places the digit if desired.
- **On-Demand Error Check:** Highlights incorrect digits in red until the next move without penalizing gameplay.
- **Auto Notes:** Automatically populates candidate notes across the entire grid (fully undoable).
- **Controls & Ergonomics:** Large digit pad with remaining clue counts, pencil notes toggle, 200-step in-memory undo, and eraser.

### 📊 Statistics & Analytical Charts
- **Overall Records:** Games played, won, win rate, and win streaks.
- **Per-Level Breakdown:** Detailed table with best completion time, average time, and average mistake count for each difficulty tier.
- **Wins-Per-Day Chart:** 14-day Canvas activity histogram visualizing daily solves.
- **Recent Games List:** Full history of recent games with completion time, date, hints used, and difficulty level.

### 📦 81-Digit Code Sharing & Portable Backups
- **81-Digit Code Sharing:** Export any puzzle as a compact 81-digit text message (clues 1–9, empty 0). Import automatically detects shared codes from copied text.
- **JSON Backup & Restore:** Complete export and import of all game history, stats, and settings via versioned JSON.
- **Smart Import Preview:** Displays new, updated, and unchanged counts before applying changes with Merge or Replace options.
- **Automated Local Backups:** Retains the last 7 daily automatic snapshots with friendly reminders to share a copy.

---

## 🎨 Design & User Experience

- **Visual Palette:** Signature Midnight Indigo paired with warm Amber accents, complemented by 4 selectable accent palettes (Indigo, Teal, Rose, Forest) and optional Android 12+ dynamic wallpaper colors.
- **Typography:** Serif clues evoke printed puzzle newspapers, while italic indigo pen styling differentiates player-entered digits.
- **Haptic Feedback:** Subtle tactile ticks on digit entry, distinct confirmation on puzzle completion, and gentle alert on mistakes.
- **Left-Handed Layout:** Full mirrored layout option flipping the top action bar and tool rows for comfortable single-thumb operation.
- **Accessibility:** High-contrast board mode, enlarged digits, reduce motion setting (aligned with Android system animation scale), and full TalkBack screen reader labels.

---

## 🔒 Privacy & Offline First

- **100% Local:** All database records, saved games, and settings reside strictly on internal SQLite / Room storage.
- **Zero Tracking:** No user identifiers, no authentication, no telemetry, and no ad SDKs.
- **Zero Network Permissions:** `android.permission.INTERNET` is neither requested nor included in the manifest.

---

## 🏗️ Technical Architecture

- **Language:** Kotlin 2.0 (100% pure Kotlin logic engine)
- **UI Toolkit:** Jetpack Compose with Material 3 design system
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with unidirectional data flow
- **Local Persistence:** Room Database v3 (KSP) with safe schema migrations
- **PRNG & Solver:** Deterministic SplitMix64 generator and bitmask MRV solver
- **SDK Target:** Minimum SDK 26 (Android 8.0) &middot; Target SDK 35 (Android 15)
- **Zero External Dependencies:** Custom dependency-free JSON parser (`MiniJson`) and self-contained algorithms.

---

## 📚 Documentation & Blueprint

Comprehensive technical documentation is maintained in the [`docs/`](docs/) directory:
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) &ndash; Module layout, data flow, Room migrations, and extension points.
- [`docs/DAILY.md`](docs/DAILY.md) &ndash; Deterministic SplitMix64 date seeding, streak rules, and golden tests.
- [`docs/DESIGN.md`](docs/DESIGN.md) &ndash; Color palettes, serif typography, icon construction, and layout specifications.
- [`docs/TECHNIQUES.md`](docs/TECHNIQUES.md) &ndash; Solving techniques, rating criteria, hint logic, and grid parser.
- [`docs/BACKUP_FORMAT.md`](docs/BACKUP_FORMAT.md) &ndash; JSON schema, upgrade pipeline, and merge strategies.
- [`docs/STATS.md`](docs/STATS.md) &ndash; Mathematical definitions for streaks, averages, and historical metrics.
- [`docs/SIGNING.md`](docs/SIGNING.md) &ndash; Production release signing instructions.
- [`docs/ROADMAP.md`](docs/ROADMAP.md) &ndash; Planned versions and architectural decisions.

---

## 📦 Changelog & Releases

For detailed change notes across all versions, see [CHANGELOG.md](CHANGELOG.md).

All version source packages and releases are available under GitHub [Releases](https://github.com/aajli-Abdallah/Cellwise/releases):
- [Cellwise v1.6.0](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.6.0) &ndash; Expert level, four rated levels, 81-digit puzzle code share & import.
- [Cellwise v1.5.0](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.5.0) &ndash; Theme modes, 4 accent palettes, haptics, left-handed mode, accessibility.
- [Cellwise v1.3.01](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.3.01) &ndash; Design refresh: midnight indigo + amber palette, serif typography, new icon & splash.
- [Cellwise v1.3.0](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.3.0) &ndash; Technique engine, hints, check button, auto notes, paper grid import.
- [Cellwise v1.1.0](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.1.0) &ndash; Statistics screen, streaks, per-level table, wins-per-day chart.
- [Cellwise v1.0.0](https://github.com/aajli-Abdallah/Cellwise/releases/tag/v1.0.0) &ndash; First playable release: bitmask solver, generator, autosave, Material 3 UI.

---

## 🗺️ Roadmap

- [x] **v1.0.0:** Skeleton, pure Kotlin engine, playable game, autosave, settings, About screen.
- [x] **v1.1.0:** Statistics: played/won/abandoned, best and average times per level, streaks, Canvas chart.
- [x] **v1.2.0:** Backup: JSON export/import with version upgrade hook, preview, daily auto-backup, share reminder.
- [x] **v1.3.0:** Technique-based difficulty, hint engine, error check, auto-notes, newspaper grid import.
- [x] **v1.3.01:** Design correction: glowing amber cell icon, monochrome themed layer, serif type.
- [x] **v1.4.0:** Daily puzzle: offline date-seeded SplitMix64 generator, weekday levels, streak tracking.
- [x] **v1.5.0:** Appearance themes (4 accent palettes), vibration haptics, left-handed layout, accessibility.
- [x] **v1.6.0:** Expert difficulty level (triples, XY-Wing, Swordfish), 81-digit code sharing and clipboard import.
- [ ] **v2.0.0 & Future:**
  - Brain pack integration via `GameModule` (Mastermind, Make 24, Memory sequence).
  - Unified multi-game statistics.
  - Final offline brain training blueprint.

---

## 👤 Author

**AAJLI Abdallah**  
- 📧 Email: [ingenieur.aajli@gmail.com](mailto:ingenieur.aajli@gmail.com)  
- 🐙 GitHub: [@aajli-Abdallah](https://github.com/aajli-Abdallah)  
- 🌐 Website: [aajli.com](https://aajli.com)  

© 2026 AAJLI Abdallah. All rights reserved.
