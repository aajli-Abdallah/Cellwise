# Release v1.0.0 (2026-11-05)

First playable version of Cellwise.

## What's Changed
- **Project skeleton**: package-by-feature, one ViewModel, Room v1 (`games` table, schema export on), empty migrations hook.
- **Pure Kotlin engine**: bitmask solver (MRV), generator with unique-solution check, 3 levels (Easy, Medium, Hard).
- **Game**: Canvas grid, digit pad with remaining counts, notes, undo (200 steps in memory), erase, mistakes, timer, win dialog.
- **Autosave**: automatic save of the game in progress; abandoned games recorded as `ABANDONED`.
- **Settings**: highlight mistakes, highlight same number, keep screen on.
- **UI & System**: themed adaptive icon (monochrome layer), splash, dynamic Material 3 colors, edge-to-edge.
- **About screen**: version from `BuildConfig`, copyright and open-source licenses.
- **Unit tests**: solver, generator, rules, time format.

## Assets
- `Cellwise-v1.0.0.zip` (SHA256: `12971058FED51A3AACAD1F62E18BCB83007DD9258FEB7E73D3C997279AAF9F7E`)
