---
id: 20-08
title: DataStore → KMP factory
layer: data
status: TODO
depends_on: [20-05]
blocks: [20-11]
parallel_safe: false
estimate: 8h
reversible: true
owner_files:
  - app/src/main/java/com/todoapp/mobile/di/**
  - app/src/main/java/com/todoapp/mobile/data/repository/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Construct the Preferences DataStore through the multiplatform factory instead of the Android `preferencesDataStore` delegate, **writing to exactly the same file on disk** so no user data moves.

## 2. Why this way

`androidx.datastore:datastore-preferences-core` is already multiplatform — this is one of the cheapest wins in the migration. Only construction is Android-specific; the 19 preference repositories built on top need no changes at all.

**The file path is the whole risk.** The `preferencesDataStore(name = "user_prefs")` delegate resolves to `context.filesDir/datastore/user_prefs.preferences_pb`. The KMP factory takes an explicit path. Getting it wrong does not fail loudly — it creates a *new, empty* store, and every user silently appears logged out with default settings.

`EncryptedSharedPreferences` stays Android-only. It is used for the legacy token-migration path in `SessionPreferencesImpl`, plus `FCMTokenPreferencesImpl` and `LanguageRepositoryImpl`. All are already behind interfaces, so iOS supplies Keychain-backed implementations later (`30-PLATFORM/06`).

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `di/LocalStorageModule.kt` (~line 45, now a Koin module) | `preferencesDataStore(name = "user_prefs")` and the `EncryptedSharedPreferences` construction with its corruption-recovery path (deletes the master key entry and retries) |
| `data/repository/DataStoreHelper.kt` | The shared accessor the 19 preference repos use |
| `data/repository/SessionPreferencesImpl.kt` | Keys `session_access_token`, `session_refresh_token`, `session_expires_at`; the V1/V2 one-shot migrations and `ORPHAN_TOKEN_KEYS` |
| The other 18 `*PreferencesImpl.kt` in `data/repository/` | Confirm none of them construct a DataStore themselves |

## 4. Target

- `di/` Koin module — `PreferenceDataStoreFactory.createWithPath { … }`
- `data/repository/DataStorePath.kt` *(new)* — `expect fun dataStorePath(): String`, Android actual returns the delegate's exact path
- No changes to any of the 19 preference repositories

## 5. Steps

1. **Establish the current path empirically**, on a device with the app installed:
   ```bash
   adb shell run-as com.todoapp.mobile.debug ls -l files/datastore/
   ```
   Expect `user_prefs.preferences_pb`. Write down the full path.

2. **Add the `expect fun`** and its Android actual returning `context.filesDir.resolve("datastore/user_prefs.preferences_pb").absolutePath`.

3. **Replace the delegate** with `PreferenceDataStoreFactory.createWithPath { dataStorePath().toPath() }` in the Koin definition. Keep it `single` — two DataStore instances on one file corrupt each other.

4. **Leave `EncryptedSharedPreferences` alone.** Including its corruption-recovery path, which is real behaviour recovering from a known AEAD failure mode.

5. **Verify data survival by upgrade, not by unit test.** Install the previous build, log in, change the theme and palette, then install this build over it. The session, theme and palette must all survive.

6. Run the full gate.

## 6. Code skeleton

```kotlin
// data/repository/DataStorePath.kt
// The KMP factory takes an explicit path. This MUST resolve to exactly what
// preferencesDataStore(name = "user_prefs") produced, or every user silently
// starts with an empty store: logged out, default theme, palette reset.
expect fun dataStorePath(): String

// androidMain
actual fun dataStorePath(): String =
    appContext.filesDir.resolve("datastore/user_prefs.preferences_pb").absolutePath
```

```kotlin
// Koin — single, never factory: two instances over one file corrupt it.
single<DataStore<Preferences>> {
    PreferenceDataStoreFactory.createWithPath(produceFile = { dataStorePath().toPath() })
}
```

## 7. Acceptance

- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] Upgrade test: install previous build → log in → set theme + palette → install this build → **session, theme and palette survive**
- [ ] `adb shell run-as … ls files/datastore/` shows the same single `user_prefs.preferences_pb`, no second file
- [ ] The DataStore definition is `single`, not `factory`
- [ ] `EncryptedSharedPreferences` construction and its corruption-recovery path are unchanged
- [ ] All 19 preference repositories are untouched

## 8. Pitfalls

- **A wrong path fails silently.** No crash, no error — just an empty store. The upgrade test in step 5 is the only thing that catches it.
- **`single`, not `factory`.** Two DataStore instances over one file is a documented corruption path.
- **Do not "clean up" the token migrations.** `SessionPreferencesImpl` runs two one-shot migrations (pull leftovers out of `EncryptedSharedPreferences`; encrypt pre-existing plaintext) and clears `ORPHAN_TOKEN_KEYS`. Users mid-way through that history still exist.
- **Do not move `EncryptedSharedPreferences` to DataStore in this task.** Tempting, out of scope, and it would put token material through a different at-rest path without an audit.
- **`okio.Path` vs `java.io.File`.** The KMP factory takes an okio path. Convert at the boundary; do not leak `File` into common code.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# The real test — on a device, with the previous build installed first
adb shell run-as com.todoapp.mobile.debug ls -l files/datastore/
# install this build over it, relaunch, confirm: still logged in, theme + palette intact
```
