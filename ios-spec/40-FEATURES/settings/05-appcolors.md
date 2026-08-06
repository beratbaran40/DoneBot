---
id: 40-settings-05
title: App Colors (palette picker)
layer: ui
status: TODO
depends_on: [40-settings-01, 50-01]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/appcolors/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'
---

## 1. Goal

The palette-kit picker, with live previews and the theme-change reveal animation.

## 2. Why this way

**The preview cards resolve colours outside a `TDTheme { }` scope**, which is why `PaletteKit.colors()`, `style()`, `gridColors()` and `stripColors()` are callable without composition locals. If that escape hatch broke in the CMP move, every preview card renders in the *active* palette instead of the one it is advertising — a subtle bug that makes the picker useless.

**`ThemeChangeReveal` does the same thing** from a `LaunchedEffect` that runs outside the theme it hosts.

**Enum entry names are the persisted DataStore values.** Renaming one silently resets every user who had it selected.

**A recorded lesson on the reveal:** on tall screens a centre-expanding circle looks wrong; a top-down curtain wipe was chosen instead. And the animation needs the *old* frame captured before the switch, which is the displayed/target split.

## 3. Source

| Path | LOC |
|---|---|
| `ui/appcolors/` (3 files) | 228 |
| `uikit/…/theme/PaletteKit.kt` | `colors()`, `style()`, `gridColors()`, `stripColors()` |
| `navigation/ThemeChangeReveal.kt` | 108 — the wipe |
| `data/repository/PaletteRepository` | DataStore key `palette_kit` |
| `app/src/test/…/PaletteStyleTest.kt` | the guard |

## 4. Target

`shared/ui/commonMain/…/ui/appcolors/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. **Verify each preview card renders its own kit**, not the active one.
3. Verify the grid preview: MONOCHROME graph paper, PIXEL dither, ORIGINAL plain.
4. Verify the strip colours per kit.
5. Verify selecting a kit applies it app-wide and persists.
6. Verify the reveal animation on both platforms, including on a tall screen.
7. Verify the title and description strings for each kit in both languages.
8. Two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] **Each preview card renders its own kit**
- [ ] Grid preview correct per kit
- [ ] Strip colours correct per kit
- [ ] Selection applies app-wide and persists across relaunch
- [ ] Reveal animation works on both platforms and on a tall screen
- [ ] Kit titles and descriptions localized
- [ ] `PaletteStyleTest` passes
- [ ] Two themes, two languages
- [ ] Previews cover all three cards

## 8. Pitfalls

- **Preview cards must resolve outside the active theme.** If they do not, every card looks the same.
- **Do not rename `PaletteKit` entries.** They are persisted values.
- **The reveal captures the old frame first.** Without the displayed/target split it wipes to the same colour it started from.
- **Centre-expanding circles look wrong on tall screens** — the curtain wipe was a deliberate choice.
- **Adding a kit is a compile error in several exhaustive `when`s.** That is intentional.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*'
# Both platforms: open App Colors in each kit — all three cards always show their own
# colours; switch kits and watch the reveal; relaunch (persisted); EN + TR
```
