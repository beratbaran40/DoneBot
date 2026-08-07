---
id: 30-10
title: Social sign-in contract & Google Sign-In
layer: platform
status: TODO
depends_on: [20-13, 10-03]
blocks: [40-auth-02, 40-auth-03, 60-03]
parallel_safe: true
estimate: 12h
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

Define the `SocialSignIn` contract for both providers, and implement **Google Sign-In** on iOS against the existing `auth/google` endpoint.

**Sign in with Apple is not implemented here.** The contract declares its shape; the iOS implementation, the UI and the account-linking behaviour are `60-03`, and the endpoint is `70-01`. See §2.

## 2. Why this way

**Google Sign-In is nearly free, and it needs nothing that Apple's does.** The Android flow produces a Google **ID token** and POSTs it to `auth/google`; the backend verifies it and returns the same `AuthResponseData` as email/password. The iOS SDK produces the same kind of token against the same GCP project. **No backend change, and no Apple Developer Program membership** — the iOS OAuth client id is a GCP artifact, not an Apple one.

**Splitting Apple out of this task is a scheduling decision, and it is worth understanding.** When the two providers were one task, this file inherited Apple's dependencies: `10-01` (paid enrolment, weeks of identity verification) and `70-01` (a backend endpoint in a different repository). Because `40-auth-02` (login) depends on this task and almost every feature depends on login, **41 of the spec's tasks — including Home — became transitively blocked on a backend Apple endpoint.** Nothing about verifying the Home screen requires signing in with Apple. Splitting the file frees that entire subtree to proceed while enrolment is still pending.

**The contract still declares both providers.** `appleCredential()` is defined here because a data class needs no backend, and because `40-auth-02` must compile against the final interface rather than be rewritten later. Android returns `UNAVAILABLE`; iOS's implementation lands in `60-03`.

**Apple's flow has two properties that will break naive server code.** They are recorded here because they constrain the *contract shape*, even though the implementation is elsewhere:

1. **The email is returned exactly once**, on first authorization. Subsequent sign-ins return only the stable `user` identifier. A backend that looks users up by email will create a duplicate account on the second sign-in.
2. **Private relay addresses** (`@privaterelay.appleid.com`) are real, forwardable addresses that must be treated as valid.

Both are `70-01`'s concern, but the credential type must carry `email`/`fullName` as nullable first-authorization-only fields or the client can never forward what the server cannot recover.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/auth/GoogleSignInManager.kt` (78 LOC) | Credential Manager, `GetGoogleIdOption`, `setFilterByAuthorizedAccounts(false)`, `setAutoSelectEnabled(false)`, server client id from `R.string.default_web_client_id`; **five distinct localized error strings** |
| `ui/login/LoginViewModel.kt` (~173-215) | `handleSuccessfulLogin` — clears chat on a different user id, stores tokens, sets the first-login permission prompt, logs analytics, forces a fetch, navigates, syncs the FCM token. **The iOS path must do all of it.** |
| `ui/register/RegisterViewModel.kt` | Same shape |
| `data/source/remote/api/ToDoApi.kt` | `POST auth/google` |
| `data/model/network/request/GoogleLoginRequest.kt` | `{ token }` |
| `DomainException.OAuthAccountExists` | The "this email is a social-login account" path with `socialOnlyProvider` |
| `ui/login/LoginPanels.kt`, `ui/register/RegisterPanels.kt` | Where the Google button sits. **Leave room above it** — `60-03` puts the Apple button there, and Guideline 4.8 requires it to be at least as prominent. |
| `ios-spec/60-IOS-NATIVE/03-sign-in-with-apple.md` | What this task deliberately does not do |

## 4. Target

- `shared/domain/…/auth/SocialSignIn.kt` — the contract, **both** providers
- `shared/data/androidMain/…/CredentialManagerSignIn.kt` — the existing implementation, moved
- `shared/data/iosMain/…/IosSocialSignIn.kt` — `GIDSignIn`; `appleCredential()` throws `NotImplementedError("60-03")`
- `iosApp/Info.plist` — the reversed client id URL scheme

Not in this task: `AppleLoginRequest`, `POST auth/apple`, `ASAuthorizationAppleIDProvider`, the Apple button. Those are `60-03` (client) and `70-01` (backend).

## 5. Steps

1. **Define the contract** returning an ID token for Google and a richer credential for Apple. Declare `appleCredential()` now even though only `60-03` implements it — `40-auth-02` compiles against this interface and should not be rewritten later.

2. **Move the Android implementation** to `androidMain` behind it. Keep the five localized error strings — they are good UX and `CLAUDE.md` requires localized errors. Android's `supportsApple` is `false` and `appleCredential()` returns a failure.

3. **iOS Google**: add the GoogleSignIn SDK via SPM, configure with the **iOS** OAuth client id, add the reversed client id as a URL scheme, and send `idToken` to the existing `auth/google`.

4. **Stub the iOS Apple path.** `appleCredential()` in `IosSocialSignIn` throws `NotImplementedError("Implemented in 60-03")`, and `supportsApple` returns `false` until `60-03` flips it. A loud stub is correct here — the login screen gates the Apple button on `supportsApple`, so nothing renders and nothing silently misbehaves.

5. **Reuse `handleSuccessfulLogin` verbatim** for the Google path. Every step in it matters; a parallel iOS implementation will drift.

6. **Handle cancellation as a non-error.** Google reports user cancellation; it must not surface as a failure.

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
    // Declared here so 40-auth-02 compiles against the final shape.
    // Implemented in 60-03; Android and the 30-10 iOS stub both fail it.
    suspend fun appleCredential(): Result<AppleCredential>
    val supportsApple: Boolean   // Android: false. iOS: false until 60-03.
}
```

```kotlin
// shared/data/iosMain/…/IosSocialSignIn.kt — the Apple half is a loud stub here.
override val supportsApple = false      // 60-03 flips this
override suspend fun appleCredential(): Result<AppleCredential> =
    Result.failure(NotImplementedError("Sign in with Apple is implemented in 60-03"))
```

## 7. Acceptance

- [ ] `SocialSignIn` in `:shared:domain` declaring **both** providers; both platforms registered
- [ ] Android behaviour unchanged, including all five localized error cases
- [ ] iOS Google Sign-In works and returns an ID token accepted by `auth/google`
- [ ] The Google path runs `handleSuccessfulLogin` in full: chat cleared on user change, tokens stored, first-login prompt set, analytics logged, tasks fetched, FCM token synced
- [ ] Cancellation is not reported as an error
- [ ] `OAuthAccountExists` still surfaces correctly
- [ ] The reversed client id URL scheme is in `Info.plist`
- [ ] `supportsApple` is `false` on both platforms and the Apple button does not render
- [ ] `appleCredential()` fails loudly rather than returning a plausible empty credential
- [ ] **No `auth/apple` call, no Apple UI, and no `10-01`/`70-01` dependency was introduced here**

## 8. Pitfalls

- **Do not implement Sign in with Apple here.** It is `60-03`. Pulling it back in re-creates the dependency edge that blocked 41 tasks on a backend endpoint — the whole reason this file was split. If a session finds itself adding `ASAuthorizationAppleIDProvider` to this task, stop.
- **The credential type must carry `email`/`fullName` as nullable.** Apple returns them exactly once, on first authorization. Getting the *shape* wrong here forces a contract change in `60-03`, which forces a recompile of every login call site.
- **The reversed client id, not the client id**, is the URL scheme. Getting it wrong fails with an unhelpful error.
- **Google needs an *iOS* OAuth client id**, distinct from the Android one, in the same GCP project. This is a GCP console action and needs **no** Apple Developer membership.
- **`handleSuccessfulLogin` is not boilerplate.** Skipping the chat clear means one user sees another's history on the same device — a privacy bug.
- **The Apple stub must fail, not return empty.** A `Result.success` with blank fields would let the login screen appear to work and create a broken account.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# The split held — no Apple implementation leaked into this task
grep -rn "ASAuthorization\|auth/apple\|AppleLoginRequest" shared/ \
  && echo "APPLE WORK LEAKED INTO 30-10" || echo "clean"

# On a real iPhone
#   Google sign-in → account created/logged in, tasks sync
#   cancel the flow → no error surfaced
#   no Apple button is visible (supportsApple == false)
```
