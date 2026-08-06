---
id: 50-01
title: Palette kits & `TDStyle`
layer: design
status: TODO
depends_on: [50-00]
blocks: [40-settings-05]
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/theme/**
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/modifier/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'
---

## 1. Goal

Verify all three palette kits render identically on iOS, including the custom drawing that gives PIXEL and MONOCHROME their character.

## 2. Why this way

**There are two geometry systems, not three.** `monochromeStyle() = defaultStyle()` (`Style.kt:155`) — MONOCHROME differs from ORIGINAL **only in colour**. That halves the surface: only ORIGINAL and PIXEL need distinct geometry, motion and typeface verification.

What actually varies:

| Axis | ORIGINAL | MONOCHROME | PIXEL |
|---|---|---|---|
| Shapes | `RoundedCornerShape` 4/8/12/16/20 + `CircleShape` | **identical** | `PixelCornerShape(unit, steps)` — stair-stepped |
| Font | Poppins (4 weights) | Poppins | **Pixelify Sans (2 weights)** |
| `minFontSize` | 0.sp | 0.sp | **12.sp floor**, small end only |
| Border | 1.dp | 1.dp | **2.dp** |
| Elevation | `SOFT` (neumorphic blur) | `SOFT` | **`HARD`** (zero-blur, 4dp offset, always `colors.black`) |
| Grid | `Lines` 24dp, but `gridLine = Transparent` short-circuits | `Lines` 24dp graph paper | **`Dither`** 2dp checkerboard |
| Motion | `FastOutSlowInEasing` | same | **`SteppedEasing`**, 6/4/8 discrete steps |

**Four pieces of custom drawing carry the whole visual identity**, and all four are pure Compose that ports unchanged:

- `PixelCornerShape` — `Outline.Generic` stair path with `blockPx()` clamping so small badges get proportionally shorter stairs
- `GridBackground` — a 2×2 `ImageBitmap` tile painted as a repeating `ShaderBrush`. The tiling is not an optimisation detail: a per-cell loop at 2dp on a 411×900dp screen is ~92,000 cells.
- `tdShadow` / `tdDropShadow` — branch on `elevationStyle`; SOFT delegates to `neumorphicShadow`, HARD draws an offset block
- `PixelSurface` — `pixelSurface()`, `hardShadow()`, `drawBevel()`

**One thing genuinely changes on iOS, in a good way.** `PixelCornerShape` produces `Outline.Generic`, which cannot drive a platform elevation shadow below Android API 29 — hence the hard-shadow fallback. That constraint is Android-specific and simply does not exist on iOS. Do not "fix" the fallback; it is what PIXEL is supposed to look like.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `uikit/…/theme/PaletteKit.kt` | 70 | The enum, `colors()`, `style()`, `gridColors()`, `stripColors()`. **Entry names are the persisted DataStore values.** |
| `uikit/…/theme/Style.kt` | 240 | `defaultStyle()`, `monochromeStyle()` (= default), `pixelStyle()`, `SteppedEasing`, `tdCorner()`, `tdOutlineColor()` |
| `uikit/…/theme/PixelCornerShape.kt` | 119 | `stairXFirst`/`stairYFirst`, `blockPx()` |
| `uikit/…/modifier/GridBackground.kt` | — | `GridStyle { Lines, Dots, Dither }`, `drawWithCache` + `ImageShader` |
| `uikit/…/modifier/TdShadow.kt`, `NeumorphicShadow.kt`, `PixelSurface.kt` | 369 total | The elevation system |
| `navigation/ThemeChangeReveal.kt` | 108 | The palette-switch wipe; calls `colors()`/`style()` **outside** a `TDTheme` scope |
| `ui/appcolors/` | 228 | The picker; preview cards also resolve outside the theme scope |
| Exhaustive `when (TDTheme.palette)` sites | — | `TDBottomBar`, `TDNavigationRail`, `PrioritySelector`, `SearchGroupItems`, `TaskTypeBadge`, `HomeTaskList`, `TDButton`, `TDYearStrip` — **keep them exhaustive** |

## 4. Target

No new files — verification of `uikit/src/commonMain/…/{theme,modifier}/` after `20-10`.

## 5. Steps

1. **Confirm `PixelCornerShape` renders identically.** Screenshot a card, a button and a small badge in PIXEL on both platforms. The `blockPx()` clamp is the subtle part — small elements must get shorter stairs, not the same stairs scaled.

2. **Confirm `GridBackground` renders identically**, especially `Dither` at 2dp. Check performance: it must be a single tiled draw, not a loop.

3. **Confirm the elevation system.** SOFT (neumorphic blur) is the risk — `20-10` had to resolve `BlurMaskFilter`, and this is where that decision shows.

4. **Confirm `SteppedEasing`.** PIXEL animations should visibly step, not glide. Watch a progress bar and a theme change.

5. **Confirm the typeface switches.** Pixelify Sans across all ~490 `TDText` sites in PIXEL; Poppins elsewhere.

6. **Confirm `minFontSize`** lifts the small end and leaves the 96sp hero alone.

7. **Confirm out-of-scope resolution works** — `ThemeChangeReveal` and the App Colors preview cards call `colors()`/`style()` outside a `TDTheme { }`. If that broke, the palette picker shows wrong previews and the switch animation uses the wrong colours.

8. **Verify every exhaustive `when (TDTheme.palette)`** still compiles exhaustively. They are deliberately exhaustive so a new kit is a compile error rather than a silent wrong branch.

9. **Verify persistence.** The enum entry names are the stored DataStore values — a rename resets every user who selected that kit.

## 6. Code skeleton

```kotlin
// Style.kt:155 — MONOCHROME is colour-only. Two geometry systems, not three.
internal fun monochromeStyle(): TDStyle = defaultStyle()
```

```kotlin
// GridBackground — the tiling is not an optimisation detail.
// A per-cell loop at 2dp on a 411x900dp screen is ~92,000 cells per frame.
Modifier.drawWithCache {
    val tile = buildDitherTile(spacing, lineColor)     // 2x2 cell ImageBitmap, built once
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }                   // one draw call
}
```

## 7. Acceptance

- [ ] All three kits render identically on both platforms, light and dark
- [ ] PIXEL: stair-stepped corners correct at card, button **and** small-badge sizes (the `blockPx()` clamp)
- [ ] PIXEL: 2dp borders, hard shadows in `colors.black`, dither grid, Pixelify Sans everywhere
- [ ] MONOCHROME: graph-paper grid at 24dp; the four semantic accents stay chromatic
- [ ] ORIGINAL: no grid (transparent `gridLine` short-circuits); neumorphic shadows match pre-migration
- [ ] `SteppedEasing` visibly steps in PIXEL
- [ ] `minFontSize` lifts `subheading2` to 12sp in PIXEL; the 96sp hero unchanged
- [ ] Grid renders as a single tiled draw — verified in a frame profile, not by eye
- [ ] `ThemeChangeReveal` and App Colors preview cards resolve correctly outside the theme scope
- [ ] Every `when (TDTheme.palette)` is still exhaustive
- [ ] Enum entry names unchanged; a user's saved kit survives an upgrade
- [ ] `PaletteStyleTest` passes unchanged

## 8. Pitfalls

- **Do not rename `PaletteKit` entries.** They are the persisted values; a rename silently resets every user who chose that kit.
- **Do not add an `else` branch to any `when (TDTheme.palette)`.** Exhaustiveness is what makes adding a kit a compile error instead of a silent wrong branch.
- **Do not "fix" the hard-shadow fallback.** `Outline.Generic` cannot drive a platform shadow below API 29 on Android — but the hard shadow *is* the PIXEL look, not a workaround to be undone on iOS.
- **The grid tiling is load-bearing.** Replacing the `ImageShader` with a loop produces ~92,000 draw operations per frame.
- **`blockPx()` clamping is subtle.** Small badges must get proportionally shorter stairs. Uniform stairs on a 20dp badge look broken.
- **MONOCHROME keeps four chromatic accents** — blue (one-time), orange (pomodoro), green (done), red (error). Greying them out is a common misreading of "monochrome".
- **`SteppedEasing` has different step counts per property** (6/4/8). Do not unify them.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'

# Visual, both platforms, light + dark, all three kits
#   Settings → App Colors → switch kits; the reveal animation uses the right colours
#   check: card corners, small-badge corners, borders, shadows, grid, typeface
#   PIXEL: watch a progress bar — motion should step, not glide
#   confirm the picker's preview cards render each kit correctly

# Performance
#   frame profile a scrolling list in MONOCHROME (graph paper) and PIXEL (dither)
```
