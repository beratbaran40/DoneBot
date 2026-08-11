# DECISIONS — in-flight ADRs

Append-only, **and kept in numeric order** — a new ADR takes the next free number and goes at the bottom. (ADR-002 and ADR-003 were originally written out of order; they were re-sorted once, on 2026-08-07, with no content change.)

Decisions made **during execution**, when a task file was ambiguous, wrong, or silent on something that mattered. Binding on every later session.

Architecture decisions locked before execution live in `../00-CONTEXT/01-decision-record.md` and are **not** revisited here. This file is for what the spec did not anticipate.

**Write an ADR when:** you deviate from a task file, choose between two defensible implementations, discover a spec error, or make a choice a later agent could plausibly reverse by accident.

Format:

```markdown
## ADR-NNN [YYYY-MM-DD] <short imperative title>
**Task:** <task-id>
**Context:** what forced a decision.
**Decision:** what you did.
**Alternatives:** what you rejected, and why.
**Consequence:** what this constrains later. Include the test or gate that locks it, if any.
```

---

## ADR-001 [2026-08-06] Single repository — `DoneBot-iOS` is not used

**Task:** planning

**Context:** A private `DoneBot-iOS` repository was created early, before the monorepo decision (D-02) was settled. Once D-02 landed, a second repository had no source to hold, and keeping an empty repository around invites a later session to "helpfully" start putting iOS code in it.

**Decision:** Everything lives in this repository — Kotlin, Swift, the spec, release runbooks and store assets. `DoneBot-iOS` is deleted.

**Alternatives rejected:**
- *Keep it for release/store assets* — splits the release process across two repositories for no gain; the AAB size budget applies to the app bundle, not the git repo, so keeping assets out of this repo buys nothing.
- *Keep it reserved but empty* — an empty repository with a plausible name is exactly the kind of thing a future session mistakes for the intended home of iOS code.
- *XCFramework published from here, consumed there* — forces a publish→version→consume round trip on every shared change; unacceptable friction for a single developer.

**Consequence:** `iosApp/` lives at the root of this repository, sibling to `app/`. Any proposal to move iOS source out reopens D-02 and must be argued there.

**Status:** **done** — the owner deleted the repository manually on 2026-08-06; `gh repo view beratbaran40/DoneBot-iOS` returns `Could not resolve to a Repository`. See the resolved entry in `BLOCKERS.md`. D-02's consequence line was corrected to match on 2026-08-11 — it still described the deleted repository as "reserved for release runbooks and store assets", which is the exact misreading this ADR exists to prevent.

---

## ADR-002 [2026-08-06] Spec files are written in English

**Task:** planning

**Context:** The user communicates in Turkish; their working documents (`donebot prod/`) are Turkish. The codebase, `CLAUDE.md`, the public README and every symbol, path and command are English.

**Decision:** Task files are English. Progress reporting to the user is Turkish.

**Alternatives rejected:** *Turkish spec* — every code identifier, Gradle task, file path and API name would still be English, producing constant code-switching mid-sentence and inviting term drift between instruction and implementation.

**Consequence:** If the user prefers Turkish, this is a mechanical translation of the prose sections; the front-matter, commands and code skeletons stay as they are.

---

## ADR-003 [2026-08-06] Rename the repository and Gradle root project to `DoneBot`

**Task:** planning

**Context:** `DoneBot-Android` describes a repository that is about to contain the iOS app as well.

**Decision:** Renamed on GitHub (`beratbaran40/DoneBot`), local remote updated, `rootProject.name` in `settings.gradle.kts` changed to `DoneBot`, and the seven `DoneBot-Android` references in `README.md` / `README.tr.md` (CI badges and clone instructions) updated. Verified with `./gradlew assembleDebug` — build successful.

**Alternatives rejected:** *Rename later, during migration* — the rename touches the remote URL and CI badge URLs; doing it while the tree is otherwise clean makes it a one-line-per-file diff instead of a merge hazard.

**Consequence:** GitHub redirects the old URL, so existing clones keep working, but any external link or bookmark should be updated. `rootProject.name` changes the Gradle project identity, which invalidates the build cache once — expected, not a defect.

---

## ADR-004 [2026-08-07] Split the macOS/Xcode toolchain out of `10-00`

**Task:** 10-00, 10-05, 20-13

**Context:** `10-00` bundled three unrelated jobs: pin the JDK, upgrade macOS, install Xcode 26. It also declared `blocks: [20-01]`, and `20-01` gates the entire migration. Two consequences, both wrong:

1. **The migration could not start without Xcode.** 504 of the migration's 544 hours never touch an iOS SDK, yet all of them sat behind a multi-hour, human-only, reboot-bearing OS upgrade. `10-00`'s own step 6 told an agent to mark the task `BLOCKED` and "move on to `20-01`" — which the pick rule forbids, because `20-01` depends on `10-00`. The task contradicted itself.
2. **`20-13`'s real Xcode dependency was missing.** It runs `linkDebugFrameworkIosSimulatorArm64`; Kotlin/Native links against the real iOS SDK and shells out to `xcrun`, so it cannot run with Command Line Tools alone. The graph never said so.

**Decision:** `10-00` keeps only the JDK pin (2 h, blocks `20-01`). New task `10-05` owns the macOS upgrade and Xcode install, depends on nothing, and blocks exactly `10-03` and `20-13`.

**Alternatives rejected:**
- *Leave it and rely on the prose escape hatch* — the prose and the pick rule disagreed; an agent following the rules deadlocks on task two.
- *Make `20-01` not depend on `10-00`* — the JDK pin genuinely must precede the first Gradle invocation.

**Consequence:** With `10-05` and `10-01` both `BLOCKED`, 32 tasks / **712 hours** remain reachable — the whole migration through `20-12`, plus the design system and `70-01`. Xcode is first needed at `20-13`. `10-05` §2 records the deadline: comfortably before `20-11` starts, on a clean tree with a green gate on both sides.

---

## ADR-005 [2026-08-07] `10-03` does not depend on the paid Apple account

**Task:** 10-03

**Context:** `10-03` (the Xcode project) declared `depends_on: [10-00, 10-01, 20-13]`, while `BLOCKERS.md`'s own `10-01` entry states that "simulator development works without a paid membership". Both cannot be true. Because `40-auth-08` → `10-03`, the contradiction propagated into the feature waves.

**Decision:** `10-03` depends on `10-05` and `20-13` only. The capability provisioning that genuinely needs a membership (App Groups, push, Sign in with Apple, associated domains) is isolated in step 8b, which is skipped and recorded when `10-01` is `BLOCKED`.

**Alternatives rejected:** *Keep the dependency for signing correctness* — a free personal team signs a simulator build, and every capability that needs the paid account is verified again in `30-03` / `30-11` / `60-03` anyway. Blocking the whole task bought nothing.

**Consequence:** If `10-01` is still pending when this task runs, the capability-bearing entitlements are commented out so the build signs, and re-enabling them is a checklist item on `10-01`'s resolution. That must be recorded here when it happens.

---

## ADR-006 [2026-08-07] Split Sign in with Apple out of `30-10`

**Task:** 30-10, 60-03, 40-auth-02, 40-auth-03

**Context:** `30-10` covered Google Sign-In *and* Sign in with Apple in one task, so it inherited Apple's dependencies: `10-01` (weeks of enrolment) and `70-01` (a backend endpoint in another repository). `40-auth-02` (login) depends on `30-10`, and almost every feature depends on login. Measured on the graph: **41 of 105 tasks — including `40-core-01` Home — were transitively blocked on a backend Apple endpoint.** Nothing about verifying the Home screen requires signing in with Apple. `60-03` already existed and already covered the Apple client work, so the two files also overlapped.

**Decision:** `30-10` becomes "Social sign-in contract & Google Sign-In" (`depends_on: [20-13, 10-03]`, 12 h). It still *declares* `appleCredential()` so `40-auth-02` compiles against the final interface, and stubs it loudly on both platforms with `supportsApple = false`. `60-03` absorbs the whole Apple client path — the `ASAuthorization` flow, `AppleLoginRequest`, `POST auth/apple`, the button — and gains `10-01` as a dependency (18 h → 12 h + 10 h → 16 h). `40-auth-02`/`03` verify the Apple row only if `60-03` is `DONE`.

**Alternatives rejected:**
- *A new `30-16` for the Apple half* — duplicates `60-03`, which already specified the UI, the linking edge cases and the revoke path.
- *Drop `70-01` from `30-10` and leave the implementation there* — the dependency would have been dishonest; the code cannot be finished without the endpoint.

**Consequence:** With `10-01` blocked, reachable tasks go from 50/105 to **97/107**. The Apple credential type's shape (`email`/`fullName` nullable, first-authorization-only) is now fixed in `30-10` and must not change in `60-03`, or every login call site recompiles. `30-10`'s §9 has a grep that fails if Apple work leaks back into it.

---

## ADR-007 [2026-08-07] Test migration is its own task (`20-03b`)

**Task:** 20-03, 20-03b, 20-04

**Context:** Moving the test suite into `commonTest` was one step (§5.6) inside `20-03`, with the escape hatch "if a test cannot move cleanly, leave it in `app/src/test` for now". But the current stack is JUnit4 + `org.junit.Assert` + Robolectric, none of which exists in `commonTest` — it is an assertion rewrite across 37 files, not a `git mv`. Meanwhile `20-04`'s `verify:` line already invoked `:shared:domain:testDebugUnitTest --tests '*Recurrence*'`, which **passes vacuously** if the test never moved. The regression shields are the stated safety argument for the entire migration, and `30-01`'s `ReminderPlannerTest` is specified as a `commonTest` — so something had to make `commonTest` real, with a working stack, before either.

**Decision:** New task `20-03b` (30 h), `depends_on: [20-03]`, `blocks: [20-04]`. It classifies all 37 files into MOVE / STAY-for-now / STAY-forever, wires `commonTest` dependencies, and rewrites assertions onto `kotlin.test`. `20-03` explicitly does *not* move tests any more.

**Alternatives rejected:**
- *Leave it inside `20-03`* — silently doubles a task scoped as Gradle wiring, and puts the shields at risk inside it.
- *Defer it until `20-11`* — `20-04` is the largest semantic-risk sweep in the project and would run with an unverifiable gate.

**Consequence:** `20-04` now depends on `20-03b`, not `20-03`. The acceptance that matters is **total test-case count unchanged across all modules** — a rewrite that drops cases still shows green. The `assertTrue(msg, cond)` → `assertTrue(cond, msg)` argument-order flip is called out as the specific way a shield turns into a no-op.

---

## ADR-008 [2026-08-07] `detektAll` aggregates production source sets only

**Task:** 20-01

**Context:** `20-01`'s skeleton registered `detektAll` as depending on every `Detekt`-typed task except the typeless `detekt`. Measured on this repo, that is **eleven tasks in `:app`**: `detektMain` plus five per-variant duplicates of it (`detektDebug`, `detektRelease`, `detektReleaseLocal`, `detektBenchmarkRelease`, `detektNonMinifiedRelease`) plus five test-source tasks. `detektMain` is documented as running "across all variants", so the duplicates are five redundant type-resolution passes; the test tasks analyse sources that were never baselined, so the build would go red on unchanged code. `app/build.gradle.kts` already carries a comment recording that this exact variant fan-out is "how it broke CI", and CI passes `--max-workers=2` because the runner OOMs under detekt + tests.

**Decision:** Filter by task name to production source sets — `detektMain` for AGP modules, `detektMetadataMain` / `detektAndroidDebug` / `detektIos*Main` for KMP modules, excluding anything containing `Test`.

**Alternatives rejected:** *Keep the broad filter and baseline the test findings* — adds a new category of analysis mid-migration for no benefit, and multiplies CI cost by roughly six.

**Consequence:** `./gradlew detektAll --dry-run | grep -cE ':(app|uikit):detekt'` must print **2** today, not 22. Wall-clock within ~10% of `detektMain`. When a module becomes KMP the name filter picks up its metadata/target tasks automatically — that is the whole point of the task.

---

## ADR-009 [2026-08-07] Three `verify:` commands were unrunnable or self-contradicting

**Task:** 20-02, 20-03, 20-07

**Context:** `README.md` §0 rule 2 makes the front-matter `verify:` list load-bearing: a task may not be marked `DONE` until every command in it passes. Three of them could not pass:

- **`20-03`** asserted the full `commonMain` purity gate (`java|javax|android`) while its own step 8 said "**This will fail**, because `java.time` is still everywhere. That is expected and correct." An agent obeying rule 2 deadlocks on the third migration task, which gates everything after it.
- **`20-02`** asserted no `android|androidx` imports in `domain/`, but 12 model files legitimately import `androidx.compose.runtime.Immutable` — which the task's own §7 and §9 explicitly exclude, and which is multiplatform anyway.
- **`20-07`** contained `diff -q app/schemas/*/30.json <the regenerated 30.json>` — a literal placeholder, not a shell command. This is the schema gate on the one task that can destroy user data.

**Decision:** `20-03` asserts only the `android.` half (the part it actually guarantees) and runs the full gate as a recorded *survey* whose output is the work list for `20-04`/`20-05`/`20-13`. `20-02` pipes through `grep -v 'androidx\.compose\.runtime\.Immutable'`. `20-07` gets the real command, `diff -ru /tmp/schemas-before app/schemas`, with a pitfall stating that step 1's snapshot is its prerequisite.

**Alternatives rejected:** *Soften rule 2 to "verify where practical"* — the rule is the reason the migration is trustworthy. The commands were wrong, not the rule.

**Consequence:** Any future `verify:` line must be runnable, from the repo root, at the moment the task ends. If a check is aspirational, it belongs in §9 as a survey with the expected failure stated, never in the front matter.

---

## ADR-010 [2026-08-11] `10-05`'s gate command drops to `detektMain` with an explicit `JAVA_HOME`

**Task:** 10-05

**Context:** `10-05` §5.2, §5.6 and §9.4 all ran `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug`. That command cannot execute when the task is actually performed. `10-05` has `depends_on: []` and is human-paced, so it runs early — before `10-00` (which pins the JDK) and before `20-01` (which *creates* `detektAll`). Today it fails twice over: a plain `./gradlew` selects JDK 24 and dies with `Type T not present`, and `detektAll` does not exist, so Gradle reports `Task 'detektAll' not found`.

This is not cosmetic. The entire safety argument of `10-05` is "green gate immediately before the OS upgrade, green gate immediately after, no code change in between" — that is what attributes post-upgrade breakage to the OS instead of the migration. An unrunnable gate command silently deletes that property, and the failure reads like OS breakage, which is the exact misattribution the step exists to prevent.

**Decision:** Steps 2, 6 and §9.4 use `JAVA_HOME="/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home" ./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug`, with a pitfall in §8 explaining why it differs from the canonical gate.

**Alternatives rejected:**
- *Add `10-00` and `20-01` to `10-05`'s `depends_on`* — puts a 2-hour JDK pin and a 6-hour dependency cleanup in front of a task whose whole point (D-12) is that it runs on the owner's clock, independently of the code queue. It would also re-couple the human-gated toolchain to the migration, which is what ADR-004 separated.
- *Leave it and rely on the reader noticing* — the reader is the owner, mid-OS-upgrade, and the error message names neither cause.

**Consequence:** When both `10-00` and `20-01` are `DONE` and `10-05` is still `TODO`, the command should be raised back to the canonical gate. §8's pitfall states this; the task file is the only place the downgrade is recorded.

---

## ADR-011 [2026-08-11] `10-04` depends on `20-01`; its nightly-iOS acceptance defers to `20-13`

**Task:** 10-04, 20-01, 20-13

**Context:** Two independent defects in one file.

1. **A dependency that exists in prose but not in the graph.** `10-04` declared `depends_on: [10-00]`, so the pick rule schedules it directly after `10-02`. But its own §5 step 1 reads "Confirm the `detektAll` swap landed in `20-01`", and its acceptance requires the `lint-test` job to run `detektAll` — a Gradle task that `20-01` creates. Following the pick rule points CI at a task that does not exist and turns the build red on unchanged code.
2. **An acceptance box that cannot be ticked for months.** The same file requires `ios-nightly.yml` to "run successfully via `workflow_dispatch`". That workflow invokes `:composeApp:linkDebugFrameworkIosSimulatorArm64` (`20-13`) against `iosApp/iosApp.xcodeproj` (`10-03`). Under README §0 rule 2 the task can never be marked `DONE`, so it sits `IN_PROGRESS` indefinitely and its `owner_files` glob (`.github/workflows/**`) blocks every later task that touches CI — including `20-09`'s ceiling raise.

**Decision:** `depends_on: [10-00, 20-01]`. The nightly workflow is still authored and committed in `10-04` — it is the artifact `20-13` verifies rather than writes — but the "runs successfully" box moves to a clearly-labelled deferred section, and §9 checks registration and the absence of a `pull_request` trigger instead of dispatching a run.

**Alternatives rejected:**
- *Move the whole nightly workflow into `20-13`* — `20-13` is already the task that switches on every iOS target and fixes the resulting common-code compile errors; adding CI authoring to it grows a task that is large and `reversible`-sensitive. Keeping the CI story in one file was `10-04`'s stated purpose and it is still right.
- *Drop the nightly job from the spec until iOS compiles* — the 10× macOS billing rationale is the reason it is nightly rather than per-PR, and that reasoning is worth recording where CI is described, not eight tasks later.

**Consequence:** `10-04` is now genuinely completable when it runs. `20-13` inherits one verification it did not previously own: the first successful `gh workflow run ios-nightly.yml`. Reachability is unchanged — `20-01` was already reachable well before `10-04` in every scenario.

---

## ADR-012 [2026-08-11] The owner takes `10-01` and `10-05` first; code work starts after

**Task:** 10-01, 10-05, sequencing

**Context:** `README.md` §0 states that neither the Apple Developer account nor Xcode has to exist before `20-13`, and advises starting the migration immediately rather than waiting. That advice was verified independently on 2026-08-11 by parsing the `depends_on` front-matter of all 107 task files and computing reachability: **107/107 with nothing blocked, 97/107 (1,456 h) with `10-01` blocked, 32/107 (712 h) with both blocked**, and no dangling dependencies. The numbers match the ledger exactly.

The owner chose the opposite sequence: obtain the Apple membership and install macOS + Xcode **first**, in parallel with each other, and begin code work afterwards. This is not a correction of the spec — the spec's claim holds — it is a scheduling preference.

**Decision:** `10-01` and `10-05` are `IN_PROGRESS`, owner-executed. No code task starts until they resolve. **`feat/ios-port` is not cut and `v1.2-preKMP` is not tagged yet** — both happen on the day migration actually begins, against that day's `main`.

**Alternatives rejected:**
- *Cut the branch and tag now anyway* — Android feature work is not frozen (the owner deferred that too), so `main` keeps moving. A branch cut today is stale before its first commit, and `v1.2-preKMP` would mark a commit that is not the pre-migration state by the time migration starts. The tag's only value is being the reference point for "did this change Android behaviour?", which requires it to sit at the real boundary.
- *Start `10-00` in the meantime* — it is 2 hours and harmless, but it commits to a branch that per the point above should not exist yet, and its AAB baseline measurement is only meaningful as the number the migration is measured against.

**Consequence:** When code work starts, **all 107 tasks are reachable** and no human-gated prerequisite remains anywhere in the graph — `20-13` and `10-03` are unblocked the moment `20-12` lands, and `80-RELEASE` needs no further waiting. The runway tables in `README.md` §0 and `PROGRESS.md` become historical: they document why the graph is shaped the way it is (D-12), not an operative constraint. Do not delete them — they are the argument against re-coupling human-gated tasks to the critical path.

Because Android is not frozen, the offsetting cost is merge distance: `git merge main` into `feat/ios-port` at the start of every session and before every `reversible: false` task. After the ~400-file sweeps in `20-09` and `20-11`, a neglected merge is a day of conflict resolution.

---

## ADR-013 [2026-08-11] Spec-maintenance commits land on `main`

**Task:** spec maintenance

**Context:** D-11 says every task in this spec is committed to `feat/ios-port`, never `main`. But `ios-spec/` itself is tracked on `main` — README §6.1 says so explicitly ("the spec is tracked on `main`, the ledger is written here") — and the eight commits that authored this spec are all on `main`. Corrections to task files made *before any branch exists* have nowhere else to go, and a session that reads D-11 literally could flag them as a violation.

**Decision:** Corrections to task files, `00-CONTEXT/` and `README.md` are `docs(ios-spec):` commits on `main`, matching how the spec was authored. The rule that `90-STATE/` is written on the branch takes effect when the branch exists; until then the ledger lives on `main` with the rest of the spec.

**Alternatives rejected:** *Cut `feat/ios-port` early just to hold doc fixes* — creates the stale branch ADR-012 rejects, and puts spec corrections behind a merge before any code exists to justify one.

**Consequence:** Once `feat/ios-port` exists, `90-STATE/` edits go there and this ADR stops applying to them. Task-file corrections discovered mid-migration still merge cleanly from `main`, which is the direction D-11 already mandates.

---

## ADR-014 [2026-08-11] Sweep every `verify:` and §9 command for runnability, once

**Task:** all

**Context:** README §0 rule 2 makes `verify:` load-bearing — a task may not be `DONE` until every command in it passes. Five instances have now been found where a command could not pass at the moment its task ends: `20-03`, `20-02` and `20-07` (ADR-009), then `10-05` (ADR-010) and `10-04` (ADR-011). They were found by reading, one at a time, months apart in planning time. The failure mode is consistent and quiet: the command names a Gradle task, file or module that a *later* task creates, so it fails with `Task not found` or an empty grep — which reads as breakage, or worse, passes vacuously.

Two of the five were in the two tasks the owner is executing this week. That is not a coincidence: the earliest tasks reference the most machinery that does not exist yet.

**Decision:** Before the first code session, sweep all 107 task files mechanically. For each, evaluate every front-matter `verify:` line and every §9 command against the repo state *at that task's completion point* — that is, with all its `depends_on` satisfied and nothing later applied. Flag any command that references a Gradle task, module path, file or directory created by a task that is not a transitive dependency. Fix in place; record only the ones that change behaviour as ADRs.

**Alternatives rejected:**
- *Keep finding them one at a time* — the cost lands on whoever executes the task, usually mid-flight, and the error message never names the real cause.
- *Weaken rule 2* — ADR-009 already rejected this and the reasoning stands: the commands were wrong, not the rule.

**Consequence:** A vacuous-pass check belongs in the sweep too, not just a hard failure: a `grep` over a directory that does not exist yet exits non-zero, but a `--tests '*Foo*'` filter matching nothing exits **zero**. ADR-007 records exactly that trap in `20-04`. The sweep is cheap once and expensive never.
