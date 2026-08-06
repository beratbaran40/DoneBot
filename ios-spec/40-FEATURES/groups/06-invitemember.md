---
id: 40-groups-06
title: Invite member
layer: ui
status: TODO
depends_on: [40-groups-02]
blocks: []
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/invitemember/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Invite someone to a group by email.

## 2. Why this way

A small screen with one privacy property worth preserving: the response must not reveal whether the email belongs to an existing DoneBot account. The same account-enumeration reasoning as forgot-password.

It also triggers an `invitation_received` push to the invitee, which makes it half of the end-to-end test for `40-core-10`.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/invitemember/` (3 files) | — |
| `POST family-groups/members` | `InviteMemberRequest(groupId, email)` → `Unit?` |
| `data/source/remote/fcm/PushPayload.kt` | `invitation_received` |

## 4. Target

`shared/ui/commonMain/…/ui/groups/invitemember/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify an invitation sends and the invitee receives the push.
3. Verify the response does not reveal account existence.
4. Verify `handleEmptyRequest` (`data: null`).
5. Verify email validation and duplicate-invite handling.
6. Verify the pending-invites list refreshes with `force = true`.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Invitation sends; invitee receives the push
- [ ] Response does not reveal whether the account exists
- [ ] `data: null` handled as success
- [ ] Validation and duplicate-invite errors localized
- [ ] Pending invites refresh
- [ ] Three kits, two themes, two languages
- [ ] Previews cover idle, loading, sent, error

## 8. Pitfalls

- **Do not reveal account existence.**
- **`handleEmptyRequest`** — this returns `data: null`.
- **`force = true`** to refresh pending invites.
- **Turkish email input** — the keyboard should be an email keyboard in both languages.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: invite, confirm the push arrives; invite a non-existent address
# (same message); invite the same person twice
```
