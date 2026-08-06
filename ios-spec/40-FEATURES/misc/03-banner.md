---
id: 40-misc-03
title: Pomodoro banner
layer: ui
status: TODO
depends_on: [40-pomodoro-01]
blocks: []
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/banner/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The in-app floating pomodoro banner shown while a session runs.

## 2. Why this way

**This is the in-app counterpart to the Live Activity**, and it is fully portable — a Compose overlay inside the app's own window, not a system overlay. No platform work needed.

`TDPomodoroBanner` (256 LOC) animates MM:SS **per digit**, which is why a naive recomposition of the whole string looks wrong.

**It must not double up with the Live Activity.** When the app is foreground the banner is the right surface; the Live Activity is for when it is not. Showing both is noise.

## 3. Source

| Path | LOC |
|---|---|
| `ui/banner/` (3 files) | 296 |
| `uikit/…/TDPomodoroBanner.kt` | 256 — per-digit animation |
| `domain/engine/PomodoroEngine.kt` | the state source |

## 4. Target

`shared/ui/commonMain/…/ui/banner/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the banner appears while a session runs and disappears when it ends.
3. Verify per-digit animation is smooth against a live timer.
4. Verify tapping it opens the pomodoro screen.
5. Verify it does not overlap the top bar or bottom bar.
6. Verify it respects safe areas on iOS.
7. Verify it does not compete visually with the Live Activity.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Appears during a session, disappears after
- [ ] Per-digit animation smooth
- [ ] Tap opens the pomodoro screen
- [ ] No overlap with top or bottom bar
- [ ] Safe areas respected on iOS
- [ ] Three kits, two themes, two languages
- [ ] Previews cover focus, break and paused

## 8. Pitfalls

- **Per-digit animation.** Recomposing the whole string looks wrong.
- **Do not overlap the bars.** Check on the shortest and tallest devices.
- **Safe areas on iOS** — the banner must not sit under the Dynamic Island or home indicator.
- **It is an in-app overlay, not a system one.** No platform work required.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: start a session, navigate around (banner follows), tap it,
# check safe areas on a Dynamic Island device, 3 kits
```
