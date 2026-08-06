---
id: 40-index
title: Feature index — dependency graph & build order
layer: ui
status: TODO
depends_on: [20-11]
blocks: []
parallel_safe: true
estimate: 1h (reading)
reversible: true
owner_files: []
verify:
  - "Read-only. Mark DONE once read."
---

## 1. Goal

The order to port 53 feature areas, and what each one is waiting on. **Read this before starting any `40-*` task.**

## 2. Why this way

**Every feature task is the same shape**, which is the point: `20-11` already moved `ui/` into `:shared:ui`, so a feature task is *verification plus the platform escapes specific to that screen*. Not a rewrite.

**Order by leverage, not by folder listing.** The five bottom-bar destinations are what a user sees first and what most other screens navigate from; the auth flow gates everything; the long tail can follow. Track the burn-down as *screens verified on iOS / 46*.

**Most features have no platform dependency at all.** Of the 53, roughly 35 are pure Compose that need only a look on both platforms. The ones that genuinely need a `30-PLATFORM` contract are called out below — those are the ones worth scheduling around.

## 3. Build order

### Wave 1 — foundation (nothing else works without these)
`auth-07` auth-scaffold · `auth-08` splash · `auth-01` onboarding · `auth-02` login · `auth-03` register · `misc-01` topbar · `shared-03` shared-misc

### Wave 2 — the five tabs
`core-01` home · `core-08` calendar · `core-03` chat · `groups-01` groups-root · `core-07` activity

### Wave 3 — core task flows
`shared-01` taskform · `core-02` creationhub · `core-04` details · `core-05` search · `core-09` filteredtasks · `core-11` planyourday

### Wave 4 — pomodoro & notifications
`pomodoro-01` pomodoro · `pomodoro-02` addpomodorotimer · `pomodoro-03` pomodorosummary · `pomodoro-04` pomodorolaunch · `misc-03` banner · `core-06` notifications · `misc-02` overlay-replacement

### Wave 5 — groups subtree
`groups-02` groupdetail · `groups-03` createnewgroup · `groups-04` groupsettings · `groups-05` grouptaskdetail · `groups-06` invitemember · `groups-07` managemembers · `groups-08` memberprofile · `groups-09` transferownership · `core-10` invitations

### Wave 6 — journal
`journal-01` timeline · `journal-02` entry · `journal-03` camera

### Wave 7 — settings & the rest
`settings-01` settings · `settings-02` profile · `settings-03` avatarcrop · `settings-04` alarmsounds · `settings-05` appcolors · `settings-06` blockedusers · `settings-07` licenses · `settings-08` security-biometric · `auth-04` forgotpassword · `auth-05` resetpassword · `auth-06` changepassword · `shared-02` locationpicker · `misc-04` permissions · `misc-05` update · `misc-06` webview

## 4. Platform dependencies

Only these features need a `30-PLATFORM` contract. Everything else is pure Compose.

| Feature | Needs |
|---|---|
| `auth-02` login, `auth-03` register | `30-10` social sign-in |
| `auth-05` resetpassword | `30-13` deep links |
| `core-01` home | `30-01` reminders |
| `core-03` chat | — (backend proxy; `LocalIntentClassifier` is shared) |
| `core-04` details, `shared-01` taskform | `30-01` reminders, `30-08` photos |
| `core-06` notifications | `30-03` push |
| `journal-01` timeline | `30-06` biometric |
| `journal-02` entry | `30-08` photos |
| `journal-03` camera | `30-07` camera |
| `pomodoro-01` pomodoro | `30-04` Live Activity, `30-05` audio |
| `misc-02` overlay-replacement | `30-01` alarm presenter — **redesign, not a port** |
| `misc-05` update | `30-15` app update |
| `misc-06` webview | `30-15` web view |
| `settings-01` settings | `30-14` locale, `30-12` permissions |
| `settings-03` avatarcrop | `30-08` image codec |
| `settings-04` alarmsounds | `30-05` sound catalog |
| `settings-08` security-biometric | `30-06` biometric |
| `shared-02` locationpicker | `30-09` place search |

## 5. Size

Largest first — the ones worth splitting into sub-steps:

| Feature | Files | LOC |
|---|---|---|
| `groups-*` (whole subtree) | 42 | 9,380 |
| `journal-*` | 29 | 4,075 |
| `core-01` home | 14 | 3,510 |
| `shared-*` (`ui/common`) | 33 | 3,478 |
| `settings-01` settings | 20 | 2,759 |
| `pomodoro-01` pomodoro | 20 | 2,737 |
| `core-02` creationhub | 10 | 2,118 |
| `core-03` chat | 6 | 1,979 |
| `core-04` details | 7 | 1,961 |

The remaining 30 features are under 1,500 LOC each; most are under 500.

## 6. The shape of every feature task

Since `20-11` already moved the code, each task is:

1. **Verify it compiles in `commonMain`** — or record the specific blocking import.
2. **Wire any platform contract** it needs, from the table above.
3. **Render every reachable `UiState`** on both platforms, light and dark, all three palette kits.
4. **Compare against `docs/screenshots/`** where a reference exists (23 of the 39 areas have one).
5. **Verify previews** cover every reachable state — `CLAUDE.md` treats missing previews as incomplete.
6. **Check both languages.**

## 7. Acceptance

- [ ] This file has been read
- [ ] The wave order and the platform-dependency table are understood

## 8. Pitfalls

- **Do not port in folder order.** Wave 1 gates everything; starting elsewhere means nothing is runnable end to end.
- **Do not treat these as rewrites.** The code moved in `20-11`. If a feature task turns into a rewrite, something went wrong upstream.
- **`misc-02` overlay-replacement is the exception** — a genuine redesign with no iOS equivalent.
- **`UiState.Success` must not be clobbered on refresh.** A documented anti-pattern in this codebase, and it affects any screen with a sheet or form state.
- **Three palette kits, two themes, two languages.** That is twelve combinations per screen; check the ones a kit actually changes rather than all twelve blindly.

## 9. Verification

Read-only. Mark `DONE` once read; then start with `40-auth-07`.
