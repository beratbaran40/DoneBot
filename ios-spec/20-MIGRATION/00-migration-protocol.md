---
id: 20-00
title: Migration protocol (reference — no code)
layer: foundation
status: TODO
depends_on: []
blocks: [20-01]
parallel_safe: true
estimate: 1h (reading)
reversible: true
owner_files: []
verify:
  - "Read-only. Mark DONE once you have read it and step 0 (ship v1.2) is resolved."
---

## 1. Goal

Establish the rules that make a 500-hour in-place restructure of a **live, shipping** application safe. Every task in `20-MIGRATION` assumes you have read this. There is no code in this file.

## 2. Why this way

DoneBot has real users on Google Play. A migration that leaves the app unshippable for six months is not a migration — it is a rewrite with extra steps, and it removes the ability to hotfix a production bug. The protocol below buys one property and it is worth its cost:

> **At every commit on `main`, the Android app builds, passes its tests, and could be shipped.**

That property is what lets you stop at any point, ship a hotfix, and resume. It is also what makes each failure diagnosable: if the gate was green at commit N and red at N+1, the cause is in one commit.

---

## 3. Step 0 — ship v1.2, then freeze

Before any migration task:

1. Create `keystore.properties` at the repo root (see `BLOCKERS.md`). The keystore is at `~/donebot-upload.jks`.
2. Build and verify the signed release:
   ```bash
   ./gradlew :app:bundleRelease
   jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | head
   ```
3. Upload to Play. Read the current `versionCode`/`versionName` from `app/build.gradle.kts` — do not assume. Play burns a `versionCode` permanently once uploaded, **even if the draft is later deleted**, so the number in the file may already be ahead of what you expect.
4. Tag the pre-migration state:
   ```bash
   git tag v1.2-preKMP && git push origin v1.2-preKMP
   ```
5. Cut a hotfix branch: `git branch release/1.2.x && git push -u origin release/1.2.x`
6. **Freeze Android feature work.** Only critical bug fixes, and those land on `release/1.2.x` and are cherry-picked forward.

`v1.2-preKMP` is the reference point for every "did this change Android behaviour?" question for the next six months.

---

## 4. The two techniques

### 4.1 KMP-shaped, Android-only first

A Kotlin Multiplatform module declaring **only** `androidTarget()` produces output equivalent to `com.android.library`. The entire restructure — 11 modules, Hilt→Koin, Retrofit→Ktor, Room→Room KMP, `R`→`Res`, Compose→CMP — happens with **zero iOS targets declared**. iOS is switched on in `20-13`, one line per module.

**Rule: no `iosArm64()` / `iosSimulatorArm64()` in any module before `20-13`.** Declaring them early surfaces a wall of unrelated errors and destroys the ability to verify one change at a time.

### 4.2 The `androidMain` ratchet

When a file moves into a shared module it lands in `src/androidMain/kotlin` **first** — same package, same imports, same semantics, green build. Only once its JVM dependencies are removed does it move to `src/commonMain/kotlin`.

The gate:

```bash
! grep -rqE '^import (java|javax|android)\.' shared/*/src/commonMain
```

**Rule: a file that will not compile in `commonMain` is not a failure.** Put it in `androidMain`, record the specific blocking import in the task notes, and move on. A later task removes that dependency.

This is what makes "always green" real rather than aspirational: every file move is independently verifiable and independently revertable.

---

## 5. Package names do not change

Files keep `com.todoapp.mobile.*` when they move between modules. Moving `domain/` into `:shared:domain` must produce **zero import changes** in `:app`.

Renaming packages *and* moving modules in one step turns a mechanical move into a 500-file diff where a real error is invisible. The one sanctioned exception is `:uikit`'s namespace (`com.example.uikit` → `com.todoapp.uikit`) in `20-10`, which is isolated to one module and has its own acceptance check.

---

## 6. Commit discipline

- **One task, one logical change, small commits within a task.** A file move is a commit. The follow-up that removes a JVM dependency is another commit.
- Use `git mv` so history follows the file.
- Never mix a move with an edit in the same commit. Move first, verify green, then edit. A moved-and-edited file shows as delete+add and the real change becomes unreviewable.
- Commit message format is in `../README.md` §6. No `Co-Authored-By`.

---

## 7. The gate — after every commit, without exception

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
```

- **`detektAll`, not `detektMain`** — from `20-01` onward. `detektMain` is an AGP-variant task; once a module is KMP it produces `detektMetadataMain`/`detektAndroidDebug` instead, and `detektMain` keeps *succeeding while checking nothing*.
- **`testDebugUnitTest`, not `test`** — the release unit-test variant OOMs the Kotlin daemon.
- **Never pipe Gradle through `grep`/`tail`** — the pipe masks the exit code and a failed build reads as success. Redirect to a log file and check the exit status.

After any dependency change, additionally:

```bash
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab
```

and record the size in `90-STATE/PROGRESS.md`.

---

## 8. Regression shields

These tests encode behaviour that must not change. Treat any semantic difference as a bug in your migration, not as a test that needs updating:

| Test | Guards |
|---|---|
| `RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest` | `firesOn` / `clampedDayOfMonth` — the shared predicate behind the task list *and* both schedulers |
| `CalendarGridTest`, `DayTapOutcomeTest` | Calendar grid maths and the range-picker tap grammar |
| `HealthPointsCalculatorTest` | Hearts arithmetic |
| `AlarmRequestCodesTest`, `TaskAlarmLifecycleTest` | Alarm request-code namespacing and lifecycle |
| `TokenRefreshAuthenticatorTest` | 401 → single-flight refresh → retry |
| `SyncWorkerTest`, `FetchTasksWorkerTest` | Retry classification |
| `MainViewModelLogoutTest` | Logout wipes everything **except** the journal |
| `PaletteStyleTest`, `PixelIconMapTest` | Design-system invariants |

**When a test must change** (for example `TokenRefreshAuthenticatorTest` after Ktor replaces the OkHttp `Authenticator`), the rewritten test must assert the **same behaviours**, not the same implementation. Record the rewrite in `DECISIONS.md`.

---

## 9. One-way doors

Tasks marked `reversible: false` — `20-04`, `20-05`, `20-07`, `20-09`, `20-11`. Before starting one:

1. `git status` is clean.
2. The previous task is committed and the gate is green.
3. You have read the task's Pitfalls section in full.

`20-07` (Room → Room KMP) is the highest-consequence task in the project: it can corrupt data for existing installs. Its schema-diff gate is not optional.

---

## 10. Order and why

| # | Task | Why here |
|---|---|---|
| 1 | Dead deps + `detektAll` | Banks AAB headroom before anything grows it; fixes a silent quality-gate loss *before* the first KMP module exists |
| 2 | Domain Android leaks | Two files. Must precede the domain module move. |
| 3 | `:shared:core` + `:shared:domain` | Smallest, purest layer first — proves the module pattern cheaply |
| 4 | `kotlinx-datetime` | Touches 93 files across every layer; do it while the UI is still in one place |
| 5 | Hilt → Koin | Must precede Ktor/Room because those tasks rewire DI |
| 6 | Retrofit → Ktor | Independent of Room; contained to 5 files |
| 7 | Room → Room KMP | Highest risk, isolated between two lower-risk steps |
| 8 | DataStore | Trivial once DI is Koin |
| 9 | Resources | Must precede `:uikit`, which references resources |
| 10 | `:uikit` → CMP | The cleanest large module — proves the CMP pattern before the 48k-LOC UI layer |
| 11 | `:shared:ui` + `:composeApp` | The bulk. Everything it needs now exists. |
| 12 | `:app` shell | Cleanup; unlocks an Android 1.3 release |
| 13 | iOS targets | Only now, when everything compiles as KMP-shaped Android |

---

## 11. If you get stuck

- **Gate fails and you cannot fix it** → `BLOCKED`, exact error in `BLOCKERS.md`, next task.
- **A move breaks something unrelated** → revert the move, record what broke, split it into smaller moves.
- **A dependency has no KMP artifact** → `BLOCKED`. Do not substitute a library on your own; that is an architecture decision.
- **The gate was green and is now red with no relevant change** → `./gradlew --stop && ./gradlew :app:clean`. Stale KSP/Hilt codegen is a known failure mode in this repo.

## 7. Acceptance

- [ ] This file has been read in full
- [ ] v1.2 is on Play, or `BLOCKED` with the reason recorded
- [ ] `v1.2-preKMP` tag exists and is pushed
- [ ] `release/1.2.x` branch exists and is pushed
- [ ] The Android feature freeze is understood: only critical fixes, on `release/1.2.x`

## 8. Pitfalls

- **Skipping step 0.** Starting the migration before v1.2 ships means the next production bug has no shippable branch to fix it from.
- **Treating the gate as advisory.** It is the only thing standing between this migration and an unshippable app.
- **Batching commits "to save time".** The savings are illusory; the cost lands the first time you need to bisect.

## 9. Verification

```bash
git tag --list 'v1.2-preKMP'
git branch -a | grep 'release/1.2.x'
./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug   # detektAll from 20-01 on
```
