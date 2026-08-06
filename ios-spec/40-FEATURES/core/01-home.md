---
id: 40-core-01
title: Home
layer: ui
status: TODO
depends_on: [40-auth-02, 30-01, 50-02, 50-03]
blocks: [40-core-02, 40-core-04]
parallel_safe: false
estimate: 24h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/home/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The main task list — 14 files, 3,510 LOC, the screen users spend most of their time in.

## 2. Why this way

**Home is where six subsystems meet:** the task list, filters and section tabs, the FAB menu, skeleton loading, the pending-photo row, permission prompts and the secret-photo banner. It is also the primary sync trigger and the primary reminder-scheduling trigger.

**Split the verification, do not do it in one pass.** `HomeViewModel` is ~1,110 lines and `HomeContent` ~1,090 — both already over the file-size budget, and `CLAUDE.md` is explicit that oversized files are debt, not precedent. Extract while you are in here.

**Three specific traps live on this screen**, all recorded:

1. **`UiState.Success` clobbering.** The photo-picker RESUMED→STARTED→RESUMED cycle triggers `loadData()`, which wipes an open sheet unless UI-owned fields are copied from the previous `Success`.
2. **`Modifier.weight(0f)` crashes.** Section tabs must gate on `count > 0`.
3. **Optimistic feedback.** Confetti and haptics must be driven off a state transition via `LaunchedEffect(state)`, never fired from the click handler — otherwise a ViewModel that blocks the action still plays the celebration.

## 3. Source

| Path | LOC |
|---|---|
| `ui/home/` (14 files) | 3,510 |
| `ui/home/HomeViewModel.kt` | ~1,110 — over budget |
| `ui/home/HomeContent.kt` | ~1,090 — over budget |
| `ui/home/HomeTaskList.kt` | the stripe, `when (TDTheme.palette)` |
| `data/repository/TaskSyncRepositoryImpl.kt` | pull-to-refresh → `resetCooldown()` + `fetchTasks(force = true)` |
| `uikit/…/TDTaskCardWithCheckbox.kt` (529) | confetti |
| `docs/screenshots/home/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/home/` — verification plus extraction of the two oversized files.

## 5. Steps

1. Verify all 14 files compile in `commonMain`.
2. Verify every `UiState` branch: loading skeleton, empty, populated, error.
3. Verify filters, section tabs and the FAB menu.
4. Verify pull-to-refresh triggers a forced fetch with the cooldown reset.
5. **Verify sheet state survives a photo-picker round trip.**
6. Verify completion: confetti plays, hearts update, a recurring task completes **for that day only**.
7. Verify reminder rescheduling fires after every mutation (`30-01`).
8. Verify drag-to-reorder on iOS.
9. Verify the permission prompt row and the secret-photo banner.
10. Extract at least the two oversized files into same-package components.
11. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 14 files compile in `commonMain`
- [ ] Every `UiState` branch renders correctly on both platforms
- [ ] Pull-to-refresh forces a fetch and resets the cooldown
- [ ] **Sheet state survives the photo-picker cycle**
- [ ] Confetti fires from a state transition, not the click handler
- [ ] Recurring completion is per-day
- [ ] Reminders reschedule after mutations
- [ ] Drag-to-reorder works on iOS
- [ ] No `weight(0f)` crash with zero-count sections
- [ ] `HomeViewModel` and `HomeContent` are smaller than before
- [ ] Guest mode: tasks create locally with no account
- [ ] Three kits, two themes, two languages
- [ ] Previews cover loading, empty, populated and error

## 8. Pitfalls

- **Do not overwrite `Success` wholesale on refresh.** Copy UI-owned fields (open sheet, form state, pending dialog ids) from the previous state.
- **`Modifier.weight(0f)` crashes.** Gate on `count > 0`.
- **Do not fire confetti optimistically.** `LaunchedEffect(state)`.
- **Recurring completion is per-day**, in `task_daily_completions`, not a boolean.
- **Do not grow the oversized files further.** Extract.
- **Personal task times are minutes, group times are seconds.** Home shows both.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: every state, filters, tabs, FAB, pull-to-refresh, complete a task,
# open the photo picker and return (sheet survives), reorder, guest mode, 3 kits, EN + TR
```
