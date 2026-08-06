---
id: 30-13
title: Deep links & universal links
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-auth-05]
parallel_safe: true
estimate: 12h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/navigation/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Every deep link that works on Android works on iOS, routed through the **same** sealed `DeepLink` type and the same handler.

## 2. Why this way

**The routing is already centralised, which is the whole reason this is cheap.** Deep links are deliberately *not* declared in the nav graph — there is no `navDeepLink` anywhere. Instead, `MainActivity` forwards intents to `MainViewModel.onPushIntent`, which parses them into a sealed `DeepLink` and emits a navigation effect. iOS needs a second *entry point* into that same parser, not a second implementation.

**Move the parser to `commonMain`.** It is pure string→sealed-class mapping. Duplicating it in Swift is how the two platforms end up disagreeing about what `todoapp://reset-password?token=x` means.

**Both link forms must work on iOS.** The custom scheme `todoapp://reset-password` and the verified https App Link on the backend host. There is a recorded lesson here: **Gmail strips custom-scheme links**, which is why the https landing page exists. On iOS the https form becomes a Universal Link, which needs an `apple-app-site-association` file served from the backend — the Android `assetlinks.json` equivalent, and a `70-BACKEND` task.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `MainViewModel.kt` (~161-183, 236-250) | `onPushIntent`, the sealed `DeepLink`: `Group`, `GroupTask`, `Task`, `Invitations`, `NotificationsInbox`, `ResetPassword` |
| `MainActivity.kt` | `onCreate` / `onNewIntent` forwarding |
| `app/src/main/AndroidManifest.xml` | Two intent filters: `todoapp://reset-password` (`autoVerify="false"`) and the https App Link (`autoVerify="true"`) |
| `navigation/CurrentRouteTracker.kt` | Push suppression when already on the target |
| `data/source/remote/fcm/TDFireBaseMessagingService.kt` | Builds the deep-link `PendingIntent` |
| `navigation/NavGraph.kt` | `NavigationEffectController` — where effects are consumed |

## 4. Target

- `shared/ui/commonMain/…/navigation/DeepLinkParser.kt` — the shared parser
- `iosApp/iOSApp.swift` — `onOpenURL` and `NSUserActivity` handling
- `iosApp/iosApp.entitlements` — associated domains
- `70-BACKEND` — serve `apple-app-site-association`

## 5. Steps

1. **Extract the parser** from `MainViewModel` into a pure `commonMain` function. Both platforms call it.

2. **Register the URL scheme** in `Info.plist` (`CFBundleURLTypes` → `todoapp`).

3. **Add the associated domain** entitlement: `applinks:donebot-backend.onrender.com`.

4. **Serve `apple-app-site-association`** from the backend at `/.well-known/apple-app-site-association` — JSON, `Content-Type: application/json`, **no `.json` extension**, no redirects. A `70-BACKEND` task.

5. **Handle both entry points in Swift**: `onOpenURL` for the custom scheme, `onContinueUserActivity` for universal links. Both call the same Kotlin parser.

6. **Handle cold start.** A link that launches the app arrives before the composition is ready; queue it and replay after Koin and navigation are up. Android already handles this via `onCreate` forwarding.

7. **Keep suppression working.** `CurrentRouteTracker` prevents a redundant navigation when the user is already there.

## 6. Code skeleton

```kotlin
// shared/ui/commonMain/…/navigation/DeepLinkParser.kt
// Pure. Both platforms call this; duplicating it in Swift is how the two
// platforms end up disagreeing about what a link means.
fun parseDeepLink(url: String): DeepLink? = when {
    url.startsWith("todoapp://reset-password") -> DeepLink.ResetPassword(url.queryParam("token") ?: return null)
    url.contains("/reset-password") -> DeepLink.ResetPassword(url.queryParam("token") ?: return null)
    else -> null
}
```

```swift
// iosApp/iOSApp.swift
WindowGroup {
    ComposeView()
        .onOpenURL { url in DeepLinkBridge.shared.handle(url.absoluteString) }
        .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
            guard let url = activity.webpageURL else { return }
            DeepLinkBridge.shared.handle(url.absoluteString)
        }
}
```

```json
// Served by the backend at /.well-known/apple-app-site-association
// application/json, NO .json extension, NO redirect — any of those breaks it silently.
{
  "applinks": {
    "details": [{ "appIDs": ["TEAMID.com.todoapp.mobile"], "components": [{ "/": "/reset-password*" }] }]
  }
}
```

## 7. Acceptance

- [ ] The parser lives in `commonMain`; both platforms use it
- [ ] Android behaviour unchanged
- [ ] iOS: `todoapp://reset-password?token=…` opens the reset screen
- [ ] iOS: the https link opens the app as a Universal Link, not Safari
- [ ] Every `DeepLink` case routes correctly from a notification tap
- [ ] Cold start works — a link that launches the app is not dropped
- [ ] Suppression works — no redundant navigation when already on the target
- [ ] `apple-app-site-association` is served correctly (verified with `curl`)
- [ ] Associated domain entitlement present; `CFBundleURLTypes` registered

## 8. Pitfalls

- **`apple-app-site-association` must have no file extension**, be served as `application/json`, and **must not redirect**. Any of those breaks universal links silently — the link just opens Safari.
- **Universal links are cached by the OS.** After changing the AASA file, delete and reinstall the app; iOS will not re-fetch on its own.
- **A universal link tapped inside your own app's web view does not leave the app.** Test from Notes or Mail, not from Safari's address bar (typing a URL there deliberately bypasses universal links).
- **Gmail strips custom-scheme links.** That is why the https form exists — keep both.
- **Cold-start links arrive before the composition is ready.** Queue and replay.
- **Do not duplicate the parser in Swift.**
- **The team id prefix in `appIDs`** is `TEAMID.bundleid`. A wrong team id fails silently.

## 9. Verification

```bash
# AASA is served correctly
curl -sI https://donebot-backend.onrender.com/.well-known/apple-app-site-association | head
curl -s  https://donebot-backend.onrender.com/.well-known/apple-app-site-association | jq .

./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a device
#   send yourself the https reset link by email → tapping opens the app, not Safari
#   xcrun simctl openurl booted "todoapp://reset-password?token=test"
#   tap each notification type → correct screen
#   kill the app, tap a link → cold start routes correctly
```
