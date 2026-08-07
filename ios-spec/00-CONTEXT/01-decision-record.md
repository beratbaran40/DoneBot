# Decision Record — locked

These decisions were made deliberately, with the alternatives costed. **Do not relitigate them.** If you believe one is wrong, write the argument in `90-STATE/DECISIONS.md` and continue executing the current decision — do not act on the disagreement.

---

## D-01 · Kotlin Multiplatform + Compose Multiplatform, migrating this repo in place

**Decision.** Convert the existing Android repository to KMP/CMP, then add an iOS target. Share domain, data, design system, and UI. Write only genuinely platform-bound surfaces natively.

**Cost, for comparison with the rejected alternatives below:** the task files total **~1,590 estimated hours** — ≈ 9 months at 40 h/week, ≈ 14 months at 25 h/week. This path is not dramatically cheaper in raw hours than a rewrite; what it buys is a *shippable Android app at every commit*, a single implementation of the sync state machine, recurrence engine and health-points maths, and one UI to maintain afterwards rather than two forever.

**Why.** The layer breakdown forced this:

| Layer | LOC | % |
|---|---|---|
| `ui/` | 48,405 | 71% |
| `data/` | 12,397 | 18% |
| `domain/` | 2,482 | 3.7% |
| `navigation/` | 2,077 | 3.1% |
| other | ~2,600 | 4% |
| `:uikit` | 16,455 | — |

The domain layer is thin — most business logic lives in repositories and ViewModels. So "share the domain, rewrite the UI" would have shared ~15k of 90k LOC while still paying the full KMP conversion cost. With **full feature parity** as a hard requirement, sharing the UI is what makes the schedule possible at all.

**Alternatives rejected.**
- *KMP core + SwiftUI UI* — ~10–14 months; low sharing ratio because domain is thin; **UI maintained twice forever**, which is the recurring cost that dominates after ship.
- *Pure SwiftUI rewrite* — ~12–18 months; 90k LOC reimplemented; sync state machine, recurrence engine and health-points math duplicated, which is where silent divergence would appear.

The three estimates overlap more than they look. The deciding factor is not the first ship date, it is that both alternatives leave two implementations of the same logic in production.

**The accepted trade-off.** On iOS, Compose renders through Skia/Metal inside a `ComposeUIViewController` — it does not map to UIKit controls. Text input, scroll physics and selection are Compose's iOS implementations, not the system's. This is acceptable here specifically because DoneBot's design is already deliberately non-standard (three palette kits, pixel-art icon set, mascot, skeuomorphic polaroid camera), so there is little "stock native look" to lose. It would be the wrong call for an app that wants to look like Settings.app.

**Android is unaffected in kind.** Compose Multiplatform resolves to Google's `androidx.compose.*` artifacts on the Android target. The output is still an AAB, still R8, still the same manifest, services and receivers. What changes is the dependency stack (Hilt→Koin, Retrofit→Ktor, `java.time`→`kotlinx-datetime`), not the nature of the app.

---

## D-02 · Monorepo

**Decision.** The iOS app lives in this repository. `iosApp/` sits at the repo root as an Xcode project, sibling to `app/`.

**Why.** With shared Kotlin, a split repo forces publish→version→consume round trips on every shared change. For a single developer that friction compounds daily.

**Consequence.** The separate `DoneBot-iOS` GitHub repository is reserved for release runbooks, store assets and fastlane configuration — not source. See `90-STATE/DECISIONS.md`.

---

## D-03 · `:app` stays an AGP application module and never gets the KMP plugin

**Decision.** `app/build.gradle.kts` keeps `com.android.application` and its current plugin set. It shrinks to ~1,800 LOC — manifest-declared components only.

**Why.** That file carries google-services, crashlytics, firebase-perf, the baselineprofile plugin, the release `signingConfig`, `ndkVersion = "27.2.12479018"`, `androidResources.localeFilters`, the `releaseLocal` build type with its `beforeVariants` disabling, and R8 rules. All are AGP-application concerns that work today. `:app` never needs to compile for iOS, so applying the KMP plugin buys nothing and risks all of it.

**Enforcement.** If `app/build.gradle.kts` starts growing KMP configuration, this decision has been violated — stop and reassess.

---

## D-04 · Ship Android v1.2 first, then freeze Android feature work

**Decision.** v1.2 goes to Play. Read the current `versionCode` from `app/build.gradle.kts` rather than trusting any number written here — it moves, and an uploaded code is burned forever even if the draft is deleted. Tag `v1.2-preKMP`. Cut `release/1.2.x` for hotfixes and **`feat/ios-port` for the work**. No new Android features during migration.

**All port work lands on `feat/ios-port`, never on `main`** — see D-11.

**Why.** Development velocity on this repo has been ~10 commits/day. An in-place restructure running against that rate produces continuous merge conflicts and doubles the touch cost of every new screen.

---

## D-05 · Full feature parity, plus iPad, plus three iOS-only additions in 1.0

**Decision.** All 39 feature areas ship in iOS 1.0. Universal app (iPhone + iPad). Additionally: **WidgetKit** widgets, **App Intents/Siri**, **Sign in with Apple**.

**Why iPad is cheap here.** The Android app already has adaptive layout — `ResponsiveContainer`, `TDNavigationRail`, `GroupsTwoPane`, window size classes. `material3-window-size-class` is available in common code, so this ports rather than gets rebuilt.

**Why widgets are worth it.** Android has no widgets at all. On iOS this is a genuine differentiator and improves App Store discoverability. The Live Activity work (required for Pomodoro anyway) shares infrastructure with it.

**If the schedule slips, cut in this order:** App Intents/Siri → WidgetKit → iPad. Never cut Sign in with Apple (see D-06).

---

## D-06 · Sign in with Apple is mandatory, not optional

**Decision.** Implement Sign in with Apple in iOS 1.0, with a matching `POST auth/apple` endpoint in the backend.

**Why.** App Store Review Guideline 4.8 requires an equivalent privacy-preserving login option for any app that uses a third-party login service to set up or authenticate the primary account. The exception covers apps that use **exclusively** their own account system. DoneBot offers Google Sign-In alongside its own email/password system, so the exception does not apply and the requirement fires.

**This is a submission blocker.** `70-BACKEND/01` is on the critical path for `80-RELEASE`.

---

## D-07 · Reminders become a declarative, budgeted plan

**Decision.** Replace the imperative `AlarmScheduler` (`schedule`/`cancelRecurring`) with a whole-world reconcile: `ReminderScheduler.apply(specs: List<ReminderSpec>)`, idempotent, returning a coverage report.

**Why.** iOS enforces a hard ceiling of 64 pending notification requests and cannot run code at fire time — so Android's self-re-arming alarms have no analogue. A global budget cannot be expressed through per-call scheduling. Splitting `RescheduleAllAlarmsUseCase` into a pure `BuildReminderSpecsUseCase` (shared) plus a platform `apply` also removes the class of bug where the two platforms disagree about which reminders exist.

**Android keeps its proven internals.** `AlarmSchedulerImpl` becomes a thin adapter that diffs specs into the existing request-code calls. `AlarmRequestCodes.kt` and its test are untouched.

Full design: `30-PLATFORM/01-notifications-and-alarms.md`.

---

## D-08 · Do not declare `iosX64`

**Decision.** Only `iosArm64` and `iosSimulatorArm64`.

**Why.** The development machine is Apple Silicon and Xcode 27 drops Intel support. Declaring `iosX64` adds ~33% to Kotlin/Native build work for a target nobody will run.

---

## D-09 · Raise the AAB ceiling to 24 MiB, deliberately and once

**Decision.** After the resource migration completes, raise `AAB_MAX_BYTES` in `.github/workflows/ci.yml` from 20 MiB to 24 MiB, with a comment recording the measured deltas.

**Why.** Baseline is 18.17 MiB against a 20 MiB ceiling — 9% headroom. Projected net change is +0.3…+1.2 MiB (Ktor and CMP resources up, dead Maps deps and Hilt down). That lands under 20 MiB but with no slack. Deciding this now is engineering; discovering it at 19.9 MiB mid-migration is a crisis.

**Not a licence to be careless.** Every dependency-touching task still records the measured size. The `sqlite-bundled`-on-Android trap (+3–4 MiB) remains forbidden.

---

## D-10 · Spec language is English

**Decision.** Task files are written in English.

**Why.** The codebase, `CLAUDE.md`, all symbols, commands and paths are English. Keeping the spec in the same language eliminates term drift between instruction and code.

---

## D-11 · All port work happens on a branch, never on `main`

**Decision.** Every task in this spec — including the `20-MIGRATION` tasks that modify Android code — is committed to **`feat/ios-port`**, branched from `main`. `main` is never committed to directly.

**Why.** `main` is the branch the live Android app ships from, and the owner keeps working there: bug fixes, releases, sometimes mid-session. Landing a ~1,590-hour restructure on it would mean the branch that must stay releasable at all times is simultaneously half-migrated, for the better part of a year. Keeping the port on its own branch means a production hotfix is always one `git checkout main` away.

**Direction of flow.** Merge `main` **into** `feat/ios-port` to pick up Android fixes. Never the reverse, until the owner decides the port is ready.

**The one deliberate exception.** `20-12` reaches milestone M5, where the fully-migrated app is Android-shippable with zero iOS code written. Releasing Android 1.3 from there is a **merge to `main` that the owner decides on** — not something an agent does. Ask; do not merge.

**Enforcement.** `README.md` §0 rule 0 requires checking `git branch --show-current` before the first commit of any session. Two symptoms mean the branch changed underneath you: dirty files you did not touch, and an edit from earlier in the session having reverted.

**Merge hygiene.** `ios-spec/` itself is tracked on `main`, so `90-STATE/` conflicts on every merge from it. Keep the branch's `PROGRESS.md`; take both sides of the append-only `BLOCKERS.md` / `DECISIONS.md` and re-sort by date.

---

## D-12 · Human-gated prerequisites are never on the critical path

**Decision.** No task that a human must unblock — the Apple Developer enrolment (`10-01`), the macOS upgrade and Xcode install (`10-05`) — may gate work that does not genuinely need it. Where the graph said otherwise, the graph was wrong and was corrected.

**Why.** This spec is written to be executed without a human in the loop, and its two slowest prerequisites are the two a human owns. Measured on the original graph, `10-01` alone made **54 of 105 tasks unreachable**, and a single bundled task (`30-10`, Google + Apple sign-in together) put **41 tasks including the Home screen** behind a backend Apple endpoint. None of that was intended; it was the compounding of three individually reasonable-looking dependency edges.

**What changed.** `10-00` split into a 2-hour JDK pin and a separate `10-05` for macOS/Xcode (ADR-004). `10-03` dropped its `10-01` dependency, since simulator development needs no paid membership (ADR-005). `30-10` split, with the Apple half folded into the `60-03` that already specified it (ADR-006).

**Result.** With both human-gated tasks blocked, 32 tasks / **712 hours** still run. With only `10-01` blocked, 97 of 107.

**Enforcement.** When adding a dependency, ask whether the task can be *verified* without it, not whether it feels related. If a task needs a human prerequisite for one step out of ten, gate the step and record it — do not gate the task.

---

## D-13 · Estimates are honest, and the plan says so

**Decision.** The task-file estimates total **~1,590 hours**, and every summary in this spec states that figure and its calendar translation (≈ 9 months at 40 h/week, ≈ 14 months at 25 h/week) rather than a rounder, friendlier number.

**Why.** Earlier drafts described this as a "six-month restructure" in three places while the estimates said otherwise. That understates the commitment being made, and — worse — it makes D-01's rejected alternatives ("~10–14 months", "~12–18 months") look further away than they are, which is exactly the comparison a reader uses to sanity-check the architecture decision. D-01 still holds, but it holds on the *duplication* argument, not on a schedule advantage.

**Consequence.** If an estimate is revised during execution, update `PROGRESS.md`'s summary total in the same commit. A stale total is worse than none.
