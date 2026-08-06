# Decision Record — locked

These decisions were made deliberately, with the alternatives costed. **Do not relitigate them.** If you believe one is wrong, write the argument in `90-STATE/DECISIONS.md` and continue executing the current decision — do not act on the disagreement.

---

## D-01 · Kotlin Multiplatform + Compose Multiplatform, migrating this repo in place

**Decision.** Convert the existing Android repository to KMP/CMP, then add an iOS target. Share domain, data, design system, and UI. Write only genuinely platform-bound surfaces natively.

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
- *KMP core + SwiftUI UI* — ~10–14 months; low sharing ratio because domain is thin; UI maintained twice forever.
- *Pure SwiftUI rewrite* — ~12–18 months; 90k LOC reimplemented; sync state machine, recurrence engine and health-points math duplicated, which is where silent divergence would appear.

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

**Decision.** v1.2 goes to Play. Read the current `versionCode` from `app/build.gradle.kts` rather than trusting any number written here — it moves, and an uploaded code is burned forever even if the draft is deleted. Tag `v1.2-preKMP`. Cut `release/1.2.x` for hotfixes. Migration lands on `main`. No new Android features during migration.

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
