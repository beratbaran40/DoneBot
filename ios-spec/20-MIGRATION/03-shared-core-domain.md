---
id: 20-03
title: Create `:shared:core` + `:shared:domain` (androidTarget only)
layer: domain
status: TODO
depends_on: [10-02, 20-02]
blocks: [20-03b, 20-05, 20-09]
parallel_safe: false
estimate: 20h
reversible: true
owner_files:
  - settings.gradle.kts
  - shared/**
  - app/build.gradle.kts
  - app/src/main/java/com/todoapp/mobile/domain/**
  - app/src/main/java/com/todoapp/mobile/common/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  # Only the `android.` half of the purity gate is achievable here. `java.time` (20-04),
  # `javax.inject` (20-05) and `java.util.Locale` (20-13) are still expected in commonMain
  # after this task — see step 8. Asserting the full gate here would fail by design.
  - "! grep -rqE '^import android\\.' shared/domain/src/commonMain shared/core/src/commonMain"
---

## 1. Goal

Create the first two shared modules and move `domain/` and `common/` into them. Both declare **only** `androidTarget()`. Android output is unchanged; `:app` source is unchanged except for the deleted files.

This task sets the module pattern that eight later tasks copy. Getting it exactly right here is worth more than getting it fast.

## 2. Why this way

**Smallest, purest layer first.** `domain/` is 2,482 lines with no Android dependencies (after `20-02`) and no framework coupling. If the KMP module pattern is wrong — source-set layout, Gradle wiring, detekt/ktlint coverage, test placement — this is the cheapest possible place to find out.

**Package names do not change.** Files keep `com.todoapp.mobile.domain.*` and `com.todoapp.mobile.common.*`. Moving a module *and* renaming packages in one step turns a mechanical `git mv` into a 500-file diff where a real error is invisible. The move must produce **zero import changes in `:app`**.

**`androidTarget()` only.** A KMP module with a single Android target produces output equivalent to `com.android.library` — so this restructure is invisible to the Android build. iOS targets arrive in `20-13`, after everything compiles as KMP-shaped Android. See `20-00` §4.1.

**Two modules, not one.** `common/` holds cross-cutting helpers (`DomainException`, `ErrorMessages`, `DayModeCalculator`, `HeartsFormat`, `TimeFormat`) that the domain depends on. Keeping them separate means `:shared:core` has no dependencies at all, which makes it the natural place for anything a later module needs without pulling in domain types.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `settings.gradle.kts` | Current `include(...)` list and `rootProject.name` |
| `app/build.gradle.kts` | The dependency block `:app` will gain; the `kotlinOptions`/`compileOptions` values the new modules must match (Java 17) |
| `gradle/libs.versions.toml` | The plugin aliases registered in `10-02` |
| `uikit/build.gradle.kts` | An existing library module's shape — namespace, compileSdk, minSdk 24, detekt/ktlint wiring. Your new modules mirror this, plus KMP. |
| `app/src/main/java/com/todoapp/mobile/domain/` | 67 files. Note the subpackages: `model/`, `repository/`, `usecase/`, `alarm/`, `engine/`, `security/`, `analytics/`, `location/`, `ambience/`, `update/`, `constants/` |
| `app/src/main/java/com/todoapp/mobile/common/` | 12 files, 1,061 LOC |
| `app/src/test/java/com/todoapp/mobile/` | Which tests cover domain/common — they move too |
| `app/detekt.yml`, `uikit/detekt.yml` | New modules need their own detekt config |

## 4. Target

```
settings.gradle.kts                       include(":shared:core", ":shared:domain")
shared/core/build.gradle.kts              new
shared/core/src/commonMain/kotlin/…       ← app/…/common/
shared/core/src/commonTest/kotlin/…       ← the common tests
shared/domain/build.gradle.kts            new
shared/domain/src/commonMain/kotlin/…     ← app/…/domain/
shared/domain/src/commonTest/kotlin/…     ← the domain tests
shared/core/detekt.yml                    copy of app/detekt.yml
shared/domain/detekt.yml                  copy of app/detekt.yml
app/build.gradle.kts                      + implementation(projects.shared.domain)
```

## 5. Steps

1. **Enable type-safe project accessors** in `settings.gradle.kts` if not already on:
   ```kotlin
   enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
   ```
   Then `include(":shared:core")` and `include(":shared:domain")`.

2. **Write `shared/core/build.gradle.kts`** (skeleton below). Match `:app`'s Java 17 target and `:uikit`'s `minSdk 24`.

3. **Move `common/` with `git mv`**, file by file, into `shared/core/src/commonMain/kotlin/com/todoapp/mobile/common/`. Preserve the package declaration exactly.

4. **Wire `:app` to `:shared:core`** and run the gate. If any `:app` file needed an import change, you renamed something — undo it.

5. **Repeat for `:shared:domain`.** It depends on `:shared:core`. Move all 67 files preserving packages.

6. **Do not move the tests here.** Test migration is `20-03b`, its own task, and it is a bigger job than it looks: `org.junit.Assert` and Robolectric do not exist in `commonTest`, so every assertion has to be rewritten. Leave all 37 test files in `app/src/test` for now — they still compile and still run against the moved production code, because the packages did not change. Instead, **record which test files cover `domain/` and `common/`**; that list is `20-03b`'s work list.

7. **Copy detekt configs** into the new modules and confirm `detektAll` picks them up:
   ```bash
   ./gradlew detektAll --dry-run | grep -E ':shared:(core|domain):detekt'
   ```

8. **Verify commonMain purity — the achievable half.** This task's gate is the `android.` half only:
   ```bash
   ! grep -rqE '^import android\.' shared/domain/src/commonMain shared/core/src/commonMain
   ```
   That must pass; `20-02` cleared the two Android leaks, so anything appearing here is something you introduced.

   The **full** ratchet gate is the one in `20-00` §4.2:
   ```bash
   ! grep -rqE '^import (java|javax|android)\.' shared/*/src/commonMain
   ```
   **It will fail after this task, by design.** Three separate later tasks close it: `java.time` → `20-04`, `javax.inject` → `20-05`, `java.util.Locale` → `20-13`. Run it anyway, as a *survey*, and record the output — that list is the work list for those three tasks:
   ```bash
   grep -rhoE '^import (java|javax)\.[a-z.]+' shared/domain/src/commonMain shared/core/src/commonMain \
     | sort | uniq -c | sort -rn
   ```

   > If a file has JVM dependencies *beyond* those three known categories, move it to `androidMain` and record why. Do not force it.

9. **Run the full gate.** Then run `:app:bundleRelease` and confirm the size is within ±50 KiB of baseline.

## 6. Code skeleton

```kotlin
// shared/core/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    // androidTarget ONLY. iOS targets arrive in 20-13 — see 20-00 §4.1.
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.todoapp.mobile.core"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt { config.setFrom("$projectDir/detekt.yml") }
```

```kotlin
// shared/domain/build.gradle.kts — as above, plus:
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core)
            implementation(libs.kotlinx.coroutines.core)
            // Domain models carry @Immutable — the Compose runtime is multiplatform.
            implementation(libs.androidx.compose.runtime)
        }
    }
}
android { namespace = "com.todoapp.mobile.domain" }
```

```kotlin
// app/build.gradle.kts — added to dependencies
implementation(projects.shared.domain)   // transitively brings :shared:core via api()
```

## 7. Acceptance

- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] `:app:bundleRelease` within **±50 KiB** of the `20-01` measurement
- [ ] `app/src/main/java/com/todoapp/mobile/domain/` and `common/` no longer exist
- [ ] **Zero import changes in `:app`** — `git diff` shows only deletions there, no edits
- [ ] `git log --follow` works on a moved file (proves `git mv` was used)
- [ ] `detektAll --dry-run` lists `:shared:core` and `:shared:domain` detekt tasks
- [ ] `! grep -rqE '^import android\.' shared/*/src/commonMain` passes
- [ ] All 37 tests still run and pass, unmoved, from `app/src/test`
- [ ] The list of test files covering `domain/` and `common/` is recorded as `20-03b`'s work list
- [ ] The `java.*` / `javax.*` import survey is recorded, split by category (`java.time` → `20-04`, `javax.inject` → `20-05`, `java.util` → `20-13`)
- [ ] No module declares an iOS target

## 8. Pitfalls

- **Do not rename packages.** `com.todoapp.mobile.domain.*` stays. The Gradle module path and the Kotlin package are independent, and coupling them here costs a 500-file diff for zero benefit.
- **Do not use `implementation` for `:shared:core` inside `:shared:domain`.** Use `api` — domain types appear in signatures `:app` consumes, so the dependency must be transitive.
- **`git mv`, not delete-and-create.** Otherwise history is lost and every moved file reviews as new.
- **Move first, edit later, in separate commits.** A moved-and-edited file shows as delete+add and the real change becomes invisible.
- **The full purity gate fails after this task — that is the plan.** Do not "fix" it by moving 60 domain files into `androidMain`; that defeats the ratchet. Leave them in `commonMain` and let `20-04` / `20-05` / `20-13` close it. **This task's own `verify:` asserts only the `android.` half**, which is the part that must be true now — if you find yourself editing that command to make it pass, stop and re-read step 8.
- **Do not move the tests here.** It looks like a natural part of "move the layer", and it is not: `org.junit.Assert` and Robolectric have no `commonTest` equivalent, so it is an assertion-rewrite across 37 files, not a `git mv`. It has its own task (`20-03b`) with its own estimate. Bundling it here silently doubles this task and puts the regression shields — the safety net for the entire migration — at risk inside a task that was scoped as Gradle wiring.
- **`minSdk` differs between modules.** `:app` is 26, `:uikit` is 24. Pick 26 for shared modules unless a concrete reason says otherwise, and be consistent across all of them.
- **Detekt/ktlint do not auto-apply to new modules.** A module with no detekt config silently contributes nothing to `detektAll`. Step 7's check is not ceremonial.
- **Stale KSP/Hilt codegen.** Hilt still processes `:app`, and `@Inject` constructors now live in another module. If you get `NoSuchFile` or missing generated classes: `./gradlew --stop && ./gradlew :app:clean`. Known failure mode in this repo.

## 9. Verification

```bash
# 1. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 2. Size parity
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 3. :app was not edited, only reduced
git diff --stat v1.2-preKMP -- app/src/main/java/com/todoapp/mobile/ui/   # expect: no changes

# 4. Detekt covers the new modules
./gradlew detektAll --dry-run | grep -E ':shared:(core|domain):detekt'

# 5a. THE GATE for this task — the android. half must be clean
grep -rnE '^import android\.' shared/domain/src/commonMain shared/core/src/commonMain \
  && echo "ANDROID LEAK" || echo "clean"

# 5b. The full ratchet — expected to fail here; this survey IS the input for 20-04/20-05/20-13
grep -rhoE '^import (java|javax)\.[a-z.]+' shared/domain/src/commonMain shared/core/src/commonMain \
  | sort | uniq -c | sort -rn

# 6. History survived the move
git log --follow --oneline shared/domain/src/commonMain/kotlin/com/todoapp/mobile/domain/model/Recurrence.kt | head -5
```
