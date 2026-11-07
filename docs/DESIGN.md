# Design system (v1.5.0)

## Idea
"Cellwise" = a grid of cells and one wise cell. Eight calm indigo cells around one amber cell with a spark. The same motif is the icon, the splash, the Home logo (the centre cell breathes) and the accent colour.

## Palette (`ui/theme/Theme.kt`)
| Role | Light | Dark |
|---|---|---|
| primary (indigo) | #4338CA | #B4B8FF |
| primaryContainer | #E3E4FF | #332F91 |
| tertiary (amber, text/accents) | #B45309 | #FFC857 |
| tertiaryContainer (hints) | #FFE8BF | #5E3C00 |
| background / surface | #FBF9FF / #FFFFFF | #100F22 / #1A1933 |
| error | #D1293D | #FF8896 |
`Spark` = #FFB020, the icon's centre-cell amber, for progress and the logo. Own palette by default; Settings > Appearance > Wallpaper colors switches to Material You colors (Android 12+).

## Typography
Serif (system) for display, headline and title styles and for all grid and pad digits; system sans-serif for body text. No font files, so no licensing or size cost. In the grid: clues = serif bold, your digits = serif italic in primary (a "pen" feel), notes = small sans-serif.

## Components
- Board: framed in a rounded container; rounded cell highlights (selected = container + ring, peers = light tint, same digit = stronger tint, hint = amber); thin soft lines, thick indigo box lines.
- Home: logo, two-tone wordmark, progress card (gradient) for the game in progress, level cards with 1-3 amber dots, tile row.
- Digit pad: primaryContainer keys with remaining counts. Tool row: six tonal buttons, active = primary. Hint/check panel: amber container.
- Shapes: 10/14/20/28 dp.

## Icon construction (viewBox 108, safe zone = centre 66)
Background `ic_launcher_background.xml`: linear gradient #2B2780 to #6D28D9 plus a soft top-left light. Foreground `ic_launcher_foreground.xml`: 3x3 rounded cells (17 wide, pitch 21, radius 4.5, origin 24.5), corners white, edges 56% white, centre amber gradient (#FFD166 to #FF8A1F) with a white 4-point star (outer 6.4, inner 2.1) and a radial amber glow. Monochrome `ic_launcher_monochrome.xml`: same cells, star cut out of the centre cell (even-odd) for Android 13 themed icons. Splash `ic_splash.xml`: foreground at 288 dp on the solid #2B2780 splash background.

## Themes and palettes (v1.5.0)
Theme mode: system, light or dark (the status and navigation bar icons follow the chosen mode). Palettes: Indigo (hand-tuned, default) plus Teal, Rose and Forest, each generated from a single hue with HSL tones (`generated()` in `Theme.kt`): primary 40% / 82% lightness (light / dark), containers 92% / 30%, neutrals tinted with the same hue. Tertiary (amber) and error colours are shared by all palettes. The brand cards (progress card, daily card), the icon and the splash keep their fixed indigo / amber colours.

## Comfort and accessibility
- Left-handed: the top bar and the tool row are laid out right-to-left (the digit pad keeps 1-9 left to right) and the back arrow points right.
- Haptics: `TextHandleMove` tick on cell and digit taps, `LongPress` when the mistake counter increases and when a game is won.
- High contrast: stronger thin and thick lines, thicker board border. Large numbers: digits 0.68 and notes 0.29 of the cell (default 0.58 / 0.25).
- Reduce motion: the logo stops pulsing; also on when the system animation scale is 0.
- TalkBack: the board announces the selected cell (row, column, empty / clue / your digit); tools and digit keys have text descriptions and button roles. The Canvas board itself is not navigable cell by cell; this is a known limit.
