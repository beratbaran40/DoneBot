---
id: 20-02
title: Close the two Android leaks in `domain/`
layer: domain
status: TODO
depends_on: [20-01]
blocks: [20-03]
parallel_safe: false
estimate: 4h
reversible: true
owner_files:
  - app/src/main/java/com/todoapp/mobile/domain/repository/AlarmSoundPreferences.kt
  - app/src/main/java/com/todoapp/mobile/domain/security/Authenticator.kt
  - app/src/main/java/com/todoapp/mobile/data/repository/AlarmSoundPreferencesImpl.kt
  - app/src/main/java/com/todoapp/mobile/ui/security/**
  - app/src/main/java/com/todoapp/mobile/ui/alarmsounds/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  # androidx.compose.runtime.Immutable is multiplatform and stays — it is NOT a leak.
  # Without the grep -v this command fails on 12 legitimate model files.
  - "! grep -rnE '^import (android|androidx)\\.' app/src/main/java/com/todoapp/mobile/domain/ | grep -v 'androidx\\.compose\\.runtime\\.Immutable'"
---

## 1. Goal

Make `domain/` free of Android types so it can move to `commonMain` in one step instead of landing in `androidMain` and being drained file by file. Exactly two files leak today.

## 2. Why this way

The domain layer is 67 files and 2,482 lines, and `CLAUDE.md` already states it holds "interfaces and models only, no Android dependencies." That is *almost* true — two files break it:

- `domain/repository/AlarmSoundPreferences.kt:3` — `android.net.Uri`
- `domain/security/Authenticator.kt:3` — `androidx.fragment.app.FragmentActivity`

Fixing them here, as an ordinary Android-only refactor with the full test suite as a shield, is far cheaper than discovering them mid-module-move. It also means `20-03` gets to be a pure `git mv` whose only risk is Gradle wiring.

**Both fixes point the same direction:** the domain should describe *what* it needs, not *how* the platform supplies it. A sound is an identifier, not a content URI. Authentication is a suspend function returning a result, not an Activity handle.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `domain/repository/AlarmSoundPreferences.kt` | The `Uri` in the interface signature |
| `data/repository/AlarmSoundPreferencesImpl.kt` | How the URI is persisted and resolved; `RingtoneManager` usage |
| `common/RingtoneHolder.kt` | Playback and ringer-mode etiquette — the natural home for URI resolution |
| `ui/alarmsounds/` (3 files, 325 LOC) | The picker UI and how it lists system ringtones |
| `domain/security/Authenticator.kt` | The `FragmentActivity` parameter |
| `ui/security/biometric/BiometricAuthenticator.kt` | The `BiometricPrompt` implementation (51 LOC) |
| `ui/journal/JournalViewModel.kt`, secret-mode call sites in `ui/home`, `ui/calendar`, `ui/search`, `ui/filteredtasks`, `ui/settings` | Every caller you must update |
| `domain/usecase/security/IsSecretModeActiveUseCase.kt`, `OnSecretModeEventUseCase.kt` | Domain-side consumers |

## 4. Target

- `domain/repository/AlarmSoundPreferences.kt` — `Uri` → `String` sound id
- `domain/model/AlarmSoundId.kt` *(new)* — a small value type, or a typealias plus constants
- `data/repository/AlarmSoundPreferencesImpl.kt` — resolves id ↔ `Uri` on the Android side
- `common/RingtoneHolder.kt` — accepts an id, resolves internally
- `domain/security/Authenticator.kt` — `suspend fun authenticate(reason: String): AuthOutcome`
- `ui/security/biometric/BiometricAuthenticator.kt` — implements the new signature, resolves the Activity itself
- Every call site of both interfaces

## 5. Steps

1. **Confirm the leak set is still exactly two files.**
   ```bash
   grep -rnE '^import (android|androidx)\.' app/src/main/java/com/todoapp/mobile/domain/
   ```
   `androidx.compose.runtime.Immutable` on model classes is **fine** — the Compose runtime is multiplatform. Only `android.*` and Android-only `androidx.*` count.

2. **Alarm sound: define the identifier.** A sound is either a system ringtone or a bundled asset. Model it as an opaque `String` id that the platform layer resolves. On Android an id maps to a `RingtoneManager` URI; on iOS it maps to a bundled `.caf` filename.

3. **Change the interface** to store and observe `String?` instead of `Uri?`. Null keeps meaning "system default."

4. **Move URI resolution into the data layer.** `AlarmSoundPreferencesImpl` and `RingtoneHolder` own the `Uri` type from here on. The domain never sees it.

5. **Update `ui/alarmsounds/`** to work in ids. The picker still enumerates system ringtones on Android — that enumeration is platform code and belongs behind a catalog interface (fully specified later in `30-PLATFORM/15`; for now a plain Android implementation is correct).

6. **Authenticator: remove the Activity.** Replace the `FragmentActivity` parameter with a suspend function returning a result type. The Android implementation resolves the current Activity itself.

7. **Update every caller** — journal lock and the five secret-mode surfaces. The call becomes an ordinary suspend call inside a coroutine rather than something that must be handed an Activity.

8. **Run the gate.** `MainViewModelLogoutTest` and any biometric-adjacent test must pass unchanged.

9. **Verify the leak is closed** with the grep from step 1 — it must return nothing.

## 6. Code skeleton

```kotlin
// domain/repository/AlarmSoundPreferences.kt
//
// Was: Uri?. A content URI is an Android concept; the domain only needs to know
// *which* sound, not how the platform addresses it. Android resolves the id to a
// RingtoneManager URI; iOS resolves it to a bundled .caf filename.
interface AlarmSoundPreferences {
    fun observeAlarmSoundId(): Flow<String?>      // null = system default
    suspend fun setAlarmSoundId(id: String?)
}
```

```kotlin
// domain/security/Authenticator.kt
//
// Was: authenticate(activity: FragmentActivity, ...). Holding an Activity in a domain
// signature forces every caller to thread one through, and is meaningless off-Android.
// The platform implementation resolves its own presentation context.
enum class AuthOutcome { SUCCESS, FAILED, CANCELLED, UNAVAILABLE, LOCKED_OUT }

interface Authenticator {
    suspend fun authenticate(reason: String): AuthOutcome
    fun availability(): BiometricAvailability
}
```

```kotlin
// ui/security/biometric/BiometricAuthenticator.kt (Android impl)
class BiometricAuthenticator @Inject constructor(
    private val activityProvider: () -> FragmentActivity?,   // supplied by DI
) : Authenticator {
    override suspend fun authenticate(reason: String): AuthOutcome =
        suspendCancellableCoroutine { cont ->
            val activity = activityProvider() ?: return@suspendCancellableCoroutine
                cont.resume(AuthOutcome.UNAVAILABLE)
            // BiometricPrompt callbacks → AuthOutcome. Resume exactly once.
        }
}
```

## 7. Acceptance

- [ ] `grep -rnE '^import (android|androidx)\.' app/…/domain/` returns **nothing** except `androidx.compose.runtime.Immutable`
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] Alarm sound selection round-trips: pick a sound, kill the app, reopen, the same sound is selected
- [ ] An alarm actually plays the selected sound (manual, on a device or emulator)
- [ ] Journal biometric lock still gates entry
- [ ] Secret mode still authenticates on all five surfaces (home, calendar, search, filtered tasks, settings)
- [ ] A user who had a sound selected before the change still has it selected after (migration verified — see Pitfalls)
- [ ] No behavioural change to `MainViewModelLogoutTest`

## 8. Pitfalls

- **This changes a persisted value's type.** Existing installs have a `Uri` string on disk. Decide explicitly: either the id format *is* the stored URI string (zero migration, slightly leaky abstraction) or you write a one-shot migration. **The zero-migration option is recommended** — treat the URI string as an opaque id on Android and let the iOS implementation use its own namespace. Record the choice in `DECISIONS.md`. Silently changing the format orphans every user's sound selection.
- **`suspendCancellableCoroutine` must resume exactly once.** `BiometricPrompt` can fire both an error callback and a cancellation. Guard with `cont.isActive`, or the app crashes with `IllegalStateException: Already resumed`.
- **`BiometricPrompt` requires a `FragmentActivity`.** `MainActivity` already extends it — do not "simplify" it to `ComponentActivity`.
- **Do not store the Activity.** Hold a provider (`() -> FragmentActivity?`) that reads the current one. A stored Activity leaks across configuration changes — an explicit anti-pattern in `CLAUDE.md`.
- **Do not build the full platform contract here.** `30-PLATFORM/06` and `30-PLATFORM/15` specify the final shapes. This task does the minimum to de-Android the domain; over-engineering now means rework later.
- **The alarm-sound picker enumerates system ringtones.** That has no iOS equivalent (bundled sounds only). Do not try to solve that here — it is a `30-PLATFORM/15` concern with a documented degradation.

## 9. Verification

```bash
# 1. The leak is closed
grep -rnE '^import (android|androidx)\.' app/src/main/java/com/todoapp/mobile/domain/ \
  | grep -v 'androidx.compose.runtime.Immutable' \
  && echo "STILL LEAKING" || echo "clean"

# 2. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Manual, on a device
#    Settings → Alarm Sounds: pick a non-default sound, force-stop, reopen → still selected
#    Create a task with a reminder 2 minutes out → the chosen sound plays
#    Journal → biometric prompt appears and gates entry
#    Secret mode → authenticates from home, calendar, search, filtered tasks, settings
```
