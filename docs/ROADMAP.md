# Roadmap

| Version | Scope | Status |
|---|---|---|
| 1.0.0 | Skeleton, engine, playable game, autosave, settings, About | done |
| 1.1.0 | Statistics: played/won/abandoned, best and average time per level, streaks, mistakes, Canvas chart, history list | done |
| 1.2.0 | Backup: JSON export/import with `appVersion` + upgrades hook, preview (new/updated/unchanged), Merge vs Replace, daily auto-backup, share reminder, FileProvider, open JSON from WhatsApp, optional release signing | done |
| 1.3.0 | Hints (technique named), error check, auto-notes, technique-based difficulty, **newspaper grid import** (below) | done |
| 1.4.0 | Daily puzzle (date-seeded, offline) with streak | done |
| 1.5.0 | Settings polish: themes, left-handed layout, haptics, accessibility (landscape/tablet dropped by decision) | done |
| 1.6.0 | Expert level, share/import a puzzle as a plain 81-digit code | done |
| 2.0.0 | Brain pack via `GameModule`: Mastermind, Make 24, memory sequence; unified stats; final blueprint | planned |

## Merge rules (fixed)
Game history: match by UUID, union. In-progress game: newest `updatedAt` wins. Settings: applied only on Replace. Aggregated stats are recomputed from history, never imported as stored numbers.

## 1.3.0 - Newspaper grid import
Goal: play a grid seen in a newspaper.
1. **Setup mode**: empty grid, digits entered as givens (distinct color). Two fast inputs: tap a cell and type (cursor auto-advances), or type a whole row as 9 characters (`53..7....`, `.` or `0` = empty), which matches how people copy a printed grid.
2. **Validation while typing**: duplicate digits in a row, column or box are highlighted.
3. **Solver check on "Start"**: 0 solutions = typo (conflict shown); more than 1 = a clue is missing or misread; exactly 1 = playable. A "show the solution" button is not included in 1.3.0 (possible later).
4. Imported puzzles are stored as normal `games` rows with `imported = 1`, so stats and backups need no special case.
5. **Dropped for now (owner decision, 2026-11-05; maybe later if resources allow)**: camera assist. Photo, drag four corners, perspective-warp to 9x9, on-device digit recognition per cell, then a review screen with low-confidence cells flagged, then the same validation. It adds a dependency (ML Kit text recognition) and cannot be fully verified without real newspaper photos, so manual entry ships first.
