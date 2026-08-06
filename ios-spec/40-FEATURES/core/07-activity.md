---
id: 40-core-07
title: Activity (heatmap & hearts)
layer: ui
status: TODO
depends_on: [40-core-01, 50-02]
blocks: [60-01]
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/activity/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The contribution heatmap, year strip, statistics and the health-points hearts bar — a bottom-bar tab.

## 2. Why this way

**The hearts are a data-correctness surface, not a visual one.** Twelve hearts tracked in half-heart units: +1 per ended day with ≥1 completion, −1 per idle day, clamped. Only *fully ended* days fold into the stored checkpoint; today is applied live on top; a null `lastSettledEpochDay` means "first run — start full." A rounding or boundary error here shows as a wrong heart count, which users read as lost progress.

**This screen also feeds `60-01`'s Health widget**, which must show exactly the same count. A mismatch between app and widget is the kind of bug that erodes trust in the number.

**Note the name.** `ui/activity/` is the contribution/heatmap feature — not the Android `Activity` class.

## 3. Source

| Path | LOC |
|---|---|
| `ui/activity/` (5 files) | 1,229 |
| `domain/usecase/ComputeHealthPointsUseCase` + `HealthPointsCalculator` | the pure maths |
| `common/HeartsFormat.kt` | formatting |
| `uikit/…/TDActivityHeatmap.kt` (407), `TDHealthBar.kt` (176) | the drawing |
| `uikit/…/HeatmapBucket.kt` (14) | bucketing |
| `app/src/test/…/HealthPointsCalculatorTest.kt` | the guard |
| `docs/screenshots/activity/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/activity/` — verification.

## 5. Steps

1. Verify all 5 files compile in `commonMain`.
2. Verify the heatmap: weekday gutter, month labels, legend, correct buckets.
3. **Verify heart counts against `HealthPointsCalculator`** across a day boundary: complete a task today, cross midnight, confirm the checkpoint settles correctly.
4. Verify the first-run case — null `lastSettledEpochDay` starts full.
5. Verify the year strip and its `when (TDTheme.palette)` branch.
6. Verify the statistics cards.
7. Verify tapping a heatmap cell drills into `filteredtasks`.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 5 files compile in `commonMain`
- [ ] Heatmap renders correctly at 344/360/411dp and on iPad
- [ ] **Heart count matches `HealthPointsCalculator` exactly**, including half-hearts
- [ ] Day-boundary behaviour correct: today live, ended days settled
- [ ] First run starts full
- [ ] Year strip renders in all three kits
- [ ] Statistics correct
- [ ] Heatmap cell drills into filtered tasks
- [ ] `HealthPointsCalculatorTest` passes
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, sparse and dense years

## 8. Pitfalls

- **Half-heart rounding is a correctness issue.** It represents real user progress.
- **Only fully-ended days settle.** Folding today into the checkpoint corrupts the stored value permanently.
- **Null `lastSettledEpochDay` means start full**, not start empty.
- **The widget must match.** `60-01` reads the same number; a divergence is a trust problem.
- **Heatmap layout is grid maths.** Check the gutter and month labels at the narrowest width.
- **This is not the Android `Activity` class.**

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*HealthPoints*'
# Both platforms: complete a task, check hearts; cross midnight with the device clock;
# fresh install starts full; tap a heatmap cell; 3 kits, EN + TR
```
