---
id: 40-core-10
title: Invitations inbox
layer: ui
status: TODO
depends_on: [40-groups-01, 30-03]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/invitations/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Accept or decline group invitations.

## 2. Why this way

**This screen is a push destination**, reached from `invitation_received` — so it is one of the practical end-to-end tests of `30-03` plus `30-13`.

**Accepting mutates group state**, which means the TTL caches in `GroupRepositoryImpl` must be invalidated. That repository keeps in-memory caches that mutations do **not** invalidate on their own; a reload with `force = true` is required or the new group does not appear.

## 3. Source

| Path | LOC |
|---|---|
| `ui/invitations/` (4 files) | 637 |
| `GET family-groups/invitations/me`, `POST …/accept`, `POST …/decline` | the endpoints |
| `data/repository/GroupRepositoryImpl.kt` | the TTL caches |
| `MainViewModel` `DeepLink.Invitations` | the push route |

## 4. Target

`shared/ui/commonMain/…/ui/invitations/` — verification.

## 5. Steps

1. Verify all 4 files compile in `commonMain`.
2. Verify the list loads.
3. Verify accept: the group appears in Groups — **with `force = true`**.
4. Verify decline removes the invitation.
5. Verify the `invitation_received` push routes here.
6. Verify empty state.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 4 files compile in `commonMain`
- [ ] Invitation list loads
- [ ] **Accept makes the group appear immediately** (cache invalidated)
- [ ] Decline removes it
- [ ] `invitation_received` push routes here
- [ ] Empty state renders
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, one invitation and several

## 8. Pitfalls

- **`GroupRepositoryImpl` TTL caches do not self-invalidate.** Reload with `force = true` after accepting, or the group is invisible until the TTL expires.
- **Accept and decline are both `Unit`-returning**; use `handleEmptyRequest`.
- **The push may arrive while the user is already here.** `CurrentRouteTracker` suppression applies.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: invite from one, accept on iOS, confirm the group appears at once;
# decline another; tap the push notification from a killed app
```
