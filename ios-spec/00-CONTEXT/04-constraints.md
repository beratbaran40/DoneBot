# Constraints — Apple's, and this repo's

Hard limits. None of these are negotiable by an implementing agent. Where a limit forces a product degradation, the degradation and its user-facing wording are specified — do not invent your own.

---

## 1. Apple platform limits

### 1.1 Toolchain (verified 2026-08)

| Constraint | Value | Consequence |
|---|---|---|
| Minimum SDK for App Store | **iOS 26 SDK**, mandatory since 28 Apr 2026 | Xcode 26+ required for any new submission *or update* |
| Xcode 26 minimum macOS | **macOS Sequoia 15.6**; Xcode 26.4 needs **macOS Tahoe 26.2** | Dev machine is on macOS 14.5 → upgrade is a prerequisite **for `20-13` onward only**, not for the migration (task `10-05`, D-12) |
| Kotlin/Native iOS targets | Need the **full Xcode**, not Command Line Tools | `linkDebugFrameworkIosSimulatorArm64` shells out to `xcrun` against the real iOS SDK |
| Intel Macs | Xcode 26 is universal; **Xcode 27 drops Intel** | Machine is Apple Silicon — fine |
| Deployment target | Independent of build SDK | Can still support older iOS; set in `10-FOUNDATION/03` |

Building with the iOS 26 SDK does **not** force users onto iOS 26.

### 1.2 Local notifications — the binding constraint

| Limit | Detail |
|---|---|
| **64 pending requests** | Hard system cap per app. Additional `add()` calls are dropped silently. |
| **No code at fire time** | `UNNotificationServiceExtension` runs for *push* only, never local. Android's self-re-arming alarm has no analogue. |
| Repeating triggers | `UNCalendarNotificationTrigger(repeats: true)` costs **1 request forever** — but supports no end date, so any rule with `until` must be materialized. |
| Interruption levels | `.timeSensitive` breaks through Focus and is available to all apps (user-revocable). `.critical` requires an Apple entitlement granted essentially only to safety apps — **do not plan on it.** |
| Sounds | Bundled `.caf`/`.aiff`/`.wav`, **≤ 30 s**. There is no system ringtone picker. |

Design that lives with this: `30-PLATFORM/01-notifications-and-alarms.md`.

### 1.3 Background execution

- `BGAppRefreshTask` / `BGProcessingTask` are **opportunistic**, never guaranteed. Effectively disabled in Low Power Mode, and may never run for a user who force-quits.
- Silent push (`content-available: 1`) is the more reliable trigger, but is itself rate-limited.
- **Never make correctness depend on background execution.** Foreground reconcile is the primary path; background is an optimization.

### 1.4 Capabilities with no iOS equivalent

| Android | iOS reality |
|---|---|
| `SYSTEM_ALERT_WINDOW` overlay over other apps | **Impossible.** Replaced by `.timeSensitive` banner + Live Activity. |
| `FLAG_SECURE` (blocks screenshots & recording) | **No API.** Only the app-switcher snapshot can be hidden, via `willResignActive`. Screenshots cannot be blocked. |
| Arbitrary foreground services | No equivalent. Pomodoro uses ActivityKit Live Activity + `AVAudioSession` background audio. |
| `RingtoneManager` system sound list | Bundled sounds only. |
| WorkManager unique work / `ExistingWorkPolicy` | `BGTaskScheduler` has no equivalent semantics. |

### 1.5 Media

- **iOS cannot play Ogg Vorbis.** The three ambience loops (`app/src/main/res/raw/ambience_{fireplace,rain,handpan}.ogg`) need an AAC/`.m4a` sibling. `tools/prep_ambience.sh` must be extended.

### 1.6 App Store requirements

| Requirement | Detail |
|---|---|
| **Sign in with Apple** | Mandatory — Google Sign-In is offered, so the "own system only" exception does not apply (see D-06). |
| `PrivacyInfo.xcprivacy` | Required. Must declare required-reason APIs — `UserDefaults`, file timestamps, disk space all apply here. Written in `10-FOUNDATION`, not at submission. |
| Account deletion in-app | Guideline 5.1.1(v). Already satisfied — `DELETE users/me` exists. Verify it stays reachable. |
| Export compliance | HTTPS + platform crypto only → `ITSAppUsesNonExemptEncryption = false`. |
| Demo account | Review requires working credentials, including a populated group. |
| Screenshots | 6.9" iPhone required; 13" iPad required because the app is Universal. EN + TR listings. |
| Apple Developer Program | 99 USD/yr, individual enrolment, identity verification can take weeks. |

---

## 2. This repository's limits

### 2.1 The CI gate

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
```

- **`detektAll`, not `detektMain`.** `detektMain` is an AGP-variant task. Once a module becomes KMP it produces `detektMetadataMain`/`detektAndroidDebug`, and `detektMain` keeps succeeding while checking nothing. Introduced in `20-MIGRATION/01`; use `detektMain` before that. **It aggregates production source sets only** — the unfiltered `withType<Detekt>()` returns eleven tasks per Android module and reproduces the variant fan-out that already broke CI once (ADR-008).
- **`testDebugUnitTest`, not `test`** — the release unit-test variant OOMs the Kotlin daemon.
- **Never pipe Gradle output through `grep`/`tail`.** The pipe masks the exit code; a failed build reads as success.

### 2.2 The AAB size budget

Baseline **18.17 MiB** against `AAB_MAX_BYTES = 20971520` (20 MiB). Projected ledger:

| Change | Δ |
|---|---|
| Remove `maps-compose` (the only genuinely severable Maps artifact) | **~0** — see note |
| Hilt → Koin | −0.2…−0.5 MiB |
| Retrofit → Ktor | +0.3…+0.8 MiB ⚠ |
| CMP resources (+ loss of AAB language split for those strings) | +0.3…+0.7 MiB ⚠ |
| Coil 2 → 3 | +0.1 MiB |
| Room KMP with correct drivers | ~0 |
| **Room KMP with `sqlite-bundled` on Android — FORBIDDEN** | **+3…+4 MiB ❌** |

**Note on the Maps row — measured, not estimated.** An earlier plan assumed removing `play-services-maps`, `play-services-location` and `maps-compose` would bank 0.2–0.6 MiB. It will not. `com.google.android.libraries.places:places:3.5.0` transitively depends on `play-services-location:21.0.1` and `play-services-maps:17.0.0`, so removing those two *explicit* declarations does not remove them from the build. Only `maps-compose` is severable, and R8 already strips unreferenced code. **Do not plan around headroom from this task** — `20-01` records whatever it actually measures.

Consequence: there is less slack than the ledger originally suggested, which makes the deliberate ceiling raise below more necessary, not less.

Ceiling is raised to 24 MiB once, deliberately, in `20-MIGRATION/09` (see D-09).

### 2.3 JDK

Gradle requires **JDK/JBR 21**. JDK 24 fails with `Type T not present`. The correct runtime is at
`/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home` (openjdk 21.0.9) and is **not** wired into `gradle.properties` — `10-FOUNDATION/00` fixes this permanently. That task is the JDK pin *only*; the macOS/Xcode toolchain is `10-FOUNDATION/05` and is not a prerequisite for any migration task before `20-13`.

### 2.4 Data safety

- Room is at **version 30**: auto-migrations 1→30 with **4 `AutoMigrationSpec`s**, **2 manual migrations**, and **30 committed schema JSONs** (`1.json`…`30.json`) in `app/schemas/`.
- **The schema JSON is the contract.** After the Room KMP port, regenerated `30.json` must `diff` byte-identical against the committed file. A difference means the schema changed and every existing install is at risk.
- `MigrationTest.kt` stays an instrumented test (needs a real device).

### 2.5 Localization

- EN and TR at **exact parity**, enforced by convention and the `check-l10n` skill. **1,135** app string keys + 9 plurals, **73** uikit keys, per language — 1,208 in total across the two modules. (An earlier revision of this line read "1,208 app keys + 73 uikit", double-counting uikit; `03-source-map.md` §9 and `20-MIGRATION/09` have the correct split.)
- Only `en` and `tr` ship (`androidResources.localeFilters`).
- No user-visible string may be hardcoded in Kotlin or Compose.

### 2.6 Style

`/CLAUDE.md` at the repo root remains authoritative. Highest-frequency rules: no hardcoded colors or text styles (`TDTheme.*` only), MVI three-file structure per feature, previews for every reachable `UiState`, line length 160, new UI files under ~300 lines, **no Material icons exist** (every icon is a project drawable via `tdPainter`).

---

## 3. Degradations, and exactly how they are communicated

Never ship a silent degradation. Each row below has a specified user-facing surface.

| Android today | iOS 1.0 | Where the user is told |
|---|---|---|
| Full-screen alarm overlay over any app | Banner, `.timeSensitive` | Settings → Notifications: explain Time Sensitive, with a button to `UIApplication.openNotificationSettingsURLString` |
| Any system ringtone | 6 bundled `.caf` sounds | Alarm Sounds screen simply has no "System" section |
| Unlimited armed reminders | ≤ 60 | Live Settings row: **"Scheduled through 27 Aug · 4 later reminders will be set the next time you open DoneBot"** |
| Alarm pierces silent mode | Respects the ringer switch | Onboarding notification-permission step |
| Re-arms after reboot with no launch | Repeating triggers survive; the materialized window survives but does not extend | Same "Scheduled through" row |
| `FLAG_SECURE` on journal / secret mode | Screenshots cannot be blocked | Journal privacy setting text must not promise screenshot protection on iOS |
| Live Pomodoro countdown notification | Live Activity, or one end-of-session notification | — |

The **"Scheduled through &lt;date&gt;"** row is the single most important item in this table. It converts an invisible platform limit into a visible, actionable state, and it is what stops the iOS app reading as broken.
