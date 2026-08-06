---
id: 30-10
title: Google Sign-In & Sign in with Apple
layer: platform
status: TODO
depends_on: [10-01, 20-13, 70-01]
blocks: [40-auth-02, 40-auth-03, 60-03]
parallel_safe: true
estimate: 18h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/auth/**
  - shared/data/src/androidMain/**/auth/**
  - shared/data/src/iosMain/**/auth/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Google Sign-In on iOS, and Sign in with Apple on both the client and the backend. **Sign in with Apple is a submission blocker.**

## 2. Why this way

**Google Sign-In is nearly free.** The Android flow produces a Google **ID token** and POSTs it to `auth/google`; the backend verifies it and returns the same `AuthResponseData` as email/password. The iOS SDK produces the same kind of token against the same GCP project. **No backend change.**

**Sign in with Apple is required, and this was initially assessed wrongly.** Guideline 4.8 requires an equivalent privacy-preserving login option for any app that uses a third-party login service to set up or authenticate the primary account. The exemption covers apps using **exclusively** their own account system. DoneBot offers Google alongside email/password, so the exemption does not apply. It is not optional, and it needs a new backend endpoint (`70-01`).

**Apple's flow has two properties that will break naive server code:**

1. **The email is returned exactly once**, on first authorization. Subsequent sign-ins return only the stable `user` identifier. A backend that looks users up by email will create a duplicate account on the second sign-in.
2. **Private relay addresses** (`@privaterelay.appleid.com`) are real, forwardable addresses that must be treated as valid.

Both are `70-01`'s concern, but the client must send the first-authorization payload in full or the server can never recover it.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/auth/GoogleSignInManager.kt` (78 LOC) | Credential Manager, `GetGoogleIdOption`, `setFilterByAuthorizedAccounts(false)`, `setAutoSelectEnabled(false)`, server client id from `R.string.default_web_client_id`; **five distinct localized error strings** |
| `ui/login/LoginViewModel.kt` (~173-215) | `handleSuccessfulLogin` — clears chat on a different user id, stores tokens, sets the first-login permission prompt, logs analytics, forces a fetch, navigates, syncs the FCM token. **The iOS path must do all of it.** |
| `ui/register/RegisterViewModel.kt` | Same shape |
| `data/source/remote/api/ToDoApi.kt` | `POST auth/google` |
| `data/model/network/request/GoogleLoginRequest.kt` | `{ token }` |
| `DomainException.OAuthAccountExists` | The "this email is a social-login account" path with `socialOnlyProvider` |
| `ui/login/LoginPanels.kt`, `ui/register/RegisterPanels.kt` | Button placement — Apple's button has Human Interface requirements |

## 4. Target

- `shared/domain/…/auth/SocialSignIn.kt` — the contract
- `shared/data/androidMain/…/CredentialManagerSignIn.kt` — the existing implementation
- `shared/data/iosMain/…/IosSocialSignIn.kt` — `GIDSignIn` + `ASAuthorizationAppleIDProvider`
- `data/model/network/request/AppleLoginRequest.kt` *(new)*
- `iosApp/Info.plist` — the reversed client id URL scheme

## 5. Steps

1. **Define the contract** returning an ID token for Google and a richer credential for Apple.

2. **Move the Android implementation** to `androidMain` behind it. Keep the five localized error strings — they are good UX and `CLAUDE.md` requires localized errors.

3. **iOS Google**: add the GoogleSignIn SDK via SPM, configure with the **iOS** OAuth client id, add the reversed client id as a URL scheme, and send `idToken` to the existing `auth/google`.

4. **iOS Apple**: `ASAuthorizationAppleIDProvider` with `.fullName` and `.email` scopes. Send the identity token, the authorization code, and — **only present on first authorization** — the email and full name.

5. **Add `POST auth/apple`** to the API interface, mirroring `auth/google`'s response shape so the ViewModel path is identical.

6. **Reuse `handleSuccessfulLogin` verbatim.** Every step in it matters; a parallel iOS implementation will drift.

7. **Place the Apple button per Apple's Human Interface Guidelines** — at least as prominent as other sign-in buttons, and using the system-provided button style.

8. **Handle cancellation as a non-error.** Both providers report user cancellation; it must not surface as a failure.

## 6. Code skeleton

```kotlin
// shared/domain/…/auth/SocialSignIn.kt
data class AppleCredential(
    val identityToken: String,
    val authorizationCode: String,
    // Apple returns these ONLY on first authorization. Later sign-ins give the stable
    // user id alone — a backend keying on email would create a duplicate account.
    val email: String?,
    val fullName: String?,
    val userIdentifier: String,
)

interface SocialSignIn {
    suspend fun googleIdToken(): Result<String>
    suspend fun appleCredential(): Result<AppleCredential>
    val supportsApple: Boolean   // Android: false for now
}
```

```swift
// iosApp — Apple sign-in request
let request = ASAuthorizationAppleIDProvider().createRequest()
request.requestedScopes = [.fullName, .email]
ASAuthorizationController(authorizationRequests: [request]).performRequests()
```

## 7. Acceptance

- [ ] `SocialSignIn` in `:shared:domain`; both platforms registered
- [ ] Android behaviour unchanged, including all five localized error cases
- [ ] iOS Google Sign-In works and returns an ID token accepted by `auth/google`
- [ ] iOS Sign in with Apple works against `auth/apple` (`70-01`)
- [ ] First Apple authorization sends email and full name; later ones do not — **and the second sign-in resolves to the same account**
- [ ] A private relay address is accepted and stored
- [ ] Both paths run `handleSuccessfulLogin` in full: chat cleared on user change, tokens stored, first-login prompt set, analytics logged, tasks fetched, FCM token synced
- [ ] The Apple button follows Apple's HIG and is at least as prominent as Google's
- [ ] Cancellation is not reported as an error on either provider
- [ ] `OAuthAccountExists` still surfaces correctly
- [ ] The reversed client id URL scheme is in `Info.plist`
- [ ] Sign in with Apple capability enabled on the App ID

## 8. Pitfalls

- **Apple returns the email exactly once.** If the client does not forward it on first authorization, it is unrecoverable — the user has to revoke the app in iOS Settings to get another chance.
- **Private relay addresses are real.** Rejecting `@privaterelay.appleid.com` is a rejection-worthy bug and breaks a legitimate user.
- **The reversed client id, not the client id**, is the URL scheme. Getting it wrong fails with an unhelpful error.
- **Google needs an *iOS* OAuth client id**, distinct from the Android one, in the same GCP project.
- **`handleSuccessfulLogin` is not boilerplate.** Skipping the chat clear means one user sees another's history on the same device — a privacy bug.
- **Apple's button has HIG requirements.** Reviewers do check prominence and style.
- **Sign in with Apple must be enabled on the App ID** before the entitlement works; enabling it later means regenerating provisioning profiles.
- **Test the revoke path.** iOS Settings → Apple ID → Sign in with Apple → revoke. The next sign-in behaves like a first authorization again.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone
#   Google sign-in → account created/logged in, tasks sync
#   Apple sign-in, first time → email + name captured server-side
#   log out, Apple sign-in again → SAME account, no duplicate
#   choose "Hide My Email" → relay address accepted
#   cancel each flow → no error surfaced
#   iOS Settings → revoke → sign in again → treated as first authorization
```
