---
id: 30-02
title: Background sync
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [30-03]
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/sync/**
  - shared/data/src/androidMain/**/worker/**
  - shared/data/src/iosMain/**/sync/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Put a `BackgroundSync` contract in front of WorkManager, and implement it on iOS with `BGTaskScheduler` — while making correctness independent of whether background execution ever happens.

## 2. Why this way

**Android's sync is already foreground-dominated, which is what makes this tractable.** Reading the trigger list in `TaskSyncRepositoryImpl` and its callers: home load, pull-to-refresh, calendar open, after a chat turn that mutates tasks, after login, after register, after token refresh, on push, on logout. WorkManager exists mostly to *retry* what the foreground started, not to be the primary path.

iOS has nothing comparable. `BGAppRefreshTask` is opportunistic, disabled under Low Power Mode, and may never run for a user who force-quits. **So the design rule is: background is an optimization, never a correctness requirement.** The offline queue already guarantees eventual consistency — `syncStatus` rows survive until pushed. What background execution buys is *sooner*, not *at all*.

**WorkManager's unique-work semantics have no iOS equivalent.** `ExistingWorkPolicy.KEEP` on `sync_work` is load-bearing: `REPLACE` would cancel a running push mid-flight, leaving a committed POST with a still-`PENDING_CREATE` local row. On iOS the equivalent is an in-process serial queue plus the persisted `syncStatus` — the state machine already makes re-entry safe.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/repository/TaskSyncRepositoryImpl.kt` | Unique work names `sync_work` / `fetch_work`, `ExistingWorkPolicy.KEEP` (and the comment explaining why not `REPLACE`), the **60 s in-memory cooldown**, `resetCooldown()`, exponential backoff from 30 s |
| `data/worker/SyncWorker.kt` | Push; retries on `NoInternet \| Server \| ServerUnreachable \| Unauthorized`, `MAX_ATTEMPT = 2`; Firebase trace `sync_pending_tasks` |
| `data/worker/FetchTasksWorker.kt` | Pull; retries on the same set **minus** `Unauthorized` |
| `data/worker/RescheduleAlarmsWorker.kt` | Expedited, with a non-expedited fallback |
| `data/repository/TaskRepositoryImpl.kt` | `syncLocalTasksToServer`, `reconcileRemoteIntoLocal`, the `@Singleton syncMutex`, `isRetryable` (~line 1246) |
| `data/network/BackendWarmUp.kt` | Foreground ping for the Render cold start |
| `data/network/NetworkMonitor.kt` | `ConnectivityManager` callback → `StateFlow<Boolean>` |

## 4. Target

- `shared/domain/…/sync/BackgroundSync.kt` — the contract
- `shared/data/androidMain/…/WorkManagerBackgroundSync.kt` — wraps the existing 3 workers
- `shared/data/iosMain/…/BgTaskBackgroundSync.kt` — `BGTaskScheduler`
- `iosApp/AppDelegate.swift` — task registration
- `iosApp/Info.plist` — `BGTaskSchedulerPermittedIdentifiers`

## 5. Steps

1. **Define the contract** with the four operations the app actually asks for: push, fetch, reminder rebuild, periodic registration.

2. **Android: wrap, do not rewrite.** The workers, cooldown, backoff and `KEEP` policy all stay exactly as they are.

3. **iOS: register two identifiers** — `com.todoapp.mobile.refresh` (`BGAppRefreshTask`) and `com.todoapp.mobile.sync` (`BGProcessingTask`, for longer work with a network requirement). Both must appear in `BGTaskSchedulerPermittedIdentifiers` or scheduling silently fails.

4. **Reschedule on every run.** iOS grants exactly one execution per submitted request. Forgetting to resubmit means it runs once, ever.

5. **Set an expiration handler.** iOS kills the task without warning; unfinished work must leave `syncStatus` consistent. It already does — that is the point of the state machine.

6. **Serialize on iOS with a `Mutex`**, mirroring the `@Singleton syncMutex`. Push and pull must never interleave.

7. **Keep `BackendWarmUp` on foreground.** Render cold starts affect iOS identically.

8. **Preserve the 60 s cooldown.** It exists to stop a screen-change storm from hammering the backend, and applies equally on iOS.

## 6. Code skeleton

```kotlin
// shared/domain/…/sync/BackgroundSync.kt
enum class SyncReason { USER_REFRESH, AFTER_MUTATION, AFTER_LOGIN, PUSH, PERIODIC }

interface BackgroundSync {
    fun requestSync(reason: SyncReason)      // push local → remote
    fun requestFetch()                       // pull remote → local
    fun requestReminderRebuild()
    fun registerPeriodic()                   // iOS: BGTaskScheduler; Android: no-op
}
```

```kotlin
// shared/data/iosMain/…/BgTaskBackgroundSync.kt
class BgTaskBackgroundSync(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
    private val scope: CoroutineScope,
) : BackgroundSync {

    // Mirrors the @Singleton syncMutex: push and pull must never interleave.
    private val mutex = Mutex()

    override fun registerPeriodic() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(REFRESH_ID, null) { task ->
            // iOS grants ONE execution per submitted request. Resubmit immediately or
            // this runs exactly once, ever.
            submitRefresh()
            val job = scope.launch { mutex.withLock { runSync() } }
            task?.setExpirationHandler { job.cancel() }   // killed without warning; syncStatus stays consistent
            job.invokeOnCompletion { task?.setTaskCompletedWithSuccess(it == null) }
        }
        submitRefresh()
    }

    private fun submitRefresh() {
        val request = BGAppRefreshTaskRequest(REFRESH_ID).apply {
            earliestBeginDate = NSDate().dateByAddingTimeInterval(4 * 60 * 60.0)
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }

    private companion object { const val REFRESH_ID = "com.todoapp.mobile.refresh" }
}
```

## 7. Acceptance

- [ ] `BackgroundSync` contract in `:shared:domain`; both implementations registered in Koin
- [ ] Android behaviour unchanged — `SyncWorkerTest` and `FetchTasksWorkerTest` pass untouched
- [ ] Unique-work `KEEP` policy and the 60 s cooldown preserved
- [ ] iOS identifiers registered **and** listed in `BGTaskSchedulerPermittedIdentifiers`
- [ ] Every background run resubmits the next request
- [ ] Expiration handler cancels cleanly; `syncStatus` stays consistent after a kill
- [ ] iOS: push and pull serialized by a `Mutex`
- [ ] Foreground sync works with background refresh **disabled in Settings** — the correctness gate
- [ ] `BackendWarmUp` pings on foreground on iOS
- [ ] Manual: create a task offline → go online → foreground the app → it syncs

## 8. Pitfalls

- **Never make correctness depend on background execution.** A user with Background App Refresh off must still get a fully working app. Test with it disabled.
- **Simulator background behaviour is not representative.** It is far more permissive than a real device. Use `e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"..."]` in LLDB, and confirm on hardware.
- **Forgetting to resubmit** means the task runs once and never again — and nothing reports it.
- **Missing `BGTaskSchedulerPermittedIdentifiers`** makes `submitTaskRequest` fail silently.
- **Do not change `KEEP` to `REPLACE`.** The comment in `TaskSyncRepositoryImpl` explains the exact corruption it prevents.
- **Do not remove the 60 s cooldown.** It stops a screen-change storm from hammering a cold-starting Render dyno.
- **Low Power Mode disables background refresh entirely.** Plan the degraded experience; do not treat it as an error state.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*SyncWorker*' --tests '*FetchTasksWorker*'

# iOS, on hardware — and with Background App Refresh OFF for the correctness gate
#   create a task offline → online → foreground → syncs
#   force-quit, reopen → pending rows still push
#   LLDB: simulate the BGTask launch and confirm it resubmits
```
