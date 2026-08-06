---
id: 40-misc-04
title: Permission prompts
layer: ui
status: TODO
depends_on: [30-12]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/permissions/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The permission gate and prompt UI — notification, camera, overlay and exact-alarm.

## 2. Why this way

**Two of the four permissions do not exist on iOS.** Overlay and exact alarms have no counterpart, and `30-12`'s `isSupported` is what lets this UI hide those rows entirely rather than showing a button that can never succeed.

**Three-state handling is a recorded bug fix**: a permanently-denied permission shows no dialog when requested again, so a two-state model makes the UI look frozen. The third state must route to system Settings.

**The moment matters more on iOS.** Denial is permanent — there is no second prompt — so the first-login prompt point (`setFirstLoginPermissionPromptPending`) is the right place, not app launch.

## 3. Source

| Path | LOC |
|---|---|
| `ui/permissions/` (3 files) | 378 |
| `common/Permissions.kt` | 53 — the three-state check |
| `uikit/…/TDNotificationPermissionItem.kt`, `TDOverlayPermissionItem.kt`, `TDPermissionPromptCard.kt` | the components |
| `docs/screenshots/permissions/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/permissions/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify the notification prompt appears at first login, not at launch.
3. Verify the camera prompt appears when the polaroid camera is opened.
4. **Verify overlay and exact-alarm rows are hidden on iOS.**
5. Verify permanently-denied routes to Settings.
6. Verify status refreshes on return from Settings.
7. Verify the prompt copy explains *why*, in both languages.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Notification prompt at first login, not launch
- [ ] Camera prompt in context
- [ ] **Overlay and exact-alarm rows hidden on iOS**
- [ ] Permanently-denied routes to Settings
- [ ] Status refreshes on return
- [ ] Copy explains why, in both languages
- [ ] Three kits, two themes, two languages
- [ ] Previews cover granted, denied, permanently-denied, unsupported

## 8. Pitfalls

- **Two-state handling is the recorded bug.** Keep three.
- **Hide unsupported rows.** A dead "Grant" button is worse than no row.
- **iOS denial is permanent.** Ask at a moment the user understands.
- **Refresh on foreground.** A cached status goes stale when the user changes it in Settings.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Fresh install on both platforms: first login prompt, deny, confirm the Settings
# route, grant in Settings and return (status refreshes); iOS shows no overlay row
```
