---
id: 40-auth-04
title: Forgot password
layer: ui
status: TODO
depends_on: [40-auth-07]
blocks: [40-auth-05]
parallel_safe: true
estimate: 3h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/forgotpassword/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Request a password-reset email.

## 2. Why this way

A small screen with one important property: **the confirmation must not reveal whether the email exists.** Enumerating accounts is a real leak and the backend deliberately returns the same response either way — the UI must not undo that with a helpful "no such account" message.

The email it triggers contains an https landing link rather than a custom scheme, because **Gmail strips `todoapp://` links** — a recorded lesson in this project. That is why `30-13`'s universal-link setup matters for this flow.

## 3. Source

| Path | LOC |
|---|---|
| `ui/forgotpassword/` (3 files) | 307 |
| `POST auth/forgot-password` | `ForgotPasswordRequest(email)` → `Unit?` |
| `common/Extensions.kt` | `handleEmptyRequest` — this endpoint returns `data: null` |

## 4. Target

`shared/ui/commonMain/…/ui/forgotpassword/` — verification.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify the request succeeds and the email arrives.
3. Verify the confirmation is identical for a known and an unknown email.
4. Verify the `handleEmptyRequest` path — a 2xx with `data: null` must succeed, not error.
5. Verify network-error handling.
6. Both languages.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] Reset email arrives on both platforms
- [ ] **Confirmation identical for known and unknown emails**
- [ ] `data: null` success is handled, not treated as an error
- [ ] Network error localized
- [ ] Previews cover idle, loading, sent and error

## 8. Pitfalls

- **Do not reveal whether the account exists.** The backend deliberately does not; the UI must match.
- **`handleRequest` rejects a 2xx with `data: null`.** Use `handleEmptyRequest` — a documented gotcha.
- **The email link is https, not the custom scheme**, because Gmail strips custom schemes.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: known email, unknown email (same message), airplane mode, EN + TR
```
