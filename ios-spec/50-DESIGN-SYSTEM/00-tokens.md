---
id: 50-00
title: Design tokens — colors, typography, shapes
layer: design
status: TODO
depends_on: [20-10]
blocks: [50-01, 50-02, 50-03, 50-04]
parallel_safe: true
estimate: 8h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/theme/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'
---

## 1. Goal

Verify that the token layer survived the CMP move intact, and that every value renders identically on both platforms. **This is a verification task, not a transformation task** — `20-10` already moved the code.

## 2. Why this way

The token layer is pure Compose: data classes, composition locals, `@Composable` getters. Nothing in it touches Android. It moves to `commonMain` unchanged, which is exactly why it needs *checking* rather than *porting* — a silent regression here affects all 490 `TDText` call sites and every screen.

The scale:

| Layer | Detail |
|---|---|
| `TDColor` | **45 fields**, `@Immutable data class`; field 45 is a nested 30-field `PolaroidColors` |
| Palette factories | **6** — `{default,monochrome,pixel}{Light,Dark}Colors()`, each defining all 45 explicitly |
| Explicit colour literals | **~270** (45 × 6), plus polaroid variants |
| `TDTypography` | **15 text styles**, all `@Composable` getters so colour resolves at read time |
| `TDShapes` | 6 slots — `tiny/small/medium/large/xLarge` (4/8/12/16/20 dp) + `pill`/`circle` |
| Fonts | Poppins ×4, Pixelify Sans ×2 |

**Two properties are easy to break and hard to notice.**

1. **Typography must stay `@Composable` getters.** A top-level `val TextStyle` cannot read the kit — that exact bug once pinned every button label to Poppins until `TDButton` was fixed. If anything in the move turned a getter into a constant, the PIXEL kit silently loses its typeface.
2. **The naming is not fully semantic.** `purple` is blue in MONOCHROME and NES blue in PIXEL; `brown`/`beige`/`softPink` are greys in some kits. Anyone "tidying up" names breaks three palettes at once.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `uikit/…/theme/Color.kt` | 647 | The 45-field `TDColor` (lines ~601-647) and the six factories (~11, 108, 205, 301, 396, 499) |
| `uikit/…/theme/Type.kt` | 273 | 15 styles; `TDTypography(fontFamily, displayFontFamily, minFontSize)`; every style sets `LineHeightStyle(Center, Trim.None)` |
| `uikit/…/theme/Style.kt` | 240 | `TDStyle`/`TDShapes`/`TDMotion`, `SteppedEasing`, `tdCorner()`, `tdOutlineColor()` |
| `uikit/…/theme/TDTheme.kt` | 125 | 7 composition locals; the `remember(palette)` construction |
| `uikit/…/theme/PolaroidColors.kt` | 103 | The 30-field nested struct |
| `uikit/…/theme/ComponentColors.kt` | 41 | M3 `TextFieldColors` / `TimePickerColors` adapters |
| `app/…/theme/PaletteStyleTest.kt` | — | **Locks the shape values. Must pass unchanged.** |
| `CLAUDE.md` § Color Usage | — | The semantic token table and its rules |

## 4. Target

No new files. This task verifies `uikit/src/commonMain/kotlin/com/todoapp/uikit/theme/` after `20-10`.

## 5. Steps

1. **Confirm all nine theme files are in `commonMain`** and that `TDTheme.kt`'s only platform dependency is `SystemBarsEffect`.

2. **Verify the 45 fields are intact**, including the nested `polaroid`. A dropped field is a compile error; a *reordered* default parameter is not, and silently changes colours.

3. **Verify all six factories exist** and each defines all 45 values explicitly.

4. **Verify typography is still `@Composable` getters.** Grep for any top-level `val` of type `TextStyle` or `FontFamily` in the theme package — there must be none.

5. **Verify the fonts load** from `composeResources` on both platforms.

6. **Run `PaletteStyleTest`.** It locks the canonical dp values. Any change means a shape slot was remapped.

7. **Render a token gallery** — a debug screen showing all 45 swatches, 15 text styles and 6 shape slots — and screenshot it on both platforms in both themes for all three kits. **18 screenshots.** This is the only reliable way to catch a value drift.

8. **Verify `LineHeightStyle`.** Every style sets `LineHeightStyle(Center, Trim.None)`. Compose's iOS text metrics differ slightly; if baselines look off, this is the first place to look — but do **not** change the values to compensate on one platform.

## 6. Code skeleton

```kotlin
// The property that must survive: typography is a @Composable getter, not a constant.
// A top-level `val` cannot read the active kit — that exact bug once pinned every
// button label to Poppins regardless of palette.
val TDTheme.typography: TDTypography
    @Composable @ReadOnlyComposable get() = LocalTypography.current
```

```kotlin
// Debug-only token gallery — the verification surface for this task.
@Composable
fun TokenGallery() {
    Column {
        // 45 colour swatches, each labelled with its field name
        // 15 text styles, each rendering its own name
        // 6 shape slots
        // border width, elevation style, grid style
    }
}
```

## 7. Acceptance

- [ ] All nine theme files compile in `commonMain`; only `SystemBarsEffect` is platform-bound
- [ ] `TDColor` has all 45 fields including the nested `polaroid`
- [ ] All six palette factories present, each defining 45 values explicitly
- [ ] `TDTypography` has all 15 styles, all `@Composable` getters
- [ ] **No top-level `val` of type `TextStyle` or `FontFamily` anywhere in the theme package**
- [ ] Both font families load on both platforms
- [ ] `PaletteStyleTest` passes unchanged
- [ ] Token gallery screenshotted on both platforms × light/dark × 3 kits (**18 images**), compared pairwise
- [ ] `minFontSize` floor applies in PIXEL (`subheading2` lifts from 10sp to 12sp) and the 96sp pomodoro hero is **not** scaled
- [ ] Text baselines look correct on iOS; no per-platform value tweaks were made

## 8. Pitfalls

- **Do not "fix" the token names.** `purple` is blue in two kits; `brown`/`beige`/`softPink` are greys in some. They are kept for source compatibility and renaming them breaks three palettes.
- **A top-level `TextStyle` cannot read the kit.** This has already happened once in this codebase.
- **Reordered default parameters change colours silently.** The factories pass all 45 values positionally-by-name; an order change compiles fine and produces wrong output.
- **Do not compensate for iOS text metrics by changing token values.** Fix it in the platform text configuration, or accept the difference — a divergent token table is the start of two design systems.
- **`minFontSize` applies only to the small end of the ramp.** It deliberately does not scale the 96sp pomodoro display.
- **`surface` is not a card background.** Cards use `lightPending`; `surface` is sheet/dialog/app-bar chrome. `CLAUDE.md` lists this as an anti-pattern.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'

# Structural checks
grep -c "val " uikit/src/commonMain/kotlin/com/todoapp/uikit/theme/Color.kt      # TDColor fields
grep -rn "^val .*: TextStyle\|^val .*: FontFamily" uikit/src/commonMain/kotlin/com/todoapp/uikit/theme/ \
  && echo "TOP-LEVEL STYLE — kit cannot be read" || echo "clean"

# Visual: token gallery, both platforms, light+dark, 3 kits = 18 screenshots, compared pairwise
```
