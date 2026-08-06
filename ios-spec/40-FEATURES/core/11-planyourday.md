---
id: 40-core-11
title: Plan your day
layer: ui
status: TODO
depends_on: [40-core-01, 30-01]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/planyourday/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The daily-plan nudge: pick a time, get a reminder to plan the day.

## 2. Why this way

**Small screen, but it owns one of the 64 iOS notification slots permanently.** The daily-plan reminder is the archetypal repeating trigger — a fixed time every day, expressible as a single `UNCalendarNotificationTrigger(repeats: true)` that costs one slot forever. `30-01`'s budget explicitly reserves it.

**It also has a `DayMode` gate.** A recorded lesson: some UI is hidden between 12:00 and 18:00 by `DayModeCalculator`. Anyone testing at midday will conclude the feature is broken.

## 3. Source

| Path | LOC |
|---|---|
| `ui/planyourday/` (3 files) | 359 |
| `domain/alarm/BuildDailyPlanAlarmItem.kt` | the alarm item |
| `domain/repository/DailyPlanPreferences.kt` | persistence |
| `common/DayModeCalculator.kt` | the visibility gate |
| `uikit/…/TDPlanTimePickerField.kt`, `TDOverlayDailyPlanNotificationCard.kt` | the components |

## 4. Target

`shared/ui/commonMain/…/ui/planyourday/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the time picker and persistence.
3. Verify the reminder is scheduled as a **repeating** trigger on iOS, costing one slot.
4. Verify it fires at the chosen time on consecutive days without opening the app.
5. Verify disabling it cancels the trigger.
6. **Verify `DayMode` gating** — check outside 12:00–18:00 before concluding anything is broken.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Time picker works and the choice persists
- [ ] iOS schedules it as a repeating trigger using **one** slot
- [ ] Fires on consecutive days with the app never opened
- [ ] Disabling cancels it
- [ ] `DayMode` gating behaves as on Android
- [ ] Three kits, two themes, two languages
- [ ] Previews cover enabled and disabled

## 8. Pitfalls

- **`DayModeCalculator` hides UI between 12:00 and 18:00.** Test outside that window.
- **This must be a repeating trigger, not a materialized one.** Materializing it wastes window slots for no reason.
- **It is reserved in the budget.** Do not let it be dropped by the precedence rules.
- **Disabling must remove the pending request**, not just the preference.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: set a time, confirm it fires for 3 days without opening the app;
# on iOS confirm it is one repeating pending request; disable and confirm removal
```
