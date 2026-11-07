# Statistics (v1.1.0)

Source: the `games` table only (one row per game). Nothing is stored as an aggregate; every number is recomputed by `Stats.compute` (pure Kotlin, unit-tested). The ViewModel exposes `stats` as a StateFlow that is collected only while the Statistics screen is open, so playing does not trigger recomputation.

## Definitions
- **Played** = won + abandoned. Games in progress are ignored.
- **Win rate** = won / played, rounded to a whole percent.
- **Best time** = minimum `elapsedSec` among wins. **Average time** = mean `elapsedSec` among wins (integer seconds).
- **Average mistakes** = mean `mistakes` among wins. **Flawless** = wins with 0 mistakes (kept in the model, not shown yet).
- **Streak**: finished games ordered by end time (`finishedAt`, or last update for abandoned games). A win adds 1, an abandoned game resets to 0. Current = value after the last game; best = maximum reached.
- **Wins per day**: wins grouped by local calendar day (device time zone), last 14 days including today.
- **History**: finished games, newest first, limited to 50.

## Daily puzzle
Source: games with `dailyDay != 0` and status WON; see `docs/DAILY.md`. Daily games also count in the normal level statistics.

## Notes
- History rows show hints used (`hints` column, since v1.3.0). Imported puzzles are rated by the solver, get a level and count like any other game.
- Timer counts only foreground play time, so times are comparable between flights and desk play.
- Imported puzzles (v1.3.0) will be normal `games` rows and appear here without code changes.
- Backups (v1.2.0) will export `games` rows, not statistics.
