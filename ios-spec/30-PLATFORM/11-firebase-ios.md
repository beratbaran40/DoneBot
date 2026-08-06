---
id: 30-11
title: Firebase on iOS
layer: platform
status: TODO
depends_on: [10-01, 10-03, 30-00]
blocks: [30-03]
parallel_safe: true
estimate: 16h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/analytics/**
  - shared/data/src/androidMain/**/{analytics,log,perf}/**
  - shared/data/src/iosMain/**/firebase/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Analytics, Crashlytics, Performance and App Check on iOS — with the same **consent gating** the Android app enforces.

## 2. Why this way

**Firebase stays in Swift; Kotlin talks to a protocol.** There is no official Firebase KMP SDK, and third-party wrappers add a dependency for something the app touches in four narrow places. A Swift-implemented protocol injected at startup is smaller, more debuggable, and keeps Firebase version upgrades on the Swift side.

**Consent gating is the part that must not be lost.** The Android app is careful here and the same rules apply on iOS:

- Crashlytics + Analytics follow `CrashAnalyticsPreferences` — **opt-out, default ON**
- Performance follows `TelemetryPreferences` — **opt-in, default OFF**
- **Never collect in debug builds**
- Manifest meta-data holds Analytics and Performance off until reconciliation runs

`Info.plist` has the direct equivalents (`FirebaseAutomaticScreenReportingEnabled`, `firebase_performance_collection_enabled`, `FIREBASE_ANALYTICS_COLLECTION_ENABLED`). Setting them correctly is what stops collection before the user's stored choice is read.

**App Check uses App Attest on iOS**, not Play Integrity, with DeviceCheck as the fallback below iOS 14. Same Firebase project, different provider.

**No AdID, on either platform.** Android removes the AD_ID permission trio deliberately. On iOS that means **not** linking `AdSupport` / `AppTrackingTransparency` — which keeps the app out of App Tracking Transparency entirely and keeps the privacy nutrition label clean.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `Application.kt` | The reactive consent reconciliation at startup, `installAppCheck()`, the debug-build guard |
| `data/analytics/FirebaseAnalyticsHelper.kt` (85 LOC) | The event taxonomy |
| `docs/analytics-events.md` | The documented taxonomy |
| `data/log/CrashlyticsTree.kt` (60 LOC) | Timber tree; `setUserId`; token-decrypt custom keys |
| `data/perf/PerfTrace.kt` (47 LOC) | Custom trace `sync_pending_tasks` |
| `domain/repository/{CrashAnalyticsPreferences,TelemetryPreferences}.kt` | The consent flags and their defaults |
| `app/src/main/AndroidManifest.xml` | The collection-off meta-data and the `tools:node="remove"` AD_ID trio |
| `app/build.gradle.kts` | Firebase BOM 34.9.0; the five products |

## 4. Target

- `shared/domain/…/analytics/` — `AnalyticsHelper` (exists), `CrashReporter`, `PerfTracer`, `AppAttest`
- `shared/data/iosMain/…/firebase/` — bridges calling a Swift protocol
- `iosApp/FirebaseBridge.swift` — the Swift implementations
- `iosApp/GoogleService-Info.plist`
- `iosApp/Info.plist` — the collection-off keys

## 5. Steps

1. **Register the iOS app** in the existing Firebase project (`10-01`); download `GoogleService-Info.plist`.

2. **Add Firebase via SPM**: Analytics, Crashlytics, Performance, AppCheck, Messaging. Not CocoaPods.

3. **Define the four contracts** in `:shared:domain`. `AnalyticsHelper` already exists.

4. **Implement the Swift bridge**, injected into Koin at startup so Kotlin never imports Firebase.

5. **Set the collection-off keys in `Info.plist`**, mirroring the Android manifest. Reconciliation turns collection on only after reading the user's stored choice.

6. **Port the consent reconciliation** to run on iOS at the same point in startup.

7. **Never collect in debug.** Mirror the Android guard.

8. **App Check with App Attest**, DeviceCheck fallback. Register the DeviceCheck private key in the Firebase console.

9. **Add the dSYM upload build phase.** Without it Crashlytics reports are unsymbolicated and useless.

10. **Do not link `AdSupport` or `AppTrackingTransparency`.** Keeps the app out of ATT and the nutrition label clean.

## 6. Code skeleton

```kotlin
// shared/domain/…/analytics/CrashReporter.kt
interface CrashReporter {
    fun log(message: String)
    fun setKey(key: String, value: String)
    fun setUserId(id: String?)
    fun recordNonFatal(throwable: Throwable)
    fun setEnabled(enabled: Boolean)
}
```

```kotlin
// shared/data/iosMain/…/firebase/FirebaseBridge.kt
// Kotlin never imports Firebase; Swift implements this and injects it at startup.
interface FirebaseNativeBridge {
    fun logEvent(name: String, params: Map<String, Any>)
    fun setAnalyticsEnabled(enabled: Boolean)
    fun crashLog(message: String)
    fun setCrashlyticsEnabled(enabled: Boolean)
    fun setPerformanceEnabled(enabled: Boolean)
}
```

```xml
<!-- iosApp/Info.plist — mirrors the Android manifest: collection stays OFF until
     reconciliation reads the user's stored consent. -->
<key>FIREBASE_ANALYTICS_COLLECTION_ENABLED</key><false/>
<key>firebase_performance_collection_enabled</key><false/>
<key>FirebaseAutomaticScreenReportingEnabled</key><false/>
```

## 7. Acceptance

- [ ] iOS app registered in the existing Firebase project; `GoogleService-Info.plist` in the bundle
- [ ] All four contracts in `:shared:domain`; both platforms registered
- [ ] Analytics events appear in the console with the **same names** as Android (`docs/analytics-events.md`)
- [ ] A forced test crash appears in Crashlytics, **symbolicated** (dSYM upload works)
- [ ] Performance traces appear when the user has opted in, and **not** otherwise
- [ ] Consent gating verified: Crashlytics/Analytics opt-out **default ON**, Performance opt-in **default OFF**
- [ ] **Nothing is collected in debug builds**
- [ ] App Check with App Attest succeeds; backend App Check enforcement passes
- [ ] `Info.plist` collection keys present and false
- [ ] `AdSupport` / `AppTrackingTransparency` **not linked**; no ATT prompt
- [ ] Toggling consent in Settings takes effect without a restart, as on Android

## 8. Pitfalls

- **Without the dSYM upload phase, every crash report is unsymbolicated.** They arrive, look fine in the count, and are useless.
- **Bitcode-style dSYM issues.** If crashes are unsymbolicated, check whether dSYMs are being generated at all (`DEBUG_INFORMATION_FORMAT = dwarf-with-dsym` for Release).
- **App Attest needs the capability on the App ID** and a registered DeviceCheck key.
- **Collection defaults are opposite between the two products.** Analytics/Crashlytics opt-out ON; Performance opt-in OFF. Getting this backwards is a privacy incident, not a bug.
- **Do not link `AdSupport`.** It drags the app into App Tracking Transparency and changes the nutrition label.
- **`GoogleService-Info.plist` is committed deliberately**, like `google-services.json`. It is not a secret — but the git history is a known exposure, so API key restrictions and App Check are what actually protect the project.
- **Do not use a Firebase KMP wrapper.** A Swift protocol is smaller and keeps upgrades on the Swift side.
- **Analytics event names must match Android exactly**, or the funnels in the console silently split in two.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
grep -rn "AdSupport\|AppTrackingTransparency" iosApp/ && echo "ATT LINKED" || echo "clean"

# On a device
#   force a test crash → appears symbolicated in Crashlytics
#   Settings → toggle crash/analytics consent → collection follows immediately
#   Settings → enable performance → traces appear; disable → they stop
#   debug build → nothing reaches the console
#   App Check → backend accepts the request
```
