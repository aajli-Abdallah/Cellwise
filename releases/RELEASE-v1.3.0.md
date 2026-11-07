# Release v1.3.0 (2026-11-05)

Technique rating engine, smart hints, and newspaper grid import.

## What's Changed
- **Technique engine (pure Kotlin)**: naked & hidden singles, pointing pairs, box/line reduction, naked & hidden pairs, X-Wing.
- **Technique-based difficulty rating**: Easy (singles only), Medium (up to locked candidates / naked pairs), Hard (hidden pairs, X-Wing or beyond).
- **In-game assist tools**:
  - **Hint button**: identifies technique name, highlights unit and cell; Reveal places digit; conflict grid highlighted in red.
  - **Check button**: marks incorrect digits until the next move.
  - **Auto notes button**: populates candidates for all open cells (undoable).
- **Import grid from paper (Home > Import a grid)**: tap-and-type with auto-advancing cursor or paste raw grid string; real-time duplicate validation; solver verifies unique solution.
- **Database & Migration**: Room schema v2 (`games` table gains `hints`, `imported`, `rating`). Backups upgraded on import.
- **Redesigned tool row**: 6 action buttons optimized for one-hand access on all screen sizes.
- **Unit tests**: technique accuracy, rating verification, generator bounds, hint progression, string parser validation.

## Assets
- `Cellwise-v1.3.0.zip` (SHA256: `52CC8601081C125FC15B058CBEFF8553ED1C1EE983DA27AF3606290C62FDAFA9`)
