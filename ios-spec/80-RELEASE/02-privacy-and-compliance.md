---
id: 80-02
title: Privacy manifest & compliance
layer: release
status: TODO
depends_on: [10-03]
blocks: [80-04]
parallel_safe: true
estimate: 8h
reversible: true
owner_files:
  - iosApp/PrivacyInfo.xcprivacy
  - iosApp/Info.plist
verify:
  - "plutil -lint iosApp/PrivacyInfo.xcprivacy"
---

## 1. Goal

A correct `PrivacyInfo.xcprivacy`, verified third-party SDK manifests, export-compliance declaration, and an audit of the app's user-facing privacy claims against what iOS can actually deliver.

## 2. Why this way

**The privacy manifest is required and its reason codes are checked automatically.** Apple validates required-reason API declarations at upload. A missing or wrong code is a rejection, and the error message points at an API rather than at the manifest.

DoneBot uses at least three required-reason API categories:
- **`UserDefaults`** (`CA92.1`) — settings and the language override
- **File timestamps** (`C617.1`) — journal photos and pending uploads
- **Disk space** (`E174.1`) — image caching

**Third-party SDKs need manifests too, and yours must aggregate them.** Firebase and GoogleSignIn ship their own; verify they are present and current, because an SDK without a manifest blocks the whole upload.

**The most important item here is not a file — it is an audit.** `30-15` establishes that iOS cannot block screenshots. The journal and secret mode are privacy features, and any UI text or store copy promising screenshot protection would be a false claim about a privacy feature on that platform. This task is where that gets checked across the app, the App Store description and the privacy policy.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `iosApp/PrivacyInfo.xcprivacy` | Written in `10-03`; verified here |
| `iosApp/Info.plist` | Usage strings, `ITSAppUsesNonExemptEncryption` |
| `ios-spec/30-PLATFORM/15-misc-platform.md` | `blocksScreenshots` and the copy implication |
| `app/src/main/res/values{,-tr}/strings.xml` | Privacy-related strings to audit |
| `ui/settings/` privacy section, `ui/journal/` | Where those strings appear |
| Backend `/legal/privacy.html` | The privacy policy — must cover iOS |
| `donebot prod/PRODUCTION_READINESS_RAPOR.md` | The Android privacy review; the same rigour applies |

## 4. Target

- `iosApp/PrivacyInfo.xcprivacy` — verified
- `iosApp/Info.plist` — export compliance
- String audit across app, listing and policy

## 5. Steps

1. **Verify every required-reason API** the app actually uses. Do not guess: build, upload a test build, and let Apple's validation tell you. Add codes only for APIs genuinely used.

2. **Verify third-party manifests.** Check each SPM dependency's bundle for a `PrivacyInfo.xcprivacy`. Firebase and GoogleSignIn ship them; update the SDK if one is missing.

3. **Declare data collection in the manifest**, consistent with the App Store Connect answers from `80-01`. The two must agree — they are cross-checked.

4. **Set `ITSAppUsesNonExemptEncryption = false`.** The app uses HTTPS and platform crypto only, which is exempt. This avoids the per-submission export-compliance question.

5. **Audit every usage string in `Info.plist`.** Specific and user-facing. "This app uses the camera" is rejected; "DoneBot uses the camera to take polaroid photos for your journal entries" is not.

6. **Audit privacy claims across three surfaces** — in-app strings, App Store description, privacy policy — against what iOS delivers. Specifically:
   - Nothing may claim screenshot or screen-recording protection on iOS
   - Journal privacy copy should describe what is true: device-only storage, biometric lock, never uploaded
   - Any text implying the app blocks screen capture must be conditional on `blocksScreenshots` or reworded

7. **Verify the privacy policy covers iOS** — App Store distribution, APNs, Apple sign-in.

8. **Confirm account deletion is reachable in-app** (Guideline 5.1.1(v)). `DELETE users/me` exists; verify the path is present and works on iOS.

## 6. Code skeleton

```xml
<!-- iosApp/PrivacyInfo.xcprivacy — declare only APIs actually used.
     Apple validates these at upload and reports the API, not the manifest. -->
<key>NSPrivacyTracking</key><false/>
<key>NSPrivacyTrackingDomains</key><array/>
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

```xml
<!-- HTTPS + platform crypto only → exempt. Avoids the per-submission question. -->
<key>ITSAppUsesNonExemptEncryption</key><false/>
```

## 7. Acceptance

- [ ] `PrivacyInfo.xcprivacy` passes `plutil -lint` and is in the app bundle
- [ ] All required-reason APIs declared; a test upload produces no privacy warnings
- [ ] Every third-party SDK ships a privacy manifest
- [ ] Manifest data-collection declarations match the App Store Connect answers
- [ ] `NSPrivacyTracking` = false; no tracking domains
- [ ] `ITSAppUsesNonExemptEncryption` = false
- [ ] Every `Info.plist` usage string is specific and user-facing
- [ ] **No in-app string, listing text or policy claim promises screenshot protection on iOS**
- [ ] Journal privacy copy accurately describes device-only storage and biometric lock
- [ ] The privacy policy covers iOS distribution, APNs and Apple sign-in
- [ ] In-app account deletion is reachable and works on iOS

## 8. Pitfalls

- **Missing required-reason codes block upload**, and the error names the API, not the manifest.
- **A third-party SDK without a manifest blocks the whole upload.** Check every one.
- **Manifest and App Store Connect answers are cross-checked.** A mismatch is a rejection.
- **Claiming screenshot protection on iOS is a false statement about a privacy feature.** Worse than the missing capability itself.
- **Vague usage strings are rejected.** Say what the app does with the access.
- **`NSPrivacyTracking = true` requires ATT.** The app does not track.
- **Do not over-declare.** Declaring data you do not collect makes the label worse and is inaccurate.
- **Account deletion must be in-app**, not a support-email instruction.

## 9. Verification

```bash
plutil -lint iosApp/PrivacyInfo.xcprivacy
plutil -p iosApp/PrivacyInfo.xcprivacy

# Third-party manifests
find ~/Library/Developer/Xcode/DerivedData -name "PrivacyInfo.xcprivacy" | head -20

# Upload a build to TestFlight — Apple validates privacy at ingest
# Then audit the copy:
grep -rn -i "screenshot\|screen record\|ekran görüntüsü\|ekran kayd" \
  app/src/main/res/values/strings.xml app/src/main/res/values-tr/strings.xml
```
