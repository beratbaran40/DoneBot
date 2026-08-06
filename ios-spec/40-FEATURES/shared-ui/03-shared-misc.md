---
id: 40-shared-03
title: Remaining shared UI
layer: ui
status: TODO
depends_on: [20-11, 30-15]
blocks: [40-core-01]
parallel_safe: false
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/common/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Everything left in `ui/common/` after the task form and location picker are split out — the cross-screen helpers most of the app depends on.

## 2. Why this way

**This is infrastructure, not a feature**, and almost every screen touches some of it. It goes early (Wave 1) because leaving it late blocks everything else.

**It is also where most of the remaining Android escapes live.** `ui/` has 33 `Context` uses, 11 `Intent`, 10 `Uri`, 6 `Settings` and the WebView imports — and a large share of them are here. Each routes through a `30-15` contract or moves to `androidMain`.

**Two components carry recorded rules.** `SecureScreen` must not promise screenshot protection on iOS. `ResponsiveContainer` must read the window size class from the composition and never cache it, because iPad resizes live in Split View.

## 3. Source

| Path | Note |
|---|---|
| `ui/common/` (33 files, 3,478 LOC) | minus `taskform/` and `locationpicker/` |
| `ResponsiveContainer.kt` | the adaptive switch — **do not cache** |
| `LockScreenOrientation.kt`, `AnimationsEnabled.kt` | → `30-15` `ScreenBehavior` |
| `SecureScreen.kt` | → `30-15`; **`blocksScreenshots` is false on iOS** |
| `ScreenInfoDialog.kt` | the info-dialog system |
| `FeedbackIntent.kt`, `TaskLocationIntent.kt` | → `30-15` `ExternalLinks` |
| `components/ImageCropOverlay.kt` (299) | pure Canvas |
| `BottomSheetNavigator.kt` (282) | custom sheet navigator |
| `AppPixelIcons.kt` | merged into `LocalPixelIconMap` |

## 4. Target

`shared/ui/commonMain/…/ui/common/` — verification plus contract wiring.

## 5. Steps

1. Verify the files compile in `commonMain`, or record each blocking import.
2. Route every `Intent`, `Context`, `Uri` and `Settings` use through a `30-15` contract.
3. **Verify `ResponsiveContainer` reads the size class live**, never cached.
4. Verify `SecureScreenEffect()` and that `blocksScreenshots` is false on iOS.
5. Verify `LockScreenOrientation` and `AnimationsEnabled` map to iOS equivalents.
6. Verify `ScreenInfoDialog` on every `hasInfoDialog` screen.
7. Verify `BottomSheetNavigator` — sheets present differently on iPad.
8. Verify `ImageCropOverlay` renders identically.
9. Verify the pixel-icon map merge.
10. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Files compile in `commonMain`, or blocking imports are recorded
- [ ] No raw `Intent`, `Context`, `Uri` or `Settings` use left in `commonMain`
- [ ] **`ResponsiveContainer` responds to a live iPad resize**
- [ ] `SecureScreenEffect()` works; `blocksScreenshots` false on iOS
- [ ] Orientation lock and Reduce Motion map correctly
- [ ] Info dialog works on every flagged screen
- [ ] Sheets present correctly on iPhone **and** iPad
- [ ] `ImageCropOverlay` renders identically
- [ ] Pixel-icon map merged; PIXEL swaps work
- [ ] Three kits, two themes, two languages
- [ ] Previews cover the shared components

## 8. Pitfalls

- **Never cache the window size class.** iPad resizes live with no configuration change.
- **`blocksScreenshots` is false on iOS.** Copy must not claim otherwise.
- **Sheets present as form sheets or popovers on iPad**, not bottom sheets. Check every one.
- **This is Wave 1.** Leaving it late blocks every other feature.
- **Do not `expect`/`actual` Toast.** Snackbar.
- **The pixel-icon map merges two module maps.** Missing one leaves half the icons unswapped in PIXEL.

## 9. Verification

```bash
grep -rnE '^import android\.' shared/ui/src/commonMain/**/common/ && echo "ESCAPES REMAIN" || echo "clean"
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# iPad: drag the Split View divider on a ResponsiveContainer screen;
# open every sheet on iPhone and iPad; check info dialogs; PIXEL icon swaps
```
