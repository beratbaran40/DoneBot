---
id: 40-groups-09
title: Transfer ownership
layer: ui
status: TODO
depends_on: [40-groups-04]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/transferownership/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Hand group ownership to another member.

## 2. Why this way

**This is irreversible from the current user's side** — after transferring, they cannot transfer it back. That makes it a destructive-class action even though nothing is deleted: bottom placement, clear confirmation, and unambiguous wording about what is being given up.

It also emits `group_ownership_transferred` to the new owner, which is one of the 10 push types and one of the deep-link destinations.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/transferownership/` (4 files) | — |
| `PUT family-groups/{groupId}/transfer-ownership` | `TransferOwnershipRequest(userId)` |
| `data/source/remote/fcm/PushPayload.kt` | `group_ownership_transferred` |

## 4. Target

`shared/ui/commonMain/…/ui/groups/transferownership/` — verification.

## 5. Steps

1. Verify all 4 files compile in `commonMain`.
2. Verify the member picker lists eligible members only.
3. Verify transfer works and both devices reflect the new roles after refresh.
4. Verify the new owner receives the push.
5. Verify the confirmation states clearly that the action cannot be undone.
6. Verify only the owner can reach this screen.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 4 files compile in `commonMain`
- [ ] Picker lists eligible members
- [ ] Transfer works; both devices show the new roles after `force = true`
- [ ] New owner receives `group_ownership_transferred`
- [ ] Confirmation clearly states irreversibility
- [ ] Owner-only access enforced
- [ ] Three kits, two themes, two languages
- [ ] Previews cover picker, confirm and success

## 8. Pitfalls

- **Irreversible from the user's side.** The confirmation must say so plainly.
- **Both devices need `force = true`** to see the new roles.
- **Owner-only.** Enforce in the UI as well as the server.
- **The push routes to the group.** Verify the deep link.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: transfer ownership; confirm roles update on both; confirm the push
# arrives and routes to the group; confirm the old owner can no longer transfer
```
