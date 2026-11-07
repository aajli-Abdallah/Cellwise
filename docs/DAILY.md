# Daily puzzle (v1.4.0)

## What the player sees
A card on Home: date, level, streak (flame), the last 7 days as dots (amber = solved, ring = today) and one action: play, continue, or "solved today". The same numbers appear in Statistics, and daily games are tagged "Daily" in the history.

## Same puzzle on every phone, offline
- The date is the phone's local calendar date turned into an epoch day (`LocalDate.toEpochDay()`).
- `Daily.generate(epochDay)` = `SudokuGenerator.generate(levelFor(day), SeededRandom(seed(day)))`.
- `SeededRandom` is our own SplitMix64, so the random bits do not depend on the platform or on a Kotlin version.
- Level by weekday: Mon-Tue Easy, Wed-Fri Medium, Sat-Sun Hard. The shown level is the real technique rating of the puzzle.
- Generation takes about 10-90 ms on the JVM (a few times more on a phone).

## Known limitation (accepted)
Any change to the generator, the solver's iteration order, the technique rating or the seeding changes the puzzle of every date, including past ones. Old finished daily games keep their own puzzle (every game row stores its puzzle), so history and statistics are not affected. The unit test `goldenPuzzleForDay20400` fails when this happens, so it is always a conscious decision: update the constant and this note. Friends using different app versions would see different puzzles for the same date.
Phones with a wrong date or time zone get another date's puzzle; there is no server to check.

## Change history of the generator
- 1.4.0: first version.
- 1.6.0: new techniques and an Expert level. Easy and Medium days are unchanged. Hard days may differ (day 20400 is unchanged, day 20401 changed), because Hard now means "solvable with the known techniques, up to XY-Wing" instead of also including puzzles that need guessing. Golden tests record both.

## Streak rules (`Daily.compute`)
- A day counts when a daily game of that date was won (any attempt; one win per date counts once).
- Current streak: consecutive solved days ending today, or ending yesterday if today is not solved yet (it is still alive until midnight). A missed day resets it to 0. Best streak: longest run ever.
- Replaying a solved daily is not offered. An abandoned daily can be started again the same day.
- Starting the daily while another game is in progress asks to abandon it, like any new game.

## Data
`games.dailyDay` (schema v3): epoch day of the daily puzzle, 0 for normal games. Backups carry it (`dailyDay`, 1.4.0 upgrade step), so the streak survives a restore. The streak itself is never stored: it is recomputed from won daily games.
