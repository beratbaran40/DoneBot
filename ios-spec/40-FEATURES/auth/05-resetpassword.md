---
id: 40-auth-05
title: Reset password
layer: ui
status: TODO
depends_on: [40-auth-04, 30-13]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/resetpassword/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Consume a reset token from a deep link and set a new password.

## 2. Why this way

**This is the only screen reached exclusively by deep link**, which makes it the practical end-to-end test of `30-13`. Both link forms must work: the https universal link (what the email actually contains, because Gmail strips custom schemes) and `todoapp://reset-password` as a fallback.

**Cold start is the failure case.** Tapping the link when the app is not running delivers the URL before the composition exists. Android handles this by forwarding from `onCreate`; iOS needs the same queue-and-replay, and this screen is where a missed one is visible.

## 3. Source

| Path | LOC |
|---|---|
| `ui/resetpassword/` (4 files) | 345 |
| `MainViewModel.kt` (~161-183) | link parsing → `DeepLink.ResetPassword(token)` |
| `navigation/Screen.kt` | `ResetPassword(token)` |
| `POST auth/reset-password` | `ResetPasswordRequest(token, newPassword)` |
| `ios-spec/30-PLATFORM/13-deeplinks.md` | both link forms |

## 4. Target

`shared/ui/commonMain/…/ui/resetpassword/` — verification.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify the https universal link opens this screen with the token.
3. Verify the custom scheme does too.
4. **Verify cold start** — kill the app, tap the link, the token must survive.
5. Verify an expired or invalid token shows a clear, localized message.
6. Verify success routes to login.
7. Both languages.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] https universal link opens the screen with the token, not Safari
- [ ] `todoapp://reset-password?token=…` works
- [ ] **Cold-start link is not dropped**
- [ ] Expired/invalid token shows a localized message
- [ ] Success routes to login and the new password works
- [ ] Previews cover idle, loading, invalid-token and success

## 8. Pitfalls

- **Cold start is the case that breaks.** The URL arrives before the composition; queue and replay.
- **The email contains the https link.** Test that one, not just the scheme.
- **An expired token needs a real message and a path forward** — a generic error strands the user.
- **`Screen.ResetPassword` is R8-name-sensitive** like every route. Do not rename it.

## 9. Verification

```bash
xcrun simctl openurl booted "todoapp://reset-password?token=test"
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: real reset email from Gmail, app running and app killed; expired token
```
