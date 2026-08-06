---
id: 40-misc-06
title: Web view
layer: ui
status: TODO
depends_on: [30-15]
blocks: []
parallel_safe: true
estimate: 3h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/webview/**
  - shared/ui/src/iosMain/**/webview/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The in-app web view for the privacy policy and terms.

## 2. Why this way

**Small surface, but it is compliance-visible.** App Review checks that the privacy policy is reachable, and the consent footer on the auth screens links here. If it fails to load, that is a rejection path.

`WKWebView` in a `UIKitView` is the iOS side; `android.webkit.WebView` in an `AndroidView` stays on Android. The contract is `30-15`'s `WebViewHost`.

**Both URLs come from `BuildConfig`** on Android — which does not exist in common code, so they resolve through `AppInfo` (`30-15`).

## 3. Source

| Path | LOC |
|---|---|
| `ui/webview/` (3 files) | 221 |
| `navigation/Screen.kt` | `WebView(url)` |
| `app/build.gradle.kts` | `PRIVACY_POLICY_URL`, `TERMS_OF_SERVICE_URL` |
| `ui/auth/AuthConsentFooter.kt` | the entry point |

## 4. Target

`shared/ui/commonMain/…/ui/webview/` plus the platform host.

## 5. Steps

1. Verify the files compile in `commonMain` with the platform host in `iosMain`/`androidMain`.
2. Verify both URLs load on both platforms.
3. Verify the loading and error states.
4. Verify `TDTopBar` with back and title.
5. Verify links inside the page behave sensibly — external links should leave to the browser.
6. Verify it works from the auth consent footer, before login.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Files compile with the platform host
- [ ] Privacy policy and terms both load on both platforms
- [ ] Loading and error states render
- [ ] `TDTopBar` correct
- [ ] External links leave to the browser
- [ ] Works from the auth footer before login
- [ ] Three kits, two themes, two languages
- [ ] Previews cover loading and error

## 8. Pitfalls

- **The privacy policy must be reachable.** Review checks it.
- **It must work before login** — the consent footer is on the auth screens.
- **`BuildConfig` does not exist in common code.** Use `AppInfo`.
- **`WKWebView` needs `ignoresSafeArea` handling** or content sits under the notch.
- **Do not let external links navigate inside the web view.** Open them in the browser.

## 9. Verification

```bash
curl -I <PRIVACY_POLICY_URL>
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: open both documents from Settings and from the auth footer;
# airplane mode (error state); tap an external link
```
