# Release v1.6.0 (2026-11-06)

Expert difficulty level, advanced solving techniques, and 81-digit puzzle code sharing.

## What's Changed
- **Expert Level & Advanced Solver**:
  - Implemented Naked Triple, Hidden Triple, XY-Wing, and Swordfish techniques.
  - 4 distinct rated levels:
    - **Easy**: singles only.
    - **Medium**: locked candidates, naked pairs.
    - **Hard**: hidden pairs, triples, X-Wing, XY-Wing.
    - **Expert**: Swordfish or higher complexity logic.
  - Hints and first-use alerts updated with technique descriptions.
- **Puzzle Sharing & Import**:
  - Export puzzle as plain 81-digit string representation (clues 1-9, empty 0).
  - Import automatically extracts 81-digit puzzle codes from clipboard or pasted text.
- **Statistics**: added Expert difficulty tracking row.
- **Unit Tests**: added 37 tests covering advanced technique soundness across 120 puzzles, Expert rating thresholds, and share-code extraction.

## Assets
- `Cellwise-v1.6.0.zip` (SHA256: `66EB1B7F612310708B6F79229761E63255600D7A2A39B731DCD58DAA4AD208BA`)
