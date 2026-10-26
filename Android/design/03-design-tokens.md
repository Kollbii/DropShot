# Deliverable 3: Design Tokens (Material Design 3)

The machine-readable token file is in [`03-tokens/design-tokens.json`](03-tokens/design-tokens.json). The generator script that calculates every tone and checks every contrast pair is in [`03-tokens/generate_palette.py`](03-tokens/generate_palette.py).

---

## 1. Color System

### 1.1 Color Grading: Mood, Temperature, and Saturation
- **Mood:** Focused, energetic, and legible under bright court lights. In dark theme (the default during a match), deep warm-charcoal surfaces reduce glare on glass court walls while the score panels stay crisp. In light theme, warm off-white surfaces mimic a painted plaster court wall (`#FDF9F2`).
- **Temperature:** Warm-led with a cool counterweight. The primary palette (squash-ball terracotta orange, seed `#D9501E`), secondary palette (court-outline clay brown, seed `#7A5B45`), and neutral palette (warm cream, seed `#FBF5EA`) have a warm temperature. The tertiary palette (court teal, seed `#0F7C86`) provides a cool contrast for analytical charts and secondary badges.
- **Saturation:** Controlled chroma across tones. Neutral surfaces use very low chroma (`C = 4` in CIE LCh, `C = 9` for neutral variant) so large surfaces never cause eye fatigue. Accent roles (`primary`, `tertiary`, `error`, and the 4 player colors) use higher chroma (`C = 50` to `80`) at tones `T40` (light) and `T80` (dark) so interactive controls and score panels stand out immediately.

### 1.2 The 60-30-10 Visual Allocation Rule
- **60% Neutral Surfaces:** `surface`, `surfaceDim`, `surfaceContainerLow`, `surfaceContainer`, and `surfaceContainerHigh` cover the screen background, navigation containers, cards, and dialogs.
- **30% Secondary and Structural Containers:** `secondaryContainer`, `surfaceVariant`, `outlineVariant`, and the two player score panels (`playerBlue` / `playerRed` / `playerGreen` / `playerGold` at tone `T40` or `T30`) structure the workspace.
- **10% Primary and Semantic Accents:** `primary`, `tertiary`, and semantic status badges (`success`, `warning`, `error`, `info`) highlight the primary call-to-action button, active selection indicators, game/match ball alerts, and the court timer warning frame.

### 1.3 Non-Color Signal Rule
Color is **never** the only signal in DropShot:
1. **Player identity:** Each player has a name, a position (left/start for Player 1, right/end for Player 2), a text color name in selectors (`Blue`, `Red`, `Green`, `Gold`), and a distinct line pattern in charts (solid line with circle markers for Player 1; dashed line with diamond markers for Player 2).
2. **Court timer warning levels:** In addition to the green, amber, and red border frame, the court timer button displays an explicit icon and status word: `[Clock icon] COURT · 19:00 · 0:28 · OK`, `[Warning icon] COURT · 19:00 · 0:14 · WARN`, and `[Alert icon] COURT · 19:00 · END`. Optional haptic vibration pulses 1, 2, or 3 times at the three warning thresholds.
3. **Game ball and match ball:** In addition to the pulsing border on the leading player's panel, the top status bar reads `GAME BALL: [Player name]` or `MATCH BALL: [Player name]` and a squash-ball icon appears next to the player's points.

### Tonal palettes

| Palette | Seed | T0 | T10 | T20 | T30 | T40 | T50 | T60 | T70 | T80 | T90 | T95 | T99 | T100 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| primary | `#D9501E` | `#000000` | `#321200` | `#5B1B00` | `#852400` | `#AF3000` | `#CE4C1C` | `#EE6735` | `#FF8C60` | `#FFB59A` | `#FFDBCE` | `#FFEDE7` | `#FFFBFA` | `#FFFFFF` |
| secondary | `#7A5B45` | `#000000` | `#2C1700` | `#432B19` | `#5B412F` | `#755945` | `#8F715D` | `#AA8B75` | `#C5A58F` | `#E2C0A9` | `#FFDCC5` | `#FFEDE2` | `#FFFCF9` | `#FFFFFF` |
| tertiary | `#0F7C86` | `#000000` | `#001F23` | `#00363B` | `#004F56` | `#006972` | `#19838D` | `#3E9DA8` | `#5DB9C3` | `#79D4DF` | `#96F1FC` | `#CAF9FF` | `#F5FEFF` | `#FFFFFF` |
| neutral | `#FBF5EA` | `#000000` | `#1E1B16` | `#33302B` | `#494640` | `#615E58` | `#7A7770` | `#94908A` | `#AEABA4` | `#CAC6BF` | `#E6E2DB` | `#F4F0E9` | `#FFFCF6` | `#FFFFFF` |
| neutralVariant | `#FBF5EA` | `#000000` | `#201B0E` | `#363023` | `#4C4639` | `#655E50` | `#7E7668` | `#989081` | `#B2AA9B` | `#CEC6B6` | `#EAE2D1` | `#F9F0DF` | `#FFFCF6` | `#FFFFFF` |
| error | `#B3261E` | `#000000` | `#380C00` | `#690000` | `#93000B` | `#BF0312` | `#E03429` | `#FF5542` | `#FF8A75` | `#FFB4A5` | `#FFDAD3` | `#FFEDE9` | `#FFFBFB` | `#FFFFFF` |
| success | `#2E7D32` | `#000000` | `#062100` | `#003908` | `#005312` | `#206C26` | `#3D863E` | `#58A156` | `#72BC6F` | `#8ED889` | `#A9F5A4` | `#C8FFC2` | `#F5FFF3` | `#FFFFFF` |
| warning | `#B26B00` | `#000000` | `#291800` | `#482900` | `#683D00` | `#895100` | `#AC6700` | `#CA8023` | `#E99A3E` | `#FFB86D` | `#FFDCBD` | `#FFEEDF` | `#FFFCF9` | `#FFFFFF` |
| info | `#1F63C6` | `#000000` | `#001A40` | `#002F67` | `#004491` | `#225BB5` | `#4874D1` | `#678DEE` | `#8DA8FF` | `#B7C4FF` | `#DCE1FF` | `#EEF0FF` | `#FCFCFF` | `#FFFFFF` |
| playerBlue | `#3184FF` | `#000000` | `#001B3F` | `#002F66` | `#004590` | `#005CBC` | `#0074EA` | `#538DFF` | `#8CA8FF` | `#B7C4FF` | `#DCE1FF` | `#EEF0FF` | `#FCFCFF` | `#FFFFFF` |
| playerRed | `#F45C5C` | `#000000` | `#400100` | `#680012` | `#92001D` | `#BB152C` | `#DA3A43` | `#FA585A` | `#FF8983` | `#FFB3AD` | `#FFDAD6` | `#FFEDEB` | `#FFFBFB` | `#FFFFFF` |
| playerGreen | `#39B87F` | `#000000` | `#002112` | `#003822` | `#005233` | `#006C45` | `#008858` | `#00A56B` | `#36C184` | `#58DE9E` | `#77FBBA` | `#BEFFDB` | `#F3FFF8` | `#FFFFFF` |
| playerGold | `#FFC857` | `#000000` | `#251A00` | `#402D00` | `#5C4200` | `#7A5900` | `#997000` | `#B98900` | `#D8A21A` | `#F7BD3B` | `#FFDEA8` | `#FFEFD7` | `#FFFCF7` | `#FFFFFF` |

Additional neutral tones for the M3 surface roles: N4 `#120E05`, N6 `#16130C`, N12 `#221F1A`, N17 `#2C2A24`, N22 `#37342F`, N24 `#3C3933`, N87 `#DDDAD2`, N92 `#ECE8E0`, N94 `#F1EDE6`, N96 `#F7F3EC`, N98 `#FDF9F2`.


### Color roles: light

| Role | HEX |
|---|---|
| primary | `#AF3000` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#FFDBCE` |
| onPrimaryContainer | `#321200` |
| secondary | `#755945` |
| onSecondary | `#FFFFFF` |
| secondaryContainer | `#FFDCC5` |
| onSecondaryContainer | `#2C1700` |
| tertiary | `#006972` |
| onTertiary | `#FFFFFF` |
| tertiaryContainer | `#96F1FC` |
| onTertiaryContainer | `#001F23` |
| error | `#BF0312` |
| onError | `#FFFFFF` |
| errorContainer | `#FFDAD3` |
| onErrorContainer | `#380C00` |
| background | `#FDF9F2` |
| onBackground | `#1E1B16` |
| surface | `#FDF9F2` |
| onSurface | `#1E1B16` |
| surfaceVariant | `#EAE2D1` |
| onSurfaceVariant | `#4C4639` |
| outline | `#7E7668` |
| outlineVariant | `#CEC6B6` |
| inverseSurface | `#33302B` |
| inverseOnSurface | `#F4F0E9` |
| inversePrimary | `#FFB59A` |
| surfaceDim | `#DDDAD2` |
| surfaceBright | `#FDF9F2` |
| surfaceContainerLowest | `#FFFFFF` |
| surfaceContainerLow | `#F7F3EC` |
| surfaceContainer | `#F1EDE6` |
| surfaceContainerHigh | `#ECE8E0` |
| surfaceContainerHighest | `#E6E2DB` |
| surfaceTint | `#AF3000` |
| scrim | `#000000` |
| shadow | `#000000` |

### Color roles: dark

| Role | HEX |
|---|---|
| primary | `#FFB59A` |
| onPrimary | `#5B1B00` |
| primaryContainer | `#852400` |
| onPrimaryContainer | `#FFDBCE` |
| secondary | `#E2C0A9` |
| onSecondary | `#432B19` |
| secondaryContainer | `#5B412F` |
| onSecondaryContainer | `#FFDCC5` |
| tertiary | `#79D4DF` |
| onTertiary | `#00363B` |
| tertiaryContainer | `#004F56` |
| onTertiaryContainer | `#96F1FC` |
| error | `#FFB4A5` |
| onError | `#690000` |
| errorContainer | `#93000B` |
| onErrorContainer | `#FFDAD3` |
| background | `#16130C` |
| onBackground | `#E6E2DB` |
| surface | `#16130C` |
| onSurface | `#E6E2DB` |
| surfaceVariant | `#4C4639` |
| onSurfaceVariant | `#CEC6B6` |
| outline | `#989081` |
| outlineVariant | `#4C4639` |
| inverseSurface | `#E6E2DB` |
| inverseOnSurface | `#33302B` |
| inversePrimary | `#AF3000` |
| surfaceDim | `#16130C` |
| surfaceBright | `#3C3933` |
| surfaceContainerLowest | `#120E05` |
| surfaceContainerLow | `#1E1B16` |
| surfaceContainer | `#221F1A` |
| surfaceContainerHigh | `#2C2A24` |
| surfaceContainerHighest | `#37342F` |
| surfaceTint | `#FFB59A` |
| scrim | `#000000` |
| shadow | `#000000` |

### Color roles: lightHighContrast

| Role | HEX |
|---|---|
| primary | `#5B1B00` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#852400` |
| onPrimaryContainer | `#FFFFFF` |
| secondary | `#432B19` |
| onSecondary | `#FFFFFF` |
| secondaryContainer | `#5B412F` |
| onSecondaryContainer | `#FFFFFF` |
| tertiary | `#00363B` |
| onTertiary | `#FFFFFF` |
| tertiaryContainer | `#004F56` |
| onTertiaryContainer | `#FFFFFF` |
| error | `#690000` |
| onError | `#FFFFFF` |
| errorContainer | `#93000B` |
| onErrorContainer | `#FFFFFF` |
| background | `#FDF9F2` |
| onBackground | `#000000` |
| surface | `#FDF9F2` |
| onSurface | `#000000` |
| surfaceVariant | `#EAE2D1` |
| onSurfaceVariant | `#201B0E` |
| outline | `#363023` |
| outlineVariant | `#4C4639` |
| inverseSurface | `#33302B` |
| inverseOnSurface | `#FFFFFF` |
| inversePrimary | `#FFDBCE` |
| surfaceDim | `#DDDAD2` |
| surfaceBright | `#FDF9F2` |
| surfaceContainerLowest | `#FFFFFF` |
| surfaceContainerLow | `#F7F3EC` |
| surfaceContainer | `#F1EDE6` |
| surfaceContainerHigh | `#ECE8E0` |
| surfaceContainerHighest | `#E6E2DB` |
| surfaceTint | `#5B1B00` |
| scrim | `#000000` |
| shadow | `#000000` |

### Color roles: darkHighContrast

| Role | HEX |
|---|---|
| primary | `#FFDBCE` |
| onPrimary | `#000000` |
| primaryContainer | `#FFB59A` |
| onPrimaryContainer | `#000000` |
| secondary | `#FFDCC5` |
| onSecondary | `#000000` |
| secondaryContainer | `#E2C0A9` |
| onSecondaryContainer | `#000000` |
| tertiary | `#96F1FC` |
| onTertiary | `#000000` |
| tertiaryContainer | `#79D4DF` |
| onTertiaryContainer | `#000000` |
| error | `#FFDAD3` |
| onError | `#000000` |
| errorContainer | `#FFB4A5` |
| onErrorContainer | `#000000` |
| background | `#16130C` |
| onBackground | `#FFFFFF` |
| surface | `#16130C` |
| onSurface | `#FFFFFF` |
| surfaceVariant | `#4C4639` |
| onSurfaceVariant | `#F9F0DF` |
| outline | `#EAE2D1` |
| outlineVariant | `#CEC6B6` |
| inverseSurface | `#E6E2DB` |
| inverseOnSurface | `#000000` |
| inversePrimary | `#852400` |
| surfaceDim | `#16130C` |
| surfaceBright | `#3C3933` |
| surfaceContainerLowest | `#120E05` |
| surfaceContainerLow | `#1E1B16` |
| surfaceContainer | `#221F1A` |
| surfaceContainerHigh | `#2C2A24` |
| surfaceContainerHighest | `#37342F` |
| surfaceTint | `#FFDBCE` |
| scrim | `#000000` |
| shadow | `#000000` |

### Semantic colors

| Role | Light | Dark | Light high contrast | Dark high contrast |
|---|---|---|---|---|
| success.color | `#206C26` | `#8ED889` | `#003908` | `#A9F5A4` |
| success.onColor | `#FFFFFF` | `#003908` | `#FFFFFF` | `#000000` |
| success.container | `#A9F5A4` | `#005312` | `#005312` | `#8ED889` |
| success.onContainer | `#062100` | `#A9F5A4` | `#FFFFFF` | `#000000` |
| warning.color | `#895100` | `#FFB86D` | `#482900` | `#FFDCBD` |
| warning.onColor | `#FFFFFF` | `#482900` | `#FFFFFF` | `#000000` |
| warning.container | `#FFDCBD` | `#683D00` | `#683D00` | `#FFB86D` |
| warning.onContainer | `#291800` | `#FFDCBD` | `#FFFFFF` | `#000000` |
| error.color | `#BF0312` | `#FFB4A5` | `#690000` | `#FFDAD3` |
| error.onColor | `#FFFFFF` | `#690000` | `#FFFFFF` | `#000000` |
| error.container | `#FFDAD3` | `#93000B` | `#93000B` | `#FFB4A5` |
| error.onContainer | `#380C00` | `#FFDAD3` | `#FFFFFF` | `#000000` |
| info.color | `#225BB5` | `#B7C4FF` | `#002F67` | `#DCE1FF` |
| info.onColor | `#FFFFFF` | `#002F67` | `#FFFFFF` | `#000000` |
| info.container | `#DCE1FF` | `#004491` | `#004491` | `#B7C4FF` |
| info.onContainer | `#001A40` | `#DCE1FF` | `#FFFFFF` | `#000000` |

### Player identity colors

| Player color | Role | Light | Dark | Light high contrast | Dark high contrast |
|---|---|---|---|---|---|
| Blue | fill | `#005CBC` | `#005CBC` | `#004590` | `#004590` |
| Blue | onFill | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| Blue | accentOnSurface | `#005CBC` | `#B7C4FF` | `#004590` | `#DCE1FF` |
| Red | fill | `#BB152C` | `#BB152C` | `#92001D` | `#92001D` |
| Red | onFill | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| Red | accentOnSurface | `#BB152C` | `#FFB3AD` | `#92001D` | `#FFDAD6` |
| Green | fill | `#006C45` | `#006C45` | `#005233` | `#005233` |
| Green | onFill | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| Green | accentOnSurface | `#006C45` | `#58DE9E` | `#005233` | `#77FBBA` |
| Gold | fill | `#7A5900` | `#7A5900` | `#5C4200` | `#5C4200` |
| Gold | onFill | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| Gold | accentOnSurface | `#7A5900` | `#F7BD3B` | `#5C4200` | `#FFDEA8` |

### Contrast ratios: light

| Foreground | Background | Use | Ratio | Minimum | Result |
|---|---|---|---|---|---|
| onPrimary `#FFFFFF` | primary `#AF3000` | Normal text | 6.47:1 | 4.5:1 | Pass |
| onPrimaryContainer `#321200` | primaryContainer `#FFDBCE` | Normal text | 13.32:1 | 4.5:1 | Pass |
| onSecondary `#FFFFFF` | secondary `#755945` | Normal text | 6.42:1 | 4.5:1 | Pass |
| onSecondaryContainer `#2C1700` | secondaryContainer `#FFDCC5` | Normal text | 13.25:1 | 4.5:1 | Pass |
| onTertiary `#FFFFFF` | tertiary `#006972` | Normal text | 6.43:1 | 4.5:1 | Pass |
| onTertiaryContainer `#001F23` | tertiaryContainer `#96F1FC` | Normal text | 13.35:1 | 4.5:1 | Pass |
| onError `#FFFFFF` | error `#BF0312` | Normal text | 6.49:1 | 4.5:1 | Pass |
| onErrorContainer `#380C00` | errorContainer `#FFDAD3` | Normal text | 13.29:1 | 4.5:1 | Pass |
| onBackground `#1E1B16` | background `#FDF9F2` | Normal text | 16.35:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surface `#FDF9F2` | Normal text | 16.35:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surfaceContainerLowest `#FFFFFF` | Normal text | 17.16:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surfaceContainerLow `#F7F3EC` | Normal text | 15.52:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surfaceContainer `#F1EDE6` | Normal text | 14.71:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surfaceContainerHigh `#ECE8E0` | Normal text | 14.05:1 | 4.5:1 | Pass |
| onSurface `#1E1B16` | surfaceContainerHighest `#E6E2DB` | Normal text | 13.29:1 | 4.5:1 | Pass |
| onSurfaceVariant `#4C4639` | surface `#FDF9F2` | Normal text | 8.92:1 | 4.5:1 | Pass |
| onSurfaceVariant `#4C4639` | surfaceContainer `#F1EDE6` | Normal text | 8.03:1 | 4.5:1 | Pass |
| onSurfaceVariant `#4C4639` | surfaceContainerHighest `#E6E2DB` | Normal text | 7.25:1 | 4.5:1 | Pass |
| onSurfaceVariant `#4C4639` | surfaceVariant `#EAE2D1` | Normal text | 7.27:1 | 4.5:1 | Pass |
| primary `#AF3000` | surface `#FDF9F2` | Normal text | 6.17:1 | 4.5:1 | Pass |
| primary `#AF3000` | surfaceContainer `#F1EDE6` | Normal text | 5.54:1 | 4.5:1 | Pass |
| secondary `#755945` | surface `#FDF9F2` | Normal text | 6.12:1 | 4.5:1 | Pass |
| tertiary `#006972` | surface `#FDF9F2` | Normal text | 6.13:1 | 4.5:1 | Pass |
| error `#BF0312` | surface `#FDF9F2` | Normal text | 6.18:1 | 4.5:1 | Pass |
| inverseOnSurface `#F4F0E9` | inverseSurface `#33302B` | Normal text | 11.57:1 | 4.5:1 | Pass |
| inversePrimary `#FFB59A` | inverseSurface `#33302B` | Normal text | 7.71:1 | 4.5:1 | Pass |
| outline `#7E7668` | surface `#FDF9F2` | UI part / large text | 4.28:1 | 3.0:1 | Pass |
| outline `#7E7668` | surfaceContainerHighest `#E6E2DB` | UI part / large text | 3.48:1 | 3.0:1 | Pass |
| primary `#AF3000` | surfaceContainer `#F1EDE6` | UI part / large text | 5.54:1 | 3.0:1 | Pass |
| secondary `#755945` | surface `#FDF9F2` | UI part / large text | 6.12:1 | 3.0:1 | Pass |
| success.onColor `#FFFFFF` | success.color `#206C26` | Normal text | 6.49:1 | 4.5:1 | Pass |
| success.onContainer `#062100` | success.container `#A9F5A4` | Normal text | 13.29:1 | 4.5:1 | Pass |
| success.color `#206C26` | surface `#FDF9F2` | Normal text | 6.19:1 | 4.5:1 | Pass |
| warning.onColor `#FFFFFF` | warning.color `#895100` | Normal text | 6.48:1 | 4.5:1 | Pass |
| warning.onContainer `#291800` | warning.container `#FFDCBD` | Normal text | 13.24:1 | 4.5:1 | Pass |
| warning.color `#895100` | surface `#FDF9F2` | Normal text | 6.17:1 | 4.5:1 | Pass |
| info.onColor `#FFFFFF` | info.color `#225BB5` | Normal text | 6.50:1 | 4.5:1 | Pass |
| info.onContainer `#001A40` | info.container `#DCE1FF` | Normal text | 13.31:1 | 4.5:1 | Pass |
| info.color `#225BB5` | surface `#FDF9F2` | Normal text | 6.19:1 | 4.5:1 | Pass |
| error.onColor `#FFFFFF` | error.color `#BF0312` | Normal text | 6.49:1 | 4.5:1 | Pass |
| error.onContainer `#380C00` | error.container `#FFDAD3` | Normal text | 13.29:1 | 4.5:1 | Pass |
| error.color `#BF0312` | surface `#FDF9F2` | Normal text | 6.18:1 | 4.5:1 | Pass |
| playerBlue.onFill `#FFFFFF` | playerBlue.fill `#005CBC` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerBlue.accent `#005CBC` | surface `#FDF9F2` | Normal text | 6.14:1 | 4.5:1 | Pass |
| playerRed.onFill `#FFFFFF` | playerRed.fill `#BB152C` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerRed.accent `#BB152C` | surface `#FDF9F2` | Normal text | 6.14:1 | 4.5:1 | Pass |
| playerGreen.onFill `#FFFFFF` | playerGreen.fill `#006C45` | Normal text | 6.50:1 | 4.5:1 | Pass |
| playerGreen.accent `#006C45` | surface `#FDF9F2` | Normal text | 6.19:1 | 4.5:1 | Pass |
| playerGold.onFill `#FFFFFF` | playerGold.fill `#7A5900` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerGold.accent `#7A5900` | surface `#FDF9F2` | Normal text | 6.14:1 | 4.5:1 | Pass |

### Contrast ratios: dark

| Foreground | Background | Use | Ratio | Minimum | Result |
|---|---|---|---|---|---|
| onPrimary `#5B1B00` | primary `#FFB59A` | Normal text | 7.70:1 | 4.5:1 | Pass |
| onPrimaryContainer `#FFDBCE` | primaryContainer `#852400` | Normal text | 7.23:1 | 4.5:1 | Pass |
| onSecondary `#432B19` | secondary `#E2C0A9` | Normal text | 7.73:1 | 4.5:1 | Pass |
| onSecondaryContainer `#FFDCC5` | secondaryContainer `#5B412F` | Normal text | 7.27:1 | 4.5:1 | Pass |
| onTertiary `#00363B` | tertiary `#79D4DF` | Normal text | 7.73:1 | 4.5:1 | Pass |
| onTertiaryContainer `#96F1FC` | tertiaryContainer `#004F56` | Normal text | 7.23:1 | 4.5:1 | Pass |
| onError `#690000` | error `#FFB4A5` | Normal text | 7.70:1 | 4.5:1 | Pass |
| onErrorContainer `#FFDAD3` | errorContainer `#93000B` | Normal text | 7.22:1 | 4.5:1 | Pass |
| onBackground `#E6E2DB` | background `#16130C` | Normal text | 14.36:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surface `#16130C` | Normal text | 14.36:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surfaceContainerLowest `#120E05` | Normal text | 14.91:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surfaceContainerLow `#1E1B16` | Normal text | 13.29:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surfaceContainer `#221F1A` | Normal text | 12.72:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surfaceContainerHigh `#2C2A24` | Normal text | 11.11:1 | 4.5:1 | Pass |
| onSurface `#E6E2DB` | surfaceContainerHighest `#37342F` | Normal text | 9.60:1 | 4.5:1 | Pass |
| onSurfaceVariant `#CEC6B6` | surface `#16130C` | Normal text | 10.93:1 | 4.5:1 | Pass |
| onSurfaceVariant `#CEC6B6` | surfaceContainer `#221F1A` | Normal text | 9.68:1 | 4.5:1 | Pass |
| onSurfaceVariant `#CEC6B6` | surfaceContainerHighest `#37342F` | Normal text | 7.30:1 | 4.5:1 | Pass |
| onSurfaceVariant `#CEC6B6` | surfaceVariant `#4C4639` | Normal text | 5.52:1 | 4.5:1 | Pass |
| primary `#FFB59A` | surface `#16130C` | Normal text | 10.89:1 | 4.5:1 | Pass |
| primary `#FFB59A` | surfaceContainer `#221F1A` | Normal text | 9.64:1 | 4.5:1 | Pass |
| secondary `#E2C0A9` | surface `#16130C` | Normal text | 10.90:1 | 4.5:1 | Pass |
| tertiary `#79D4DF` | surface `#16130C` | Normal text | 10.86:1 | 4.5:1 | Pass |
| error `#FFB4A5` | surface `#16130C` | Normal text | 10.88:1 | 4.5:1 | Pass |
| inverseOnSurface `#33302B` | inverseSurface `#E6E2DB` | Normal text | 10.18:1 | 4.5:1 | Pass |
| inversePrimary `#AF3000` | inverseSurface `#E6E2DB` | Normal text | 5.01:1 | 4.5:1 | Pass |
| outline `#989081` | surface `#16130C` | UI part / large text | 5.86:1 | 3.0:1 | Pass |
| outline `#989081` | surfaceContainerHighest `#37342F` | UI part / large text | 3.92:1 | 3.0:1 | Pass |
| primary `#FFB59A` | surfaceContainer `#221F1A` | UI part / large text | 9.64:1 | 3.0:1 | Pass |
| secondary `#E2C0A9` | surface `#16130C` | UI part / large text | 10.90:1 | 3.0:1 | Pass |
| success.onColor `#003908` | success.color `#8ED889` | Normal text | 7.76:1 | 4.5:1 | Pass |
| success.onContainer `#A9F5A4` | success.container `#005312` | Normal text | 7.25:1 | 4.5:1 | Pass |
| success.color `#8ED889` | surface `#16130C` | Normal text | 10.89:1 | 4.5:1 | Pass |
| warning.onColor `#482900` | warning.color `#FFB86D` | Normal text | 7.74:1 | 4.5:1 | Pass |
| warning.onContainer `#FFDCBD` | warning.container `#683D00` | Normal text | 7.19:1 | 4.5:1 | Pass |
| warning.color `#FFB86D` | surface `#16130C` | Normal text | 10.89:1 | 4.5:1 | Pass |
| info.onColor `#002F67` | info.color `#B7C4FF` | Normal text | 7.71:1 | 4.5:1 | Pass |
| info.onContainer `#DCE1FF` | info.container `#004491` | Normal text | 7.27:1 | 4.5:1 | Pass |
| info.color `#B7C4FF` | surface `#16130C` | Normal text | 10.91:1 | 4.5:1 | Pass |
| error.onColor `#690000` | error.color `#FFB4A5` | Normal text | 7.70:1 | 4.5:1 | Pass |
| error.onContainer `#FFDAD3` | error.container `#93000B` | Normal text | 7.22:1 | 4.5:1 | Pass |
| error.color `#FFB4A5` | surface `#16130C` | Normal text | 10.88:1 | 4.5:1 | Pass |
| playerBlue.onFill `#FFFFFF` | playerBlue.fill `#005CBC` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerBlue.accent `#B7C4FF` | surface `#16130C` | Normal text | 10.91:1 | 4.5:1 | Pass |
| playerRed.onFill `#FFFFFF` | playerRed.fill `#BB152C` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerRed.accent `#FFB3AD` | surface `#16130C` | Normal text | 10.86:1 | 4.5:1 | Pass |
| playerGreen.onFill `#FFFFFF` | playerGreen.fill `#006C45` | Normal text | 6.50:1 | 4.5:1 | Pass |
| playerGreen.accent `#58DE9E` | surface `#16130C` | Normal text | 10.91:1 | 4.5:1 | Pass |
| playerGold.onFill `#FFFFFF` | playerGold.fill `#7A5900` | Normal text | 6.45:1 | 4.5:1 | Pass |
| playerGold.accent `#F7BD3B` | surface `#16130C` | Normal text | 10.86:1 | 4.5:1 | Pass |

### Contrast ratios: lightHighContrast

| Foreground | Background | Use | Ratio | Minimum | Result |
|---|---|---|---|---|---|
| onPrimary `#FFFFFF` | primary `#5B1B00` | Normal text | 13.11:1 | 4.5:1 | Pass |
| onPrimaryContainer `#FFFFFF` | primaryContainer `#852400` | Normal text | 9.33:1 | 4.5:1 | Pass |
| onSecondary `#FFFFFF` | secondary `#432B19` | Normal text | 13.14:1 | 4.5:1 | Pass |
| onSecondaryContainer `#FFFFFF` | secondaryContainer `#5B412F` | Normal text | 9.37:1 | 4.5:1 | Pass |
| onTertiary `#FFFFFF` | tertiary `#00363B` | Normal text | 13.20:1 | 4.5:1 | Pass |
| onTertiaryContainer `#FFFFFF` | tertiaryContainer `#004F56` | Normal text | 9.32:1 | 4.5:1 | Pass |
| onError `#FFFFFF` | error `#690000` | Normal text | 13.12:1 | 4.5:1 | Pass |
| onErrorContainer `#FFFFFF` | errorContainer `#93000B` | Normal text | 9.35:1 | 4.5:1 | Pass |
| onBackground `#000000` | background `#FDF9F2` | Normal text | 20.01:1 | 4.5:1 | Pass |
| onSurface `#000000` | surface `#FDF9F2` | Normal text | 20.01:1 | 4.5:1 | Pass |
| onSurface `#000000` | surfaceContainerLowest `#FFFFFF` | Normal text | 21.00:1 | 4.5:1 | Pass |
| onSurface `#000000` | surfaceContainerLow `#F7F3EC` | Normal text | 18.99:1 | 4.5:1 | Pass |
| onSurface `#000000` | surfaceContainer `#F1EDE6` | Normal text | 18.00:1 | 4.5:1 | Pass |
| onSurface `#000000` | surfaceContainerHigh `#ECE8E0` | Normal text | 17.19:1 | 4.5:1 | Pass |
| onSurface `#000000` | surfaceContainerHighest `#E6E2DB` | Normal text | 16.27:1 | 4.5:1 | Pass |
| onSurfaceVariant `#201B0E` | surface `#FDF9F2` | Normal text | 16.34:1 | 4.5:1 | Pass |
| onSurfaceVariant `#201B0E` | surfaceContainer `#F1EDE6` | Normal text | 14.70:1 | 4.5:1 | Pass |
| onSurfaceVariant `#201B0E` | surfaceContainerHighest `#E6E2DB` | Normal text | 13.28:1 | 4.5:1 | Pass |
| onSurfaceVariant `#201B0E` | surfaceVariant `#EAE2D1` | Normal text | 13.31:1 | 4.5:1 | Pass |
| primary `#5B1B00` | surface `#FDF9F2` | Normal text | 12.49:1 | 4.5:1 | Pass |
| primary `#5B1B00` | surfaceContainer `#F1EDE6` | Normal text | 11.24:1 | 4.5:1 | Pass |
| secondary `#432B19` | surface `#FDF9F2` | Normal text | 12.52:1 | 4.5:1 | Pass |
| tertiary `#00363B` | surface `#FDF9F2` | Normal text | 12.58:1 | 4.5:1 | Pass |
| error `#690000` | surface `#FDF9F2` | Normal text | 12.50:1 | 4.5:1 | Pass |
| inverseOnSurface `#FFFFFF` | inverseSurface `#33302B` | Normal text | 13.14:1 | 4.5:1 | Pass |
| inversePrimary `#FFDBCE` | inverseSurface `#33302B` | Normal text | 10.18:1 | 4.5:1 | Pass |
| outline `#363023` | surface `#FDF9F2` | UI part / large text | 12.48:1 | 3.0:1 | Pass |
| outline `#363023` | surfaceContainerHighest `#E6E2DB` | UI part / large text | 10.14:1 | 3.0:1 | Pass |
| primary `#5B1B00` | surfaceContainer `#F1EDE6` | UI part / large text | 11.24:1 | 3.0:1 | Pass |
| secondary `#432B19` | surface `#FDF9F2` | UI part / large text | 12.52:1 | 3.0:1 | Pass |
| success.onColor `#FFFFFF` | success.color `#003908` | Normal text | 13.22:1 | 4.5:1 | Pass |
| success.onContainer `#FFFFFF` | success.container `#005312` | Normal text | 9.35:1 | 4.5:1 | Pass |
| success.color `#003908` | surface `#FDF9F2` | Normal text | 12.59:1 | 4.5:1 | Pass |
| warning.onColor `#FFFFFF` | warning.color `#482900` | Normal text | 13.19:1 | 4.5:1 | Pass |
| warning.onContainer `#FFFFFF` | warning.container `#683D00` | Normal text | 9.31:1 | 4.5:1 | Pass |
| warning.color `#482900` | surface `#FDF9F2` | Normal text | 12.56:1 | 4.5:1 | Pass |
| info.onColor `#FFFFFF` | info.color `#002F67` | Normal text | 13.10:1 | 4.5:1 | Pass |
| info.onContainer `#FFFFFF` | info.container `#004491` | Normal text | 9.39:1 | 4.5:1 | Pass |
| info.color `#002F67` | surface `#FDF9F2` | Normal text | 12.49:1 | 4.5:1 | Pass |
| error.onColor `#FFFFFF` | error.color `#690000` | Normal text | 13.12:1 | 4.5:1 | Pass |
| error.onContainer `#FFFFFF` | error.container `#93000B` | Normal text | 9.35:1 | 4.5:1 | Pass |
| error.color `#690000` | surface `#FDF9F2` | Normal text | 12.50:1 | 4.5:1 | Pass |
| playerBlue.onFill `#FFFFFF` | playerBlue.fill `#004590` | Normal text | 9.32:1 | 4.5:1 | Pass |
| playerBlue.accent `#004590` | surface `#FDF9F2` | Normal text | 8.88:1 | 4.5:1 | Pass |
| playerRed.onFill `#FFFFFF` | playerRed.fill `#92001D` | Normal text | 9.38:1 | 4.5:1 | Pass |
| playerRed.accent `#92001D` | surface `#FDF9F2` | Normal text | 8.93:1 | 4.5:1 | Pass |
| playerGreen.onFill `#FFFFFF` | playerGreen.fill `#005233` | Normal text | 9.31:1 | 4.5:1 | Pass |
| playerGreen.accent `#005233` | surface `#FDF9F2` | Normal text | 8.87:1 | 4.5:1 | Pass |
| playerGold.onFill `#FFFFFF` | playerGold.fill `#5C4200` | Normal text | 9.40:1 | 4.5:1 | Pass |
| playerGold.accent `#5C4200` | surface `#FDF9F2` | Normal text | 8.96:1 | 4.5:1 | Pass |

### Contrast ratios: darkHighContrast

| Foreground | Background | Use | Ratio | Minimum | Result |
|---|---|---|---|---|---|
| onPrimary `#000000` | primary `#FFDBCE` | Normal text | 16.28:1 | 4.5:1 | Pass |
| onPrimaryContainer `#000000` | primaryContainer `#FFB59A` | Normal text | 12.33:1 | 4.5:1 | Pass |
| onSecondary `#000000` | secondary `#FFDCC5` | Normal text | 16.30:1 | 4.5:1 | Pass |
| onSecondaryContainer `#000000` | secondaryContainer `#E2C0A9` | Normal text | 12.35:1 | 4.5:1 | Pass |
| onTertiary `#000000` | tertiary `#96F1FC` | Normal text | 16.28:1 | 4.5:1 | Pass |
| onTertiaryContainer `#000000` | tertiaryContainer `#79D4DF` | Normal text | 12.30:1 | 4.5:1 | Pass |
| onError `#000000` | error `#FFDAD3` | Normal text | 16.22:1 | 4.5:1 | Pass |
| onErrorContainer `#000000` | errorContainer `#FFB4A5` | Normal text | 12.32:1 | 4.5:1 | Pass |
| onBackground `#FFFFFF` | background `#16130C` | Normal text | 18.54:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surface `#16130C` | Normal text | 18.54:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surfaceContainerLowest `#120E05` | Normal text | 19.25:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surfaceContainerLow `#1E1B16` | Normal text | 17.16:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surfaceContainer `#221F1A` | Normal text | 16.42:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surfaceContainerHigh `#2C2A24` | Normal text | 14.35:1 | 4.5:1 | Pass |
| onSurface `#FFFFFF` | surfaceContainerHighest `#37342F` | Normal text | 12.39:1 | 4.5:1 | Pass |
| onSurfaceVariant `#F9F0DF` | surface `#16130C` | Normal text | 16.39:1 | 4.5:1 | Pass |
| onSurfaceVariant `#F9F0DF` | surfaceContainer `#221F1A` | Normal text | 14.51:1 | 4.5:1 | Pass |
| onSurfaceVariant `#F9F0DF` | surfaceContainerHighest `#37342F` | Normal text | 10.95:1 | 4.5:1 | Pass |
| onSurfaceVariant `#F9F0DF` | surfaceVariant `#4C4639` | Normal text | 8.28:1 | 4.5:1 | Pass |
| primary `#FFDBCE` | surface `#16130C` | Normal text | 14.37:1 | 4.5:1 | Pass |
| primary `#FFDBCE` | surfaceContainer `#221F1A` | Normal text | 12.73:1 | 4.5:1 | Pass |
| secondary `#FFDCC5` | surface `#16130C` | Normal text | 14.39:1 | 4.5:1 | Pass |
| tertiary `#96F1FC` | surface `#16130C` | Normal text | 14.38:1 | 4.5:1 | Pass |
| error `#FFDAD3` | surface `#16130C` | Normal text | 14.32:1 | 4.5:1 | Pass |
| inverseOnSurface `#000000` | inverseSurface `#E6E2DB` | Normal text | 16.27:1 | 4.5:1 | Pass |
| inversePrimary `#852400` | inverseSurface `#E6E2DB` | Normal text | 7.23:1 | 4.5:1 | Pass |
| outline `#EAE2D1` | surface `#16130C` | UI part / large text | 14.39:1 | 3.0:1 | Pass |
| outline `#EAE2D1` | surfaceContainerHighest `#37342F` | UI part / large text | 9.62:1 | 3.0:1 | Pass |
| primary `#FFDBCE` | surfaceContainer `#221F1A` | UI part / large text | 12.73:1 | 3.0:1 | Pass |
| secondary `#FFDCC5` | surface `#16130C` | UI part / large text | 14.39:1 | 3.0:1 | Pass |
| success.onColor `#000000` | success.color `#A9F5A4` | Normal text | 16.28:1 | 4.5:1 | Pass |
| success.onContainer `#000000` | success.container `#8ED889` | Normal text | 12.33:1 | 4.5:1 | Pass |
| success.color `#A9F5A4` | surface `#16130C` | Normal text | 14.38:1 | 4.5:1 | Pass |
| warning.onColor `#000000` | warning.color `#FFDCBD` | Normal text | 16.22:1 | 4.5:1 | Pass |
| warning.onContainer `#000000` | warning.container `#FFB86D` | Normal text | 12.33:1 | 4.5:1 | Pass |
| warning.color `#FFDCBD` | surface `#16130C` | Normal text | 14.33:1 | 4.5:1 | Pass |
| info.onColor `#000000` | info.color `#DCE1FF` | Normal text | 16.26:1 | 4.5:1 | Pass |
| info.onContainer `#000000` | info.container `#B7C4FF` | Normal text | 12.35:1 | 4.5:1 | Pass |
| info.color `#DCE1FF` | surface `#16130C` | Normal text | 14.35:1 | 4.5:1 | Pass |
| error.onColor `#000000` | error.color `#FFDAD3` | Normal text | 16.22:1 | 4.5:1 | Pass |
| error.onContainer `#000000` | error.container `#FFB4A5` | Normal text | 12.32:1 | 4.5:1 | Pass |
| error.color `#FFDAD3` | surface `#16130C` | Normal text | 14.32:1 | 4.5:1 | Pass |
| playerBlue.onFill `#FFFFFF` | playerBlue.fill `#004590` | Normal text | 9.32:1 | 4.5:1 | Pass |
| playerBlue.accent `#DCE1FF` | surface `#16130C` | Normal text | 14.35:1 | 4.5:1 | Pass |
| playerRed.onFill `#FFFFFF` | playerRed.fill `#92001D` | Normal text | 9.38:1 | 4.5:1 | Pass |
| playerRed.accent `#FFDAD6` | surface `#16130C` | Normal text | 14.35:1 | 4.5:1 | Pass |
| playerGreen.onFill `#FFFFFF` | playerGreen.fill `#005233` | Normal text | 9.31:1 | 4.5:1 | Pass |
| playerGreen.accent `#77FBBA` | surface `#16130C` | Normal text | 14.39:1 | 4.5:1 | Pass |
| playerGold.onFill `#FFFFFF` | playerGold.fill `#5C4200` | Normal text | 9.40:1 | 4.5:1 | Pass |
| playerGold.accent `#FFDEA8` | surface `#16130C` | Normal text | 14.36:1 | 4.5:1 | Pass |
---

## 2. Typography System (Material 3 Type Scale)

- **Font family:** `Roboto` (Android system `sans-serif`, `FontFamily.Default`).
- **Numeric alignment:** All match clocks, game scores, point counters, and statistics tables enable OpenType tabular numerals (`fontFeatureSettings = "tnum"`) so digits never jump horizontally when the clock ticks or the score changes.
- **Minimum body text size:** `16 sp` (`bodySmall`, `bodyMedium`, `titleSmall`, and `labelLarge` are all at least `16 sp`; `bodyLarge` is `18 sp`).
- **System font scaling up to 200%:**
  - All standard text styles use `sp` units and scale linearly with the Android system font scale from `100%` (`1.0x`) to `200%` (`2.0x`).
  - Line heights scale proportionally with the font size.
  - All cards, rows, and list items use `Modifier.heightIn(min = ...)` instead of fixed heights so text wraps to additional lines at `200%` scale without clipping.
  - The giant scoreboard digits (`scoreDisplay`) use an auto-sizing container (`BoxWithConstraints`) that fills the available score panel while keeping a minimum padding of `16 dp` from the panel edges.

| Style Role | Font Family | Weight | Size (`sp`) | Line Height (`sp`) | Letter Spacing (`sp`) | Font Features | Primary Use in DropShot |
|---|---|---|---|---|---|---|---|
| `displayLarge` | Roboto | Regular (`400`) | `57` | `64` | `-0.25` | `tnum` | Match finished overlay title |
| `displayMedium` | Roboto | Regular (`400`) | `45` | `52` | `0.0` | `tnum` | Large tablet summary numerals |
| `displaySmall` | Roboto | Regular (`400`) | `36` | `44` | `0.0` | `tnum` | Onboarding hero numerals |
| `headlineLarge` | Roboto | Regular (`400`) | `32` | `40` | `0.0` | Standard | Screen titles on Expanded tablets |
| `headlineMedium` | Roboto | Regular (`400`) | `28` | `36` | `0.0` | Standard | Player names on score panels |
| `headlineSmall` | Roboto | Regular (`400`) | `24` | `32` | `0.0` | Standard | Top app bar titles, dialog titles |
| `titleLarge` | Roboto | Medium (`500`) | `22` | `28` | `0.0` | Standard | Card titles, match player pairings |
| `titleMedium` | Roboto | Medium (`500`) | `18` | `26` | `0.15` | Standard | Section headers, player list names |
| `titleSmall` | Roboto | Medium (`500`) | `16` | `24` | `0.1` | Standard | Table column headers, subheaders |
| `bodyLarge` | Roboto | Regular (`400`) | `18` | `28` | `0.15` | Standard | Primary descriptions, onboarding body |
| `bodyMedium` | Roboto | Regular (`400`) | `16` | `24` | `0.25` | Standard | Default body copy, dialog messages |
| `bodySmall` | Roboto | Regular (`400`) | `16` | `24` | `0.4` | Standard | Secondary metadata, helper text, timestamps |
| `labelLarge` | Roboto | Medium (`500`) | `16` | `24` | `0.1` | Standard | Button labels, filter chips, switches |
| `labelMedium` | Roboto | Medium (`500`) | `14` | `20` | `0.5` | `tnum` | Navigation bar labels, chart axis ticks |
| `labelSmall` | Roboto | Medium (`500`) | `12` | `16` | `0.5` | `tnum` | Compact chart data point badges |
| `scoreDisplay` (custom) | Roboto | Bold (`700`) | `96` to `240` (auto-fit) | `1.0 × size` | `0.0` | `tnum` | Current game points on Player 1 & Player 2 panels |
| `gameScore` (custom) | Roboto | Bold (`700`) | `32` | `40` | `0.0` | `tnum` | Games won (`GAMES 2`) on score panels |
| `clock` (custom) | Roboto | Medium (`500`) | `22` | `28` | `0.0` | `tnum` | Match timer and court countdown timer |

---

## 3. Layout, Grid, Window Size Classes, and System Insets

### 3.1 Spatial Grid and Touch Targets
- **Base grid:** `8 dp` spatial grid, with `4 dp` half-steps allowed only for internal icon-to-label spacing and compact badges.
- **Spacing scale:**
  - `s0` = `0 dp`
  - `s1` = `4 dp`
  - `s2` = `8 dp`
  - `s3` = `16 dp`
  - `s4` = `24 dp`
  - `s5` = `32 dp`
  - `s6` = `48 dp`
  - `s7` = `64 dp`
- **Minimum touch target:** Every interactive element (buttons, icon buttons, chips, switches, dropdown fields, chart tabs, and table rows) has a minimum touch target of `48 dp × 48 dp` (`Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`).
- **Screen side margins:** Minimum `16 dp` on Compact width screens and `24 dp` on Medium and Expanded width screens.

### 3.2 Window Size Classes and Orientation Support
| Window Size Class | Width Breakpoint | Height Breakpoint | Columns | Side Margin | Gutter | Layout Adaptation in DropShot |
|---|---|---|---|---|---|---|
| **Compact** | `< 600 dp` | `< 480 dp` (phone landscape) | `4` | `16 dp` | `8 dp` | **Portrait:** Bottom `NavigationBar`, single-column lists, stacked Player 1 (top) and Player 2 (bottom) score panels. **Landscape:** Start `NavigationRail` (or top bar), side-by-side Player 1 (left) and Player 2 (right) score panels. |
| **Medium** | `600 dp` to `839 dp` | `480 dp` to `899 dp` | `8` | `24 dp` | `16 dp` | Start `NavigationRail`, 2-column grid for Player Detail statistics cards, side-by-side score panels with `16 dp` gap. |
| **Expanded** | `>= 840 dp` | `>= 900 dp` | `12` | `24 dp` | `24 dp` | Start `NavigationRail`, 2-pane list-detail layout on History and Players screens (list on the left `400 dp` pane, detail on the right pane), full side-by-side scoreboard on Match screen. |

### 3.3 System Bars and Display Cutout Handling
- **General screens (History, Players, Settings, Onboarding, Sign-In, Help):** The app calls `enableEdgeToEdge()` and applies `WindowInsets.safeDrawing` padding so that no interactive control or text overlaps the status bar, the navigation bar, or the camera display cutout in either portrait or landscape orientation.
- **Match screen:** The app sets `FLAG_KEEP_SCREEN_ON`. The background and the `CourtAlertView` warning frame extend edge-to-edge behind the system bars, while the top bar and the two score panels apply `WindowInsets.systemBars.union(WindowInsets.displayCutout)` padding so a camera cutout on the left or right edge in landscape never covers a button or a player's score.

---

## 4. Shape, Elevation, and Motion Tokens

### 4.1 Shape Scale (Corner Radius)
| Token | Corner Radius | Component Mapping |
|---|---|---|
| `none` | `0 dp` | Full-screen backgrounds, court alert border overlay |
| `extraSmall` | `4 dp` | Text field focus indicator, chart tooltip box |
| `small` | `8 dp` | Filter chips, color swatches, snackbars, court timer badge |
| `medium` | `12 dp` | Standard cards, match history rows, dropdown menus |
| `large` | `16 dp` | Player 1 and Player 2 score panels, chart containers |
| `extraLarge` | `28 dp` | Modal dialogs, bottom sheets |
| `full` | `50%` (`CircleShape`) | Filled buttons, outlined buttons, icon buttons, switches |

### 4.2 Elevation Levels
| Token | Elevation (`dp`) | Tonal Surface Role | Component Mapping |
|---|---|---|---|
| `level0` | `0 dp` | `surface` / `surfaceContainerLowest` | Screen background, outlined cards, score panels |
| `level1` | `1 dp` | `surfaceContainerLow` | Elevated match history cards, summary banners |
| `level2` | `3 dp` | `surfaceContainer` | Top app bar on scroll, navigation bar, navigation rail |
| `level3` | `6 dp` | `surfaceContainerHigh` | Modal dialogs, snackbars, dropdown menus |
| `level4` | `8 dp` | `surfaceContainerHigh` | Dragged or hovered interactive cards |
| `level5` | `12 dp` | `surfaceContainerHighest` | Fullscreen overlays (Match Finished overlay) |

### 4.3 Motion Durations, Easing Curves, and "Remove Animations"
| Duration Token | Value (`ms`) | Easing Token | Cubic Bezier Curve | Primary Use in DropShot |
|---|---|---|---|---|
| `short1` | `50 ms` | `linear` | `cubic-bezier(0.0, 0.0, 1.0, 1.0)` | Touch ripple start |
| `short2` | `100 ms` | `standard` | `cubic-bezier(0.2, 0.0, 0.0, 1.0)` | Button press state, switch thumb step |
| `short3` | `150 ms` | `standardAccelerate` | `cubic-bezier(0.3, 0.0, 1.0, 1.0)` | Dialog and menu exit |
| `short4` | `200 ms` | `standard` | `cubic-bezier(0.2, 0.0, 0.0, 1.0)` | Score increment flash, chip selection |
| `medium1` | `250 ms` | `standardDecelerate` | `cubic-bezier(0.0, 0.0, 0.0, 1.0)` | Dialog and snackbar enter |
| `medium2` | `300 ms` | `emphasized` | `cubic-bezier(0.2, 0.0, 0.0, 1.0)` | Tab and screen transitions, chart pager |
| `medium4` | `400 ms` | `emphasizedDecelerate` | `cubic-bezier(0.05, 0.7, 0.1, 1.0)` | Bottom sheet enter |
| `long2` | `500 ms` | `emphasized` | `cubic-bezier(0.2, 0.0, 0.0, 1.0)` | Match Finished overlay entrance |
| `extraLong2` | `800 ms` | `standard` | `cubic-bezier(0.2, 0.0, 0.0, 1.0)` | Game ball / Match ball border pulse (when animations are enabled) |

**Respecting the system setting "Remove animations":**
- DropShot checks `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (and `AccessibilityManager`).
- When animations are removed:
  1. All transition durations resolve to `0 ms` (state changes happen immediately in 1 frame).
  2. The `FireworksView` particle animation on special scores (`6-7` and `6-9`) is disabled and not added to the view hierarchy.
  3. The game-ball / match-ball pulsing border (`ValueAnimator` on panel stroke) becomes a static high-contrast `4 dp` border around the leading player's panel, paired with the text `GAME BALL: [Name]` or `MATCH BALL: [Name]`.

---

## 5. Complete JSON Design Tokens

The full JSON file is stored at [`03-tokens/design-tokens.json`](03-tokens/design-tokens.json). Its top-level structure is shown below:

```json
{
  "meta": {
    "name": "DropShot",
    "version": "1.0.0",
    "designSystem": "Material Design 3",
    "toneModel": "Tone = CIE L*. Hue and chroma in CIE LCh(ab), chroma reduced to fit sRGB."
  },
  "color": {
    "seed": {
      "primary": "#D9501E",
      "secondary": "#7A5B45",
      "tertiary": "#0F7C86",
      "neutral": "#FBF5EA",
      "neutralVariant": "#FBF5EA",
      "error": "#B3261E",
      "success": "#2E7D32",
      "warning": "#B26B00",
      "info": "#1F63C6",
      "playerBlue": "#3184FF",
      "playerRed": "#F45C5C",
      "playerGreen": "#39B87F",
      "playerGold": "#FFC857"
    },
    "scheme": {
      "light": {
        "primary": "#AF3000",
        "onPrimary": "#FFFFFF",
        "primaryContainer": "#FFDBCE",
        "onPrimaryContainer": "#321200",
        "secondary": "#755945",
        "onSecondary": "#FFFFFF",
        "secondaryContainer": "#FFDCC5",
        "onSecondaryContainer": "#2C1700",
        "tertiary": "#006972",
        "onTertiary": "#FFFFFF",
        "tertiaryContainer": "#96F1FC",
        "onTertiaryContainer": "#001F23",
        "error": "#BF0312",
        "onError": "#FFFFFF",
        "errorContainer": "#FFDAD3",
        "onErrorContainer": "#380C00",
        "background": "#FDF9F2",
        "onBackground": "#1E1B16",
        "surface": "#FDF9F2",
        "onSurface": "#1E1B16",
        "surfaceVariant": "#EAE2D1",
        "onSurfaceVariant": "#4C4639",
        "outline": "#7E7668",
        "outlineVariant": "#CEC6B6",
        "inverseSurface": "#33302B",
        "inverseOnSurface": "#F4F0E9",
        "inversePrimary": "#FFB59A",
        "surfaceDim": "#DDDAD2",
        "surfaceBright": "#FDF9F2",
        "surfaceContainerLowest": "#FFFFFF",
        "surfaceContainerLow": "#F7F3EC",
        "surfaceContainer": "#F1EDE6",
        "surfaceContainerHigh": "#ECE8E0",
        "surfaceContainerHighest": "#E6E2DB",
        "surfaceTint": "#AF3000",
        "scrim": "#000000",
        "shadow": "#000000"
      },
      "dark": {
        "primary": "#FFB59A",
        "onPrimary": "#5B1B00",
        "primaryContainer": "#852400",
        "onPrimaryContainer": "#FFDBCE",
        "secondary": "#E2C0A9",
        "onSecondary": "#432B19",
        "secondaryContainer": "#5B412F",
        "onSecondaryContainer": "#FFDCC5",
        "tertiary": "#79D4DF",
        "onTertiary": "#00363B",
        "tertiaryContainer": "#004F56",
        "onTertiaryContainer": "#96F1FC",
        "error": "#FFB4A5",
        "onError": "#690000",
        "errorContainer": "#93000B",
        "onErrorContainer": "#FFDAD3",
        "background": "#16130C",
        "onBackground": "#E6E2DB",
        "surface": "#16130C",
        "onSurface": "#E6E2DB",
        "surfaceVariant": "#4C4639",
        "onSurfaceVariant": "#CEC6B6",
        "outline": "#989081",
        "outlineVariant": "#4C4639",
        "inverseSurface": "#E6E2DB",
        "inverseOnSurface": "#33302B",
        "inversePrimary": "#AF3000",
        "surfaceDim": "#16130C",
        "surfaceBright": "#3C3933",
        "surfaceContainerLowest": "#120E05",
        "surfaceContainerLow": "#1E1B16",
        "surfaceContainer": "#221F1A",
        "surfaceContainerHigh": "#2C2A24",
        "surfaceContainerHighest": "#37342F",
        "surfaceTint": "#FFB59A",
        "scrim": "#000000",
        "shadow": "#000000"
      }
    }
  }
}
```
