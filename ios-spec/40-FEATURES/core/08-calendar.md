---
id: 40-core-08
title: Calendar
layer: ui
status: TODO
depends_on: [40-core-01, 30-14, 50-02]
blocks: []
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/calendar/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The month calendar with task markers and overdue indicators — a bottom-bar tab.

## 2. Why this way

**Calendar is the densest date-logic screen in the app**, which makes it the best functional test of the `kotlinx-datetime` migration and the `PlatformFormatting` contract at once. `YearMonth`, week maths, Monday-first grid, localized month and weekday names, per-day completion state — all of it converges here.

**Two things to check specifically.** First, the localized names: this is where a Sunday-first/Monday-first indexing error in `30-14` shows up as visibly wrong labels. Second, `CalendarViewModel` **starts in `Success`, not `Loading`** — a recorded quirk. It uses `isRefreshing` instead, so anyone adding a `Loading` branch will find it never renders.

## 3. Source

| Path | LOC |
|---|---|
| `ui/calendar/` (4 files) | 1,217 |
| `ui/calendar/CalendarViewModel.kt` (~76, ~125) | forced fetch on open and on date change; starts in `Success` |
| `uikit/…/components/CalendarGrid.kt` | Monday-first 6×7 grid |
| `uikit/…/components/TDDatePicker.kt` (614) | range selection, task dots |
| `app/src/test/…/CalendarGridTest.kt` | the guard |
| `docs/screenshots/calendar/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/calendar/` — verification.

## 5. Steps

1. Verify all 4 files compile in `commonMain`.
2. **Verify the grid starts on Monday and the label says Monday** — the `30-14` indexing check.
3. Verify localized month and weekday names in EN and TR on both platforms.
4. Verify task markers and overdue indicators.
5. Verify month navigation across year boundaries.
6. Verify a date change triggers a forced fetch.
7. Verify per-day completion state for recurring tasks.
8. Verify `isRefreshing` rather than a `Loading` branch.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 4 files compile in `commonMain`
- [ ] **Grid is Monday-first and correctly labelled in both languages**
- [ ] Month and weekday names localized correctly on both platforms
- [ ] Task markers and overdue indicators correct
- [ ] Month navigation works across year boundaries
- [ ] Date change forces a fetch
- [ ] Per-day completion correct for recurring tasks
- [ ] `CalendarGridTest` passes
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty month, populated month and overdue

## 8. Pitfalls

- **Sunday-first vs Monday-first** is the most likely defect, and it reads as a translation bug.
- **`CalendarViewModel` starts in `Success`.** A `Loading` branch added here never renders.
- **`YearMonth` needs `kotlinx-datetime` 0.7+.**
- **Recurring completion is per-day.** A month view showing a boolean is wrong for routines.
- **Do not localize by reading the system locale.** Use the in-app override.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*CalendarGrid*'
# Both platforms, EN + TR: first column is Monday and says so; navigate across a year
# boundary; complete a recurring task and check only that day is marked
```
