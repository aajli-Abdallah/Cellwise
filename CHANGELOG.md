# Changelog (newest first)

## 1.6.0 - 2026-11-06
- Expert level and four real levels. New techniques in the solver: naked triple, hidden triple, XY-Wing, Swordfish. Easy = singles; Medium = up to locked candidates / naked pairs; Hard = hidden pairs, triples, X-Wing, XY-Wing (always solvable with these techniques); Expert = Swordfish or beyond, i.e. advanced logic or trial and error (measured: Expert puzzles stall the technique solver). Hints and "first use ..." messages know the new names.
- Share a puzzle (📤 on the game screen): plain text with an 81-digit code (digits 1-9 clues, 0 empty, level in the message only). Import a grid now finds a shared code inside any pasted text and starts with the clipboard content when it holds a puzzle.
- Technique ids stored in `games.rating` are unchanged (old values keep their meaning); difficulty order now follows the enum order.
- Daily puzzle: Easy and Medium days are unchanged. Hard days (Sat-Sun) can change because Hard no longer includes puzzles that need guessing (accepted limitation, see docs/DAILY.md); golden tests updated.
- Level statistics gain an Expert row; Home shows four level cards with 1-4 dots.
- Unit tests: soundness of every new technique on 120 puzzles, Expert vs Hard, share code extraction (37 tests).

## 1.5.0 - 2026-11-06
- Appearance: theme mode (system / light / dark, system bar icons follow it) and four accent palettes (Indigo, Teal, Rose, Forest) built from one hue each; amber stays the shared accent; wallpaper colors still override the palette.
- Gameplay: vibration feedback (light tick on taps, stronger on a mistake and on a win) and a left-handed layout that mirrors the top bar and the tool row.
- Accessibility: high contrast board, large numbers, reduce motion (also follows the system animation scale), TalkBack descriptions for the board (selected cell, row, column, clue or your digit), tool buttons and digit keys, button roles.
- Backup: "Save to this phone..." writes a backup to a file you choose (system save dialog, e.g. Downloads). It is not counted as a copy outside the phone for the reminder.
- Settings are included in backups; settings missing from an older backup keep their current value. No database or backup-format change.
- Not included: landscape / tablet layout (decision).

## 1.4.0 - 2026-11-06
- Daily puzzle (Home): one puzzle per calendar date of the phone, the same on every phone and fully offline (own SplitMix64 generator seeded by the epoch day). Level by weekday: Mon-Tue Easy, Wed-Fri Medium, Sat-Sun Hard.
- Streak = consecutive days solved (alive until the end of the day if yesterday was solved), best streak, days solved, 7-day strip on the Home card; the same numbers in Statistics; daily games are tagged in the history.
- Room schema v3 (migration 2 to 3): `games.dailyDay` (epoch day, 0 = normal game). Backups: new field with a 1.4.0 upgrade step; older backups still import.
- Accepted limitation: changing the generator or solver later changes the puzzle of every date. A golden-value unit test fails if that happens by accident.
- Unit tests: determinism, golden puzzle, weekday levels, streak rules, backup upgrade chain.

## 1.3.01 - 2026-11-06
Design correction of 1.3.0 (no logic, database or backup-format change).
- New icon: a 3x3 board whose centre cell glows amber with a spark ("the wise cell") on an indigo-to-violet gradient; separate monochrome layer for themed icons; matching splash on a solid brand background. Source geometry documented in `docs/DESIGN.md`.
- Own palette (midnight indigo + amber, light and dark) instead of wallpaper colors; "Wallpaper colors" is now an optional setting (Settings > Appearance, off by default, included in backups).
- Serif display type and digits: bold serif clues, italic indigo "pen" digits, rounded highlights and a ring on the selected cell, framed board, timer pill.
- Home: living logo (breathing centre cell), two-tone wordmark, progress card for the game in progress, level cards with difficulty dots and descriptions, tile row for Import / Statistics / Backup / Settings.
- Win dialog shows hints; settings split into Appearance and Gameplay; About shows the logo.

## 1.3.0 - 2026-11-05
- Technique engine (pure Kotlin): naked and hidden singles, pointing pairs, box/line reduction, naked pairs, hidden pairs, X-Wing. Used for the rating, the hints and the generator.
- Difficulty is now the technique rating: Easy = singles only, Medium = up to locked candidates / naked pairs, Hard = hidden pairs, X-Wing or beyond. The generator retries until the rating matches the level.
- Hint button: names the technique and highlights the unit and cell; Reveal places the digit; a wrong grid is shown in red instead. Check button: marks wrong digits until the next change. Auto notes button: fills all candidates (undoable).
- Import a grid from paper (Home > Import a grid): tap-and-type with auto-advancing cursor or paste/type the grid as text; repeated digits turn red; the solver reports no solution, several solutions or a unique puzzle; the puzzle is rated and played like any game. Camera scan is not included (decision).
- Room schema v2 (migration 1 to 2): `games` gains `hints`, `imported`, `rating`. Backups from 1.2.0 are upgraded on import by a new 1.3.0 upgrade step.
- Statistics history shows hints used. Tools row redesigned (six buttons) to keep the grid, tools and digit pad on small screens.
- Unit tests: techniques never contradict the solution, rating, generator level, hints reach the solution, grid text parsing and validation, upgrade step, new fields.

## 1.2.0 - 2026-11-05
- Backup screen (Home > Backup): share a backup file, import from file picker, restore from automatic backups.
- Opening a `.json` backup from WhatsApp, Files or mail (VIEW and SEND intent filters) goes straight to the import preview.
- Backup file: `format`, `appVersion` (default 1.2.0 when missing), `createdAt`, copyright, sections (`games`, `settings`). Ordered upgrades hook (empty for now). Files from a newer app version are refused.
- Import preview with new / updated / unchanged / removed / invalid counts for Merge and Replace; Replace asks for confirmation.
- Merge rules: games matched by id, newest `updatedAt` wins, only the newest game in progress stays in progress; settings applied only on Replace.
- All database writes, including imports, go through one ordered queue.
- Automatic local backup once per day (first launch, only when there is data), newest 7 kept; reminder to share a copy after 7 days without sharing (snooze 2 days).
- Own dependency-free JSON reader/writer (`MiniJson`), FileProvider, optional release signing from `keystore.properties`.
- No schema change (Room v1). Unit tests: JSON, versions, codec, upgrades, reminder, game mapper and merge.

## 1.1.0 - 2026-11-05
- Statistics screen (Home > Statistics): games won, win rate, current and best win streak, per-level table (won/played, best time, average time, average mistakes), wins-per-day chart (last 14 days, Canvas), recent games list (last 50).
- Pure `Stats` logic recomputed from the `games` table (no stored aggregates, no schema change, still Room v1). Abandoned games count as played and break the streak; games in progress are ignored.
- Fix (1.0.0): missing `import androidx.compose.ui.unit.dp` in SudokuGrid.kt.
- Unit tests for levels, streaks, per-day counts, history order and empty data.

## 1.0.0 - 2026-11-05
First playable version.
- Project skeleton: package-by-feature, one ViewModel, Room v1 (`games` table, schema export on), empty migrations hook.
- Pure Kotlin engine: bitmask solver (MRV), generator with unique-solution check, 3 levels.
- Game: Canvas grid, digit pad with remaining counts, notes, undo (200 steps, in memory), erase, mistakes, timer, win dialog.
- Autosave of the game in progress; abandoned games are recorded as ABANDONED (used by stats in 1.1.0).
- Settings: highlight mistakes, highlight same number, keep screen on.
- Themed adaptive icon (monochrome layer), splash, dynamic colors, edge-to-edge.
- About screen: version from BuildConfig, copyright, licenses.
- Unit tests: solver, generator, rules, time format.
