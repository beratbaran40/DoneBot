---
id: 50-03
title: Components tier B — animation-driven
layer: design
status: TODO
depends_on: [50-00]
blocks: []
parallel_safe: true
estimate: 14h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/components/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Verify the fourteen animation-driven components on iOS — motion timing, gesture handling, and Reduce Motion behaviour.

## 2. Why this way

Animation ports unchanged (Compose animation is platform-neutral), but three things around it are worth checking deliberately:

1. **`SteppedEasing` in PIXEL.** Motion should visibly step. Easy to lose if a transition is reimplemented.
2. **Gesture-driven components.** `TDOverlayNotificationCard` and `TDOverlayDailyPlanNotificationCard` are swipe-dismissable; `TDGroupsSummary` has drag states. iOS gesture recognition and fling physics differ, and these are the components where that shows.
3. **Reduce Motion.** `LocalReduceMotion` exists on Android; `30-15` maps it to `UIAccessibility.isReduceMotionEnabled`. Every animation here must honour it — an accessibility requirement, not polish.

| Component | LOC | Watch for |
|---|---|---|
| `TDGroupsSummary` | 455 | Drag states |
| `TDMonthlyDatePicker` | 362 | Month transition |
| `TDGroupTaskCard` | 358 | |
| `TDOverlayNotificationCard` | 325 | **Swipe-to-dismiss** |
| `TDTaskCard` | 292 | |
| `TDStatisticCard` | 282 | Value count-up |
| `TDPomodoroBanner` | 256 | Per-digit animated MM:SS |
| `TDMascotDialog` | 234 | Mascot + radial halo + speech bubble |
| `TDTaskCompletionCard` | 225 | |
| `TDSkeleton` | 199 | Shimmer brush |
| `TDOverlayDailyPlanNotificationCard` | 190 | **Swipe-to-dismiss** |
| `TDGeneralProgressBar` | 160 | 20 pixel segments |
| `TDHeartsDepletedDialog` | 92 | |
| `TDChatThinkingIndicator` | 71 | Looping |

## 3. Source — read before writing

All under `uikit/src/commonMain/kotlin/com/todoapp/uikit/components/`. Also:

| Path | Why |
|---|---|
| `uikit/…/theme/Style.kt` | `TDMotion`, `SteppedEasing`, `stepped` flag |
| `ui/common/` → `LocalReduceMotion` | The accessibility gate |
| `shared/ui/…/platform/ScreenBehavior` (`30-15`) | Where `isReduceMotionEnabled` comes from |
| `docs/screenshots/` | Static references — animation needs live checking |
| `uikit/…/components/TDMascotDialog.kt` | Recently extracted from two copies — verify both call sites still render |

## 4. Target

No new files — verification after `20-10`.

## 5. Steps

1. **Confirm all fourteen compile in `commonMain`.**

2. **Check swipe-to-dismiss on both overlay cards.** iOS fling velocity differs; the dismiss threshold may feel wrong even though the code is identical. If it does, adjust the threshold **in shared code with a comment**, not per platform.

3. **Check `TDGroupsSummary` drag.** Long-press-to-drag timing differs between platforms.

4. **Check `SteppedEasing` in PIXEL** on `TDGeneralProgressBar` (20 discrete segments) and the card transitions. Stepping must be visible.

5. **Check `TDSkeleton`'s shimmer.** Continuous animation — verify it stops when off-screen rather than running forever.

6. **Check `TDPomodoroBanner`'s per-digit animation** against a running timer; digits must not tear or lag.

7. **Verify Reduce Motion.** With it enabled, animations should shorten or disappear — not merely run faster. Check every one of the fourteen.

8. **Check `TDChatThinkingIndicator`** stops when the response arrives. A looping indicator left running is a battery bug.

9. **Check `TDMascotDialog`** at both call sites.

## 6. Code skeleton

```kotlin
// Every animation must honour Reduce Motion. This is an accessibility requirement,
// not a polish item — and "run it faster" is not honouring it.
@Composable
fun tdAnimationSpec(): AnimationSpec<Float> =
    if (LocalReduceMotion.current) snap() else TDTheme.motion.standard
```

## 7. Acceptance

- [ ] All fourteen compile in `commonMain`
- [ ] Motion timing feels equivalent on both platforms
- [ ] Swipe-to-dismiss works on both overlay cards with a threshold that feels right on iOS
- [ ] `TDGroupsSummary` drag works; long-press timing is acceptable
- [ ] `SteppedEasing` visibly steps in PIXEL, including the 20-segment progress bar
- [ ] Skeleton shimmer runs while visible and **stops when off-screen**
- [ ] Pomodoro banner digits animate cleanly against a live timer
- [ ] **Reduce Motion honoured by all fourteen** — animations shorten or disappear, not just speed up
- [ ] Chat thinking indicator stops when the response arrives
- [ ] `TDMascotDialog` renders at both call sites
- [ ] No animation leaks: navigating away and back 20 times does not degrade frame rate

## 8. Pitfalls

- **Do not tune thresholds per platform.** If a swipe threshold feels wrong on iOS, change it in shared code with a comment explaining why. Two thresholds become two behaviours.
- **Reduce Motion means less motion, not faster motion.** Snap or cross-fade; do not scale the duration.
- **Infinite animations must stop when off-screen.** Shimmer and the thinking indicator are the two that will not, if the `LaunchedEffect` key is wrong.
- **Long-press timing differs.** iOS users expect a slightly different feel; test with real fingers, not a mouse in the simulator.
- **`TDPomodoroBanner` animates per digit.** A naive recomposition animates the whole string and looks wrong.
- **`TDMascotDialog` was recently extracted from two copies.** Verify both, or one silently regresses.
- **Frame drops on iOS often come from over-recomposition, not from animation.** Profile before assuming the animation is the problem.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# Live, on both platforms, all three kits
#   Home: skeleton on load, task cards, swipe an overlay notification
#   Groups: drag to reorder, summary card
#   Pomodoro: banner digits against a live timer
#   Chat: thinking indicator appears and stops
#   Activity: statistic card count-up, hearts depleted dialog
#   PIXEL: confirm stepping is visible
#   enable Reduce Motion → check all fourteen
#   navigate away and back 20 times → frame rate stable
```
