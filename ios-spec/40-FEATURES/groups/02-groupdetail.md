---
id: 40-groups-02
title: Group detail (3 tabs)
layer: ui
status: TODO
depends_on: [40-groups-01]
blocks: [40-groups-05, 40-groups-07, 40-groups-08]
parallel_safe: false
estimate: 14h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/groupdetail/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The three-tab group screen — Overview, Members, Activity — plus the add-task sheet, first-invite dialog, pending invites and overview filters. 11 files, the largest single sub-screen in the groups tree.

## 2. Why this way

**Every group mutation in the app passes through here or lands here**, which makes it the screen where the TTL-cache trap bites hardest: adding a task, inviting a member, or changing a role must all reload with `force = true` or the UI shows stale data with no error.

**Member avatars** use `MemberAvatar(initials, size, avatarUrl, avatarVersion)`; the version is a persisted cache-bust token, not a per-emission value — a recorded lesson. If avatars stop updating after a change, that is why.

**Group task times are seconds**, personal task times are minutes. This screen shows group tasks.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/groupdetail/` (11 files) | part of 9,380 |
| `ui/groups/groupdetail/GroupDetailMembersTab.kt` | `MemberAvatar` |
| `GET family-groups/{id}`, `…/activity`, `…/tasks` | the endpoints |
| `navigation/Screen.kt` | `GroupDetail(groupId, groupName, initialTab, showFirstInvite)` |
| `docs/screenshots/groups/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/groups/groupdetail/` — verification.

## 5. Steps

1. Verify all 11 files compile in `commonMain`.
2. Verify all three tabs load and switch.
3. Verify `initialTab` and `showFirstInvite` route arguments.
4. Verify the add-task sheet creates a group task **in seconds**.
5. Verify every mutation reloads with `force = true`.
6. Verify member avatars load and update after a change (`avatarVersion`).
7. Verify the activity feed.
8. Verify overview filters and pending invites.
9. Verify reporting (`POST family-groups/{id}/reports`) is reachable — a review requirement.
10. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 11 files compile in `commonMain`
- [ ] Three tabs load and switch; `initialTab` honoured
- [ ] `showFirstInvite` shows the dialog once
- [ ] Add-task creates a group task with **seconds**
- [ ] Every mutation reloads with `force = true`
- [ ] Member avatars load and refresh after an avatar change
- [ ] Activity feed renders
- [ ] Overview filters and pending invites work
- [ ] Reporting is reachable
- [ ] Three kits, two themes, two languages
- [ ] Previews cover each tab, empty and populated

## 8. Pitfalls

- **`force = true` after every mutation.** The TTL caches are the most common stale-UI cause in this feature.
- **`avatarVersion` is persisted, not per-emission.** Otherwise avatars never refresh.
- **Seconds, not minutes**, for group task times.
- **Reporting must be reachable.** Guideline 1.2 for user-generated content.
- **Do not clobber `Success`** — this screen has sheet state.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: add a task, invite, change a role — each reflects immediately;
# change a member avatar and confirm it refreshes; report content
```
