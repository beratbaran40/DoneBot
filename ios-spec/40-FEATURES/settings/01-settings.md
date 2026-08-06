---
id: 40-settings-01
title: Settings
layer: ui
status: TODO
depends_on: [40-core-01, 30-12, 30-14, 30-01]
blocks: [40-settings-02, 40-settings-04, 40-settings-05]
parallel_safe: false
estimate: 16h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/settings/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The settings hub — eight sections, plus the notification-settings and secret-mode sub-screens. 20 files, 2,759 LOC.

## 2. Why this way

**Settings is where several platform differences become user-visible at once**, which is why it depends on three contracts:

- **`30-01`** — the coverage row. **"Scheduled through 27 Aug · 4 later reminders will be set the next time you open DoneBot"** is the single most important piece of copy in the iOS port. It converts an invisible 64-slot limit into a visible, actionable state, and it is what stops the app reading as broken.
- **`30-14`** — in-app language switching, which on iOS requires a restart for system-formatted values and must say so honestly.
- **`30-12`** — the permissions pager, which must **hide** rows for permissions iOS does not have (overlay, exact alarms) rather than showing dead buttons.

**Section order is settled**: Profile → … → Account → Danger Zone, with destructive actions last. This screen is also the worked example of the file-splitting rule — 1,040 lines became a 35-line entry point plus `SettingsContent` and one file per section.

**Consent toggles must keep their asymmetric defaults**: crash/analytics opt-out default ON, performance opt-in default OFF.

## 3. Source

| Path | LOC |
|---|---|
| `ui/settings/` (20 files) | 2,759 |
| `ui/settings/NotificationSettingsScreen.kt`, `SecretModeSettingsScreen.kt` | sub-screens |
| `ui/settings/SettingsPermissionPager.kt` | the permission rows |
| `ui/settings/DataExportSaver.kt` | GDPR export via SAF → needs an iOS equivalent (share sheet) |
| `uikit/…/TDSettingsGroup.kt`/`TDSettingsItem.kt`/`TDSwitch.kt` | the One UI components |
| `docs/screenshots/settings/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/settings/` — verification plus the coverage row.

## 5. Steps

1. Verify all 20 files compile in `commonMain`.
2. **Add the reminder coverage row**, driven by `ReminderScheduleReport.coverageHorizon` and `dropped.size`, localized EN + TR.
3. **Add the Time Sensitive explanation** with a button to `openNotificationSettings()`.
4. Verify language switching, including the honest restart notice on iOS.
5. Verify theme and palette switching, including the reveal animation.
6. Verify the permissions pager hides unsupported rows on iOS.
7. Verify consent toggles keep their asymmetric defaults and take effect immediately.
8. Verify data export — SAF on Android, share sheet on iOS.
9. Verify account deletion is reachable and its typed confirmation is localized.
10. Verify section order and Danger Zone placement.
11. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 20 files compile in `commonMain`
- [ ] **Coverage row shows the correct date and dropped count, in both languages**
- [ ] Time Sensitive explanation present with a working Settings button
- [ ] Language switching works; iOS restart notice is honest
- [ ] Theme and palette switching work, reveal animation included
- [ ] Unsupported permission rows hidden on iOS
- [ ] Consent defaults correct: crash/analytics ON, performance OFF; changes immediate
- [ ] Data export works on both platforms
- [ ] Account deletion reachable; typed confirmation localized
- [ ] Section order preserved; destructive actions last
- [ ] Three kits, two themes, two languages
- [ ] Previews cover each section

## 8. Pitfalls

- **The coverage row is the most important copy in the port.** Without it the 64-slot limit reads as broken reminders.
- **Do not show dead permission rows.** `isSupported = false` means hide.
- **Consent defaults are asymmetric.** Reversing them is a privacy incident.
- **Typed confirmations are localized and case-sensitive per language.**
- **Destructive actions last.**
- **Do not regrow the split files.**
- **The restart notice must be honest.** A language switch that half-works with no explanation reads as a bug.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# iOS: create enough reminders to exceed the window, check the coverage row;
# switch language (restart notice), switch palette (reveal), toggle consents,
# export data, reach account deletion and type the confirmation in Turkish
```
