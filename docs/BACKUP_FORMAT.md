# Backup format (v1.2.0)

JSON, UTF-8, pretty-printed. Written by `BackupCodec.build`, read by `BackupCodec.parse` (pure Kotlin, unit-tested).

```json
{
  "format": "cellwise-backup",
  "appVersion": "1.2.0",
  "createdAt": 1762340000000,
  "copyright": "© 2026 AAJLI Abdallah. All rights reserved.",
  "sections": {
    "games":    { "schema": 1, "items": [ { "id": "...", "difficulty": "EASY|MEDIUM|HARD", "puzzle": "81 digits, 0 = empty", "solution": "81 digits", "currentValues": "81 digits", "notes": "81 comma-separated bitmasks", "mistakes": 0, "elapsedSec": 0, "status": "IN_PROGRESS|WON|ABANDONED", "createdAt": 0, "updatedAt": 0, "finishedAt": null, "hints": 0, "imported": 0, "rating": 0, "dailyDay": 0 } ] },
    "settings": { "schema": 1, "values": { "highlightMistakes": true, "keepScreenOn": true, "highlightSameNumber": true, "dynamicColors": false, "themeMode": 0, "palette": 0, "haptics": true, "leftHanded": false, "highContrast": false, "largeNumbers": false, "reduceMotion": false } }
  }
}
```

## Versions
- `appVersion` = app version that wrote the file. **A file without it is treated as 1.2.0** (`BackupCodec.DEFAULT_VERSION`, the first version that wrote backups).
- Versions compare number by number (`1.4.0 < 1.4.01 < 1.10.0`).
- A file newer than the running app is refused ("update the app first").
- Older files go through `BackupUpgrades.ALL`: ordered steps `BackupUpgrade(toVersion) { map -> ... }`, each applied when `file < toVersion <= app`. Rule: when the format changes, add a step; never remove old ones, so old backups stay importable. History: 1.2.0 none; 1.3.0 adds `hints`, `imported`, `rating` (0) to every games item; 1.4.0 adds `dailyDay` (0). The mapper also defaults them to 0, so a file missing them is still readable.
- A section carries its own `schema` number for future per-section changes.

Settings keys missing in a file (older backups) keep this phone's current value on Replace. "Save to this phone" (v1.5.0) writes the same file to a location chosen with the system save dialog.

## Validation
Items are validated one by one (`GameBackupMapper.fromMap`): id, difficulty, 81-digit puzzle/solution/values, 81 notes, givens consistent between puzzle, solution and values, non-negative numbers, known status. Invalid items are skipped and counted in the preview. Files over 20 MB are refused. Unknown sections are ignored and listed.

## Merge rules (per table, decided before the import code)
| Section | Merge | Replace |
|---|---|---|
| games | match by `id`; new ids added; same id -> newest `updatedAt` wins; equal rows unchanged | the file becomes the table; local ids missing from the file are deleted |
| settings | not applied | replaced |
After either mode only the newest `IN_PROGRESS` game stays in progress; any other becomes `ABANDONED`.
Statistics are not stored in backups; they are recomputed from `games`.

## Import flow
1. Source: file picker, an Android VIEW/SEND intent (WhatsApp, Files, mail), or an automatic local backup.
2. Parse, upgrade, then compute a plan for Merge and for Replace (new / updated / unchanged / removed / invalid).
3. The user picks the mode; Replace asks for confirmation.
4. The import runs inside the single database writer queue in one Room transaction, then the game in progress is reloaded.

## Local and shared copies
Automatic: once per day on the first launch, only if there is data, in `filesDir/backups/` (newest 7 kept, written through a temp file then renamed). Shared: a fresh file in `cacheDir/exports/` sent with ACTION_SEND through FileProvider. The reminder appears after 7 days without a share (snooze 2 days) and only if at least one game is finished.
