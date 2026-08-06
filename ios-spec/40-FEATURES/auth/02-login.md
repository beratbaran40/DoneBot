---
id: 40-auth-02
title: Login
layer: ui
status: TODO
depends_on: [40-auth-07, 30-10, 50-04]
blocks: [40-core-01]
parallel_safe: false
estimate: 8h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/login/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Email/password, Google and Apple sign-in, with `handleSuccessfulLogin` running identically on both platforms.

## 2. Why this way

**`handleSuccessfulLogin` does seven things and every one matters.** Clears chat if a *different* user id was cached (a privacy requirement on shared devices), stores the token triple, caches the user, sets the first-login permission prompt, logs the analytics event, force-fetches tasks, and syncs the pending FCM token. A parallel iOS path that misses one produces a subtle bug weeks later.

**This screen also depends on the two riskiest platform areas at once:** social sign-in (`30-10`) and text input (`50-04`). Focus traversal from email to password is the specific thing most likely to feel wrong on iOS.

**`redirectAfterLogin` is load-bearing.** A recorded lesson in this project: the login redirect recreates the ViewModel, so a pending action must be persisted rather than held in memory.

## 3. Source

| Path | LOC |
|---|---|
| `ui/login/` (4 files) | 704 |
| `ui/login/LoginViewModel.kt` (~173-215) | `handleSuccessfulLogin` |
| `ui/login/LoginPanels.kt` | button placement; Apple goes at least as prominently |
| `DomainException.OAuthAccountExists` | the social-only account path with `socialOnlyProvider` |
| `docs/screenshots/login/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/login/` — verification plus the Apple button from `60-03`.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify email/password login end to end against the live backend.
3. Verify Google sign-in on both platforms.
4. Verify Apple sign-in on iOS (`60-03`).
5. **Verify all seven steps of `handleSuccessfulLogin`** fire on iOS — particularly the chat clear on user change.
6. Verify `redirectAfterLogin` survives the ViewModel recreation.
7. Verify `OAuthAccountExists` shows the right message with the right provider name.
8. Verify focus traversal: email → password → submit.
9. Both languages; all error strings localized.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] All three sign-in methods work on iOS
- [ ] **All seven `handleSuccessfulLogin` steps verified**, including the chat clear on user change
- [ ] `redirectAfterLogin` works across the ViewModel recreation
- [ ] `OAuthAccountExists` shows the correct provider
- [ ] Focus traversal works with a software and a hardware keyboard
- [ ] Keyboard never covers the focused field
- [ ] All error states render and are localized in both languages
- [ ] Previews cover idle, loading, error and the social-only-account state

## 8. Pitfalls

- **Do not fork `handleSuccessfulLogin`.** Skipping the chat clear means one user sees another's history on a shared device.
- **`redirectAfterLogin` must be persisted, not held.** The redirect recreates the ViewModel — a recorded lesson here.
- **Google Sign-In needs a real account on the device.** Zero accounts produces a `NoCredentialException`, not an error dialog. Another recorded lesson.
- **The Apple button must be at least as prominent as Google's** (Guideline 4.8).
- **Error strings must be localized.** Five distinct Google error cases exist; all need TR.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: all three sign-in methods, wrong password, no network,
# social-only account, redirect flow, hardware keyboard traversal, EN + TR
```
