---
id: 10-02
title: Gradle / KMP plugin setup
layer: foundation
status: TODO
depends_on: [10-00]
blocks: [20-03]
parallel_safe: false
estimate: 8h
reversible: true
owner_files:
  - gradle/libs.versions.toml
  - build.gradle.kts
  - settings.gradle.kts
  - gradle.properties
verify:
  - ./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Add the Kotlin Multiplatform and Compose Multiplatform plugins to the version catalog and register them (without applying them) in the root build file, so that `20-03` can create the first shared module. **No module becomes KMP in this task.** Android output must be bit-for-bit equivalent to before.

## 2. Why this way

**Plugins are declared here and applied later, deliberately.** Declaring a plugin in the catalog and the root `plugins { … apply false }` block changes nothing about how anything compiles. It means the *next* task is a two-line change to one module rather than a change to four files at once. When something breaks in `20-03`, the cause is unambiguous.

**Version alignment is the whole risk in this task.** Kotlin, KSP, Compose Multiplatform and the Compose compiler must agree, and this project is already pinned to a specific Kotlin (2.2.21) with a matching KSP (`2.2.21-2.0.4`). Picking a CMP version that expects a different Kotlin produces errors that look like unrelated compilation failures. Resolve the alignment here, once, while nothing else is moving.

**The version catalog is the single source of truth.** This project already keeps every coordinate in `gradle/libs.versions.toml`. Adding versions anywhere else fragments it.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `gradle/libs.versions.toml` | Existing `[versions]`, `[libraries]`, `[plugins]` structure and naming convention. Match it exactly. Note `kotlin = "2.2.21"`, `ksp = "2.2.21-2.0.4"`, `agp = "8.13.1"`, `composeBom = "2025.12.00"`. |
| `build.gradle.kts` (root) | The `plugins { … apply false }` block. You are appending, not restructuring. |
| `settings.gradle.kts` | `pluginManagement` and `dependencyResolutionManagement` repositories. CMP artifacts need `google()` and `mavenCentral()`, both already present. |
| `gradle.properties` | Existing heap settings and feature flags. |
| `/CLAUDE.md` § "Modules & Build Config" | The stated build facts, so you can tell what must not change. |

## 4. Target

- `gradle/libs.versions.toml` — add versions, plugin aliases, and the KMP-capable library coordinates that `20-03` onward will consume
- `build.gradle.kts` (root) — register the new plugins with `apply false`
- `gradle.properties` — add the KMP/CMP flags
- `settings.gradle.kts` — verify repositories only; change nothing unless something is missing

## 5. Steps

1. **Pin the versions.** Choose the Compose Multiplatform release whose bundled Compose compiler matches Kotlin 2.2.21. Verify the pairing on the JetBrains compatibility page before writing it down — do not guess. If Kotlin must be bumped to satisfy CMP, that is a separate decision: record it in `DECISIONS.md` and treat the bump as its own step with a full gate run.

2. **Add to `[versions]`** in `gradle/libs.versions.toml`: `composeMultiplatform`, `kotlinxDatetime` (0.7.x — 0.6 lacks `YearMonth`, which this codebase uses in 15 places), `ktor`, `koin`, `coil3`, `lifecycleKmp`, `navigationKmp`.

3. **Add to `[plugins]`**: `kotlinMultiplatform`, `composeMultiplatform`, `composeCompiler`, and (for `20-07`) `androidxRoom`.

4. **Add to `[libraries]`** the coordinates later tasks consume. Do not remove anything — the Android-only coordinates stay until the task that actually replaces them.

5. **Register in the root `build.gradle.kts`** with `apply false`.

6. **Add the KMP flags** to `gradle.properties`.

7. **Run the full gate.** Nothing should change. If `assembleDebug` output differs in size or content, something was applied that should not have been.

## 6. Code skeleton

```toml
# gradle/libs.versions.toml — [versions], appended
composeMultiplatform = "<the release matching Kotlin 2.2.21 — verify, do not guess>"
kotlinxDatetime      = "0.7.1"    # 0.7+ required: YearMonth is used in 15 files
ktor                 = "3.x.x"
koin                 = "4.x.x"
coil3                = "3.x.x"
lifecycleKmp         = "2.10.0"   # androidx.lifecycle publishes KMP artifacts
navigationKmp        = "<org.jetbrains.androidx.navigation release>"

# [plugins], appended
kotlinMultiplatform  = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose",              version.ref = "composeMultiplatform" }
composeCompiler      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
androidxRoom         = { id = "androidx.room",                       version.ref = "room" }
```

```kotlin
// build.gradle.kts (root) — appended to the existing plugins block
plugins {
    // … existing entries unchanged …
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidxRoom) apply false
}
```

```properties
# gradle.properties — appended

# Kotlin Multiplatform
kotlin.mpp.enableCInteropCommonization=true
# Native debug builds link far faster with static caches.
kotlin.native.cacheKind=static

# Compose Multiplatform resources: generate the accessor object per module.
org.jetbrains.compose.experimental.uikit.enabled=true
```

## 7. Acceptance

- [ ] `./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug` passes
- [ ] `./gradlew :app:bundleRelease` produces an AAB within **±50 KiB** of the baseline recorded in `10-00`
- [ ] `git diff` touches only the four files in `owner_files`
- [ ] No module's `build.gradle.kts` was modified
- [ ] `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` shows no new dependency
- [ ] The chosen CMP ↔ Kotlin pairing is recorded in `DECISIONS.md` with the source consulted

## 8. Pitfalls

- **Do not apply any plugin to any module in this task.** `apply false` is the entire point. Applying KMP to `:uikit` here turns a clean two-line next task into a debugging session.
- **Do not touch `app/build.gradle.kts`.** Per decision D-03, `:app` stays an AGP application module and never gets the KMP plugin. If you find yourself editing it, stop.
- **Kotlin/KSP/CMP version alignment is not optional.** A mismatched Compose compiler produces errors pointing at application code rather than at the version mismatch. Verify the pairing against JetBrains' published compatibility table.
- **Do not remove Android-only coordinates yet.** Retrofit, Hilt and Coil 2 stay in the catalog until the tasks that replace them (`20-05`, `20-06`, `20-11`). Removing them early breaks `:app` immediately.
- **`kotlin.native.cacheKind=static` matters more than it looks.** Without it, debug Kotlin/Native link times on a 90k-LOC codebase become the dominant cost of every iOS iteration.
- **Configuration cache.** This project has it enabled. Adding plugins invalidates it once; that is expected. A *persistent* configuration-cache failure is a real problem — investigate rather than disabling the cache.
- **Do not add `iosX64`** anywhere, ever (decision D-08). No target declarations belong in this task at all.

## 9. Verification

```bash
# 1. Full gate — nothing should change
./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug

# 2. Size parity against the 10-00 baseline
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 3. No dependency graph change
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > /tmp/deps-after.txt
# diff against a copy taken before this task

# 4. Plugins resolve without being applied
./gradlew help
```
