---
id: 30-00
title: Platform contract index (reference — no code)
layer: platform
status: TODO
depends_on: []
blocks: [30-01]
parallel_safe: true
estimate: 1h (reading)
reversible: true
owner_files: []
verify:
  - "Read-only. Mark DONE once read."
---

## 1. Goal

One table of every platform capability the app needs, its Kotlin shape, both implementations, and what degrades on iOS. Each row has a task file with the detail.

## 2. Why this way

**Prefer an interface over `expect class`.** Interfaces are Koin-injectable, mockable in `commonTest`, and do not force a 1:1 file layout across source sets. `expect fun` is reserved for leaf value-returning helpers where an interface would be ceremony (`uses24HourClock()`, `appVersionName`).

**Contracts live in `:shared:domain`, implementations in `:shared:data`.** The domain describes *what* it needs; the platform layer decides *how*. This is the same reasoning that removed `android.net.Uri` and `FragmentActivity` from the domain in `20-02`.

**Every degradation is declared here and surfaced in the UI.** A capability that silently does nothing on iOS is a bug report waiting to happen. Where a row says a feature degrades, `00-CONTEXT/04-constraints.md` §3 specifies the user-facing wording.

## 3. The contracts

| # | Contract | Android | iOS | Degrades | Task |
|---|---|---|---|---|---|
| 1 | **ReminderScheduler** | `AlarmSchedulerImpl` adapter | `IosReminderScheduler` | **64-slot ceiling, no self-re-arm** | `30-01` |
| 2 | **Notifier** | `NotificationService` | `UNUserNotificationCenter` | — | `30-01` |
| 3 | **AlarmPresenter** | `OverlayService` (full-screen) | `.timeSensitive` banner | **no overlay** | `30-01` |
| 4 | **OngoingSessionPresenter** | `PomodoroForegroundService` | Live Activity | needs iOS 16.1+ | `30-04` |
| 5 | **BackgroundSync** | WorkManager (3 workers) | `BGTaskScheduler` | **opportunistic** | `30-02` |
| 6 | **PushMessaging** | `TDFireBaseMessagingService` | Firebase iOS + APNs | silent-push throttling | `30-03` |
| 7 | **BiometricGate** | `BiometricPrompt` | `LAContext` | — | `30-06` |
| 8 | **SecureTokenStore** | `TokenCipher` + DataStore | Keychain | iOS is *simpler* | `30-06` |
| 9 | **CameraCapture** | CameraX in `AndroidView` | `AVCaptureSession` in `UIKitView` | — | `30-07` |
| 10 | **PhotoStorage** | `filesDir` | `NSFileManager` Documents | — | `30-08` |
| 11 | **ImageCodec** | `BitmapFactory` / `compress` | `UIImage` | — | `30-08` |
| 12 | **PlaceSearch** | Places SDK | `MKLocalSearch` | different result set, **no API key** | `30-09` |
| 13 | **SocialSignIn** | Credential Manager | `GIDSignIn` + `ASAuthorization` | Apple needs a backend endpoint | `30-10` |
| 14 | **Analytics / CrashReporter / PerfTracer / AppAttest** | Firebase Android | Firebase iOS via Swift bridge | — | `30-11` |
| 15 | **PermissionController** | runtime permissions | `UN*` / `AVCaptureDevice` | `SYSTEM_ALERT_WINDOW` unsupported | `30-12` |
| 16 | **DeepLinks** | manifest intent filters | universal links + URL scheme | — | `30-13` |
| 17 | **LocaleController** | `AppCompatDelegate` | `AppleLanguages` + restart prompt | needs `CFBundleLocalizations` | `30-14` |
| 18 | **PlatformFormatting** | `java.time` `TextStyle` | `NSDateFormatter` symbols | — | `30-14` |
| 19 | **AmbiencePlayer** | `MediaPlayer` + audio focus | `AVAudioPlayer` + `AVAudioSession` | **`.ogg` unplayable → ship `.m4a`** | `30-05` |
| 20 | **AlarmSoundCatalog** | `RingtoneManager` cursor | bundled `.caf` only | **no system sound picker** | `30-05` |
| 21 | **ConnectivityMonitor** | `ConnectivityManager` | `NWPathMonitor` | — | `30-15` |
| 22 | **ExternalLinks** | `Intent` | `UIApplication.open` etc. | — | `30-15` |
| 23 | **ScreenBehavior** | orientation, keep-awake, animator scale | `supportedInterfaceOrientations`, `isIdleTimerDisabled`, `isReduceMotionEnabled` | — | `30-15` |
| 24 | **SecureScreenFlag** | `FLAG_SECURE` | hide on `willResignActive` only | **screenshots cannot be blocked** | `30-15` |
| 25 | **AppUpdateChecker** | Play in-app update | iTunes lookup API | — | `30-15` |
| 26 | **WebViewHost** | `android.webkit.WebView` | `WKWebView` in `UIKitView` | — | `30-15` |
| 27 | **AppInfo** | `BuildConfig` + `Build.*` | `Bundle.main` + `UIDevice` | `BuildConfig` is Android-only | `30-15` |
| 28 | **Logger** | Timber | `os_log` | — | `30-15` |
| 29 | **Haptics** | CMP `LocalHapticFeedback` covers it | same | `ToneGenerator` (1 use) drops | `30-15` |

Two things are **not** contracts because the stdlib covers them: `java.util.UUID` → `kotlin.uuid.Uuid`, and `android.util.Base64` → `kotlin.io.encoding.Base64`.

## 4. Placement

```
shared/domain/src/commonMain/…/platform/     the interfaces + value types
shared/data/src/androidMain/…/platform/      Android implementations
shared/data/src/iosMain/…/platform/          iOS implementations
composeApp/src/*/…/                          Koin registration per platform
```

Compose-shaped capabilities (`CameraCapture`, `WebViewHost`, `ScreenBehavior`, `SecureScreenFlag`) are `@Composable expect fun` in `:shared:ui` instead, because they need to sit inside the composition.

## 5. Rules

1. **Interface in `:shared:domain`, implementation in `:shared:data`.** The exception is the Compose-shaped ones above.
2. **A capability that cannot exist on a platform must say so**, via a `capabilities`/`isSupported` property — never by silently doing nothing.
3. **Every contract needs a fake for `commonTest`.** If it cannot be faked, it is shaped wrong.
4. **Do not invent contracts.** This list is derived from the Android code's actual Android-API usage. Adding one means finding a real need first.
5. **Suspend by default** for anything doing I/O, and return a result type rather than throwing.

## 7. Acceptance

- [ ] This file has been read
- [ ] The relationship between contracts and their task files is understood

## 8. Pitfalls

- **Over-abstraction.** 29 contracts is already a lot. A capability used once, in one screen, may just belong in `androidMain`/`iosMain` of that screen.
- **Leaking platform types through the interface.** No `Uri`, no `Bitmap`, no `Context`, no `UIImage` in a `commonMain` signature. That is the exact mistake `20-02` fixed.
- **Forgetting the degradation is a product decision.** "iOS cannot do X" needs a UI answer, not just a `false` return.

## 9. Verification

Read-only. Mark `DONE` once read.
