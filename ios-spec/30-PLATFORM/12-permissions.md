---
id: 30-12
title: Permissions
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-misc-04]
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/permission/**
  - shared/data/src/androidMain/**/permission/**
  - shared/data/src/iosMain/**/permission/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

One `PermissionController` covering notifications, camera and photo library — plus an explicit `isSupported` answer for the Android-only permissions that have no iOS counterpart.

## 2. Why this way

**Three-state, not two.** `CLAUDE.md` records a real bug here: a permission can be *permanently denied*, where requesting it again shows no dialog at all and the UI appears frozen. The correct handling is `GRANTED | DENIED | PERMANENTLY_DENIED`, with the third routing the user to system Settings. iOS has the same shape — `UNAuthorizationStatus.denied` never re-prompts.

**`isSupported` is what keeps the UI honest.** Android's permission list includes things iOS has no concept of — `SYSTEM_ALERT_WINDOW` (the alarm overlay), exact alarms. Rather than returning `DENIED` (which invites a pointless "grant" button), the contract answers `isSupported = false` and the UI hides the row entirely.

**Ask at a moment the user understands.** The Android app already gets this right: it sets `setFirstLoginPermissionPromptPending(true)` after login and prompts then, in context. iOS denial is stickier than Android's, so this matters more, not less.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `common/Permissions.kt` (53 LOC) | The three-state check, `Settings.canDrawOverlays` |
| `ui/permissions/` (3 files, 378 LOC) | The prompt UI: notification, camera, overlay, exact-alarm gates |
| `uikit/…/components/TDNotificationPermissionItem.kt`, `TDOverlayPermissionItem.kt`, `TDPermissionPromptCard.kt` | The shared components |
| `ui/settings/SettingsPermissionPager.kt` | The settings surface |
| `MainViewModel` / login flow | `setFirstLoginPermissionPromptPending` — the "right moment" |

## 4. Target

- `shared/domain/…/permission/PermissionController.kt`
- `shared/data/androidMain/…/AndroidPermissionController.kt`
- `shared/data/iosMain/…/IosPermissionController.kt`

## 5. Steps

1. **Define the enum and the contract**, including `isSupported`.

2. **Android: wrap the existing three-state logic.** No behaviour change.

3. **iOS:**
   - notifications → `UNUserNotificationCenter.requestAuthorization([.alert, .sound, .badge])`; read `getNotificationSettings` for status
   - camera → `AVCaptureDevice.requestAccess(for: .video)`
   - photo library → `PHPhotoLibrary.requestAuthorization(for: .readWrite)`
   - overlay, exact alarms → `isSupported = false`

4. **Map iOS statuses onto the three states.** `.notDetermined` → can prompt; `.denied` → `PERMANENTLY_DENIED` (iOS never re-prompts); `.authorized`/`.provisional` → `GRANTED`.

5. **Route `PERMANENTLY_DENIED` to Settings** via `UIApplication.openSettingsURLString`, through `ExternalLinks` (`30-15`).

6. **Hide unsupported rows.** The permissions pager and settings must not show an overlay row on iOS.

7. **Consider provisional authorization** for notifications (`.provisional` delivers quietly without a prompt). It is a real option for a task app — record the decision either way.

## 6. Code skeleton

```kotlin
// shared/domain/…/permission/PermissionController.kt
enum class AppPermission { NOTIFICATIONS, CAMERA, PHOTO_LIBRARY, OVERLAY, EXACT_ALARM }

// Three states, not two: a permanently-denied permission shows no dialog when
// requested again, and a two-state model makes that look like a frozen UI.
enum class PermissionStatus { GRANTED, DENIED, PERMANENTLY_DENIED, NOT_DETERMINED, UNSUPPORTED }

interface PermissionController {
    suspend fun request(permission: AppPermission): PermissionStatus
    suspend fun status(permission: AppPermission): PermissionStatus
    fun isSupported(permission: AppPermission): Boolean
}
```

```kotlin
// shared/data/iosMain/…/IosPermissionController.kt
class IosPermissionController : PermissionController {
    // OVERLAY and EXACT_ALARM have no iOS concept. Answering UNSUPPORTED (rather than
    // DENIED) is what lets the UI hide the row instead of offering a dead "Grant" button.
    override fun isSupported(permission: AppPermission) = when (permission) {
        AppPermission.OVERLAY, AppPermission.EXACT_ALARM -> false
        else -> true
    }
}
```

## 7. Acceptance

- [ ] `PermissionController` in `:shared:domain`; both platforms registered
- [ ] Android behaviour unchanged, including the three-state handling
- [ ] iOS: notification, camera and photo permissions request and report correctly
- [ ] `OVERLAY` and `EXACT_ALARM` report `UNSUPPORTED` on iOS and their UI rows are hidden
- [ ] A permanently-denied permission routes to Settings instead of silently doing nothing
- [ ] Notification permission is requested at the first-login moment, not at launch
- [ ] Returning from Settings refreshes the status without a restart
- [ ] The provisional-authorization decision is recorded in `DECISIONS.md`

## 8. Pitfalls

- **Two-state permission handling is a known bug in this codebase.** `CLAUDE.md` documents it. Keep three states.
- **iOS denial is permanent.** There is no second prompt. Ask at a moment the user understands, or lose notifications forever.
- **`UNSUPPORTED` ≠ `DENIED`.** Returning `DENIED` produces a "Grant" button that can never succeed.
- **Status can change while the app is backgrounded.** Re-read on foreground; do not cache across a background trip.
- **Photo library has limited access on iOS 14+.** `.limited` is neither granted nor denied — decide how to present it and record the choice.
- **Do not request at launch.** Both platforms penalise it; iOS penalises it permanently.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a device
#   fresh install → log in → notification prompt appears at the right moment
#   deny → status is PERMANENTLY_DENIED; the UI offers Settings
#   grant in Settings, return → status refreshes without a restart
#   camera and photo library the same
#   Settings → permissions pager shows NO overlay row on iOS
```
