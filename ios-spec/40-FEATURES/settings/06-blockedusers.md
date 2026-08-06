---
id: 40-settings-06
title: Blocked users
layer: ui
status: TODO
depends_on: [40-settings-01]
blocks: [40-groups-08]
parallel_safe: true
estimate: 3h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/blockedusers/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Manage the local block list.

## 2. Why this way

**Blocking is deliberately client-local** — `BlockedUsersPreferences` in DataStore, no server round trip. That is a design choice: content from a blocked user is hidden on this device immediately, with no dependency on network or backend state.

**It is also App Store compliance surface.** Guideline 1.2 expects a blocking mechanism for user-generated content, and a reviewer will look for it. It must be reachable from Settings *and* from a member profile.

## 3. Source

| Path | LOC |
|---|---|
| `ui/blockedusers/` (3 files) | 228 |
| `domain/repository/BlockedUsersPreferences.kt` | DataStore-backed |
| `ui/groups/memberprofile/` | the other entry point |

## 4. Target

`shared/ui/commonMain/…/ui/blockedusers/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the list renders blocked users.
3. Verify unblocking restores their content immediately.
4. Verify the block list persists across relaunch.
5. Verify blocking hides content in groups, activity feeds and chat surfaces.
6. Verify the empty state.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] List renders blocked users
- [ ] Unblock restores content immediately
- [ ] Block list persists across relaunch
- [ ] Blocked content hidden everywhere it appears
- [ ] Empty state renders
- [ ] Reachable from Settings and from a member profile
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty and populated

## 8. Pitfalls

- **Do not add a server call.** Blocking is client-local by design.
- **Blocking must hide content everywhere**, not just in one list — group activity, task assignees, member lists.
- **Reachability is compliance surface.** Guideline 1.2.
- **The list survives logout?** Decide explicitly — it is device-local preference data, and `clearLocalSession` currently wipes preferences. Record the choice.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: block from a member profile, confirm their content disappears from
# every surface; unblock from Settings; relaunch (list persists)
```
