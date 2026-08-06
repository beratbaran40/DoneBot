---
id: 40-groups-08
title: Member profile
layer: ui
status: TODO
depends_on: [40-groups-02, 40-settings-06]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/memberprofile/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

A group member's profile, with admin actions, reporting and blocking.

## 2. Why this way

**This screen carries the App Store user-generated-content requirements.** Guideline 1.2 expects a way to report objectionable content and to block a user. Both exist — `POST family-groups/{groupId}/reports` and client-local blocking via `BlockedUsersPreferences` — and both must be **reachable and obvious**, because a reviewer will look for them.

Blocking is deliberately client-local: the blocked user's content is hidden on this device without a server round trip.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/memberprofile/` (3 files) | — |
| `POST family-groups/{groupId}/reports` | `ReportContentRequest(targetType, targetUserId?, targetRef?, reason?)` |
| `domain/repository/BlockedUsersPreferences.kt`, `ui/blockedusers/` | client-local blocking |
| `navigation/Screen.kt` | `MemberProfile(groupId, userId, isCurrentUserAdmin)` |

## 4. Target

`shared/ui/commonMain/…/ui/groups/memberprofile/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the profile loads with avatar and stats.
3. **Verify reporting is reachable and sends.**
4. **Verify blocking is reachable** and hides the member's content locally.
5. Verify `isCurrentUserAdmin` gates the admin actions.
6. Verify unblocking from the blocked-users screen restores content.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Profile loads with avatar
- [ ] **Report is reachable, obvious and sends successfully**
- [ ] **Block is reachable** and hides content on this device
- [ ] Admin actions gated by `isCurrentUserAdmin`
- [ ] Unblock restores content
- [ ] Three kits, two themes, two languages
- [ ] Previews cover admin and non-admin views

## 8. Pitfalls

- **Report and block must be easy to find.** A reviewer looking for Guideline 1.2 compliance needs to reach them without hunting.
- **Blocking is client-local by design.** Do not add a server call.
- **`isCurrentUserAdmin` is a route argument**, so it can be stale if roles changed since navigation. Re-check against loaded data.
- **Reporting needs a reason.** Sending without one produces a useless report.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Two accounts: open a member profile, report with a reason, block them
# (their content disappears), unblock from settings (it returns)
```
