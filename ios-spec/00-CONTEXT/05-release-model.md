# Release model — the two pipelines, side by side

Reference document, not a task. Nothing depends on it being marked `DONE`.

`80-RELEASE/*` specifies the iOS release steps one at a time. This file answers the question that sits underneath them and is asked exactly once, early: **the Android release is a signed AAB — what is the equivalent on iOS, and what does one repository change about shipping two apps from it?**

Read it before `80-01`, and before the first time you touch anything version-numbered.

---

## 1. The two pipelines

### Android, today

```bash
./gradlew :app:bundleRelease                                   # → app-release.aab
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

Signing is one key. `keystore.properties` supplies the **upload key**; Play App Signing re-signs with the key Google holds, and that second key is what devices verify. Play generates per-device split APKs from the AAB. Upload is a browser drag or the Play Developer API; review is mostly automated and usually completes in hours.

### iOS

```bash
# 1. Archive — a build product with symbols, not yet a distributable
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -archivePath build/DoneBot.xcarchive archive

# 2. Export — the .ipa comes out here; this is the AAB's counterpart
xcodebuild -exportArchive -archivePath build/DoneBot.xcarchive \
  -exportOptionsPlist iosApp/ExportOptions.plist -exportPath build/ipa

# 3. Upload, with the App Store Connect API key from 10-01
xcrun altool --upload-app -f build/ipa/DoneBot.ipa --type ios \
  --apiKey "$ASC_KEY_ID" --apiIssuer "$ASC_ISSUER"
```

Then: **processing** (minutes to about an hour, and it can fail *after* a successful upload — an "uploaded" build is not yet a usable build) → **TestFlight** → **App Review**, performed by a person → release, which may be immediate, manual, or phased.

The structural similarity is real: App Store Connect slices the uploaded build per device family, the way Play generates splits from an AAB. The differences below are where the analogy stops.

---

## 2. What has no Android equivalent

| | Detail |
|---|---|
| **Signing is three things, not one** | An Apple Distribution **certificate**, an App Store **provisioning profile**, and the **entitlements** the profile authorises. All three are issued from the Apple Developer account. This is the precise point where the paid membership stops being optional: you can build and run on the simulator forever with a free personal team, but you cannot produce a distributable `.ipa` without `10-01`. |
| **dSYM upload** | Crash reports arrive unsymbolicated unless the dSYMs are uploaded to Crashlytics. **Nothing warns you.** The Android counterpart — `ndk { debugSymbolLevel = "FULL" }` — at least surfaces a Play Console upload warning; here the failure is entirely silent and only visible as unreadable crash reports weeks later. `80-04` forces a test crash specifically to prove symbolication. |
| **`PrivacyInfo.xcprivacy`** | A privacy manifest declaring required-reason API usage — `UserDefaults`, file timestamps, disk space all apply to this app. Written in `10-FOUNDATION`, deliberately not at submission time (`80-02`). |
| **`ITSAppUsesNonExemptEncryption`** | Export compliance, answered in `Info.plist`. HTTPS + platform crypto only → `false`. Omit it and every single upload prompts. |
| **Beta App Review** | External TestFlight testers require a review pass of their own, typically a day or two. Internal testers (up to 100 App Store Connect users) do not. |
| **App Review can reject** | A human reads the app against the guidelines. For DoneBot the known trip-wires are Guideline 4.8 (**Sign in with Apple is mandatory** because Google Sign-In is offered — D-06, a submission blocker) and 5.1.1(v) (in-app account deletion, already satisfied by `DELETE users/me` — verify it stays reachable). |
| **TestFlight builds expire** | 90 days. Relevant for a long beta. |

---

## 3. Version numbers do not map one-to-one

| Android | iOS | Behaviour |
|---|---|---|
| `versionCode` | `CFBundleVersion` (build number) | Android: strictly monotonic across the whole app, and **an uploaded code is burned forever** even if the draft is deleted — vc9 was lost exactly this way. iOS: must increase **within a marketing version**; a new marketing version may restart the sequence. |
| `versionName` | `CFBundleShortVersionString` | The string users see. Independent of the build counter on both platforms. |

**Two independent counters.** The iOS build number is not derived from `versionCode` and there is no reason to synchronise them — trying to keep them equal means every Android release burns an iOS number for nothing. Record both in `PROGRESS.md` at each release instead.

---

## 4. What one repository changes about shipping

The monorepo shares the *source*, not the release process. AAB still comes out of Gradle, IPA still comes out of Xcode. Three consequences are worth writing down, because each is cheap to set up now and expensive to retrofit.

**1 · Namespace the tags.** The repository has no tags at all today. `v1.2-preKMP` is a one-off marker for the pre-migration boundary; everything after it is `android/v1.3`, `ios/v1.0`. One history producing two store timelines under one flat `git tag` list becomes unreadable within a few releases, and `git describe` starts answering the wrong question.

**2 · Filter CI by path.** GitHub bills macOS runners at **10× minutes**, and Kotlin/Native linking is slow. Without a path filter a pure-Android pull request wakes a macOS runner. This is why `10-04` puts iOS on a nightly schedule rather than on `pull_request`, and the reasoning belongs here too because it is a *repository-shape* consequence, not a CI preference.

**3 · A shared-code hotfix touches both stores.** After `20-11`, an urgent Android fix in `shared/` is also a change to the iOS app — possibly to the exact build sitting in App Review. Android can ship it in hours; iOS cannot. Plan the fix knowing the two platforms will be on different versions of that code for days, and prefer fixes that are correct on both rather than Android-shaped patches.

---

## 5. The monorepo balance sheet

D-02 is locked and is **not** relitigated here. This section records what the decision bought and what hygiene debt it created, so neither is rediscovered as a surprise.

**What it buys**

- **Atomic shared changes.** A domain signature change compiles — or breaks — on both platforms in the same commit. No `publish → version → consume` round trip, which for a single developer is friction that compounds daily. This was D-02's stated reason.
- **Silent divergence becomes impossible.** The sync state machine, the recurrence engine and the health-points maths exist once. D-01 chose KMP over a SwiftUI rewrite on exactly this argument — not on schedule.
- **`git bisect` stays meaningful.** "Which commit broke iOS" is answerable in one history.
- **One gate.** `ktlintCheck detektAll testDebugUnitTest assembleDebug` covers the shared code both platforms run.
- **Android ships from the migration.** At `20-12` (M5) the fully-migrated app is Android-releasable with zero iOS code written. A split repository has no equivalent of that milestone.

**What it costs**

| Cost | How much it bites here |
|---|---|
| **Xcode and Android Studio open the same tree** | **The real one.** This repository already has a recorded incident of an IDE mangling files that were being edited underneath it. Add Xcode's indexer and a concurrent Gradle sync and the risk is concrete, not theoretical. Practice: one IDE at a time on a given file, and never edit while the other is syncing. |
| macOS runner billing | Contained by the nightly schedule + path filter (§4.2). Costly only if forgotten. |
| `project.pbxproj` merge conflicts | An XML blob that conflicts readily. With a single developer this is minor; it would dominate on a team. |
| `.gitignore` surface grows | `xcuserdata/`, `DerivedData/`, `*.xcworkspace/xcuserdata`. Worth care: this repo has already been bitten by an unanchored ignore pattern silently swallowing files. |
| Two toolchains, one machine | JDK 21 (pinned in `10-00`) **and** Xcode 26 on macOS 15.6+. Budget ~45 GB for Xcode plus simulator runtimes plus the OS installer. |
| Access control is all-or-nothing | Granting anyone iOS access grants the whole Android source. Irrelevant for a solo developer; a blocker for contract work. |

**Net:** for shared Kotlin plus one developer, the pluses are structural and the minuses are hygiene. The two that actually bite — parallel IDE editing and CI path filtering — are both preventable, and both are cheaper to prevent than to discover.
