---
id: 40-groups-01
title: Groups root (+ two-pane)
layer: ui
status: TODO
depends_on: [40-core-01, 50-06]
blocks: [40-groups-02, 40-groups-03, 40-core-10]
parallel_safe: false
estimate: 12h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The groups list and the tablet/iPad two-pane layout — a bottom-bar tab and the entry to the largest feature subtree in the app (42 files, 9,380 LOC).

## 2. Why this way

**`GroupsTwoPane` is the worked example of adaptive layout** in this codebase, which makes it the reference for `50-06` on iPad. Selection state must survive a width change — on iPad the window can resize live in Split View with no configuration change.

**Two recorded traps live in the group data layer**, and both show first on this screen:

1. **TTL caches do not self-invalidate.** `GroupRepositoryImpl` caches groups, detail, activity and tasks in memory; mutations do not clear them. Every write path needs `force = true`.
2. **Duplicate groups.** A shipped fix uses a mutex plus a unique `remote_id` index (`MIGRATION_25_26` dedups existing rows). Rapid create or a retried request must not produce two.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/GroupScreen.kt`, `GroupsTwoPane.kt`, VM/Contract/PreviewProvider | root of 9,380 |
| `data/repository/GroupRepositoryImpl.kt` | TTL caches, the mutex |
| `uikit/…/TDGroupsSummary.kt` (455), `TDFamilyGroupCard.kt` | avatars, drag states |
| `navigation/Screen.kt` | `Groups(pendingDeleteGroupId)` |
| `docs/screenshots/groups/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/groups/` (root files) — verification.

## 5. Steps

1. Verify the root files compile in `commonMain`.
2. Verify the list loads and group avatars render with auth (`AsyncImage` + Bearer).
3. Verify creating a group refreshes with `force = true`.
4. **Verify rapid create does not duplicate.**
5. Verify two-pane on iPad: selection survives a Split View resize.
6. Verify the bottom bar on phone, the rail on iPad.
7. Verify `pendingDeleteGroupId` routing.
8. Verify empty state and guest behaviour.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Root files compile in `commonMain`
- [ ] Group list loads; avatars load with the Bearer token
- [ ] Create refreshes immediately (`force = true`)
- [ ] **Rapid create produces one group, not two**
- [ ] Two-pane works on iPad; **selection survives a live resize**
- [ ] Bottom bar on phone, rail on iPad
- [ ] `pendingDeleteGroupId` routes correctly
- [ ] Empty state renders; guest behaviour sensible
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, one group and several

## 8. Pitfalls

- **TTL caches do not self-invalidate.** `force = true` after every write.
- **Duplicate groups are a shipped bug class.** The mutex and unique index must both survive.
- **Do not cache the window size class** — iPad resizes live.
- **Group avatars need the auth `OkHttpClient`.** Coil is wired app-wide; verify it survived on iOS.
- **Group task times are seconds**, not minutes.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: create a group, tap create twice quickly (one group), avatars load;
# iPad: two-pane, drag the Split View divider while a group is selected
```
