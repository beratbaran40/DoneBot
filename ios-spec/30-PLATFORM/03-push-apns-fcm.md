---
id: 30-03
title: Push — APNs via FCM
layer: platform
status: TODO
depends_on: [10-01, 20-13, 30-11]
blocks: [70-02]
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/push/**
  - shared/data/src/androidMain/**/fcm/**
  - shared/data/src/iosMain/**/push/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Deliver the existing 10 push payload types to iOS through APNs, with **no backend change** — the same FCM send path, the same payloads, the same deep links.

## 2. Why this way

**The backend does not need to know iOS exists.** Firebase Cloud Messaging fans out to APNs when given an iOS registration token. The server keeps calling FCM with the same payload; only the token's platform differs. This is the single largest piece of free leverage in the port.

What does change is on the device:

- **Notification permission is prompted**, like Android 13+. `requestAuthorization` should be asked at a moment the user understands, not at launch.
- **Data-only pushes need `content-available: 1`** and are rate-limited. `task_list_changed` currently triggers a forced sync on Android; on iOS it becomes a *hint*, not a guarantee.
- **The token is per-install and rotates.** Same as Android; the existing `POST devices/fcm-token` / `DELETE devices/fcm-token` flow carries over unchanged.
- **`device_tokens` needs a `platform` column** for `70-02`'s reminder push to target correctly. Not needed for basic delivery.

**Firebase stays on the Swift side.** Kotlin talks to a protocol injected at startup (`30-11`), so `PushMessaging`'s iOS implementation is a thin bridge.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/source/remote/fcm/TDFireBaseMessagingService.kt` (342 LOC) | Token refresh handling, the cached `largeIconBitmap`, deep-link `PendingIntent` into `MainActivity`, **notification suppression when `CurrentRouteTracker` says the user is already on the target screen**, targeted repository refreshes per payload type |
| `data/source/remote/fcm/PushPayload.kt` (214 LOC) | The 10 types: `task_assigned`, `task_completed`, `task_due_soon`, `invitation_received`, `invitation_accepted`, `invitation_declined`, `group_invite`, `group_task_changed`, `task_list_changed`, `group_ownership_transferred` |
| `data/repository/FCMTokenPreferencesImpl.kt` | Token persistence and the pending-sync flag |
| `MainViewModel.kt` (~lines 161-250) | `onPushIntent`, the sealed `DeepLink` type |
| `navigation/CurrentRouteTracker.kt` | The suppression mechanism |
| `UserRepositoryImpl.syncPendingFcmToken()` | Called after login |

## 4. Target

- `shared/domain/…/push/PushMessaging.kt` + a shared `PushPayload` parser
- `shared/data/androidMain/…/FirebasePushMessaging.kt`
- `shared/data/iosMain/…/IosPushMessaging.kt`
- `iosApp/AppDelegate.swift` — APNs registration, `MessagingDelegate`

## 5. Steps

1. **Move `PushPayload` parsing to `commonMain`.** It is pure JSON→sealed-class mapping; both platforms use it. This is where a divergence would otherwise appear.

2. **Define the contract** (skeleton below) exposing a token flow and a message flow.

3. **Android: wrap the existing service.** `TDFireBaseMessagingService` keeps doing what it does and feeds the flows.

4. **iOS: register for remote notifications** in `AppDelegate`, set `Messaging.messaging().delegate`, forward the APNs token to Firebase, and forward FCM token updates into the Kotlin flow.

5. **Request notification authorization at a considered moment**, not at launch. The Android app already has a first-login permission prompt (`setFirstLoginPermissionPromptPending`) — reuse that moment.

6. **Preserve suppression.** `CurrentRouteTracker` already exists; on iOS the `willPresent` delegate decides whether to show a banner. Same rule: if the user is looking at the target screen, do not banner them.

7. **Route taps into the same `DeepLink` path.** `didReceive` → the same sealed type `MainViewModel.onPushIntent` produces. One routing implementation, two entry points.

8. **Treat `task_list_changed` as a hint on iOS.** It should trigger a sync when the app gets background time, but the foreground reconcile remains the guarantee.

9. **Send `platform` with the token registration** so `70-02` can target correctly. `FCMTokenRequest` already carries `deviceId`, `deviceName` and `timeZone` — adding `platform` is additive and backward-compatible.

## 6. Code skeleton

```kotlin
// shared/domain/…/push/PushMessaging.kt
interface PushMessaging {
    suspend fun currentToken(): String?
    val tokenUpdates: Flow<String>
    val messages: Flow<PushPayload>
    suspend fun requestAuthorization(): Boolean
    suspend fun deleteToken()
}
```

```swift
// iosApp/AppDelegate.swift
func application(_ app: UIApplication,
                 didFinishLaunchingWithOptions opts: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    FirebaseApp.configure()
    Messaging.messaging().delegate = self
    UNUserNotificationCenter.current().delegate = self
    app.registerForRemoteNotifications()
    return true
}

func application(_ app: UIApplication,
                 didRegisterForRemoteNotificationsWithDeviceToken token: Data) {
    // Firebase needs the raw APNs token before it can mint an FCM token.
    Messaging.messaging().apnsToken = token
}

func messaging(_ messaging: Messaging, didReceiveRegistrationToken token: String?) {
    guard let token else { return }
    PushBridge.shared.onTokenRefresh(token)   // → Kotlin tokenUpdates flow
}
```

## 7. Acceptance

- [ ] `PushPayload` parsing lives in `commonMain` and both platforms use it
- [ ] iOS receives a push for all 10 payload types (test each from the Firebase console or the backend)
- [ ] Token registers on login and unregisters on logout (`POST` / `DELETE devices/fcm-token`)
- [ ] Tapping a notification opens the correct screen for every deep-link type
- [ ] Suppression works: no banner while the user is on the target screen
- [ ] Notification permission is requested at the first-login moment, not at launch
- [ ] Data-only `task_list_changed` triggers a sync when background time is granted
- [ ] Token registration includes `platform`
- [ ] **No backend change was required** for basic delivery
- [ ] Android push behaviour unchanged

## 8. Pitfalls

- **APNs token before FCM token.** `Messaging.messaging().apnsToken = token` must happen in `didRegisterForRemoteNotifications`, or FCM never produces a token and there is no error.
- **The APNs `.p8` key must be uploaded to Firebase** with the correct Key ID and Team ID (`10-01`). A wrong Team ID fails silently at send time.
- **Sandbox vs production APNs.** Development builds use sandbox, TestFlight and App Store use production. Firebase handles this if the key is uploaded correctly — but a push that works in debug and not in TestFlight is almost always this.
- **Silent push is rate-limited and not guaranteed.** `content-available: 1` is a hint. Never rely on it for correctness.
- **`UNUserNotificationCenter.delegate` must be set before the app finishes launching**, or a cold-start tap is dropped.
- **Do not request notification permission at launch.** Denial is sticky and users deny prompts they do not understand.
- **The Android service caches a `largeIconBitmap`.** iOS has no equivalent; do not try to port it.
- **Test on hardware.** Push does not work on the simulator for real APNs delivery.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone
#   log in → token registers (verify server-side)
#   send each of the 10 payload types → correct banner, correct deep link on tap
#   open the target screen, send again → suppressed
#   log out → token deleted server-side
#   TestFlight build → push still arrives (production APNs)
```
