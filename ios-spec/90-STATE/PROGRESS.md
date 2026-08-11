# PROGRESS — the ledger

> ## ⚠ Working branch: `feat/ios-port`
>
> **All work in this spec is committed here, never to `main`** (decision D-11).
> Check before your first commit of every session:
> ```bash
> git branch --show-current     # must NOT be main
> ```
> `main` is the live Android app's branch and the owner keeps working there.
> Merge `main` **into** this branch to pick up Android fixes — never the reverse.
> The only merge back to `main` is the owner's decision at milestone M5.
>
> **This file will conflict on every merge from `main`, by design.** The spec is
> tracked on `main`; the ledger is written on `feat/ios-port`. When `git merge main`
> conflicts here, **always keep the branch's version** (`git checkout --ours
> 90-STATE/PROGRESS.md`) — `main` never has newer status than the branch doing the work.
> The same applies to `BLOCKERS.md` and `DECISIONS.md`, which are append-only:
> take both sides and re-sort by date.

**Single source of truth for task status.** Update in the same commit as the status flip. Never batch updates.

Status vocabulary: `TODO` · `IN_PROGRESS` · `BLOCKED` · `DONE`

Pick rule: lowest phase, then lowest id, among tasks whose `depends_on` are all `DONE` and whose `owner_files` do not collide with an `IN_PROGRESS` task. Full rules in `../README.md` §2.1.

---

## Summary

| Phase | Total | DONE | IN_PROGRESS | BLOCKED | TODO | Estimate |
|---|---|---|---|---|---|---|
| 10 · Foundation | 6 | 0 | 2 | 0 | 4 | 56 h |
| 20 · Migration | 15 | 0 | 0 | 0 | 15 | 574 h |
| 30 · Platform | 16 | 0 | 0 | 0 | 16 | 278 h |
| 40 · Features | 53 | 0 | 0 | 0 | 53 | 400 h |
| 50 · Design system | 7 | 0 | 0 | 0 | 7 | 104 h |
| 60 · iOS native | 3 | 0 | 0 | 0 | 3 | 66 h |
| 70 · Backend | 2 | 0 | 0 | 0 | 2 | 52 h |
| 80 · Release | 5 | 0 | 0 | 0 | 5 | 60 h |
| **Total** | **107** | **0** | **2** | **0** | **105** | **1,590 h** |

> ### ⚠ Current sequencing — read before picking a task (ADR-012)
>
> **No code task starts until `10-01` and `10-05` resolve.** The owner is executing both
> now, in parallel, by choice — not because the graph requires it. `README.md` §0 is right
> that neither is needed to begin; this is a scheduling preference, and the payoff is that
> **all 107 tasks are reachable** when code work starts, with no human-gated prerequisite
> left anywhere in the graph.
>
> **`feat/ios-port` is not cut yet and `v1.2-preKMP` is not tagged yet.** Both happen on the
> day migration actually begins, against that day's `main` — Android is not frozen, so a
> branch cut today is stale before its first commit and the tag would not mark the real
> pre-migration boundary. Until the branch exists, spec-maintenance commits land on `main`
> (ADR-013).
>
> **Machine state, measured 2026-08-11:** macOS **14.5** (Xcode 26 needs ≥ 15.6) · no Xcode,
> `xcode-select -p` → `/Library/Developer/CommandLineTools` · no `simctl` · shell `java` is
> **OpenJDK 24** (the `Type T not present` trap) · JBR 21 present at
> `/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home` · on `main`, in sync
> with `origin/main`, tree clean, **no tags at all** · no `keystore.properties` · no
> `org.gradle.java.home` in `gradle.properties`.

**What 1,590 hours means.** ≈ 39 weeks at 40 h/week, ≈ 64 weeks at 25 h/week. The
critical path is only ~470 h, but with one developer the total is the real number.
Estimates are for scheduling, not budgets for an agent — see `../README.md` §2.2.

**Runway when the two human-gated tasks are `BLOCKED`** — recomputed after the
dependency corrections in ADR-004/005/006, and the reason those corrections were made.

**Independently re-verified 2026-08-11** by parsing the `depends_on` front-matter of all
107 task files and computing reachability from scratch: every row below reproduced exactly,
with no dangling dependencies and no cycles. Under ADR-012 these numbers are now
**historical** — the owner is clearing both prerequisites up front, so nothing will be
blocked when code work starts. Keep the table anyway: it is the evidence for D-12, and the
argument against ever re-coupling a human-gated task to the critical path.

| Blocked | Reachable | Hours |
|---|---|---|
| nothing | 107 / 107 | 1,590 h |
| `10-01` (Apple account) | 97 / 107 | 1,456 h |
| `10-01` + `70-01` (backend Apple endpoint) | 96 / 107 | 1,432 h |
| `10-01` + `10-05` (**no Xcode installed at all**) | 32 / 107 | 712 h |

The last row is the useful one for sequencing: **712 hours of this plan need no Xcode
and no Apple account.** Neither has to exist before `20-13`.

**AAB size ledger** — record after every dependency-touching task.

| Date | After task | AAB size | Ceiling | Note |
|---|---|---|---|---|
| 2026-08-06 | *(baseline, pre-migration)* | 18.17 MiB | 20 MiB | 9% headroom |

---

## 10 · Foundation

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 10-00 | Environment & toolchain — JDK pin | TODO | — | | **First code task.** Fixes the `Type T not present` trap. 2 h. |
| 10-01 | Apple Developer Program enrolment | IN_PROGRESS | — | 2026-08-11 | **Owner-executed, started now** (ADR-012). Check the "DoneBot" name first. Verification takes weeks. |
| 10-02 | Gradle / KMP plugin setup | TODO | 10-00 | | |
| 10-03 | iOS app shell (`iosApp/` Xcode project) | TODO | 10-05, 20-13 | | No longer needs 10-01 — simulator work needs no paid account (ADR-005). |
| 10-04 | CI: `detektAll`, nightly iOS job | TODO | 10-00, **20-01** | 2026-08-11 | `20-01` edge added — it creates `detektAll`. Nightly's green run deferred to 20-13 (ADR-011). |
| 10-05 | macOS upgrade & Xcode 26 | IN_PROGRESS | — | 2026-08-11 | **Owner-executed, started now**, in parallel with 10-01 (ADR-012). Gate command downgraded to `detektMain` + explicit `JAVA_HOME` (ADR-010). |

## 20 · Migration — sequential, Android green after every step

| id | title | status | depends_on | rev? | updated | notes |
|---|---|---|---|---|---|---|
| 20-00 | Migration protocol (read-only reference) | TODO | — | — | | The always-green rules. Read before 20-01. |
| 20-01 | Remove dead Maps deps + introduce `detektAll` | TODO | 10-00 | ✔ | | Size win may be ~0 — measure. `detektAll` must filter to production source sets. |
| 20-02 | Close the 2 Android leaks in `domain/` | TODO | 20-01 | ✔ | | `AlarmSoundPreferences`, `Authenticator` |
| 20-03 | `:shared:core` + `:shared:domain` (androidTarget only) | TODO | 20-02 | ✔ | | Tests do **not** move here — that is 20-03b. |
| 20-03b | Move the regression shields to `commonTest` | TODO | 20-03 | ✔ | | Assertion rewrite onto `kotlin.test`, not a `git mv`. Gates 20-04. |
| 20-04 | `java.time` → `kotlinx-datetime` 0.7.x | TODO | 20-03b | ✘ | | 93 files. Localized formatting needs a contract. |
| 20-05 | Hilt → Koin 4 (+ `KoinModulesTest`) | TODO | 20-03 | ✘ | | Test lands in the same PR — non-negotiable. |
| 20-06 | Retrofit + OkHttp `Authenticator` → Ktor 3 | TODO | 20-05 | ✔ | | ⚠ measure AAB immediately after. |
| 20-07 | Room → Room KMP | TODO | 20-05 | ✘ | | ⚠ **Highest data risk.** Schema JSON must diff clean. |
| 20-08 | DataStore → KMP factory (same file path) | TODO | 20-05 | ✔ | | |
| 20-09 | `:shared:resources` + `R` → `Res` | TODO | 20-03 | ✘ | | ~400 files. Raise `AAB_MAX_BYTES` here. |
| 20-10 | `:uikit` → KMP + CMP | TODO | 20-09 | ✔ | | Also fixes `com.example.uikit` namespace. |
| 20-11 | `:shared:ui` + `:composeApp` | TODO | 20-04, 20-06, 20-07, 20-08, 20-10 | ✘ | | The big one. Burn down by leverage. |
| 20-12 | Reduce `:app` to a shell (~1,800 LOC) | TODO | 20-11 | ✔ | | **Android 1.3 shippable here, zero iOS code.** |
| 20-13 | Declare iOS targets, fix common compile errors | TODO | 20-12, 10-05 | ✔ | | First `linkDebugFrameworkIosSimulatorArm64`. **First task that needs Xcode** (ADR-004). |

## 30 · Platform contracts

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 30-00 | Contract index (read-only reference) | TODO | — | | The 31 contracts, one table. |
| 30-01 | Notifications & alarms | TODO | 20-03, 20-13 | | ⚠ Highest risk. 64-slot budget design. |
| 30-02 | Background sync | TODO | 20-13 | | |
| 30-03 | Push (APNs + FCM) | TODO | 10-01, 20-13, 30-11 | | |
| 30-04 | Live Activity — Pomodoro | TODO | 20-13, 10-03 | | |
| 30-05 | Audio / ambience | TODO | 20-13 | | Needs `.m4a` transcode of the 3 loops. |
| 30-06 | Biometric + Keychain | TODO | 20-13 | | |
| 30-07 | Camera (polaroid capture) | TODO | 20-13 | | Only `LiveCameraPreview.kt` is platform-bound. |
| 30-08 | Photos & files | TODO | 20-13 | | |
| 30-09 | Location / places | TODO | 20-13 | | MapKit on iOS — no API key needed. |
| 30-10 | Social sign-in contract & Google Sign-In | TODO | 20-13, 10-03 | | Apple half split out to 60-03 — it dragged 41 tasks behind 70-01 (ADR-006). |
| 30-11 | Firebase on iOS (SPM) | TODO | 10-01, 10-03 | | Analytics, Crashlytics, Perf, App Check, Messaging. |
| 30-12 | Permissions | TODO | 20-13 | | |
| 30-13 | Deep links & universal links | TODO | 20-13 | | |
| 30-14 | Locale & appearance | TODO | 20-13 | | |
| 30-15 | Misc (haptics, share, external links, screen behavior, app info, logger, webview) | TODO | 20-13 | | |

## 40 · Features — Android `ui/` 1:1

All depend on `20-11` plus their own design-system prerequisites. `00-feature-index.md` holds the full dependency graph and build order.

### auth (8)
| id | title | status | notes |
|---|---|---|---|
| 40-auth-01 | onboarding | TODO | Start destination when logged out |
| 40-auth-02 | login | TODO | |
| 40-auth-03 | register | TODO | |
| 40-auth-04 | forgotpassword | TODO | |
| 40-auth-05 | resetpassword | TODO | Deep-link consumer |
| 40-auth-06 | changepassword | TODO | |
| 40-auth-07 | auth-scaffold | TODO | Shared chrome, outside the root Scaffold |
| 40-auth-08 | splash | TODO | |

### core (11)
| id | title | status | notes |
|---|---|---|---|
| 40-core-01 | home | TODO | 3,510 LOC — split into sub-steps |
| 40-core-02 | creationhub | TODO | 2,118 LOC |
| 40-core-03 | chat | TODO | 1,979 LOC — `LocalIntentClassifier` is shared |
| 40-core-04 | details | TODO | 1,961 LOC |
| 40-core-05 | search | TODO | |
| 40-core-06 | notifications | TODO | |
| 40-core-07 | activity | TODO | Heatmap + hearts |
| 40-core-08 | calendar | TODO | |
| 40-core-09 | filteredtasks | TODO | |
| 40-core-10 | invitations | TODO | |
| 40-core-11 | planyourday | TODO | |

### groups (9)
| id | title | status | notes |
|---|---|---|---|
| 40-groups-01 | groups-root (+ two-pane) | TODO | 9,380 LOC across the subtree |
| 40-groups-02 | groupdetail | TODO | 11 files, 3 tabs |
| 40-groups-03 | createnewgroup | TODO | |
| 40-groups-04 | groupsettings | TODO | |
| 40-groups-05 | grouptaskdetail | TODO | |
| 40-groups-06 | invitemember | TODO | |
| 40-groups-07 | managemembers | TODO | |
| 40-groups-08 | memberprofile | TODO | |
| 40-groups-09 | transferownership | TODO | |

### journal (3)
| id | title | status | notes |
|---|---|---|---|
| 40-journal-01 | journal-timeline | TODO | Biometric-gated |
| 40-journal-02 | journal-entry | TODO | |
| 40-journal-03 | journal-camera | TODO | ~1,900 LOC of Canvas + AVFoundation capture |

### pomodoro (4)
| id | title | status | notes |
|---|---|---|---|
| 40-pomodoro-01 | pomodoro | TODO | Portrait + landscape, ambience scenes |
| 40-pomodoro-02 | addpomodorotimer | TODO | |
| 40-pomodoro-03 | pomodorosummary | TODO | |
| 40-pomodoro-04 | pomodorolaunch | TODO | |

### settings (8)
| id | title | status | notes |
|---|---|---|---|
| 40-settings-01 | settings | TODO | 8 sections, 2,759 LOC |
| 40-settings-02 | profile | TODO | + health badge |
| 40-settings-03 | avatarcrop | TODO | Full-bleed, no top bar |
| 40-settings-04 | alarmsounds | TODO | iOS: bundled sounds only |
| 40-settings-05 | appcolors | TODO | Palette picker |
| 40-settings-06 | blockedusers | TODO | |
| 40-settings-07 | licenses | TODO | |
| 40-settings-08 | security-biometric | TODO | |

### misc (6)
| id | title | status | notes |
|---|---|---|---|
| 40-misc-01 | topbar | TODO | `TDTopBar` + `AvatarChip` |
| 40-misc-02 | overlay-replacement | TODO | ⚠ Redesign, not a port |
| 40-misc-03 | banner | TODO | Pomodoro in-app banner |
| 40-misc-04 | permissions | TODO | |
| 40-misc-05 | update | TODO | Play in-app update → App Store lookup |
| 40-misc-06 | webview | TODO | |

### shared-ui (3)
| id | title | status | notes |
|---|---|---|---|
| 40-shared-01 | taskform | TODO | `ui/common/taskform/` |
| 40-shared-02 | locationpicker | TODO | `ui/common/locationpicker/` |
| 40-shared-03 | shared-misc | TODO | Rest of `ui/common/` |

### index
| id | title | status | notes |
|---|---|---|---|
| 40-index | Feature dependency graph & build order | TODO | Read before starting any 40-* task |

## 50 · Design system

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 50-00 | Tokens (45 colors × 6 palettes, 15 type styles, shapes) | TODO | 20-10 | | |
| 50-01 | Palette kits & `TDStyle` | TODO | 50-00 | | MONOCHROME is colour-only — 2 geometry systems, not 3. |
| 50-02 | Components tier A — Canvas-heavy (8) | TODO | 50-00 | | Charts, heatmap, health bar, wheel picker, confetti |
| 50-03 | Components tier B — animation-driven (14) | TODO | 50-00 | | |
| 50-04 | Components tier C — mechanical (≈45) | TODO | 50-00 | | Includes the 846-LOC text-field pair |
| 50-05 | Icons & pixel-art generation pipeline | TODO | 20-09 | | 232 vectors (89 `:app` + 143 `:uikit`) + `genpixelicons.py` |
| 50-06 | Adaptive layout / iPad | TODO | 20-11 | | Window size class is already threaded through |

## 60 · iOS-native additions

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 60-01 | WidgetKit widgets | TODO | 10-03, 30-04 | | Shares App Group + Live Activity infra |
| 60-02 | App Intents / Siri | TODO | 10-03 | | |
| 60-03 | Sign in with Apple (client, end to end) | TODO | 10-01, 30-10, 70-01 | | **Submission blocker.** Now owns the whole Apple client path, not just the button. |

## 70 · Backend (executed in `~/AndroidStudioProjects/ToDoBackend`)

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 70-01 | `POST auth/apple` + Apple JWKS verification | TODO | — | | **Submission blocker.** ~3-4 days. |
| 70-02 | Reminder push + `users.timezone`/`locale`, `device_tokens.platform` | TODO | — | | ~4 days. Rides the existing 5-min tick. |

## 80 · Release

| id | title | status | depends_on | updated | notes |
|---|---|---|---|---|---|
| 80-01 | App Store Connect setup | TODO | 10-01 | | Reserve the name "DoneBot" early. |
| 80-02 | Privacy manifest & compliance | TODO | 10-03 | | `PrivacyInfo.xcprivacy` written early, not at submission. |
| 80-03 | Screenshots & store listing (EN + TR) | TODO | 40-* | | 6.9" iPhone + 13" iPad required. |
| 80-04 | TestFlight | TODO | 80-01, 80-02 | | Measure delivered/expected reminder ratio for 2 weeks. |
| 80-05 | Submission checklist | TODO | 80-03, 80-04, 60-03 | | Demo account with a populated group. |

---

## Milestone gates

Binary, mechanically checkable. See `../README.md` §3 for the commands.

| Gate | Requires | Status |
|---|---|---|
| **M0** Toolchain | 10-00 (JDK). 10-01 and 10-05 run on their own clocks and gate M6, not M0. | ☐ |
| **M1** KMP skeleton | 20-01, 20-03, 20-03b | ☐ |
| **M2** Platform-free core | 20-04 + purity grep | ☐ |
| **M3** Data on KMP | 20-06, 20-07, 20-08 + schema diff | ☐ |
| **M4** Design system on CMP | 20-10 | ☐ |
| **M5** Whole app on CMP — *Android 1.3 shippable* | 20-12 | ☐ |
| **M6** iOS compiles & launches | 10-05, 20-13, 10-03 | ☐ |
| **M7** iOS feature-complete | all 40-*, 50-06 | ☐ |
| **M8** Reminder parity proven | 30-01 + `ReminderPlannerTest` | ☐ |
| **M9** Submitted | 80-05 | ☐ |
