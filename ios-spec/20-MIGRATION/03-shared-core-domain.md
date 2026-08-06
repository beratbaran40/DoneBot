---
id: 20-03
title: Create `:shared:core` + `:shared:domain` (androidTarget only)
layer: domain
status: TODO
depends_on: [10-02, 20-02]
blocks: [20-04, 20-05, 20-09]
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
  - "! grep -rqE '^import (java|javax|android)\\.' shared/domain/src/commonMain shared/core/src/commonMain"
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

6. **Move the tests.** Domain and common tests go to the new modules' `commonTest`. They currently use JUnit4 + MockK; in `commonTest` they need the multiplatform test API. **If a test cannot move cleanly, leave it in `app/src/test` for now** and note it — a test in the wrong module is a smaller problem than a broken test.

7. **Copy detekt configs** into the new modules and confirm `detektAll` picks them up:
   ```bash
   ./gradlew detektAll --dry-run | grep -E ':shared:(core|domain):detekt'
   ```

8. **Verify commonMain purity** — the ratchet gate:
   ```bash
   ! grep -rqE '^import (java|javax|android)\.' shared/domain/src/commonMain shared/core/src/commonMain
   ```
   **This will fail**, because `java.time` is still everywhere. That is expected and correct: `20-04` fixes it. Record which files still import `java.*` — that list *is* `20-04`'s work list.

   > If a file has *other* JVM dependencies beyond `java.time`, move it to `androidMain` and record why. Do not force it.

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
- [ ] All domain/common tests still run and pass, wherever they now live
- [ ] The list of files still importing `java.*` is recorded in the task notes as `20-04`'s work list
- [ ] No module declares an iOS target

## 8. Pitfalls

- **Do not rename packages.** `com.todoapp.mobile.domain.*` stays. The Gradle module path and the Kotlin package are independent, and coupling them here costs a 500-file diff for zero benefit.
- **Do not use `implementation` for `:shared:core` inside `:shared:domain`.** Use `api` — domain types appear in signatures `:app` consumes, so the dependency must be transitive.
- **`git mv`, not delete-and-create.** Otherwise history is lost and every moved file reviews as new.
- **Move first, edit later, in separate commits.** A moved-and-edited file shows as delete+add and the real change becomes invisible.
- **The purity gate will fail on `java.time` — that is the plan.** Do not "fix" it by moving 60 domain files into `androidMain`; that defeats the ratchet. Leave them in `commonMain`, let the gate stay red for this one metric, and let `20-04` close it. Record the failure explicitly so the next session knows it is expected.
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

# 5. The ratchet — expected to fail on java.time only, and that list is 20-04's input
grep -rhoE '^import java\.[a-z.]+' shared/domain/src/commonMain shared/core/src/commonMain | sort | uniq -c | sort -rn

# 6. History survived the move
git log --follow --oneline shared/domain/src/commonMain/kotlin/com/todoapp/mobile/domain/model/Recurrence.kt | head -5
```
