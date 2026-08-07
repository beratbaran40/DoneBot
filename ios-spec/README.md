# DoneBot iOS Port — Agent Operating Manual

**Read this file completely before doing anything else. Every session starts here.**

You are implementing the port of DoneBot (a live Android app on Google Play, ~90k LOC Kotlin) to iOS, via Kotlin Multiplatform + Compose Multiplatform, inside this same repository. The work is decomposed into **107 task files totalling ~1,590 estimated hours** — roughly 39 weeks at 40 h/week, 64 weeks at 25 h/week. This document tells you how to pick a task, execute it, prove it worked, and record it — so that work continues without stalling and without a human in the loop.

**Two things do not have to exist yet.** The Apple Developer account (`10-01`) and Xcode (`10-05`) are both human-gated and both slow. Neither is needed before `20-13`: **32 tasks / 712 hours of this plan run on the current machine with no Xcode and no Apple account.** Start `10-01` on day one anyway — enrolment takes weeks — but do not wait on either to begin.

---

## 0. The six hard rules

These are not guidelines. Violating any of them is a defect, even if the code "works."

0. **Never commit to `main`.** All iOS work — every task in this spec, including the migration tasks that touch Android code — happens on a branch. `main` is the live Android app's branch and stays that way until the port is complete and deliberately merged. Before your first commit in any session, run `git branch --show-current` and confirm you are not on `main`. See §6.
1. **Android must stay green and shippable after every task.** This app has real users. The verification command in §3 must pass before you mark any task `DONE`. If it does not pass, you are not done — you are mid-task.
2. **Never mark a task `DONE` without running its `verify:` commands and seeing them pass.** Do not infer success. Do not mark `DONE` "pending a build." Paste the outcome into the ledger.
3. **Never invent scope.** Implement exactly what the task file specifies. If you discover work that must happen but is not in any task file, write it into `90-STATE/BLOCKERS.md` and keep going — do not silently expand.
4. **Never block on a human.** If a task needs something only a human can do (an Apple account, a paid subscription, a physical device, a password), record it in `90-STATE/BLOCKERS.md`, set the task `status: BLOCKED`, and immediately pick the next available task.
5. **One task = one commit.** Commit message format is defined in §6. Never bundle two task ids into one commit.

---

## 1. Orientation: read these first, once per session

In this order. Do not skip — they are short and they prevent the most expensive mistakes.

| File | Why |
|---|---|
| `00-CONTEXT/01-decision-record.md` | The locked architecture decisions and *why*. Do not relitigate these. |
| `00-CONTEXT/04-constraints.md` | Apple's hard limits and this repo's hard limits (AAB budget, CI gates). |
| `90-STATE/PROGRESS.md` | What is done, what is in flight, what is next. |
| `90-STATE/BLOCKERS.md` | What is stuck and why — so you do not re-attempt a known blocker. |
| `90-STATE/DECISIONS.md` | Decisions made *during* execution by previous sessions. Binding on you. |
| `/CLAUDE.md` (repo root) | The existing engineering rulebook for this codebase. Still authoritative for Kotlin/Compose style, naming, localization, anti-patterns. |

`00-CONTEXT/03-source-map.md` is a lookup table, not a read-through — consult it when you need to find where something lives in the Android source.

---

## 2. The work loop

Repeat until no task is available:

```
1. Read 90-STATE/PROGRESS.md
2. Pick the next task (§2.1)
3. Set its status to IN_PROGRESS in PROGRESS.md and in the task file front-matter. Commit that alone.
4. Read the task file completely, then read every path listed under "3. Source".
5. Implement "5. Steps" in order.
6. Run every command under "9. Verification". They must all pass.
7. Tick every box in "7. Acceptance". If a box cannot be ticked, you are not done.
8. Commit (§6).
9. Set status to DONE in PROGRESS.md + task file. Record the verification outcome.
10. Go to 1.
```

### 2.1 How to pick the next task

A task is **available** when all of the following hold:

- `status: TODO`
- every id in its `depends_on` has `status: DONE`
- no `IN_PROGRESS` task shares an `owner_files` glob with it

Among available tasks, pick in this order:
1. Lowest phase number (`10-` before `20-` before `30-`…).
2. Within a phase, lowest id, sorted as a **string** — that is why `20-03b` runs immediately after `20-03` and before `20-04`.

> **The phase number is a category, not an execution order.** `depends_on` is the real order, and it legitimately points "forward" in several places: `10-03` (the Xcode project) needs `20-13` (the iOS framework); `60-03` (Sign in with Apple) needs `70-01` (the backend endpoint); every `40-*` feature needs its `50-*` design-system prerequisite. A task whose `depends_on` are not all `DONE` is simply not available yet, regardless of its number.

If a task's `parallel_safe: true` and you are running alongside other agents, you may take it even if another task is `IN_PROGRESS`, provided the `owner_files` globs do not intersect.

**If no task is available**, do not idle and do not invent work. Write a dated note in `90-STATE/BLOCKERS.md` explaining precisely which dependency is holding the queue, and stop.

### 2.2 Task file anatomy

Every task file has YAML front-matter followed by the nine sections below. **Section 6 (Code skeleton) is omitted where a task writes no code** — the read-only index files and the feature-verification tasks are the usual cases. Every other section is present in every task. The four files in `00-CONTEXT/` are **reference documents, not tasks** — they deliberately have no front-matter and nothing depends on them being marked `DONE`.

```yaml
---
id: 20-07                 # phase-sequence, globally unique
title: Room → Room KMP
layer: data               # foundation | data | domain | ui | design | platform | ios-native | backend | release
status: TODO              # TODO | IN_PROGRESS | BLOCKED | DONE
depends_on: [20-03]       # ids that must be DONE first
blocks: [20-11]           # informational: what this unblocks
parallel_safe: false      # may run concurrently with other tasks?
estimate: 35h             # human-hours, for scheduling only — not a budget for you
reversible: false         # true = easy revert; false = one-way door, extra care
owner_files:              # globs this task may modify — your collision boundary
  - shared/data/src/**
verify:                   # commands that must pass; also in section 9
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---
```

| Section | What it gives you |
|---|---|
| 1. Goal | One paragraph. The definition of "this task is over." |
| 2. Why this way | Rationale + the trap being avoided. Read it; it prevents "clever" regressions. |
| 3. Source | **Exact paths in the Android source to read before writing anything.** |
| 4. Target | Exact paths to create or modify. |
| 5. Steps | Ordered, each independently verifiable. |
| 6. Code skeleton | Real Kotlin/Swift. Adapt it; do not treat it as pseudocode. |
| 7. Acceptance | Checkboxes. Mechanically checkable. All must be ticked. |
| 8. Pitfalls | Project-specific traps. These are the expensive ones. |
| 9. Verification | Exact commands. Run all of them. |

**The front-matter `verify:` list is a contract with rule 2: every command in it must be runnable, from the repo root, at the moment the task ends.** If a check is aspirational — a gate a *later* task closes — it belongs in section 9 as a survey with its expected failure stated, never in the front matter. Three of these were wrong in the first draft and deadlocked the queue (ADR-009). If you write a new one, run it before you commit it.

**`reversible: false` means:** before starting, confirm the working tree is clean and the previous task is committed. These tasks are one-way doors (schema changes, DI framework swap, ~400-file sweeps). A dirty tree turns a revert into an archaeology exercise.

---

## 3. Verification: the commands, and the trap that breaks them

### 3.1 The JDK trap — fix this before your first build

Running `./gradlew` from a shell picks up **JDK 24**, and every Gradle invocation fails with `Type T not present`. The correct JDK is on the machine but not wired up.

```bash
# Correct JDK (Android Studio's bundled runtime):
/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home   # openjdk 21.0.9
```

Task `10-FOUNDATION/00-environment.md` makes this permanent. Until it is `DONE`, prefix every Gradle call:

```bash
JAVA_HOME="/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home" ./gradlew <task>
```

If you see `Type T not present`, this is the cause. It is never a code error.

### 3.2 The Android gate — after every task, without exception

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
```

Notes that matter:
- **`detektAll`, not `detektMain`.** `detektMain` is an AGP-variant task; once a module becomes KMP it produces `detektMetadataMain`/`detektAndroidDebug` instead, and `detektMain` keeps *succeeding* while silently checking nothing. Task `20-MIGRATION/01` introduces `detektAll`. Before that task is done, use `detektMain`.
- **`testDebugUnitTest`, not `test`.** The release unit-test variant OOMs the Kotlin daemon.
- Never pipe Gradle through `grep`/`tail` — the pipe masks the exit code and a failed build reads as success. Redirect to a log file and check the exit status.

### 3.3 The size gate — after any dependency change

```bash
./gradlew :app:bundleRelease
ls -l app/build/outputs/bundle/release/*.aab
```

The ceiling lives in `.github/workflows/ci.yml` as `AAB_MAX_BYTES`. Record the measured size in `PROGRESS.md` for any task that touches dependencies. Baseline before migration: **18.17 MiB of a 20 MiB ceiling.**

### 3.4 The iOS gate — only from `20-MIGRATION/13` onward

Both commands need **Xcode installed** (`10-05`), not just Command Line Tools: Kotlin/Native links against the real iOS SDK and shells out to `xcrun`. Nothing before `20-13` needs them.

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

### 3.5 The commonMain purity gate — from `20-MIGRATION/03` onward

```bash
! grep -rqE '^import (java|javax|android)\.' shared/*/src/commonMain
```

This is the ratchet that makes the whole migration safe (§4).

---

## 4. The two techniques the whole plan rests on

Understand these or you will fight the plan.

### 4.1 KMP-shaped, Android-only first

A Kotlin Multiplatform module that declares **only** `androidTarget()` produces output equivalent to `com.android.library`. So the entire restructure — 11 modules, Hilt→Koin, Retrofit→Ktor, Room→Room KMP, `R`→`Res`, Compose→CMP — happens with **zero iOS targets declared**. iOS is switched on later, one line per module.

**Consequence for you:** do not add `iosArm64()`/`iosSimulatorArm64()` to any module before task `20-MIGRATION/13`. Doing so early surfaces a wall of unrelated errors and destroys the ability to verify one change at a time.

### 4.2 The `androidMain` ratchet

When a file moves into a shared module, it lands in `src/androidMain/kotlin` **first** — same imports, same semantics, green build. Only then, as its JVM dependencies are removed, does it move to `src/commonMain/kotlin`.

The gate is §3.5. Every file move is independently verifiable and independently revertable. This is what makes "always green" real instead of aspirational.

**Consequence for you:** if a file will not compile in `commonMain`, that is not a failure — put it in `androidMain` and record the specific blocking import in the task's notes. Someone (possibly you, later) removes that dependency in a subsequent task.

---

## 5. Handling the situations that actually come up

| Situation | What to do |
|---|---|
| A `verify:` command fails | You are mid-task. Fix it. Never mark `DONE`. If you cannot fix it after genuine effort, set `BLOCKED`, write the exact error + what you tried in `BLOCKERS.md`, move on. |
| The task file is wrong or ambiguous | Implement the *intent* stated in section 1 (Goal). Record the discrepancy in `DECISIONS.md` with the reasoning. Do not silently do something different. |
| You find a bug unrelated to your task | `BLOCKERS.md`, then keep going. Do not fix it inside an unrelated task — it corrupts the revert story for `reversible: false` tasks. |
| A dependency version does not exist / has no iOS artifact | `BLOCKED` + `BLOCKERS.md` with the exact coordinate you tried. Do not substitute a different library on your own — that is an architecture decision. |
| Something needs a human (Apple account, payment, physical device, secret) | `BLOCKED` + `BLOCKERS.md`, next task. This is expected and normal; several tasks are gated this way. |
| A task turns out to be already done | Verify it with the `verify:` commands. If they pass, mark `DONE` with a note saying it was already satisfied. |
| Two tasks conflict over the same file | The one already `IN_PROGRESS` wins. Pick a different task. |
| You are unsure whether a change is in scope | It is not. Scope is exactly section 4 (Target) + section 5 (Steps). |

---

## 6. Branching and commits

### 6.1 Never `main`

**All iOS work happens on a branch off `main`. Nothing in this spec is committed directly to `main`.**

`main` is the branch the live Android app ships from. The owner continues to work there — bug fixes, releases, occasionally mid-session. Committing iOS work to it mixes a nine-to-fourteen-month restructure into the branch that has to stay releasable at all times.

**`90-STATE/` will conflict on every `git merge main`, by design** — the spec is tracked on `main`, the ledger is written here. Keep the branch's `PROGRESS.md` (`git checkout --ours`); for the append-only `BLOCKERS.md` and `DECISIONS.md`, take both sides and re-sort.

The working branch is **`feat/ios-port`** unless `90-STATE/PROGRESS.md` records a different one.

```bash
# Start of every session — verify, do not assume
git branch --show-current          # must NOT be main

# If the branch does not exist yet
git checkout -b feat/ios-port main

# Bring in Android fixes that landed on main
git checkout feat/ios-port
git merge main
```

**Merge `main` into the branch, never the reverse**, until the port is complete and the owner decides to merge it back.

Two signals that you are on the wrong branch, both worth stopping for:
- `git status` shows dirty files you did not touch
- a file you edited earlier in the session has reverted

Both usually mean the branch changed under you. Check before committing anything.

### 6.2 Commit format

This repo uses Conventional Commits, lowercase, with the body explaining **why** rather than what.

```
<type>(<scope>): <lowercase imperative subject>

<why this change was needed — not a restatement of the diff>

spec: <task-id>
```

- Types in use here: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `build`, `perf`.
- **Do not add `Co-Authored-By` trailers.** This repo has a single author by explicit instruction.
- One task id per commit. The `spec:` trailer is how a task is traced back to its file.
- Status-flip commits (`TODO`→`IN_PROGRESS`, →`DONE`) are `chore(spec):` and stand alone.

Never commit: `keystore.properties`, `local.properties`, anything matching the existing `.gitignore`. Never run `git add .` — stage explicit paths.

---

## 7. Where things are

```
ios-spec/
├── README.md              ← you are here
├── 00-CONTEXT/            Decisions, glossary, Android source map, Apple constraints
├── 10-FOUNDATION/         JDK pin, Apple enrolment, Gradle/KMP setup, iOS shell, CI, macOS+Xcode
├── 20-MIGRATION/          Steps 0–13: the in-place KMP conversion. Sequential.
├── 30-PLATFORM/           The 31 expect/actual platform contracts + iOS implementations
├── 40-FEATURES/           One file per Android `ui/` package. 1:1 mapping.
├── 50-DESIGN-SYSTEM/      Tokens, 3 palette kits, 80 TD* components, icon pipeline, iPad
├── 60-IOS-NATIVE/         WidgetKit, App Intents/Siri, Sign in with Apple
├── 70-BACKEND/            Changes required in the separate Spring backend repo
├── 80-RELEASE/            App Store Connect, privacy, screenshots, TestFlight, submission
└── 90-STATE/              PROGRESS.md · DECISIONS.md · BLOCKERS.md  ← you write here
```

**Visual acceptance reference:** `docs/screenshots/` holds 102 PNGs across 23 screens, named `NN_{state}_{lang}_{theme}.png` (empty/populated × EN/TR × light/dark). Feature task files reference these. Note `docs/` is gitignored — it is a local reference, not a CI artifact.

**The backend is a separate repository** at `~/AndroidStudioProjects/ToDoBackend` (Spring Boot + Kotlin, deployed to Render). Tasks in `70-BACKEND/` are executed there, not here.

---

## 8. Ledger format

`90-STATE/PROGRESS.md` is the single source of truth for status. Update it in the same commit as the status flip.

```markdown
| id | title | status | updated | notes |
|---|---|---|---|---|
| 10-00 | Environment & toolchain | DONE | 2026-08-07 | JAVA_HOME pinned; gradle green |
| 20-07 | Room → Room KMP | IN_PROGRESS | 2026-08-14 | schema diff pending |
```

`90-STATE/BLOCKERS.md` — append, never rewrite history:

```markdown
## [2026-08-07] 10-01 — Apple Developer enrolment
**Needs:** a human. Individual enrolment, 99 USD/yr, identity verification.
**Impact:** blocks 10-03, all of 60-IOS-NATIVE, all of 80-RELEASE.
**Workaround:** local simulator development is unaffected; continue with 20-MIGRATION.
```

`90-STATE/DECISIONS.md` — append-only ADRs for choices made during execution:

```markdown
## ADR-003 [2026-08-14] Materialize MONTHLY day-29..31 rules on iOS
**Context:** UNCalendarNotificationTrigger(day:31) does not fire in February;
Android's clampedDayOfMonth fires on Feb 28.
**Decision:** materialize these into the rolling window instead of a repeating trigger.
**Consequence:** costs window slots; locked by ReminderPlannerTest.
```

---

## 9. Style: non-negotiable, inherited from the codebase

`/CLAUDE.md` at the repo root remains authoritative. The highest-frequency rules:

- **No hardcoded colors, text styles, or user-visible strings.** `TDTheme.colors` / `TDTheme.typography` / string resources only.
- **Both locales, always.** Any new user-visible string lands in EN *and* TR in the same change. The `check-l10n` skill audits this.
- **MVI structure stays.** `*Contract.kt` / `*ViewModel.kt` / `*Screen.kt` remain the three core files per feature.
- **Previews are part of the work**, not a follow-up. Every reachable `UiState` branch gets one.
- **Line length 160.** New UI files under ~300 lines; never grow an already-oversized file.
- **No Material icons exist in this project.** Every icon is a project drawable, drawn via `tdPainter(id)`.

---

## 10. Quick reference

```bash
# Gradle (until 10-FOUNDATION/00 is DONE, prefix with JAVA_HOME=...)
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug   # the gate
./gradlew ktlintFormat                                            # fix formatting
./gradlew :app:bundleRelease                                      # size check
./gradlew --stop && ./gradlew :app:clean                          # weird KSP/Hilt errors

# commonMain purity
! grep -rqE '^import (java|javax|android)\.' shared/*/src/commonMain

# iOS (from 20-MIGRATION/13)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

---

**Start here:** open `90-STATE/PROGRESS.md`, find the first available task, and begin.
