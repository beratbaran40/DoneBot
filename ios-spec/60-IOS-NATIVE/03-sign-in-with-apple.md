---
id: 60-03
title: Sign in with Apple — client
layer: ios-native
status: TODO
depends_on: [10-01, 30-10, 70-01]
blocks: [80-05]
parallel_safe: false
estimate: 16h
reversible: false
owner_files:
  - shared/ui/src/commonMain/**/login/**
  - shared/ui/src/commonMain/**/register/**
  - shared/data/src/iosMain/**/auth/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Implement Sign in with Apple end to end on the client: the `ASAuthorizationAppleIDProvider` flow, the `auth/apple` request, and the button on login and register — wired to `70-01`'s endpoint and meeting Apple's Human Interface requirements.

**This is a submission blocker.** The app cannot ship without it.

## 2. Why this way

**This task owns the whole Apple client path, not just the button.** `30-10` declares `appleCredential()` in the `SocialSignIn` contract and stubs it; everything that actually talks to Apple lives here. That split is deliberate: Apple sign-in needs `10-01` (paid enrolment, weeks of verification) and `70-01` (a backend endpoint in a different repository), and folding those dependencies into `30-10` would have transitively blocked login — and therefore 41 downstream tasks including Home — on a backend endpoint. Google Sign-In and the contract ship without waiting; Apple lands here when its two prerequisites are ready.

**`70-01` builds the backend.** This task is the client flow, the UI, and the account-linking edge cases — small, but the part App Review actually looks at.

**Guideline 4.8 makes this mandatory**, and it was initially assessed as optional. The exemption covers apps using **exclusively** their own account system; DoneBot offers Google Sign-In alongside email/password, so it does not apply.

**Apple checks prominence.** The button must be at least as prominent as other third-party sign-in options and use the system-provided style. A custom-drawn Apple button, or one placed below Google, is a documented rejection reason.

**The account-linking edge cases are where users get hurt.** They belong here because they are user-visible, even though the resolution logic lives in `70-01`:

- An existing email/password user signs in with Apple using the same email → must **link**, not create a second account. Otherwise their tasks vanish.
- A user who chose "Hide My Email" gets a relay address — and if they later sign in with Google using their real address, those are two different accounts. That is correct behaviour, but the UI should not make it look like data loss.

**The existing `OAuthAccountExists` path already handles the inverse.** When someone tries email/password on a social-only account, the app explains it with `socialOnlyProvider`. Apple sign-in must feed the same mechanism.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `ui/login/LoginPanels.kt`, `ui/register/RegisterPanels.kt` | Where the Google button sits; Apple goes at least as prominently |
| `ui/login/LoginViewModel.kt` (~173-215) | `handleSuccessfulLogin` — reuse verbatim |
| `ui/auth/AuthScaffold.kt`, `AuthConsentFooter.kt` | Shared auth chrome and the ToS/privacy consent |
| `DomainException.OAuthAccountExists` | The social-only account path |
| `ios-spec/30-PLATFORM/10-social-signin-google.md` | The `SocialSignIn` contract — `appleCredential()` is declared there and stubbed; you implement it here |
| `ios-spec/70-BACKEND/01-auth-apple.md` | The endpoint |
| `app/src/main/res/values{,-tr}/strings.xml` | New strings go in **both** |

## 4. Target

- `shared/data/iosMain/…/auth/IosSocialSignIn.kt` — replace `30-10`'s stub: real `appleCredential()`, `supportsApple = true`
- `shared/data/…/model/network/request/AppleLoginRequest.kt` *(new)* — `{ identityToken, authorizationCode, email?, fullName? }`
- `shared/data/…/api/ToDoApi.kt` — `POST auth/apple`, mirroring `auth/google`'s response shape
- `shared/ui/…/login/LoginPanels.kt`, `register/RegisterPanels.kt` — the button, iOS-only
- New strings in EN **and** TR
- `iosApp/iosApp.entitlements` — the capability

## 5. Steps

0. **Implement the credential flow.** `ASAuthorizationAppleIDProvider` with `.fullName` and `.email` scopes; return identity token, authorization code, the stable `userIdentifier`, and — **only present on first authorization** — email and full name. Replace `30-10`'s stub and flip `supportsApple` to `true`.

0b. **Add `POST auth/apple`** to the API interface, mirroring `auth/google`'s response shape so the ViewModel path is identical.

1. **Add the button, gated on `SocialSignIn.supportsApple`.** It must not appear on Android.

2. **Use the system-provided button.** `ASAuthorizationAppleIDButton` hosted in a `UIKitView`, or a faithful reproduction that satisfies Apple's specification. **The system button is safer** — Apple's own component cannot be rejected for styling.

3. **Place it at least as prominently as Google's.** Above it is the conventional and safest choice.

4. **Wire it to `handleSuccessfulLogin`** — the same function, not a parallel path.

5. **Handle the linking case in the UI.** When the backend links an Apple sign-in to an existing email account, the user should land in their account with their data. No special messaging needed if it works; the failure mode is what matters.

6. **Handle "Hide My Email" honestly.** If the app displays the account email anywhere, a relay address will appear. That is correct and should not be presented as an error.

7. **Add strings to both language files.** `CLAUDE.md` requires parity in the same change.

8. **Enable the capability** on the App ID and in entitlements — done in `10-01`, verify here.

9. **Test the revoke path.** iOS Settings → Apple ID → Sign in with Apple → revoke. Signing in again behaves like a first authorization but returns the **same** `sub`, so it must resolve to the existing account.

## 6. Code skeleton

```kotlin
// Login / register panel — iOS only, driven by the contract, not by a platform check.
if (socialSignIn.supportsApple) {
    AppleSignInButton(
        // Guideline 4.8: at least as prominent as other third-party sign-in options.
        // Above Google is the conventional and safest placement.
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAction(LoginContract.UiAction.OnAppleSignInClick) },
    )
    Spacer(Modifier.height(TDTheme.spacing.small))
}
GoogleSignInButton(/* … */)
```

```kotlin
// LoginViewModel — the SAME success path, not a parallel one.
is UiAction.OnAppleSignInClick -> viewModelScope.launch {
    socialSignIn.appleCredential()
        .onSuccess { credential ->
            userRepository.loginWithApple(credential)
                .onSuccess { handleSuccessfulLogin(it) }      // identical to Google/email
                .onFailure { handleAuthError(it) }
        }
        .onFailure { /* cancellation is not an error */ }
}
```

## 7. Acceptance

- [ ] `appleCredential()` is implemented on iOS and `supportsApple` is `true`; `30-10`'s stub is gone
- [ ] `POST auth/apple` exists on the API interface with the same response shape as `auth/google`
- [ ] First authorization forwards email and full name; the client never drops them
- [ ] The Apple button appears on login and register **on iOS only**
- [ ] It uses the system-provided button (or a compliant reproduction)
- [ ] It is at least as prominent as the Google button
- [ ] First authorization creates an account with email and name
- [ ] Second sign-in resolves to the **same** account with all data intact
- [ ] An existing email/password user signing in with Apple on the same email **links**, does not duplicate
- [ ] "Hide My Email" works; the relay address is accepted and displayed without looking like an error
- [ ] Cancellation is not surfaced as an error
- [ ] `handleSuccessfulLogin` runs in full — chat cleared on user change, tokens stored, first-login prompt set, analytics logged, tasks fetched, FCM token synced
- [ ] `OAuthAccountExists` still works for the inverse case
- [ ] New strings in **both** EN and TR
- [ ] Revoke → sign in again → same account
- [ ] Capability enabled on the App ID and in entitlements

## 8. Pitfalls

- **Apple returns the email exactly once.** If the client does not forward it on first authorization, it is unrecoverable — the user has to revoke the app in iOS Settings to get another chance. This is the highest-consequence line of code in the task.
- **Private relay addresses are real.** Rejecting `@privaterelay.appleid.com` is a rejection-worthy bug and breaks a legitimate user.
- **Sign in with Apple must be enabled on the App ID** before the entitlement works; enabling it later means regenerating provisioning profiles. That is why `10-01` is a dependency.
- **Prominence is checked by reviewers.** Below Google, or visually smaller, is a documented rejection.
- **Use the system button.** Apple's own component cannot be rejected for styling; a custom one can.
- **Do not build a parallel success path.** `handleSuccessfulLogin` does seven things and skipping the chat clear is a privacy bug — one user seeing another's history on a shared device.
- **Linking must not create a duplicate.** The logic is in `70-01`, but this is where a user would notice their tasks disappearing.
- **Relay addresses are legitimate.** Do not validate them away, and do not present them as a problem.
- **Cancellation is not an error.** Users tap outside the sheet constantly.
- **Test revoke.** It is the state most likely to be untested and most likely to produce a duplicate account.
- **Both language files, same change.** A missing TR string is a visible bug in half the user base.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone
#   login and register → Apple button present, prominent, system style
#   first sign-in                          → account created
#   log out, sign in again                 → SAME account, tasks intact
#   existing email account, same address   → links, no duplicate
#   "Hide My Email"                        → relay address accepted
#   cancel the sheet                       → no error shown
#   iOS Settings → revoke → sign in again  → same account
#   device in Turkish                      → all strings translated
```
