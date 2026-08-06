---
id: 50-02
title: Components tier A — Canvas-heavy
layer: design
status: TODO
depends_on: [50-00]
blocks: [40-core-07, 40-core-08]
parallel_safe: true
estimate: 16h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/components/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Verify the eight Canvas-drawn components render pixel-correctly on iOS. **They port unchanged** — Compose `Canvas` is Skia on both platforms, and iOS is Skia natively. This is a rendering-verification task.

## 2. Why this way

These eight are the components that would be genuinely expensive in a SwiftUI rewrite — custom `Path` work, particle systems, grid layout maths. They are also the ones that come free with Compose Multiplatform, which is a large part of why CMP was chosen.

**What can still go wrong is not the drawing but its inputs:** text measurement inside a Canvas, density rounding, and gesture handling on a custom layout. Those are worth checking specifically.

| Component | LOC | Canvas sites | Watch for |
|---|---|---|---|
| `TDDatePicker` | 614 | 3 | Range selection, task dots, overdue marking, `TDAnimatedCell` |
| `TDTaskCardWithCheckbox` | 529 | 2 | **Confetti particle system**, custom checkbox |
| `TDActivityHeatmap` | 407 | grid maths | Weekday gutter, month labels, legend alignment |
| `TDWeeklyBarChart` | 403 | 4 | `barShape()` flattens caps for the PIXEL kit |
| `TDWheelTimePicker` | 325 | — | LazyList snapping + custom `LazyListState` extensions |
| `TDWeeklyProgressCircularIndicator` | 231 | 2 | Arc sweep, legend |
| `TDMonthlyBarChart` | 188 | — | Same cap trick, animated |
| `TDHealthBar` | 176 | 3 | Custom `Heart`/`HeartLayer`, half-heart units |

Four pure helpers sit alongside them and are the real testable surface: `CalendarGrid.kt` (34), `DayTapOutcome.kt` (120 — the whole range-picker tap grammar as a pure function), `RangeBand.kt` (47), `HeatmapBucket.kt` (14). `CalendarGridTest` and `DayTapOutcomeTest` already guard them.

## 3. Source — read before writing

All under `uikit/src/commonMain/kotlin/com/todoapp/uikit/components/`. Also:

| Path | Why |
|---|---|
| `CalendarGrid.kt`, `DayTapOutcome.kt`, `RangeBand.kt`, `HeatmapBucket.kt` | Pure logic; the tests that guard these components |
| `app/src/test/…/CalendarGridTest.kt`, `DayTapOutcomeTest.kt` | Must pass unchanged |
| `docs/screenshots/{activity,calendar,home}/` | Visual references |
| `uikit/…/theme/Style.kt` | `barShape()` reads `elevationStyle`/`shapes` for the PIXEL cap flattening |

## 4. Target

No new files — verification after `20-10`.

## 5. Steps

1. **Confirm all eight compile in `commonMain`.** None should need a platform escape.

2. **Screenshot each against its `docs/screenshots/` reference** on both platforms, light and dark, in all three kits where the kit changes the rendering (`TDWeeklyBarChart` and `TDMonthlyBarChart` flatten caps in PIXEL).

3. **Check text inside Canvas.** `TDActivityHeatmap`'s month labels and weekday gutter, and the chart axis labels, use `drawText` with measured text. Compose's iOS text measurement can differ by a fraction; if labels clip or misalign, that is the cause.

4. **Check the confetti.** `TDTaskCardWithCheckbox` runs a particle system on completion. Verify it plays at 60fps and does not leak — completing 50 tasks in a row must not degrade.

5. **Check `TDWheelTimePicker` gestures.** Snapping uses custom `LazyListState` extensions and fling behaviour. iOS scroll physics differ; verify the wheel settles on a value rather than drifting.

6. **Check `TDHealthBar` half-hearts.** Hearts are tracked in half units; a rounding difference shows as a wrong heart count, which is a data-correctness bug in appearance.

7. **Check `TDDatePicker` range selection** against `DayTapOutcomeTest`'s grammar — start, extend, reset, same-day.

8. **Run the pure-helper tests.** They are the fastest signal.

## 6. Code skeleton

```kotlin
// The PIXEL cap flattening — a kit-dependent Canvas path. Verify in all three kits,
// not just the default.
private fun DrawScope.barShape(rect: Rect, style: TDStyle): Path =
    if (style.elevationStyle == ElevationStyle.HARD) {
        Path().apply { addRect(rect) }             // flat caps for the pixel kit
    } else {
        Path().apply { addRoundRect(RoundRect(rect, topLeft = radius, topRight = radius)) }
    }
```

## 7. Acceptance

- [ ] All eight compile in `commonMain` with no platform escape
- [ ] Each matches its `docs/screenshots/` reference on both platforms, light and dark
- [ ] Bar charts flatten caps correctly in PIXEL on both platforms
- [ ] Canvas text (heatmap labels, chart axes) is correctly positioned and not clipped on iOS
- [ ] Confetti plays smoothly; 50 consecutive completions show no degradation or leak
- [ ] `TDWheelTimePicker` snaps cleanly on iOS — no drift, no overshoot
- [ ] `TDHealthBar` renders correct half-heart counts
- [ ] `TDDatePicker` range selection follows the `DayTapOutcome` grammar
- [ ] `CalendarGridTest` and `DayTapOutcomeTest` pass unchanged
- [ ] Heatmap grid aligns with its weekday gutter and month labels at 344/360/411dp widths

## 8. Pitfalls

- **Do not rewrite any of these.** Compose Canvas is Skia on both platforms. Rewriting would be the largest wasted effort in the project.
- **Text measurement inside Canvas is the real risk**, not the shapes. Check labels first when something looks off.
- **Confetti is a particle system.** Verify it stops and releases; a leaked animation loop drains battery invisibly.
- **iOS scroll physics differ.** The wheel picker's custom snapping is the one gesture surface most likely to feel wrong.
- **Half-heart rounding is a correctness issue**, not a visual one — it represents real user data.
- **Do not change `barShape()`'s kit branch.** The flat caps are the PIXEL identity.
- **Density rounding.** Canvas maths at fractional densities can produce a 1px seam. Check at 2x and 3x.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*CalendarGrid*' --tests '*DayTapOutcome*'

# Visual, both platforms, light + dark, 3 kits
#   Activity screen  → heatmap, health bar, weekly + monthly charts
#   Calendar screen  → date picker, range selection
#   Home             → task card, complete a task → confetti
#   Add timer        → wheel time picker, verify snapping
#   compare each against docs/screenshots/
```
