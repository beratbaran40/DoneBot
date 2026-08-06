---
id: 40-core-04
title: Task detail & edit
layer: ui
status: TODO
depends_on: [40-core-01, 40-shared-01, 30-01, 30-08]
blocks: []
parallel_safe: true
estimate: 14h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/details/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Task detail and editing — subtasks, reminders, recurrence, photos, location.

## 2. Why this way

**This screen writes more fields than any other**, which makes it the practical test of the sync contract. Two rules from `TaskRepositoryImpl` matter here specifically:

1. **Editing a `SYNCED` row must flip it to `PENDING_UPDATE`**, or the next reconciliation silently overwrites the edit.
2. **`comparableFields()` is an explicit 21-field list.** A field this screen can edit but that is missing from that list will silently lose off-device edits — which already happened once, for the four location columns.

**Reminder edits are the other risk.** The `slot` column must stay stable; deriving it from a list index orphans an armed alarm on every mid-list delete. And every reminder change must trigger a reschedule (`30-01`).

## 3. Source

| Path | LOC |
|---|---|
| `ui/details/` (7 files) | 1,961 |
| `data/repository/TaskRepositoryImpl.kt` (~1187-1209) | `comparableFields()` — the 21 fields |
| `data/model/entity/TaskReminderEntity.kt` | the `slot` column |
| `ui/common/taskform/` → `40-shared-01` | shared sections |
| `docs/screenshots/details/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/details/` — verification.

## 5. Steps

1. Verify all 7 files compile in `commonMain`.
2. Verify every editable field round-trips through sync — edit on iOS, confirm on Android.
3. **Verify each edited field is in `comparableFields()`.** Anything missing loses off-device edits.
4. Verify a `SYNCED` row flips to `PENDING_UPDATE` on edit.
5. Verify subtask add/edit/delete/reorder.
6. Verify reminder add/edit/delete keeps slots stable and reschedules.
7. Verify recurrence editing, including `until`.
8. Verify photo attach, view and delete (`30-08`), including the offline queue.
9. Verify location set and clear.
10. Verify delete cancels alarms.
11. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 7 files compile in `commonMain`
- [ ] Every editable field survives a round trip through sync
- [ ] **Every editable field is present in `comparableFields()`**
- [ ] Editing a `SYNCED` row sets `PENDING_UPDATE`
- [ ] Subtask operations work, including reorder
- [ ] Reminder edits keep `slot` stable and trigger a reschedule
- [ ] Recurrence editing works, `until` included
- [ ] Photos attach, display and delete; offline queue works
- [ ] Location sets and clears
- [ ] Delete cancels armed alarms
- [ ] Three kits, two themes, two languages
- [ ] Previews cover loading, populated, editing and error

## 8. Pitfalls

- **A field missing from `comparableFields()` silently loses off-device edits.** This has happened.
- **A `SYNCED` row that stays `SYNCED` after an edit gets overwritten** on the next reconciliation.
- **`slot` must not come from a list index.** It seeds the alarm request code.
- **Deleting a task must cancel its alarms**, or an armed recurring alarm re-arms itself forever.
- **Minutes vs seconds** depending on task origin.
- **Do not clobber `Success` on refresh** — this screen has form state.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Cross-platform: edit every field on iOS, verify on Android and vice versa;
# add 3 reminders, delete the middle one, confirm the others still fire;
# delete a task with an armed alarm, confirm it does not fire
```
