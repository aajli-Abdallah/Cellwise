# Release v1.1.0 (2026-11-05)

Statistics screen and gameplay history tracking.

## What's Changed
- **Statistics screen (Home > Statistics)**: games won, win rate, current and best win streak, per-level table (won/played, best time, average time, average mistakes), wins-per-day chart (last 14 days on Canvas), recent games list (last 50).
- **Pure `Stats` logic**: recomputed dynamically from the `games` table without stored aggregates (Room v1 schema preserved). Abandoned games count as played and break streak; in-progress games ignored.
- **Fix (1.0.0)**: missing `import androidx.compose.ui.unit.dp` in `SudokuGrid.kt`.
- **Unit tests**: levels, streaks, per-day counts, history order, and empty data handling.

## Assets
- `Cellwise-v1.1.0.zip` (SHA256: `49C6E1B5EF0556739A618EB8A1F8DC789482402E1246C6958E2B1BA1628805C7`)
