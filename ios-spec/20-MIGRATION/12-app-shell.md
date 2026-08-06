---
id: 20-12
title: Reduce `:app` to a shell
layer: foundation
status: TODO
depends_on: [20-11]
blocks: [20-13]
parallel_safe: false
estimate: 20h
reversible: true
owner_files:
  - app/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:bundleRelease
  - ./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
---

## 1. Goal

Reduce `:app` to manifest-declared components and Android-only plumbing — roughly 1,800 LOC, down from 67,982. Regenerate the baseline profile and confirm startup performance has not regressed.

**This is milestone M5: Android 1.3 is shippable here, with zero iOS code written.**

## 2. Why this way

After `20-11`, `:app` holds a mixture of things that genuinely must be there (Services, Receivers, the Application class) and things that merely have not moved yet. Sorting that out now, while the Android app is the only consumer, means `20-13` turns iOS on against a clean boundary.

**The baseline profile is the hidden risk in this task.** `app/src/main/generated/baselineProfiles/baseline-prof.txt` contains method signatures. Moving classes between modules invalidates them, and a stale profile **fails silently** — no build error, no test failure, just 150–300 ms of extra cold start that nobody notices until a user review mentions it. The gate is a macrobenchmark comparison, not a green build.

**Shipping here is not optional ceremony.** Proving the migrated app is releasable — signed, profiled, benchmarked, on Play — before adding a second platform is what makes the whole "always green" discipline worth its cost.

## 3. Source — read before writing

| Path | Disposition |
|---|---|
| `app/…/Application.kt` | **Stays.** Firebase reconcile, App Check, notification channels, Places init, Koin start, WorkManager config, alarm resweep. |
| `app/…/MainActivity.kt` | **Stays**, thin: splash, edge-to-edge, push-intent forwarding, `setContent { App() }`. Must remain a `FragmentActivity`. |
| `app/…/data/notification/{NotificationService,PomodoroForegroundService,PomodoroNotificationBuilder,PomodoroNotificationChannels,PomodoroServiceController,PomodoroSessionAlarmScheduler,PomodoroSessionEndReceiver}.kt` | **Stay** — manifest-declared components |
| `app/…/ui/overlay/OverlayService.kt` + channel | **Stay** — `SYSTEM_ALERT_WINDOW`, no iOS analogue |
| `app/…/data/source/remote/fcm/TDFireBaseMessagingService.kt` | **Stays** — a Service |
| `app/…/data/alarm/{AlarmFireReceiver,BootReceiver,AlarmSchedulerImpl,AlarmRequestCodes}.kt` | **Stay** for now; `AlarmSchedulerImpl` becomes the Android adapter in `30-PLATFORM/01` |
| `app/…/data/worker/*.kt` (3) | **Stay** — WorkManager is Android-only |
| `app/…/data/{log,perf,analytics}/` | **Stay** — Firebase Android SDKs |
| `app/…/data/update/PlayAppUpdateChecker.kt` | **Stays** — Play Core |
| `app/…/StrictModeConfig.kt` | **Stays** — debug only |
| `app/src/main/res/{mipmap-*,xml,values-night}/` | **Stay** |
| `app/src/main/generated/baselineProfiles/baseline-prof.txt` | **Regenerate** |
| `donebot prod/MACROBENCHMARK_RUN.md` | The pre-migration P90 numbers to compare against |

## 4. Target

`:app` at ~1,800 LOC containing only: `Application`, `MainActivity`, the 4 Services, the 3 Receivers, the 3 Workers, the Android-only data implementations (alarm, notification, analytics, crash, perf, update), Android resources that cannot move, and the Koin Android module.

## 5. Steps

1. **Inventory what is left.** For every remaining file, answer: is it manifest-declared, does it touch an Android-only SDK, or has it simply not moved? Move the third category.

2. **Move the stragglers** to the appropriate shared module.

3. **Verify `:app`'s dependency direction.** It depends on `:composeApp`; nothing depends on `:app`.

4. **Regenerate the baseline profile** on a physical device:
   ```bash
   ./gradlew :app:generateBaselineProfile
   ```
   Never hand-edit it.

5. **Run the macrobenchmarks** and compare P90 startup against `donebot prod/MACROBENCHMARK_RUN.md`. **Gate: within +15%.** Outside that, the profile is stale or a startup path regressed — investigate before shipping.

6. **Full gate, size measurement, signed release build**, `jarsigner -verify`.

7. **Ship Android 1.3 to Play — but the merge is the owner's call.** This is the one point where `feat/ios-port` is a candidate to merge into `main` (decision D-11). **Do not merge it yourself.** Report that M5 is reached, that the gate below is green, and let the owner decide. If they approve, bump `versionCode` (read the current value from `app/build.gradle.kts`; Play burns codes permanently even for deleted drafts) and tag.

8. **Mark milestone M5 complete** in `PROGRESS.md`.

## 6. Code skeleton

```kotlin
// app/…/MainActivity.kt — the whole file, after
class MainActivity : FragmentActivity() {     // FragmentActivity: BiometricPrompt requires it
    private val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let(viewModel::onPushIntent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onPushIntent(intent)
    }
}
```

## 7. Acceptance

- [ ] `:app` is under ~2,000 LOC: `find app/src/main/java -name '*.kt' | xargs wc -l | tail -1`
- [ ] Every remaining `:app` file is manifest-declared or touches an Android-only SDK — justified in the task notes
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] Baseline profile regenerated on a physical device
- [ ] Macrobenchmark P90 startup **within +15%** of the pre-migration baseline
- [ ] `ScrollJankBenchmark` within +15%
- [ ] `:app:bundleRelease` under the (raised) ceiling; size recorded
- [ ] Signed AAB verifies: `jarsigner -verify -verbose -certs`
- [ ] Full manual smoke on a real device: launch, log in, task CRUD, reminder fires, pomodoro with Live notification, push received, sync works, all three palette kits
- [ ] Android 1.3 uploaded to Play; tagged
- [ ] M5 marked complete in `PROGRESS.md`

## 8. Pitfalls

- **A stale baseline profile fails silently.** No build error. The macrobenchmark is the only detector. Do not skip it because the build is green.
- **`generateBaselineProfile` needs a physical device.** No device means `BLOCKED` for that check — do not ship an unverified profile.
- **Never hand-patch `baseline-prof.txt`.** It is generated. Editing it produces a profile that looks right and does nothing.
- **`MainActivity` stays a `FragmentActivity`.**
- **Do not move Services or Receivers.** They are manifest-declared and must stay in the application module.
- **`Application.onCreate` ordering is load-bearing.** Timber must be planted before anything logs; Firebase consent must reconcile before collection starts; Koin must start before anything injects; Places initialises only when a key is present. Preserve the order.
- **Play burns `versionCode` permanently**, including for deleted drafts. Read the current value from the build file rather than incrementing what you remember.
- **Do not skip the Play release.** Shipping here is what proves the migration is releasable, and it is the last clean point before iOS complexity lands.

## 9. Verification

```bash
# 1. Size of :app
find app/src/main/java -name '*.kt' | xargs wc -l | tail -1

# 2. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Baseline profile + benchmarks (physical device)
./gradlew :app:generateBaselineProfile
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
# compare P90 against donebot prod/MACROBENCHMARK_RUN.md — gate is +15%

# 4. Signed release
./gradlew :app:bundleRelease
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | head
ls -l app/build/outputs/bundle/release/*.aab
```
