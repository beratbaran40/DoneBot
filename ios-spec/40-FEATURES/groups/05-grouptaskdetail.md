---
id: 40-groups-05
title: Group task detail
layer: ui
status: TODO
depends_on: [40-groups-02, 30-08]
blocks: []
parallel_safe: true
estimate: 8h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/grouptaskdetail/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Group task detail with the edit sheet and photos.

## 2. Why this way

**Group tasks are a separate data path from personal tasks**, and the recorded rule is to plumb both. `GroupRepositoryImpl` is remote-first with TTL caches and no offline write queue, whereas personal tasks are offline-first with `syncStatus`. Assuming one path covers both is a recurring source of bugs here.

**Three encoding traps converge on this screen:**
- times are **seconds**, not minutes
- `GroupTaskUpdateRequest` is **partial**, with explicit `clearAssignee` / `clearLocation` flags — omitting a field means "leave unchanged", so clearing needs the flag
- alarms use the **group request-code range** (`0x4000_0000`), because server ids collide with local Room ids

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/grouptaskdetail/` (6 files) | — |
| `PUT family-groups/{groupId}/tasks/{taskId}` | `GroupTaskUpdateRequest` — partial, with clear flags |
| `data/alarm/AlarmRequestCodes.kt` | `GROUP_REQUEST_BASE` |
| `data/model/network/request/GroupTaskRequest.kt` | seconds |

## 4. Target

`shared/ui/commonMain/…/ui/groups/grouptaskdetail/` — verification.

## 5. Steps

1. Verify all 6 files compile in `commonMain`.
2. Verify detail loads and the edit sheet saves.
3. **Verify clearing an assignee and a location uses the clear flags** — not an omitted field.
4. Verify times round-trip as seconds.
5. Verify photo attach and delete.
6. Verify reminders use the group request-code namespace and reschedule.
7. Verify per-day completion for recurring group tasks.
8. Verify chat cannot write to this task (`group_task_blocked`).
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 6 files compile in `commonMain`
- [ ] Detail loads; edit sheet saves and refreshes with `force = true`
- [ ] **Clearing assignee and location works via the clear flags**
- [ ] Times round-trip as seconds
- [ ] Photos attach and delete
- [ ] Reminders use the group namespace and reschedule
- [ ] Per-day completion correct for recurring group tasks
- [ ] Chat writes are blocked
- [ ] Three kits, two themes, two languages
- [ ] Previews cover loading, populated, editing, error

## 8. Pitfalls

- **A partial update treats an omitted field as unchanged.** Clearing needs `clearAssignee` / `clearLocation`.
- **Seconds, not minutes.**
- **Group alarms need their own request-code range.** Server ids and Room ids both start at 1.
- **No offline write queue for groups.** A mutation with no network must fail visibly, not silently queue.
- **`force = true` after every write.**

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: edit a group task, clear the assignee, clear the location, set a
# reminder; verify on the other device; try to edit it via chat (blocked)
```
