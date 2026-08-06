---
id: 20-13
title: Declare iOS targets
layer: foundation
status: TODO
depends_on: [20-12]
blocks: [10-03, 30-*]
parallel_safe: false
estimate: 40h
reversible: true
owner_files:
  - shared/**/build.gradle.kts
  - uikit/build.gradle.kts
  - composeApp/build.gradle.kts
  - gradle/libs.versions.toml
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
---

## 1. Goal

Add `iosArm64` and `iosSimulatorArm64` to every shared module, produce the `DoneBotKit` framework, and fix the compile errors that only appear once a non-JVM target exists. Android must stay green throughout.

## 2. Why this way

**This is the moment the ratchet pays off.** Everything already compiles as KMP-shaped Android with a pure `commonMain`. Turning on iOS is one block per module. The errors that surface now are exactly the ones that *could only* surface now — code that is JVM-specific in ways the Android target accepted silently.

Expect these categories:
- `@Serializable` edge cases where a JVM-only type slipped into a DTO
- `synchronized`, `ThreadLocal`, `@Volatile` — JVM concurrency primitives with different Kotlin/Native forms
- `java.util.*` leftovers the `java.time` sweep did not cover (`UUID`, `Base64`, `Locale`)
- Reflection or `Class<*>` references

**`iosX64` is deliberately not declared** (decision D-08). The dev machine is Apple Silicon and Xcode 27 drops Intel; declaring it adds ~33% to Kotlin/Native build work for a target nobody will run.

**Build times become the dominant cost from here.** `isStatic = true`, `kotlin.native.cacheKind=static` for debug, per-module klib caching from the 8-module split, and **iOS stays out of the PR CI job** — a nightly `macos-15` run only, because GitHub bills macOS minutes at 10×.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `shared/*/build.gradle.kts`, `uikit/build.gradle.kts`, `composeApp/build.gradle.kts` | The `kotlin { androidTarget() }` blocks you are extending |
| `gradle.properties` | `kotlin.native.cacheKind` from `10-02` |
| `shared/data/src/commonMain/…/model/network/` | DTOs — the most likely `@Serializable` failures |
| Any `synchronized` / `@Volatile` / `ThreadLocal` in `commonMain` | `grep -rn "synchronized\|@Volatile\|ThreadLocal" shared/*/src/commonMain` |
| `data/engine/PomodoroEngineImpl.kt` | A singleton `CoroutineScope` with a `shutdown()` path — concurrency worth re-reading under Kotlin/Native |
| `.github/workflows/ci.yml` | Where the nightly iOS job goes |

## 4. Target

- Every shared module gains `iosArm64()` + `iosSimulatorArm64()`
- `composeApp` declares the `DoneBotKit` framework
- `libs.versions.toml` gains `sqlite-bundled` for iOS only
- `.github/workflows/ci.yml` gains a nightly iOS job

## 5. Steps

1. **Turn on targets bottom-up**, one module at a time, gate green between each:
   `:shared:core` → `:shared:domain` → `:shared:resources` → `:shared:data` → `:uikit` → `:shared:ui` → `:composeApp`.
   Fix each module's errors before moving up. A wall of 400 errors from turning everything on at once is not debuggable.

2. **Declare the framework** on `:composeApp` with `baseName = "DoneBotKit"`, `isStatic = true`.

3. **Wire the iOS SQLite driver.** `sqlite-bundled` goes on the **iOS source set only** — never Android (that is the +3–4 MiB trap from `20-07`).

4. **Fix the common failures:**

   | Symptom | Fix |
   |---|---|
   | `java.util.UUID` | `kotlin.uuid.Uuid` (stdlib) |
   | `android.util.Base64` / `java.util.Base64` | `kotlin.io.encoding.Base64` (stdlib) |
   | `synchronized(lock) { }` | `kotlinx.atomicfu`, or a `Mutex` if the block suspends |
   | `@Volatile` | `kotlin.concurrent.Volatile` |
   | `ThreadLocal` | usually removable; otherwise `kotlin.native.concurrent` equivalents |
   | `java.util.Locale` | the `AppLocale` type from `20-04` |
   | `Class<*>` / reflection | remove; Kotlin/Native reflection is limited |

5. **Provide the `actual` declarations** that only had Android implementations: `AppDatabaseConstructor`, `dataStorePath()`, `PlatformFormatting`, `SystemBarsEffect`. **Minimal stubs are correct here** — `30-PLATFORM` implements them properly. A stub that throws `NotImplementedError` with a clear message is better than a wrong implementation.

6. **Link the framework:**
   ```bash
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   ```

7. **Add the nightly iOS CI job** on `macos-15`, scheduled — not on pull requests.

8. **Re-run the full Android gate.** Turning on iOS targets must not change Android output.

## 6. Code skeleton

```kotlin
// Every shared module — the same addition
kotlin {
    androidTarget()
    // iosX64 is deliberately absent: Apple Silicon only, and Xcode 27 drops Intel.
    // Declaring it would add ~33% to Kotlin/Native build work for nothing.
    iosArm64()
    iosSimulatorArm64()
}
```

```kotlin
// composeApp/build.gradle.kts
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "DoneBotKit"
            isStatic = true          // dynamic frameworks link far slower here
        }
    }
    sourceSets {
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite.bundled)   // iOS ONLY — never Android
        }
    }
}
```

```kotlin
// shared/data/src/iosMain/…/AppDatabase.ios.kt
actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase = TODO("Implemented in 30-PLATFORM")
}
```

```yaml
# .github/workflows/ios-nightly.yml — NOT on pull_request; macOS minutes bill at 10x
name: iOS nightly
on:
  schedule: [{ cron: '0 3 * * *' }]
  workflow_dispatch:
jobs:
  link:
    runs-on: macos-15
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## 7. Acceptance

- [ ] Every shared module declares `iosArm64` + `iosSimulatorArm64`; **none declares `iosX64`**
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` succeeds
- [ ] `DoneBotKit.framework` is produced
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` still passes
- [ ] `:app:bundleRelease` unchanged in size (±50 KiB) — Android must not be affected
- [ ] `sqlite-bundled` appears **only** on iOS source sets
- [ ] Every `expect` has an iOS `actual`, even if it is an explicit `TODO` stub
- [ ] No `synchronized` / `@Volatile` / `ThreadLocal` / `java.util.*` left in any `commonMain`
- [ ] Nightly iOS CI job added and **not** attached to `pull_request`
- [ ] Link time recorded in `PROGRESS.md` — the baseline for future regressions

## 8. Pitfalls

- **Do not turn on all targets at once.** Bottom-up, module by module. Hundreds of simultaneous errors across seven modules cannot be triaged.
- **`sqlite-bundled` on Android is +3–4 MiB.** iOS source set only.
- **Stub `actual`s should fail loudly.** `TODO("Implemented in 30-PLATFORM/xx")` beats a plausible-looking wrong implementation that silently misbehaves for weeks.
- **Do not put iOS builds on the PR CI job.** GitHub bills macOS runners at 10× and Kotlin/Native links are slow. Nightly is enough at this stage.
- **`isStatic = true`.** Dynamic frameworks link much slower and gain nothing here.
- **Kotlin/Native has no `java.util.concurrent`.** Concurrency in common code must use coroutines or atomicfu. `PomodoroEngineImpl`'s singleton scope is worth re-reading under this constraint.
- **Kotlin/Native reflection is limited.** Anything relying on `Class<*>` or runtime reflection has to go.
- **Expect a long first link.** Kotlin/Native has to build the full dependency graph once. Subsequent incremental links with `cacheKind=static` are far faster.
- **This task does not make the app run.** It makes it *link*. Running on a simulator needs `10-03`'s Xcode project. Do not conflate the two.

## 9. Verification

```bash
# 1. The framework links
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
ls -la composeApp/build/bin/iosSimulatorArm64/debugFramework/

# 2. Android is unaffected
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 3. No iosX64 anywhere
grep -rn "iosX64" --include="*.kts" . && echo "IOSX64 DECLARED" || echo "clean"

# 4. Bundled SQLite is iOS-only
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -i "sqlite-bundled" \
  && echo "SIZE TRAP" || echo "clean"

# 5. No JVM-only primitives in common code
grep -rn "synchronized\|@Volatile\|ThreadLocal\|java\.util\." shared/*/src/commonMain uikit/src/commonMain \
  && echo "JVM PRIMITIVES REMAIN" || echo "clean"
```
