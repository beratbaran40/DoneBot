---
id: 40-auth-06
title: Change password
layer: ui
status: TODO
depends_on: [40-auth-07]
blocks: []
parallel_safe: true
estimate: 3h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/changepassword/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Authenticated password change from Settings.

## 2. Why this way

Unlike the reset flow this runs authenticated, so it exercises the token path — and it is worth checking what happens when the access token expires mid-form. The Ktor `Auth` plugin should refresh transparently (`20-06`); if it does not, this screen is where a user notices.

It is reached from Settings and uses `TDTopBar`, so it also confirms the top-bar chrome works on a non-tab screen.

## 3. Source

| Path | LOC |
|---|---|
| `ui/changepassword/` (4 files) | 353 |
| `PUT users/me/password` | `ChangePasswordRequest(currentPassword, newPassword)` → `Unit?` |
| `navigation/AppDestination.kt` | present in `topBarItems` |

## 4. Target

`shared/ui/commonMain/…/ui/changepassword/` — verification.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify the change succeeds and the new password works on next login.
3. Verify a wrong current password shows a specific, localized error.
4. Verify the `handleEmptyRequest` path (`data: null`).
5. Verify transparent token refresh if the token expires mid-form.
6. Verify `TDTopBar` renders with a back arrow and title.
7. Both languages.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] Password change works; the new password logs in
- [ ] Wrong current password → specific localized error, not generic
- [ ] `data: null` handled as success
- [ ] Expired token refreshes transparently — no logout
- [ ] `TDTopBar` correct
- [ ] Previews cover idle, loading, wrong-password and success

## 8. Pitfalls

- **Wrong current password needs a distinct message.** A generic failure is unhelpful for the one error users actually hit.
- **`handleEmptyRequest`** — this endpoint returns `data: null`.
- **A mid-form token expiry must not log the user out.** `forceLogout` is conditional for exactly this reason.
- **Never build a custom top bar.** `TDTopBar` via `AppDestination` — a hard rule in `CLAUDE.md`.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: change password, wrong current password, log in with the new one, EN + TR
```
