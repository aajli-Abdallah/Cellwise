# Techniques, rating, hints, sharing and grid import (v1.6.0)

All in pure Kotlin (`engine/`, `feature/puzzleimport/PuzzleImport.kt`), covered by unit tests that also check every step of every technique against the real solution.

## Techniques
Difficulty order = enum declaration order. `rank` is a stable id stored in `games.rating`: never renumber, only add.
| Order | Technique (rank id) | Rule |
|---|---|---|
| 1 | Naked single (1) | a cell has one candidate left |
| 2 | Hidden single (2) | a digit has one possible cell in a row, column or box |
| 3 | Pointing pair (3) | a digit's candidates in a box lie on one line: remove it from the rest of the line |
| 4 | Box/line reduction (4) | a digit's candidates in a row/column lie in one box: remove it from the rest of the box |
| 5 | Naked pair (5) | two cells of a unit share the same two candidates: remove them from the other cells |
| 6 | Hidden pair (6) | two digits fit only the same two cells of a unit: remove the other candidates from those cells |
| 7 | Naked triple (10) | three cells of a unit hold only three digits between them: remove those digits from the rest of the unit |
| 8 | Hidden triple (11) | three digits fit only the same three cells of a unit: remove the other candidates from those cells |
| 9 | X-Wing (7) | a digit in two rows (columns) only in the same two columns (rows): remove it elsewhere in those columns (rows) |
| 10 | XY-Wing (12) | pivot {A,B} with wings {A,C} and {B,C} that both see it: C is removed from every cell seeing both wings |
| 11 | Swordfish (13) | X-Wing over three rows (columns) |
| 12 | Guess (9) | the techniques above are not enough: advanced logic or trial and error |
`TechniqueSolver` keeps a candidate mask per cell and always applies the simplest technique available (`nextStep()`).

## Rating and levels
`SudokuRater.rate(givens)` solves with the techniques and returns the hardest one used.
| Level | Hardest technique needed | Measured generator output |
|---|---|---|
| Easy | naked / hidden single | singles only, about 39 clues |
| Medium | up to pointing pair, box/line, naked pair | about 26 clues |
| Hard | hidden pair, naked / hidden triple, X-Wing, XY-Wing; always fully solved by the techniques | mostly XY-Wing or hidden pair |
| Expert | Swordfish, or the solver stalls | stalls the solver (needs advanced logic or guessing) |
The generator removes clues (symmetric pairs first, then single clues, always keeping a unique solution), rates the result, keeps removing if it is too easy, and starts over if it is too hard, up to 40 attempts. The displayed level is always the real rating. Measured on the JVM: 0-35 ms per puzzle on average, at most about 125 ms; expect a few times more on a phone.

## Hints
`HintEngine.next(values, solution)` runs the solver on the current board (givens + the player's digits, notes ignored) until a placement appears. The hint names the technique, the unit to look at and the cell; if eliminations were needed first, the last one is mentioned. If no technique applies, the digit comes from the solution (shown as "Advanced logic"). A grid containing wrong digits shows them in red instead of a hint. Each new hint increments `hints`; Reveal places the digit without a second count.

## Check and auto notes
Check marks wrong digits (compared with the solution) until the board changes; the "highlight mistakes" setting keeps them always on. Auto notes fills every empty cell with the digits not used in its row, column and box (replaces existing notes; undoable).

## Sharing a puzzle (plain 81-digit code)
The share button on the game screen sends text such as `Cellwise puzzle · Hard`, the 81-digit code and a one-line instruction. Code = givens row by row, digits 1-9 for clues and 0 for empty cells (a `.` is read as empty). The level is not part of the code: the importer rates the puzzle itself. Any app can carry it (chat, mail, notes).

## Importing a grid (printed or shared)
- Tap a cell, then a digit: the cursor moves to the next cell. Or Paste text: a shared message is found automatically (`PuzzleCode.extract`: an 81-character run of digits and dots); the dialog starts with the clipboard content when it holds a puzzle. Otherwise 81 cells row by row; digits 1-9 are clues, `0 . - _ x *` are empty, spaces and `|` are ignored, lines containing `+` or box-drawing characters are skipped (ASCII borders). A line of only dashes without `+` would be read as empty cells.
- Live: repeated digits in a row, column or box are red.
- Start: fewer than 17 clues is refused; 0 solutions = a clue is mistyped; several solutions = a clue is missing or misread; exactly one = rated and played (`imported = 1`). Not included: showing the full solution, camera scan.
