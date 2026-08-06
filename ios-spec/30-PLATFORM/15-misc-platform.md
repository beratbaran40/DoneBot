---
id: 30-15
title: Misc platform contracts
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-misc-05, 40-misc-06]
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/platform/**
  - shared/data/src/androidMain/**/platform/**
  - shared/data/src/iosMain/**/platform/**
  - shared/ui/src/*/**/platform/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The remaining nine small contracts: connectivity, external links, screen behaviour, secure screen, app update, web view, app info, logger, haptics. Individually trivial; collectively they unblock the last of the UI layer.

## 2. Why this way

These are grouped because each is 20–80 lines and none warrants its own task. They are also where **the most honest degradation in the whole port** lives.

**`FLAG_SECURE` has no iOS equivalent.** Android blocks screenshots and screen recording on the journal, auth screens and secret-mode surfaces. iOS offers **nothing** comparable — the best available is hiding content in the app-switcher snapshot via `willResignActive`. Screenshots and recording cannot be blocked.

That is a real reduction in a privacy feature, and it has a **copy implication**: any UI text promising that the journal is protected from screenshots must not appear on iOS. Shipping the Android wording would be a false claim about a privacy feature.

**`BuildConfig` is Android-only**, which surprises people late. Nine files use `android.os.Build` and several use `BuildConfig.BASE_URL`. Both need `AppInfo`.

**Haptics need no contract.** CMP's `LocalHapticFeedback` already works on both platforms. The one `ToneGenerator` use has no iOS equivalent and should simply be dropped.

## 3. Source — read before writing

| Contract | Android source | iOS approach |
|---|---|---|
| **ConnectivityMonitor** | `data/network/NetworkMonitor.kt` — `ConnectivityManager` callback → `StateFlow<Boolean>` | `NWPathMonitor` |
| **ExternalLinks** | `ui/common/FeedbackIntent.kt` (mailto), `TaskLocationIntent.kt` (geo:), `common/Permissions.kt` (settings) — 11 `Intent` uses | `UIApplication.open`, `MFMailComposeViewController`, `maps://`, `UIActivityViewController`, `openSettingsURLString` |
| **ScreenBehavior** | `ui/common/LockScreenOrientation.kt`, `AnimationsEnabled.kt` (`ANIMATOR_DURATION_SCALE`), keep-awake | `supportedInterfaceOrientations`, `UIAccessibility.isReduceMotionEnabled`, `isIdleTimerDisabled` |
| **SecureScreenFlag** | `ui/common/SecureScreen.kt` — `FLAG_SECURE` | **no equivalent** — `willResignActive` snapshot hiding only |
| **AppUpdateChecker** | `data/update/PlayAppUpdateChecker.kt`, `domain/update/AppUpdateChecker.kt` | iTunes lookup API |
| **WebViewHost** | `ui/webview/` (3 files, 221 LOC) | `WKWebView` in `UIKitView` |
| **AppInfo** | `BuildConfig`, `Build.MODEL`, `Build.VERSION` | `Bundle.main.infoDictionary`, `UIDevice` |
| **Logger** | Timber, `CrashlyticsTree` | `os_log` |
| **Haptics** | CMP `LocalHapticFeedback` | same — **no contract needed** |

## 4. Target

`shared/domain/…/platform/` — eight interfaces; `shared/data/{androidMain,iosMain}/…/platform/` — the implementations. `ScreenBehavior`, `SecureScreenFlag` and `WebViewHost` are `@Composable expect` in `:shared:ui`.

## 5. Steps

1. **Define all eight contracts** in one pass; they barely interact.

2. **Android: wrap what exists.** No behaviour changes anywhere.

3. **`ConnectivityMonitor` on iOS** — `NWPathMonitor` on a background queue, mapped to the same `StateFlow<Boolean>`.

4. **`ExternalLinks`** — mail via `MFMailComposeViewController` (falls back to a `mailto:` URL), maps via `maps://`, share via `UIActivityViewController`, settings via `openSettingsURLString`, notification settings via `openNotificationSettingsURLString`, clipboard via `UIPasteboard`.

5. **`SecureScreenFlag`** — Android keeps `FLAG_SECURE`. iOS hides content on `willResignActive` and restores on `didBecomeActive`. **Expose `blocksScreenshots: Boolean`** so the UI can adjust its copy.

6. **Audit the privacy copy.** Any string claiming screenshot protection must be conditional on `blocksScreenshots`, or reworded to something true on both platforms. This is a correctness issue, not polish.

7. **`AppUpdateChecker`** — Android keeps Play in-app updates. iOS queries the iTunes lookup API and shows the existing `TDUpdateAvailableDialog`, deep-linking to the App Store page. A no-op implementation is acceptable for 1.0; record the choice.

8. **`WebViewHost`** — `WKWebView` in `UIKitView`. Used only for privacy policy and terms.

9. **`AppInfo`** — version, build, device model, OS version, base URL. `BASE_URL` needs a KMP config source since `BuildConfig` does not exist; a generated Kotlin object or a plist read.

10. **`Logger`** — Timber on Android, `os_log` on iOS, both feeding `CrashReporter` in release.

11. **Delete the `ToneGenerator` use.** One call site, no iOS equivalent, and `LocalHapticFeedback` already covers the intent.

## 6. Code skeleton

```kotlin
// shared/ui/commonMain/…/platform/SecureScreen.kt
//
// iOS has NO equivalent of FLAG_SECURE. Screenshots and screen recording cannot be
// blocked; the most that is possible is hiding content in the app-switcher snapshot.
// blocksScreenshots exists so the UI can tell the truth about what is protected.
@Composable expect fun SecureScreenEffect()
expect val blocksScreenshots: Boolean      // Android true, iOS false
```

```kotlin
// shared/domain/…/platform/ExternalLinks.kt
interface ExternalLinks {
    fun openUrl(url: String)
    fun openMail(to: String, subject: String, body: String)
    fun openMap(lat: Double, lng: Double, label: String)
    fun share(text: String)
    fun openAppSettings()
    fun openNotificationSettings()
    fun copyToClipboard(text: String)
}
```

```kotlin
// shared/domain/…/platform/AppInfo.kt
// BuildConfig is Android-only, which is why BASE_URL needs a KMP config source.
expect val appVersionName: String
expect val appBuildNumber: String
expect val deviceModel: String
expect val osVersion: String
expect val baseUrl: String
```

## 7. Acceptance

- [ ] All eight contracts defined; both platforms registered
- [ ] Android behaviour unchanged across every one
- [ ] iOS connectivity reflects real network state, including airplane mode
- [ ] Mail, maps, share, settings, notification settings and clipboard all work on iOS
- [ ] Portrait lock works where required (polaroid camera)
- [ ] Reduce Motion is honoured on iOS as `LocalReduceMotion` is on Android
- [ ] iOS hides content in the app-switcher snapshot on secure screens
- [ ] **Privacy copy audited** — no string claims screenshot protection where `blocksScreenshots` is false
- [ ] Web view loads privacy policy and terms on both platforms
- [ ] `AppInfo` returns correct values; `BASE_URL` resolves on both platforms
- [ ] Logging reaches `CrashReporter` in release on both platforms
- [ ] The `ToneGenerator` call site is removed

## 8. Pitfalls

- **Do not claim screenshot protection on iOS.** The journal and secret mode are privacy features; a false claim about a privacy feature is worse than the missing capability.
- **`NWPathMonitor` must run on a background queue** and be cancelled on teardown, or it leaks.
- **`MFMailComposeViewController` needs a configured Mail account.** Check `canSendMail()` and fall back to `mailto:`.
- **`UIActivityViewController` needs a `popoverPresentationController` source on iPad** or it crashes. The app is Universal.
- **`isIdleTimerDisabled` must be reset.** Leaving it on drains the battery long after the pomodoro ends.
- **`BuildConfig` does not exist in common code.** `BASE_URL` needs a real KMP config source — decide and record it.
- **`os_log` is not `println`.** Use the unified logging API so release logs are actually retrievable.
- **Do not add a haptics contract.** CMP already covers it.
- **The iTunes lookup API is cached and can be stale** for hours after a release. Do not build a hard gate on it.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a device
#   airplane mode on/off → offline banner appears and clears
#   Settings → send feedback → mail composer; task location → Apple Maps; share → sheet
#   permission row → opens the right Settings page
#   journal → app switcher → content hidden
#   confirm no UI text promises screenshot blocking on iOS
#   privacy policy and terms load in the web view
#   iPad: share sheet does not crash
```
