---
id: 20-01
title: Remove genuinely-dead deps; introduce `detektAll`
layer: foundation
status: TODO
depends_on: [10-00, 20-00]
blocks: [20-02]
parallel_safe: false
estimate: 6h
reversible: true
owner_files:
  - gradle/libs.versions.toml
  - app/build.gradle.kts
  - build.gradle.kts
  - .github/workflows/ci.yml
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Two unrelated pieces of housekeeping that must both happen **before the first KMP module exists**:

1. Remove the one dependency that is genuinely unused, and demote two explicit declarations that are redundant.
2. Replace `detektMain` with an aggregate `detektAll` task so static analysis does not silently stop covering modules as they become KMP.

## 2. Why this way

**The `detektAll` change is the important half, and it is urgent.** `detektMain` is an AGP *variant* task. It exists because `:app` and `:uikit` are Android modules. The moment a module becomes KMP it produces `detektMetadataMain` / `detektAndroidDebug` instead — and `./gradlew detektMain` **keeps exiting 0 while checking nothing in that module**. A quality gate that silently stops checking 16,400 lines is worse than one that fails loudly. Fix it while there is still exactly one shape of module, so the "before" and "after" are comparable.

**The dependency half is smaller than it first appears — this has been measured, not assumed.** `com.google.android.libraries.places:places:3.5.0` transitively depends on `play-services-location:21.0.1` and `play-services-maps:17.0.0`. Removing those two *explicit* declarations therefore does **not** remove them from the build; Places drags them in regardless. Only `maps-compose` is genuinely severable, and R8 already strips unreferenced code, so the realistic AAB saving is small — possibly near zero.

This matters for planning: **do not count on banking headroom here.** The size ledger in `00-CONTEXT/04-constraints.md` assumed −0.2…−0.6 MiB from this task; that assumption is wrong. Record the measured number and let the ledger reflect reality. It makes the deliberate ceiling raise in `20-09` more necessary, not less.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `app/build.gradle.kts` lines ~320-324 | The four Maps/Places declarations |
| `gradle/libs.versions.toml` lines ~151-155 | Their catalog entries and `[versions]` refs |
| `build.gradle.kts` (root) | Where the detekt plugin is configured; the aggregate task goes here |
| `.github/workflows/ci.yml` (~line 50) | The `detektMain` invocation |
| `app/detekt.yml`, `uikit/detekt.yml`, `config/baseline.xml` | Per-module config — `detektAll` must keep honouring these |
| `app/src/main/AndroidManifest.xml` lines 21-25, 183 | The `tools:node="remove"` location permissions and the `com.google.android.geo.API_KEY` meta-data |
| `app/…/di/PlacesModule.kt`, `data/location/PlaceSearchRepositoryImpl.kt` | Proof that `google-places` **is** used and must stay |

## 4. Target

- `app/build.gradle.kts` — remove `maps-compose`; optionally demote the two transitive declarations
- `gradle/libs.versions.toml` — remove the corresponding entries and now-unused `[versions]` refs
- `build.gradle.kts` (root) — add the `detektAll` aggregate task
- `.github/workflows/ci.yml` — `detektMain` → `detektAll`

## 5. Steps

1. **Confirm the usage picture yourself.** Do not trust this file; the codebase may have moved.
   ```bash
   grep -rn --include="*.kt" -E "com\.google\.android\.gms\.maps|com\.google\.maps\.android|GoogleMap|LocationServices|FusedLocation|CameraPositionState" app/src uikit/src | grep -v LicensesScreen
   ```
   Expected: no results. If there *are* results, stop — the dependency is live and this step is void.

2. **Record the baseline size** before touching anything:
   ```bash
   ./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab
   ```

3. **Remove `maps-compose`** from `app/build.gradle.kts` and its catalog entry + `mapsCompose` version ref. This is the only genuinely-unused artifact.

4. **Decide on the two transitive declarations.** `play-services-maps` and `play-services-location` arrive via `google-places` regardless. Removing the explicit lines is honest hygiene — the app does not use them directly — but it is **not** a size change, and it means the versions are no longer pinned by this project. Recommended: remove them and let Places pin them, since the app never calls either API. Record the choice in `DECISIONS.md`.

5. **Keep `google-places`.** It backs the location picker (`PlaceSearchRepositoryImpl`, `PlacesModule`, `Application.initializePlacesSdk()`).

6. **Keep the manifest as-is.** The `tools:node="remove"` entries for `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` are load-bearing — Places injects those permissions and removing the removal would change the Play Data Safety declaration. Leave `com.google.android.geo.API_KEY` in place.

7. **Measure again and record the delta** in the `PROGRESS.md` AAB ledger. Expect a small number. Report what you measured, not what was predicted.

8. **Add the `detektAll` aggregate task** to the root build file (skeleton below).

9. **Verify `detektAll` actually covers both modules today** — compare its task graph against `detektMain`:
   ```bash
   ./gradlew detektAll --dry-run
   ```
   Both `:app` and `:uikit` detekt tasks must appear.

10. **Update CI** — replace `detektMain` with `detektAll` in `.github/workflows/ci.yml`. Note the `gh` token lacks the `workflow` scope, so edit the file locally and push over SSH; do not use the API.

11. **Run the full gate with the new task name.**

## 6. Code skeleton

```kotlin
// build.gradle.kts (root)
// Aggregate detekt across every module and every KMP source set.
//
// `detektMain` is an AGP *variant* task: it exists only for Android modules. As modules
// become KMP they produce detektMetadataMain / detektAndroidDebug / detektIosArm64Main
// instead, and `detektMain` keeps exiting 0 while checking nothing in them. This task
// binds to the Detekt *type*, so new source sets are covered automatically.
tasks.register("detektAll") {
    group = "verification"
    description = "Runs every Detekt task with type resolution across all projects."
    dependsOn(
        subprojects.flatMap { project ->
            project.tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class.java)
                .matching { it.name != "detekt" }   // the typeless variant adds nothing
        },
    )
}
```

```diff
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
-        ./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug --continue --max-workers=2 --stacktrace
+        ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug --continue --max-workers=2 --stacktrace
```

```diff
--- a/app/build.gradle.kts
+++ b/app/build.gradle.kts
     // Google Places (location picker — text search only, no map surface)
-    implementation(libs.play.services.maps)
-    implementation(libs.play.services.location)
     implementation(libs.google.places)
-    implementation(libs.maps.compose)
```

## 7. Acceptance

- [ ] `./gradlew detektAll --dry-run` lists detekt tasks for **both** `:app` and `:uikit`
- [ ] `detektAll` reports the same finding count as `detektMain` did before this change (baseline is zero findings)
- [ ] `.github/workflows/ci.yml` invokes `detektAll`
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] `maps-compose` removed from both `app/build.gradle.kts` and the catalog; no orphan `[versions]` entry left
- [ ] `google-places` still present; the location picker still builds
- [ ] Manifest unchanged — the two `tools:node="remove"` permissions and the geo API key meta-data are intact
- [ ] Measured AAB delta recorded in `PROGRESS.md`, with the honest number even if it is ~0
- [ ] The `DECISIONS.md` entry records whether the transitive declarations were removed

## 8. Pitfalls

- **The size win is small — do not fabricate it.** Two of the three "dead" artifacts are transitive via Places and will still be in the bundle. If the measurement shows ~0, write ~0.
- **Do not remove `google-places`.** It is live. `PlaceSearchRepositoryImpl` and `PlacesModule` depend on it.
- **Do not remove the manifest permission removals.** They are what keeps the Play Data Safety declaration free of location claims. `CLAUDE.md` calls these out explicitly as not-cleanup.
- **Do not delete `LicensesScreen.kt`'s Maps license text** on the grounds that the dependency is gone — Places still ships Google code and the attribution is still required.
- **`detektAll` must not become `detekt`.** The typeless `detekt` task runs without type resolution and misses the rules this project relies on. The `matching { it.name != "detekt" }` filter is deliberate.
- **Per-module configs must keep applying.** `app/detekt.yml` and `uikit/detekt.yml` differ. Depending on the tasks (rather than reimplementing them) preserves that automatically — do not "simplify" it into a single hand-rolled invocation.
- **Detekt baselines.** `config/baseline.xml` exists per module. If `detektAll` suddenly reports findings, check that baselines are still being picked up before "fixing" the code.

## 9. Verification

```bash
# 1. Aggregate covers every module
./gradlew detektAll --dry-run | grep -E ':(app|uikit):detekt'

# 2. Full gate with the new task name
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Size — record the real number
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. Places is still wired
grep -rn "libs.google.places" app/build.gradle.kts
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c "libraries.places"
```
