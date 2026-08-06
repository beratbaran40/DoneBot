---
id: 40-core-09
title: Filtered tasks
layer: ui
status: TODO
depends_on: [40-core-07]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/filteredtasks/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The drill-down list reached from a chart or heatmap cell, filtered by `(isCompleted, weekDateEpochDay)`.

## 2. Why this way

A focused screen whose route carries two typed arguments — which makes it a good check that `@Serializable` type-safe navigation survived the CMP move. Both arguments are primitives, so `toRoute` should work verbatim.

**Secret-mode gating applies here** too, as it does on home, calendar and search.

## 3. Source

| Path | LOC |
|---|---|
| `ui/filteredtasks/` (4 files) | 1,043 |
| `navigation/Screen.kt` | `FilteredTasks(isCompleted, weekDateEpochDay)` |
| `ui/activity/`, chart drill-downs | the entry points |
| `docs/screenshots/filteredtasks/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/filteredtasks/` — verification.

## 5. Steps

1. Verify all 4 files compile in `commonMain`.
2. Verify navigation from a heatmap cell and from each chart passes both arguments correctly.
3. Verify the filter applies — completed vs pending, correct day.
4. Verify secret tasks stay hidden outside secret mode.
5. Verify the empty state.
6. Verify `TDTopBar` with back and title.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 4 files compile in `commonMain`
- [ ] Both route arguments arrive correctly from every entry point
- [ ] The filter applies correctly
- [ ] Secret tasks hidden outside secret mode
- [ ] Empty state renders
- [ ] `TDTopBar` correct
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, completed-list and pending-list

## 8. Pitfalls

- **Epoch-day arguments are timezone-sensitive.** An off-by-one here shows the wrong day's tasks.
- **`Screen.FilteredTasks` is R8-name-sensitive.** Do not rename.
- **Secret filtering must apply** — this is a task list like any other.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: tap a heatmap cell and each chart bar; verify the day and
# completion filter match what was tapped; secret mode on and off
```
