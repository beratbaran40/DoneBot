---
id: 40-groups-07
title: Manage members
layer: ui
status: TODO
depends_on: [40-groups-02]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/managemembers/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Role management and member removal.

## 2. Why this way

**Removal is destructive and admin-only**, so the two established conventions apply: destructive actions at the bottom, and a confirmation the user must acknowledge. The removed member also needs their local group rows cleared on their own device — which happens via push and sync, not locally here.

**Admin gating must be enforced in the UI as well as the server.** A non-admin should not see actions they cannot perform.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/managemembers/` (3 files) | — |
| `DELETE family-groups/members/{groupId}/{userId}` | removal |
| `ui/groups/groupdetail/GroupDetailMembersTab.kt` | `MemberAvatar` |
| `navigation/Screen.kt` | `ManageMembers(groupId)` |

## 4. Target

`shared/ui/commonMain/…/ui/groups/managemembers/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the member list with avatars and roles.
3. Verify removal with confirmation, and that the list refreshes with `force = true`.
4. Verify a non-admin sees no admin actions.
5. Verify an owner cannot remove themselves — transfer ownership first.
6. Verify the removed member's device reflects it after sync.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Member list renders with avatars and roles
- [ ] Removal works with confirmation; list refreshes
- [ ] Non-admins see no admin actions
- [ ] Owner cannot remove themselves
- [ ] Removed member's device updates after sync
- [ ] Three kits, two themes, two languages
- [ ] Previews cover admin and non-admin views

## 8. Pitfalls

- **Enforce admin gating in the UI too**, not only server-side.
- **An owner removing themselves would orphan the group.** Block it; point at transfer ownership.
- **`force = true`** after removal.
- **Destructive action at the bottom**, with confirmation.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: remove a member as admin; open as non-admin (no actions);
# try to remove yourself as owner (blocked)
```
