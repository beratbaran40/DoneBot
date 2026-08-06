---
id: 40-misc-02
title: Overlay replacement (redesign)
layer: ui
status: TODO
depends_on: [30-01, 30-04]
blocks: []
parallel_safe: false
estimate: 12h
reversible: true
owner_files:
  - app/src/main/java/com/todoapp/mobile/ui/overlay/**
  - shared/ui/src/commonMain/**/alarm/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Decide and build what replaces the full-screen alarm overlay on iOS. **This is the only feature in the port that is a redesign rather than a port.**

## 2. Why this way

**`SYSTEM_ALERT_WINDOW` has no iOS equivalent, and there is no workaround.** Android's `OverlayService` (~460 LOC) draws a `TYPE_APPLICATION_OVERLAY` ComposeView over the lock screen and other apps, with its own `LifecycleRegistry` and `SavedStateRegistry`. iOS apps simply cannot draw outside their own window.

**`.critical` is not the answer.** Critical alerts bypass silence and Focus, but the entitlement is granted essentially only to health and public-safety apps. A task app will not get it. Do not design around it.

**What iOS actually offers**, in descending order of prominence:

1. **`.timeSensitive` interruption level** — breaks through Focus, available to all apps, user-revocable per app. This is the realistic ceiling.
2. **A Live Activity** for an imminent or firing alarm — persistent on the Lock Screen and in the Dynamic Island, and it shares `30-04`'s infrastructure.
3. **A Lock Screen widget** showing the next reminder.

**The honest position is that this degrades**, and the app should say so rather than let the user discover it. `00-CONTEXT/04-constraints.md` §3 specifies the Settings copy: an explanation of Time Sensitive notifications with a button to `openNotificationSettingsURLString`.

**`OverlayService` stays in `:app`** — it is an Android Service, and it keeps working there unchanged.

## 3. Source

| Path | LOC |
|---|---|
| `ui/overlay/OverlayService.kt` | ~460 — Android only, unchanged |
| `ui/overlay/OverlayServiceChannel.kt` | ~40 |
| `uikit/…/TDOverlayNotificationCard.kt` | 325 — gesture-driven, reusable in-app |
| `uikit/…/TDOverlayDailyPlanNotificationCard.kt` | 190 |
| `data/alarm/AlarmFireReceiver.kt` | chooses overlay vs notification |
| `common/Permissions.kt` | `Settings.canDrawOverlays` |
| `ios-spec/30-PLATFORM/01-notifications-and-alarms.md` | `AlarmPresenter` |

## 4. Target

- `app/…/ui/overlay/` — unchanged, Android only
- `shared/ui/commonMain/…/alarm/` — the shared in-app alarm surface
- iOS: `.timeSensitive` + optional Live Activity

## 5. Steps

1. **Verify Android is untouched.** `OverlayService` keeps working exactly as it does.
2. **Implement `AlarmPresenter` for iOS** returning `PresentationMode.BANNER`, with `.timeSensitive`.
3. **Decide whether an imminent alarm gets a Live Activity.** It is the closest available analogue to the overlay's persistence, and the infrastructure exists from `30-04`. **Recommended: yes** for alarms firing within a short window. Record the decision.
4. **Reuse `TDOverlayNotificationCard` in-app.** When the app is foreground at fire time, showing the existing card is both closer to Android's behaviour and free — the component is already shared.
5. **Add the Settings explanation** with a button to notification settings.
6. **Verify the permissions pager hides the overlay row on iOS** (`isSupported = false`).
7. **Verify the notification tap routes** to the task, exactly as the overlay's action did.

## 6. Code skeleton

```kotlin
// shared/domain/…/alarm/AlarmPresenter.kt
enum class PresentationMode { FULL_SCREEN, HEADS_UP, BANNER }

interface AlarmPresenter {
    fun present(alert: AlarmAlert): PresentationMode
    val supportsFullScreenAlert: Boolean   // Android: canDrawOverlays. iOS: always false.
}
```

```kotlin
// iOS: .timeSensitive is the realistic ceiling. .critical requires an entitlement
// granted essentially only to health and public-safety apps — do not design around it.
content.interruptionLevel = UNNotificationInterruptionLevelTimeSensitive
```

## 7. Acceptance

- [ ] Android overlay behaviour **unchanged**
- [ ] iOS alarms deliver as `.timeSensitive` banners
- [ ] They break through Focus when the user has allowed Time Sensitive notifications
- [ ] Live Activity decision made and recorded; implemented if chosen
- [ ] Foreground alarms reuse `TDOverlayNotificationCard`
- [ ] Settings explains Time Sensitive with a working button to notification settings
- [ ] Permissions pager hides the overlay row on iOS
- [ ] Notification tap routes to the task
- [ ] The degradation is documented in `DECISIONS.md`

## 8. Pitfalls

- **Do not attempt `.critical`.** The entitlement will not be granted, and building for it wastes the work.
- **Do not try to emulate an overlay.** iOS apps cannot draw outside their window. Any workaround is a rejection or a broken experience.
- **Time Sensitive is user-revocable.** The app must still function when it is off.
- **Do not show a dead overlay-permission row on iOS.**
- **`OverlayService` stays in `:app`.** It is a Service, not shared UI.
- **Say what is true.** Users who move from Android will notice; explaining it is better than letting them think it is broken.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# iOS on hardware: set an alarm, lock the device → Time Sensitive banner;
# enable a Focus mode → still breaks through; tap → opens the task;
# app foreground at fire time → the in-app card appears;
# Settings → the explanation and the button work; no overlay permission row
```
