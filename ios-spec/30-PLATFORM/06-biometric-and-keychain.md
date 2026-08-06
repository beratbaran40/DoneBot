---
id: 30-06
title: Biometric gate & secure token storage
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-journal-01, 40-settings-08]
parallel_safe: true
estimate: 14h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/security/**
  - shared/data/src/androidMain/**/auth/**
  - shared/data/src/iosMain/**/security/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Face ID / Touch ID behind the `Authenticator` contract, and JWT storage in the Keychain behind `SecureTokenStore`.

## 2. Why this way

**The `Authenticator` contract already exists** — `20-02` removed `FragmentActivity` from it, so it is `suspend fun authenticate(reason: String): AuthOutcome`. That maps almost exactly onto `LAContext.evaluatePolicy`. This is one of the cleanest ports in the project.

**Token storage is where iOS is genuinely simpler.** Android needs `TokenCipher` — AndroidKeyStore AES-256-GCM, alias `donebot_session_token_key`, `"enc1:" + base64(iv || ciphertext)`, plus a three-attempt retry with a cached last-good plaintext because transient keystore faults are real, plus two one-shot migrations. All of that exists because Android's DataStore is not itself secure storage.

The Keychain **is** secure storage. No cipher layer, no retry ladder, no migration history. Resist the urge to port `TokenCipher` — reimplementing it on iOS would be adding a second lock inside a safe.

**`kSecAttrAccessibleAfterFirstUnlock` is the right accessibility class**, and it is a deliberate choice: the Android key has no user-authentication requirement precisely so background network works while the device is locked. `WhenUnlocked` would break background sync and push-triggered refresh.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `domain/security/Authenticator.kt` | The contract after `20-02` |
| `ui/security/biometric/BiometricAuthenticator.kt` (51 LOC) | `BiometricPrompt`; the `suspendCancellableCoroutine` resume-once guard |
| `data/auth/TokenCipher.kt` (164 LOC) | **Android only.** Read it to understand what iOS does *not* need. |
| `data/repository/SessionPreferencesImpl.kt` | Keys `session_access_token`, `session_refresh_token`, `session_expires_at`; V1/V2 migrations; `ORPHAN_TOKEN_KEYS` |
| `domain/repository/JournalBiometricPreferences.kt` | The journal lock flag |
| `ui/journal/JournalViewModel.kt` | `UiState.Locked` |
| `domain/security/` + `domain/usecase/security/` | Secret mode |

## 4. Target

- `shared/data/androidMain/…/AndroidAuthenticator.kt` — the existing `BiometricPrompt` implementation
- `shared/data/iosMain/…/IosAuthenticator.kt` — `LAContext`
- `shared/domain/…/security/SecureTokenStore.kt` — the contract
- `shared/data/androidMain/…/CipheredTokenStore.kt` — `TokenCipher` over DataStore
- `shared/data/iosMain/…/KeychainTokenStore.kt` — Security framework

## 5. Steps

1. **Move the Android `BiometricPrompt` implementation** into `androidMain` behind the contract. Keep the resume-once guard.

2. **Write the iOS implementation** with `LAContext.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics)`. Map `LAError` codes onto the existing `AuthOutcome` values: `.userCancel`/`.appCancel` → `CANCELLED`, `.biometryLockout` → `LOCKED_OUT`, `.biometryNotAvailable`/`.biometryNotEnrolled` → `UNAVAILABLE`, `.authenticationFailed` → `FAILED`.

3. **Decide the no-biometrics fallback.** `deviceOwnerAuthentication` (biometrics **or** passcode) is the better default — a user with Face ID disabled should still be able to open their journal. Record the choice in `DECISIONS.md`.

4. **Add `NSFaceIDUsageDescription`.** Missing it crashes the app the first time Face ID is invoked. The string must be specific: "DoneBot uses Face ID to unlock your private journal."

5. **Define `SecureTokenStore`** and put both implementations behind it. Android keeps `TokenCipher` unchanged, including the retry ladder and both migrations.

6. **Write `KeychainTokenStore`** with `kSecClassGenericPassword`, the app's bundle id as service, `kSecAttrAccessibleAfterFirstUnlock`.

7. **Do not implement a Keychain migration path.** iOS has no prior installs.

## 6. Code skeleton

```kotlin
// shared/data/iosMain/…/IosAuthenticator.kt
class IosAuthenticator : Authenticator {
    override suspend fun authenticate(reason: String): AuthOutcome = suspendCancellableCoroutine { cont ->
        val context = LAContext()
        // deviceOwnerAuthentication (not ...WithBiometrics): falls back to the passcode,
        // so a user without Face ID enrolled can still open their journal.
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, reason) { success, error ->
            if (!cont.isActive) return@evaluatePolicy      // resume exactly once
            cont.resume(
                when {
                    success -> AuthOutcome.SUCCESS
                    error?.code == LAErrorUserCancel || error?.code == LAErrorAppCancel -> AuthOutcome.CANCELLED
                    error?.code == LAErrorBiometryLockout -> AuthOutcome.LOCKED_OUT
                    error?.code == LAErrorBiometryNotAvailable ||
                        error?.code == LAErrorBiometryNotEnrolled -> AuthOutcome.UNAVAILABLE
                    else -> AuthOutcome.FAILED
                },
            )
        }
    }
}
```

```kotlin
// shared/domain/…/security/SecureTokenStore.kt
interface SecureTokenStore {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun clear()
}
```

```kotlin
// shared/data/iosMain/…/KeychainTokenStore.kt
// No cipher layer: the Keychain IS secure storage. Porting TokenCipher here would be
// adding a second lock inside a safe.
//
// AfterFirstUnlock (not WhenUnlocked) is deliberate — background sync and push-triggered
// refresh must work while the device is locked, which is why the Android key also has
// no user-authentication requirement.
class KeychainTokenStore(private val service: String = "com.todoapp.mobile") : SecureTokenStore {
    override suspend fun put(key: String, value: String) { /* SecItemAdd / SecItemUpdate */ }
    override suspend fun get(key: String): String? = null   // SecItemCopyMatching
    override suspend fun remove(key: String) { /* SecItemDelete */ }
    override suspend fun clear() { /* SecItemDelete over the service */ }
}
```

## 7. Acceptance

- [ ] `Authenticator` and `SecureTokenStore` in `:shared:domain`; both platforms registered
- [ ] Android behaviour unchanged — journal lock and all five secret-mode surfaces
- [ ] iOS: Face ID gates the journal; cancel keeps it locked
- [ ] iOS: passcode fallback works with Face ID disabled
- [ ] Every `LAError` maps to the right `AuthOutcome`; none crashes
- [ ] `NSFaceIDUsageDescription` present and specific
- [ ] iOS tokens land in the Keychain, not `UserDefaults`
- [ ] `kSecAttrAccessibleAfterFirstUnlock` — verify background refresh works with the device locked
- [ ] Logout clears the Keychain entries
- [ ] `TokenCipher` and its two migrations are untouched on Android
- [ ] The continuation resumes exactly once — no `IllegalStateException` under rapid cancel

## 8. Pitfalls

- **Missing `NSFaceIDUsageDescription` crashes on first use.** Not a permission denial — a crash.
- **`LAContext` is single-use.** Create a fresh one per authentication; reusing it returns stale results.
- **Resume exactly once.** `evaluatePolicy` can call back more than once under cancellation. Guard with `cont.isActive`.
- **`WhenUnlocked` breaks background work.** Use `AfterFirstUnlock`.
- **Do not port `TokenCipher` to iOS.** The Keychain already provides what it provides. Adding a cipher layer adds failure modes and no security.
- **Do not use `UserDefaults` for tokens.** It is unencrypted and backed up to iCloud.
- **Biometry lockout is a distinct state.** After repeated failures iOS locks biometrics until passcode entry; surface it as `LOCKED_OUT`, not `FAILED`.
- **Keychain items survive app deletion by default.** Decide whether to clear on first launch after reinstall; the safer choice is to clear, so a reinstall is a clean session. Record it.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone
#   journal → Face ID prompt → success opens, cancel keeps it locked
#   disable Face ID → passcode fallback
#   fail biometrics repeatedly → LOCKED_OUT surfaced, not a generic error
#   log in, lock the device, wait for a background sync → still works (AfterFirstUnlock)
#   log out → Keychain entries gone
```
