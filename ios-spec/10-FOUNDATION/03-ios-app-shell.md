---
id: 10-03
title: iOS app shell (`iosApp/` Xcode project)
layer: foundation
status: TODO
depends_on: [10-00, 10-01, 20-13]
blocks: [30-04, 30-11, 60-01, 60-02, 80-02]
parallel_safe: false
estimate: 30h
reversible: true
owner_files:
  - iosApp/**
  - composeApp/build.gradle.kts
verify:
  - ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
  - "xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build"
---

## 1. Goal

Create the Xcode project that hosts `DoneBotKit`, and get DoneBot running on the simulator. This is milestone **M6**.

## 2. Why this way

**Three targets from the start, even though two are empty.** `iosApp`, `DoneBotWidget` (WidgetKit) and `DoneBotIntents` (App Intents). Adding a target later means redoing signing, App Groups and the shared-container plumbing. Creating them now — even as stubs — costs an hour and saves a day.

**`PrivacyInfo.xcprivacy` is written in this task, not at submission.** It is required, and it must declare *required-reason APIs*: `UserDefaults`, file-timestamp and disk-space access all apply to this app. Discovering that during App Review costs a rejection cycle; writing it while the project is being created costs nothing.

**Firebase comes in through SPM, not CocoaPods.** The KMP CocoaPods plugin adds real friction to the Gradle build for no benefit here, since Firebase stays on the Swift side and Kotlin reaches it through a protocol bridge (`30-PLATFORM/11`).

**The first thing to run on the simulator is a text-input torture screen, not a real screen.** CMP's iOS text field is the most-cited maturity gap, and this codebase has 846 lines of custom text-field behaviour across `TDTextField` and `TDOutlinedTextField` plus 22 files that use them. Finding a blocker there after porting 20 screens is expensive; finding it on day one is a scoped decision with a known escape hatch (`UIKitView` hosting a `UITextField` behind the same API).

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `composeApp/build.gradle.kts` | The `DoneBotKit` framework declaration from `20-13` |
| `app/src/main/AndroidManifest.xml` | The Android permission and capability set — the reference for `Info.plist` usage strings |
| `app/build.gradle.kts` | `applicationId`, `versionName` — the iOS bundle id and version mirror them |
| `ios-spec/00-CONTEXT/04-constraints.md` §1.6 | Privacy manifest and submission requirements |
| `ios-spec/90-STATE/PROGRESS.md` | Team ID, bundle id, App Group id recorded in `10-01` |

## 4. Target

```
iosApp/
├── iosApp.xcodeproj
├── iosApp/
│   ├── iOSApp.swift              @main, Koin init, framework entry
│   ├── ComposeView.swift         UIViewControllerRepresentable → MainViewController()
│   ├── AppDelegate.swift         push, notification delegate, deep links
│   ├── Info.plist                usage strings, background modes, URL schemes
│   ├── PrivacyInfo.xcprivacy     required-reason API declarations
│   ├── iosApp.entitlements       App Groups, push, Sign in with Apple, associated domains
│   └── Assets.xcassets           app icon, launch screen
├── DoneBotWidget/                stub target
├── DoneBotIntents/               stub target
└── ExportOptions.plist
composeApp/src/iosMain/…/MainViewController.kt
```

## 5. Steps

1. **Create the Xcode project** at the repo root, sibling to `app/`. Bundle id `com.todoapp.mobile` (mirrors the Android `applicationId`). Deployment target: pick the lowest iOS version you will support and record it in `DECISIONS.md` — building with the iOS 26 SDK does not force users onto iOS 26.

2. **Add the Gradle build phase** so Xcode links the current framework:
   ```bash
   cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
   It must run **before** "Compile Sources".

3. **Expose the entry point** from Kotlin — `MainViewController()` in `composeApp/src/iosMain`.

4. **Write `iOSApp.swift`** — start Koin, host `ComposeView`.

5. **Add the two extension targets** as stubs with correct bundle ids (`com.todoapp.mobile.widget`, `com.todoapp.mobile.intents`) and the App Group entitlement.

6. **Write `Info.plist`.** Every usage string must be user-facing and specific — App Review rejects vague ones:
   - `NSCameraUsageDescription` — journal polaroid camera
   - `NSPhotoLibraryUsageDescription` / `NSPhotoLibraryAddUsageDescription`
   - `NSFaceIDUsageDescription` — journal lock and secret mode
   - `UIBackgroundModes`: `audio` (pomodoro ambience), `remote-notification`, `fetch`, `processing`
   - `BGTaskSchedulerPermittedIdentifiers`
   - `CFBundleURLTypes` — `todoapp://` plus the Google Sign-In reversed client id
   - `CFBundleLocalizations` — `en`, `tr`
   - `ITSAppUsesNonExemptEncryption` = `false` (HTTPS + platform crypto only)

7. **Write `PrivacyInfo.xcprivacy`** with required-reason declarations for `UserDefaults` (`CA92.1`), file timestamps (`C617.1`), and disk space (`E174.1`). Verify each reason code against Apple's current list rather than copying blindly.

8. **Write the entitlements** — App Groups, push, Sign in with Apple, associated domains.

9. **Build and run on the simulator.**

10. **Build the text-input torture screen and run it first.** A single screen exercising: single-line, multiline, password with visibility toggle, the chat composer, EN and TR keyboards, IME insets with the keyboard open, selection handles, copy/paste, autocorrect, return-key behaviour, and focus traversal between fields. Record findings in `DECISIONS.md`. If something is unusable, the escape hatch is a `UIKitView`-hosted `UITextField` behind the existing `TDTextField` API — a contained change to two files.

## 6. Code skeleton

```kotlin
// composeApp/src/iosMain/kotlin/…/MainViewController.kt
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
```

```swift
// iosApp/iOSApp.swift
import SwiftUI
import DoneBotKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    init() { KoinInitKt.doInitKoin() }
    var body: some Scene { WindowGroup { ComposeView().ignoresSafeArea(.all) } }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
```

```xml
<!-- iosApp/PrivacyInfo.xcprivacy — required since May 2024. Verify each reason
     code against Apple's current list; do not copy blindly. -->
<key>NSPrivacyAccessedAPITypes</key>
<array>
  <dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
    <key>NSPrivacyAccessedAPITypeReasons</key><array><string>CA92.1</string></array>
  </dict>
  <dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
    <key>NSPrivacyAccessedAPITypeReasons</key><array><string>C617.1</string></array>
  </dict>
  <dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryDiskSpace</string>
    <key>NSPrivacyAccessedAPITypeReasons</key><array><string>E174.1</string></array>
  </dict>
</array>
```

## 7. Acceptance

- [ ] `xcodebuild … -scheme iosApp … build` succeeds
- [ ] The app launches on the simulator and renders the first Compose screen
- [ ] All three targets exist with correct bundle ids and the App Group entitlement
- [ ] `Info.plist` carries every usage string, background mode, URL type and `CFBundleLocalizations`
- [ ] `PrivacyInfo.xcprivacy` present with verified reason codes
- [ ] The Gradle build phase runs before "Compile Sources"
- [ ] Text-input torture screen run; findings recorded in `DECISIONS.md`
- [ ] Android still green — `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug`
- [ ] M6 marked in `PROGRESS.md`

## 8. Pitfalls

- **Vague usage strings get rejected.** "This app uses the camera" is not acceptable. Say what for: "DoneBot uses the camera to take polaroid photos for your journal entries."
- **`UIBackgroundModes: audio` must be genuine.** Declaring it without real background audio is a rejection. DoneBot has real ambience playback, so it is justified — but the feature must actually work.
- **`BGTaskSchedulerPermittedIdentifiers` must list every identifier** you register, or `BGTaskScheduler` silently refuses to schedule.
- **The Google Sign-In URL scheme is the *reversed* client id.** Getting it wrong makes sign-in fail with an unhelpful error.
- **App Group id must match everywhere** — entitlements of all three targets, and the Kotlin/Swift code that reads the container. A mismatch means widgets read an empty container with no error.
- **SPM, not CocoaPods.** The KMP CocoaPods plugin complicates the Gradle build for no gain here.
- **Do not commit `.xcuserstate` or `xcuserdata/`.** Add them to `.gitignore` in this task.
- **Do the torture screen before porting real screens.** It is the cheapest possible moment to discover a text-input blocker.
- **`ignoresSafeArea(.all)`** on `ComposeView` — Compose handles its own insets. Without it you get double padding.

## 9. Verification

```bash
# 1. Framework + app build
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build

# 2. Run it
xcrun simctl boot "iPhone 17" 2>/dev/null
xcrun simctl install booted <path-to-.app>
xcrun simctl launch booted com.todoapp.mobile

# 3. Privacy manifest is in the bundle
plutil -p <path-to-.app>/PrivacyInfo.xcprivacy

# 4. Android unaffected
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
```
