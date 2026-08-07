---
id: 70-01
title: Backend — `POST auth/apple`
layer: backend
status: TODO
depends_on: []
blocks: [30-10, 60-03, 80-05]
parallel_safe: true
estimate: 24h
reversible: true
owner_files:
  - "~/AndroidStudioProjects/ToDoBackend/**"
verify:
  - "Integration test: first authorization creates an account; second resolves to the same account"
---

> **Executed in the backend repository**, `~/AndroidStudioProjects/ToDoBackend` (Spring Boot + Kotlin, deployed to Render). Not in this repo.

## 1. Goal

Add Sign in with Apple to the backend: verify Apple's identity token, resolve or create the user, and return the same `AuthResponseData` the Google and email flows return.

## 2. Why this way

**This is a submission blocker.** App Store Review Guideline 4.8 requires an equivalent privacy-preserving login option for any app that uses a third-party login service for the primary account. The exemption covers apps using **exclusively** their own account system; DoneBot offers Google alongside email/password, so it does not apply.

**Apple's flow has two properties that break the obvious implementation.**

1. **The email arrives exactly once**, on first authorization. Every subsequent sign-in returns only the stable `sub` identifier. A backend that looks users up by email creates a duplicate account on the second sign-in — and the user's tasks vanish.

   **The `sub` must be the account key.** Store it in a dedicated column, indexed and unique.

2. **Private relay addresses are real.** `@privaterelay.appleid.com` addresses forward genuinely and must be treated as valid. Rejecting them by pattern breaks a legitimate, Apple-recommended user choice.

**Verification is JWKS-based, and every check matters.** Fetch Apple's public keys from `https://appleid.apple.com/auth/keys`, verify the RS256 signature by `kid`, then check `iss == https://appleid.apple.com`, `aud == <your bundle id>`, `exp`, and the nonce if one was sent. Skipping `aud` means any Apple token from any app is accepted.

**Return the same shape as `auth/google`.** The client's `handleSuccessfulLogin` is shared; a differently-shaped response forces a second code path.

## 3. Source — read before writing

In the backend repository:

| What | Why |
|---|---|
| The `auth` package — the `auth/google` handler | The exact pattern to mirror: token verification, user resolution, response shape |
| The `users` table and entity | Where `apple_user_id` goes |
| Flyway migrations, `h2/` **and** `postgresql/` | This project keeps both; a migration must land in both or `ddl-auto=validate` fails |
| The JWT issuing code | Access + refresh pair |
| `AuthResponseData` | The shape to return |

In this repository, for the contract:

| Path | Why |
|---|---|
| `data/source/remote/api/ToDoApi.kt` | `POST auth/google` — the shape to mirror |
| `data/model/network/request/GoogleLoginRequest.kt` | `{ token }` |
| `ios-spec/60-IOS-NATIVE/03-sign-in-with-apple.md` | The client side (`30-PLATFORM/10` defines the contract; `60-03` implements Apple) |

## 4. Target

In the backend repository:

- Flyway migration adding `users.apple_user_id` (nullable, unique, indexed) — in **both** `h2/` and `postgresql/`
- An Apple JWKS verifier with key caching
- `POST /auth/apple`
- Integration tests

## 5. Steps

1. **Write the migration** adding `apple_user_id VARCHAR(255) NULL UNIQUE` with an index. Put it in **both** dialect folders — `ddl-auto=validate` fails at boot otherwise.

   > Watch identifier casing: H2 upper-cases unquoted identifiers and Postgres lower-cases them. Write unquoted and qualified, a recorded trap in this project.

2. **Implement the JWKS verifier.** Fetch from `https://appleid.apple.com/auth/keys`, cache the keys (they rotate but not often), select by `kid`, verify RS256.

3. **Validate every claim:** `iss == https://appleid.apple.com`, `aud == <bundle id>`, `exp` not passed, and the nonce if the client sent one. **`aud` is the one that matters most** — without it, any Apple identity token from any app authenticates.

4. **Resolve the user, in this order:**
   1. by `apple_user_id` → existing user, log in
   2. else, if an email was supplied and matches an existing account → **link** `apple_user_id` to it and log in
   3. else → create a new user with `apple_user_id`, the email if supplied, and the name if supplied

5. **Handle a missing email.** On a repeat sign-in for an account that was somehow never persisted, there is no email available. Fail with a clear error rather than creating a user with a null email.

6. **Accept private relay addresses.** No pattern-based rejection anywhere in the pipeline, including any existing email validation.

7. **Return `AuthResponseData`**, identical to `auth/google`.

8. **Add the endpoint to the client API interface** in this repo.

9. **Write integration tests** covering: first authorization creates an account; second sign-in resolves to the **same** account; linking to an existing email account; an invalid signature is rejected; a wrong `aud` is rejected; an expired token is rejected; a relay address is accepted.

## 6. Code skeleton

```kotlin
// Backend — POST /auth/apple
data class AppleLoginRequest(
    val identityToken: String,
    val authorizationCode: String,
    // Apple returns these ONLY on first authorization. Never treat their absence as an error.
    val email: String? = null,
    val fullName: String? = null,
)

fun resolveAppleUser(claims: AppleClaims, request: AppleLoginRequest): User {
    // The stable subject is the account key. Keying on email would create a duplicate
    // account on the second sign-in, because Apple stops sending the email after the first.
    userRepository.findByAppleUserId(claims.sub)?.let { return it }

    request.email?.let { email ->
        userRepository.findByEmail(email)?.let { existing ->
            return userRepository.linkApple(existing, claims.sub)
        }
    }

    return userRepository.create(
        appleUserId = claims.sub,
        // Private relay addresses forward genuinely — never reject by pattern.
        email = request.email ?: claims.email,
        displayName = request.fullName,
    )
}
```

```sql
-- V<n>__add_apple_user_id.sql — in BOTH h2/ and postgresql/
ALTER TABLE users ADD COLUMN apple_user_id VARCHAR(255);
CREATE UNIQUE INDEX idx_users_apple_user_id ON users (apple_user_id);
```

## 7. Acceptance

- [ ] Migration present in **both** `h2/` and `postgresql/`; the app boots with `ddl-auto=validate`
- [ ] `POST /auth/apple` returns the same shape as `auth/google`
- [ ] JWKS verification works, with key caching
- [ ] `iss`, `aud`, `exp` and nonce all validated
- [ ] **A wrong `aud` is rejected** — explicitly tested
- [ ] First authorization creates an account with email and name
- [ ] **Second sign-in resolves to the same account** — the critical test
- [ ] An Apple sign-in whose email matches an existing account links rather than duplicating
- [ ] Private relay addresses are accepted end to end
- [ ] An invalid signature and an expired token are both rejected
- [ ] The endpoint is added to the client API interface in this repo
- [ ] Deployed to Render and verified against a real device

## 8. Pitfalls

- **Keying on email creates duplicate accounts.** Apple sends the email once. This is the single most common Sign in with Apple bug and it silently loses users' data.
- **Missing `aud` validation accepts tokens from other apps.** A real authentication bypass.
- **Rejecting `@privaterelay.appleid.com` breaks a legitimate, Apple-recommended choice.** Check any existing email validation too.
- **Flyway migrations must exist in both dialect folders.** A recorded trap in this project.
- **H2 upper-cases unquoted identifiers; Postgres lower-cases them.** Write unquoted and qualified.
- **JWKS keys rotate.** Cache, but not forever, and handle an unknown `kid` by refetching.
- **The name arrives once too.** Persist it on first authorization or it is gone.
- **Test the revoke path.** iOS Settings → Apple ID → revoke. The next sign-in behaves as a first authorization, and the `sub` **stays the same** — so it must resolve to the existing account, not create a new one.

## 9. Verification

```bash
# In the backend repository
./gradlew test
./gradlew bootRun    # boots with ddl-auto=validate → migration is correct

# Against the deployed instance
curl -X POST https://donebot-backend.onrender.com/auth/apple \
  -H 'Content-Type: application/json' \
  -d '{"identityToken":"<real token>","authorizationCode":"<code>","email":"x@privaterelay.appleid.com"}'

# End to end, on a real iPhone
#   Apple sign-in, first time      → account created, email + name stored
#   log out, Apple sign-in again   → SAME account, tasks intact
#   iOS Settings → revoke → sign in again → still the same account
```
