# Architecture (v1.4.0)

## Principles
Offline first, no account. One Gradle module, package-by-feature. Pure logic is separated from Android so it is unit-testable.
A feature may depend on `core` and `engine`; features never import each other (the root `AppViewModel`/`AppRoot` wires them).

## Packages (`com.aajli.cellwise`)
| Package | Role | Android deps |
|---|---|---|
| `engine` | `SudokuSolver`, `SudokuGenerator`, `Difficulty`, `Puzzle` | none |
| `core` | `CoreInfo` (owner/copyright), `Format`, `settings/` (store + backup section), `db/AppDatabase`, `db/Migrations`, `backup/` (`MiniJson`, `BackupCodec`, `BackupSection`, `BackupManager`) | yes |
| `feature/game` | `GameState` + `GameRules` (pure), `data/` (entity, DAO, mapper), `ui/` (grid, screen) | ui, data |
| `feature/daily` | `Daily` (date to puzzle, streak rules), `SeededRandom`, `DailyInfo`; the card is in `feature/home` | none (pure) |
| `feature/puzzleimport` | `PuzzleImport` (pure: text parsing, duplicates, solver validation), `ImportUi`, `ui/ImportScreen` | ui |
| `feature/backup` | `BackupUi` state and `ui/BackupScreen`. The games section lives in `feature/game/backup/` | ui |
| `feature/stats` | `Stats` (pure), `GameRecord`, mapper from `GameEntity`, `ui/` (screen, bar chart). Read-only on the `games` table; see `docs/STATS.md` | ui |
| `feature/home`, `feature/settings`, `feature/about` | screens | ui |
| `ui` | theme, shared components, labels | ui |
| root | `MainActivity`, `AppViewModel`, `AppRoot` | yes |

## Engine
Grid = `IntArray(81)`, 0 = empty. Solver: row/col/box bitmasks, picks the empty cell with fewest candidates, stops after `limit` solutions.
`countSolutions(grid, 2) == 1` is the uniqueness test. Generator: random full solution, remove 180-degree symmetric pairs while unique, then single cells if the clue target is missed. Difficulty is the technique rating (see `docs/TECHNIQUES.md`); the clue target (40 / 32 / 26) is only the starting point of the generator.

## Game state
`GameState` is immutable (81-element lists; notes = bitmask per cell). `GameRules` returns new states. Win = `values == solution`.
Placing a correct digit clears that digit from peer notes. A wrong digit increments `mistakes` (always counted; display is a setting).
Undo stack (values + notes, max 200) lives in the ViewModel and is not persisted. Selection and notes mode are not persisted.

## Persistence
Room `games` table, one row per game, also the statistics source (v1.1.0): `id` (UUID), `difficulty`, `puzzle`, `solution`, `currentValues` (81 chars), `notes` (81 comma-separated bitmasks), `mistakes`, `elapsedSec`, `status` (IN_PROGRESS | WON | ABANDONED), `createdAt`, `updatedAt`, `finishedAt`, and since schema v2 `hints`, `imported` (0/1), `rating` (rank of the hardest technique, 0 = unknown), since v3 `dailyDay` (epoch day of the daily puzzle, 0 = none).
All writes go through one ordered channel consumed by a single IO coroutine (no reordered saves). Saves happen after each change, every 15 s, on pause and on leaving the game. Starting a new game marks the old one ABANDONED.
Rules: schema change = bump version + migration in `Migrations.ALL`, never destructive; `exportSchema` is on (`app/schemas/`, commit it and keep every version file). Version history: 1 = 1.0.0-1.2.0, 2 = 1.3.0 (`MIGRATION_1_2` adds three INTEGER NOT NULL DEFAULT 0 columns; the entity declares matching `defaultValue = "0"`), 3 = 1.4.0 (`MIGRATION_2_3` adds `dailyDay`).

## Timer
Ticks once per second in the ViewModel only while the app is in foreground, the game screen is open and the game is not won. `elapsed` is a separate StateFlow to avoid recomposing the grid every second.

## UI
Compose Material 3, dynamic colors on Android 12+, indigo fallback. Edge-to-edge with system bar padding. The grid is a Canvas (native text for digits and notes) so there are no extra dependencies. Portrait only in 1.0.0.

## Hints, check and import (v1.3.0)
`GameState` carries transient UI state (`hint`, `checkWrong`, `showErrors`) next to the saved fields; only `hints` is saved. `GameRules.requestHint/revealHint/check/autoNotes` are pure and unit-tested. A board change clears the panel. Importing a grid validates in `PuzzleImport.validate`, then `AppViewModel.beginGame` starts it like a generated game (`imported = 1`).

## Backup (v1.2.0)
`core/backup` is feature-agnostic: `BackupManager` loops over registered `BackupSection`s (`key`, `export`, `plan`, `apply`). Registered in `AppViewModel`: `GamesBackupSection` (merge logic in the pure `GameMerge`) and `SettingsBackupSection`. Adding a feature = one new section class and one line in the list. Format, versions and merge rules: `docs/BACKUP_FORMAT.md`.
All database writes go through `dbQueue` (one ordered writer coroutine). An import is queued like any save, so a pending autosave can never overtake it. After an import the game in progress is reloaded from the database.
Files: automatic backups `filesDir/backups/auto-YYYY-MM-DD.json`; share copies `cacheDir/exports/` through FileProvider (`res/xml/file_paths.xml`). Photos or other media are not part of Cellwise, so there is nothing to include.

## Extension points
- New screen/feature: add `feature/<name>/`, a `Screen` enum value, a branch in `AppRoot`.
- New table: entity + DAO in the feature, register in `AppDatabase`, add a migration.
- Backup (v1.2.0): each feature registers a `BackupSection` (key, export, import, merge rule, upgrade step); backup code never changes when a feature is added.
- New game types (v2.0.0): `GameModule` interface; the engine package stays Android-free.
