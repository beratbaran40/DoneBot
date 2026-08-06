---
id: 40-shared-01
title: Shared task form
layer: ui
status: TODO
depends_on: [20-11, 30-01, 50-04]
blocks: [40-core-02, 40-core-04]
parallel_safe: false
estimate: 12h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/common/taskform/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The shared task-form sections used by Creation Hub, task detail and the group add-task sheet.

## 2. Why this way

**This is the highest-leverage shared UI in the app** — three screens compose it, so a defect here is a defect in all three, and a fix is a fix in all three.

**It owns the recurrence editor**, which is where the most subtle domain rule lives: `Recurrence` models UNTIL and **deliberately never COUNT**. "20 sessions" is resolved to a concrete end date at creation time, so `firesOn` stays a pure `(anchor, day) -> Boolean`. A form that stores a count instead of resolving it breaks both platforms' schedulers.

**It also owns the reminder editor**, where `slot` stability matters: slots seed alarm request codes, so deriving them from a list index orphans an armed alarm on every mid-list delete.

**And the priority selector**, which re-encodes HIGH/MEDIUM/LOW through gray-ink intensity and labels rather than hue in some kits — an exhaustive `when (TDTheme.palette)`.

## 3. Source

| Path | LOC |
|---|---|
| `ui/common/taskform/` | part of `ui/common`'s 3,478 |
| `ui/common/PrioritySelector.kt` | exhaustive palette `when` |
| `ui/common/SubtaskChecklist`, `AssigneeSelector`, `SecretCheckbox`, `OverdueBanner` | the sections |
| `domain/model/Recurrence.kt` | UNTIL not COUNT |
| `data/model/entity/TaskReminderEntity.kt` | the `slot` column |
| `uikit/…/TDRecurrencePicker.kt`, `TDReminderOffsetPicker.kt`, `TDCategoryPicker.kt` | the components |

## 4. Target

`shared/ui/commonMain/…/ui/common/taskform/` — verification.

## 5. Steps

1. Verify the form files compile in `commonMain`.
2. Verify every section renders in all three consuming screens.
3. **Verify recurrence resolves a count to a concrete end date**, never storing a count.
4. Verify `until` editing.
5. Verify reminders add/edit/delete with **stable slots**, and that each change reschedules.
6. Verify the priority selector's palette branch is exhaustive and legible in all three kits.
7. Verify the subtask checklist, assignee selector, secret checkbox and overdue banner.
8. Verify minutes vs seconds depending on which screen is composing it.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Form files compile in `commonMain`
- [ ] Every section works in all three consuming screens
- [ ] **Recurrence resolves counts to end dates; no COUNT is stored**
- [ ] `until` editing works
- [ ] Reminder slots stay stable across edits; each change reschedules
- [ ] Priority selector legible in all three kits; `when` exhaustive
- [ ] All other sections work
- [ ] Minutes vs seconds correct per consuming screen
- [ ] Three kits, two themes, two languages
- [ ] Previews cover each section and each task type

## 8. Pitfalls

- **Never store a COUNT.** Resolve it to an end date at creation, or `firesOn` stops being pure and both schedulers diverge.
- **`slot` must not come from a list index.** It seeds the alarm request code.
- **Priority is encoded by intensity and label, not hue, in some kits.** Do not "fix" it to use colour.
- **Minutes vs seconds** — this form feeds both personal and group tasks.
- **A defect here is a defect in three screens.** Verify in all three, not one.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms, all three consuming screens: create a "20 sessions" routine and
# confirm an end date was stored; add 3 reminders, delete the middle one, confirm
# the others still fire; check priority legibility in all three kits
```
