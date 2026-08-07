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

**Status:** deletion pending — the `gh` token lacks the `delete_repo` scope. See `BLOCKERS.md`.

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
