---
id: 80-04
title: TestFlight
layer: release
status: TODO
depends_on: [80-01, 80-02, 70-02]
blocks: [80-05]
parallel_safe: false
estimate: 20h
reversible: true
owner_files:
  - iosApp/ExportOptions.plist
verify:
  - "A build reaches external testers and reminder delivery is measured for two weeks"
---

## 1. Goal

Get real builds to real devices, and — the point of this task — **measure reminder delivery before submitting**.

## 2. Why this way

**Reminders are the app's core promise and the one thing that cannot be verified in a simulator.** The 64-slot budget, the rolling window, the sentinel, the coverage-horizon push from `70-02` — all of it depends on real device behaviour over real time: force-quits, Low Power Mode, restarts, background refresh being disabled, days passing.

So the exit criterion is not "the build installs." It is **a measured delivered/expected reminder ratio over two weeks**. If that ratio is poor, the reminder design needs another pass *before* review, not after users find it.

**Everything else here is mechanical**, and the parts that are not — signing, dSYM upload, push in a production APNs environment — are exactly the things that behave differently outside a debug build. TestFlight is the first place they are exercised.

**A push that works in debug and not in TestFlight is almost always the APNs environment.** Debug builds use sandbox; TestFlight and App Store use production. If the `.p8` key is uploaded correctly to Firebase this is handled, but it is the first thing to check.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `iosApp/ExportOptions.plist` | Export configuration |
| `ios-spec/30-PLATFORM/01-notifications-and-alarms.md` | What is being measured |
| `ios-spec/70-BACKEND/02-reminder-push.md` | The server-side safety net |
| `donebot prod/SIGNED_AAB_SMOKE.md` | The Android signed-build smoke protocol — the model for an iOS equivalent |
| `donebot prod/FCM_PROD_VALIDATION.md` | The Android push validation protocol — same |
| `PROGRESS.md` | App Store Connect API key from `10-01` |

## 4. Target

- `iosApp/ExportOptions.plist`
- An internal and an external TestFlight group
- A reminder-delivery measurement log

## 5. Steps

1. **Archive and export** a signed build. Upload with `xcrun altool` using the App Store Connect API key from `10-01`.

2. **Verify the dSYM upload** ran. Force a test crash and confirm it arrives in Crashlytics **symbolicated**. An unsymbolicated crash report is useless and the failure is silent.

3. **Internal testing first** — up to 100 testers, no review required. Run the full smoke: launch, sign up, sign in (all three methods), task CRUD, groups, chat, pomodoro, journal, settings, all three palette kits, both languages, iPhone and iPad.

4. **Verify push in production APNs.** Send each of the 10 payload types. If push works in debug but not here, check the APNs key and environment first.

5. **Set up the reminder measurement.** Instrument expected-vs-delivered: the app knows what it scheduled (`ReminderScheduleReport`), so log a delivery event when a notification is presented or tapped and compare. Two weeks, across at least: a heavy user, a light user, one with Background App Refresh **off**, and one who force-quits regularly.

6. **External testing** — requires a Beta App Review, usually a day or two. Notes should mention that reminders are the focus.

7. **Watch Crashlytics daily.** The gate is a crash-free rate above 99%.

8. **Verify the coverage horizon in practice.** A tester who does not open the app for a week must still receive their reminders, via `70-02`'s push. This is the whole safety net and it can only be tested here.

## 6. Code skeleton

```bash
# Archive, export, upload
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -archivePath build/DoneBot.xcarchive archive

xcodebuild -exportArchive -archivePath build/DoneBot.xcarchive \
  -exportOptionsPlist iosApp/ExportOptions.plist -exportPath build/ipa

xcrun altool --upload-app -f build/ipa/DoneBot.ipa --type ios \
  --apiKey "$ASC_KEY_ID" --apiIssuer "$ASC_ISSUER"
```

```
Reminder delivery log — the exit criterion
  tester | profile        | expected | delivered | ratio | notes
  -------|----------------|----------|-----------|-------|-------------------------
  A      | heavy, daily   |       84 |        84 | 100%  |
  B      | light, weekly  |       21 |        20 |  95%  | one beyond the horizon
  C      | BGRefresh off  |       35 |        35 | 100%  | foreground reconcile held
  D      | force-quits    |       28 |        26 |  93%  | both recovered via push
```

## 7. Acceptance

- [ ] A signed build uploads and processes without warnings
- [ ] dSYMs uploaded; a forced test crash arrives **symbolicated**
- [ ] Internal testing passes the full smoke on iPhone **and** iPad
- [ ] All three sign-in methods work in a production build
- [ ] All 10 push types deliver via **production** APNs
- [ ] External testing approved and at least 5 external testers active
- [ ] **Reminder delivery measured for two weeks across all four tester profiles**
- [ ] Delivery ratio acceptable, including for the lapsed-user profile
- [ ] `70-02`'s coverage-horizon push verified end to end with a real lapsed user
- [ ] Crash-free rate above 99%
- [ ] Live Activity, widgets and App Intents all verified on hardware
- [ ] No blocking feedback outstanding

## 8. Pitfalls

- **Do not skip the reminder measurement.** It is the only pre-submission evidence that the core feature works, and the design cannot be validated any other way.
- **Sandbox vs production APNs.** The most common "worked in debug" push failure.
- **Missing dSYMs make every crash report useless**, and nothing warns you.
- **Beta App Review takes a day or two.** Plan for it.
- **Test with Background App Refresh disabled.** A meaningful share of users have it off, and correctness must not depend on it.
- **Test across a date boundary.** Reminder scheduling is date arithmetic; midnight, month end and DST are where it breaks.
- **TestFlight builds expire after 90 days.** Relevant for a long beta.
- **Do not accumulate unaddressed feedback.** Testers stop reporting.

## 9. Verification

```bash
# Build state
curl -s -H "Authorization: Bearer $ASC_JWT" \
  "https://api.appstoreconnect.apple.com/v1/apps/$APP_ID/builds" \
  | jq '.data[0].attributes.processingState'

# dSYMs present in the archive
ls build/DoneBot.xcarchive/dSYMs/

# Then: two weeks of measurement across the four tester profiles,
# recorded in the delivery log above.
```
