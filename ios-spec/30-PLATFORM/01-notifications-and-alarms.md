---
id: 30-01
title: Reminders, notifications & the alarm presenter
layer: platform
status: TODO
depends_on: [20-03, 20-13, 30-00]
blocks: [40-core-01, 40-misc-02, 80-04]
parallel_safe: false
estimate: 50h
reversible: false
owner_files:
  - shared/domain/src/commonMain/**/alarm/**
  - shared/data/src/androidMain/**/alarm/**
  - shared/data/src/iosMain/**/notification/**
  - app/src/main/java/com/todoapp/mobile/data/alarm/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :shared:domain:testDebugUnitTest --tests '*ReminderPlanner*'
---

## 1. Goal

Replace the imperative `AlarmScheduler` with a declarative, budgeted `ReminderScheduler` that both platforms implement — preserving Android's behaviour exactly while making iOS's hard limits explicit, testable, and visible to the user.

**This is the highest-risk task in the project.** Not because it can lose data, but because reminders are the app's core promise. Getting them subtly wrong on iOS produces one-star reviews that say "it doesn't remind me."

## 2. Why this way

### What Android does today

`AlarmSchedulerImpl` arms exact `RTC_WAKEUP` alarms whose `PendingIntent`s carry their own recurrence rule as extras. `AlarmFireReceiver.rescheduleNextInstanceIfRecurring` re-arms from those extras **at fire time**. Request codes come from `AlarmRequestCodes.kt`, four disjoint ranges over the signed 32-bit space:

```
0x0100_0000  recurring personal, slot 0   (unshifted, pre-multi-reminder compatibility)
0x0200_0000  one-shot personal
0x1000_0000  recurring personal, slots 1..7
0x4000_0000  group, all slots             (server ids collide with local Room ids)
```

`MAX_REMINDER_SLOTS = 8`. Plus one daily-plan alarm. `RescheduleAllAlarmsUseCase` sweeps everything on boot and app start.

### What iOS cannot do

| Limit | Consequence |
|---|---|
| **64 pending requests, hard** | Additional `add()` calls are dropped. The budget must be owned explicitly. |
| **No code at fire time** | `UNNotificationServiceExtension` fires for *push* only. **Self-re-arming is impossible.** The entire `AlarmFireReceiver` mechanism has no analogue. |
| **No overlay** | `.timeSensitive` breaks through Focus and is available to all apps. `.critical` needs an entitlement granted essentially only to safety apps — **do not plan on it**. |
| **No system ringtone list** | Bundled `.caf`/`.aiff`/`.wav` ≤30 s only. |
| **Background refresh is opportunistic** | May never run for a user who force-quits. |

### The design

**Change the contract shape first.** The current API is imperative (`schedule`/`cancelRecurring`), which forces per-call request-code arithmetic and makes a *global* budget impossible to express. Replace it with a whole-world reconcile.

Splitting `RescheduleAllAlarmsUseCase` into a **pure** `BuildReminderSpecsUseCase` (shared) plus a platform `apply(specs)` also removes an entire bug class: the two platforms can no longer disagree about which reminders exist.

**Android keeps its proven internals.** `AlarmSchedulerImpl` becomes a thin adapter that diffs specs into the existing calls. `AlarmRequestCodes.kt` and `AlarmRequestCodesTest` are untouched.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `shared/domain/…/alarm/AlarmScheduler.kt` | 62 | The interface being replaced. `MAX_REMINDER_SLOTS = 8`. Read the KDoc on `scheduleRecurring` — "the alarm re-arms itself on fire" is the exact thing iOS cannot do. |
| `app/…/data/alarm/AlarmSchedulerImpl.kt` | 284 | `computeNextFire` (walks up to 400 days), month-end clamping, self-re-arm |
| `app/…/data/alarm/AlarmRequestCodes.kt` | 45 | The four ranges. **Pure arithmetic, already tested — preserve it.** |
| `app/…/data/alarm/AlarmFireReceiver.kt` | 156 | Re-arm-from-extras; A12 background-start exemption |
| `app/…/data/alarm/BootReceiver.kt` | 42 | `BOOT_COMPLETED` → `RescheduleAlarmsWorker` |
| `shared/domain/…/alarm/RescheduleAllAlarmsUseCase.kt` | — | Splits into pure + platform halves |
| `shared/domain/…/alarm/BuildDailyPlanAlarmItem.kt` | — | The daily-plan reminder |
| `shared/domain/…/model/Recurrence.kt` | — | **`firesOn` + `clampedDayOfMonth`.** The expressibility table below is derived from it. |
| `data/model/entity/TaskReminderEntity.kt` | — | The `slot` column — **must stay stable**, it seeds the request code |
| `app/src/test/…/AlarmRequestCodesTest.kt`, `TaskAlarmLifecycleTest.kt` | — | Must keep passing |
| `app/…/ui/overlay/OverlayService.kt` | ~460 | The full-screen presenter with no iOS analogue |

## 4. Target

```
shared/domain/…/alarm/ReminderSpec.kt              ReminderKey, ReminderSpec, capabilities, report
shared/domain/…/alarm/ReminderScheduler.kt         the contract
shared/domain/…/alarm/ReminderPlanner.kt           PURE — the budget algorithm
shared/domain/…/usecase/BuildReminderSpecsUseCase.kt
shared/data/androidMain/…/AndroidReminderScheduler.kt   adapter over AlarmSchedulerImpl
shared/data/iosMain/…/IosReminderScheduler.kt      UNUserNotificationCenter
shared/domain/src/commonTest/…/ReminderPlannerTest.kt   the gate
iosApp/AppDelegate.swift                            UNUserNotificationCenterDelegate
```

## 5. Steps

1. **Define the value types and the contract** (skeleton below).

2. **Write `ReminderPlanner` as a pure function** in `commonMain`. No platform types, no I/O, fully unit-testable. This is where the budget lives.

3. **Slot budget — 64 total:**

   | Class | Budget | Mechanism |
   |---|---|---|
   | Daily-plan reminder | 1 | `UNCalendarNotificationTrigger(repeats: true)` — costs 1 **forever** |
   | Expressible recurrences | 28 | one repeating trigger per (task, slot); weekly costs `byDay.size` |
   | Materialized rolling window | 30 | `repeats: false` for every firing in the horizon, ascending |
   | Overflow sentinel | 1 | at `horizon − 24h`: "Open DoneBot to keep your reminders scheduled" |
   | Reserve | 4 | absorbs races between rebuild and OS bookkeeping |

   The 28-slot repeating pool is the point: **5 daily routines × 2 reminder times = 10 slots covering the user forever with zero maintenance** — and that is this app's most common pattern.

4. **Expressibility table** — derived from `Recurrence.kt`:

   | Rule | Repeating trigger? | Slot cost |
   |---|---|---|
   | `DAILY`, interval 1, no `until` | ✅ `DateComponents(hour, minute)` | 1 |
   | `WEEKLY`, interval 1, no `until`, N days | ✅ N × `DateComponents(weekday, hour, minute)` | N |
   | `MONTHLY`, interval 1, no `until`, day ≤ 28 | ✅ `DateComponents(day, hour, minute)` | 1 |
   | **`MONTHLY`, day 29–31** | ❌ **materialize** | window |
   | `YEARLY`, interval 1, no `until`, not Feb 29 | ✅ | 1 |
   | **`YEARLY`, Feb 29** | ❌ **materialize** | window |
   | any `interval > 1` | ❌ materialize | window |
   | any `until != null` | ❌ (`repeats:true` has no end date) | window |
   | `NONE` (one-shot) | n/a | window |

   > **The MONTHLY day-31 row is a real semantic divergence, not an edge case.** `RecurrenceRule.firesOn` calls `clampedDayOfMonth`, so "the 31st of every month" fires on **28 February** on Android. `UNCalendarNotificationTrigger(day: 31, repeats: true)` simply **does not fire in February**. Materializing is the only way to preserve behaviour. Same for Feb 29 yearly. `ReminderPlannerTest` locks both.

5. **Precedence when over budget** — deterministic, pure, testable:
   1. Daily-plan — always
   2. Every firing in the next 48 h, ascending (one-shots and materialized interleaved)
   3. Repeating triggers for expressible recurrences, ordered by next fire
   4. Remaining window entries, ascending
   5. Sentinel

   Drop from the tail; report `dropped`.

6. **Horizon:** `min(21 days, the point at which the 30-slot materialization budget fills)`. Twenty-one days because a user who has not opened a task app in three weeks needs a *push*, not a longer window — and it keeps rebuild cheap.

7. **Identifier scheme** — a string mirror of `AlarmRequestCodes.kt`, preserving the personal/group split (server ids collide with local Room ids):
   ```
   dailyplan
   t.{taskId}.s{slot}.r              repeating, personal
   g.{taskId}.s{slot}.r              repeating, group
   t.{taskId}.s{slot}.{epochSec}     materialized one-shot
   sentinel
   ```

8. **Diff, never clear.** Never `removeAllPendingNotificationRequests()` then re-add — that opens a visible gap. Compute desired → read pending → remove `pending − desired` → add `desired − pending`. Idempotence is testable: two consecutive `apply(sameSpecs)` calls must produce an empty diff on the second.

9. **Rebuild triggers** — five, all cheap and idempotent:
   1. `applicationDidBecomeActive` — the primary path, the analogue of `Application.onCreate`'s sweep
   2. **after any local mutation touching reminders, recurrence or completion** — the same call sites that call `AlarmScheduler` today. This is the important one: the user is *in the app* when they create a task.
   3. after a successful sync
   4. `BGAppRefreshTask` — best effort
   5. on receipt of any push

10. **Write the Android adapter.** It maps specs onto the existing `schedule`/`scheduleRecurring`/`cancelRecurring` calls. `capabilities.maxPendingRequests = null`.

11. **Write `IosReminderScheduler` in Kotlin/Native**, against the `platform.UserNotifications` cinterop — **not Swift**. This keeps the diffing logic where `commonTest` can reach it, and nothing in `UNUserNotificationCenter` needs Swift.

12. **Swift keeps only the delegate** — foreground presentation and tap routing into the same deep-link path `MainViewModel.onPushIntent` uses today.

13. **Surface the coverage horizon in Settings.** See §7 and `00-CONTEXT/04-constraints.md` §3.

## 6. Code skeleton

```kotlin
// shared/domain/…/alarm/ReminderSpec.kt
@Serializable
data class ReminderKey(val scope: Scope, val taskId: Long, val slot: Int) {
    enum class Scope { PERSONAL, GROUP, DAILY_PLAN }
}

data class ReminderSpec(
    val key: ReminderKey,
    val title: String,
    val body: String,
    val leadMinutes: Long,            // "N minutes before"; 0 = at time
    val recurrence: RecurrenceRule,   // frequency == NONE ⇒ one-shot
    val anchorDate: LocalDate,
    val timeOfDay: LocalTime,
    val soundId: String?,             // catalog id, never a Uri (see 20-02)
    val timeSensitive: Boolean = true,
)

data class ReminderCapabilities(
    val maxPendingRequests: Int?,       // 64 on iOS, null on Android
    val supportsSelfRearming: Boolean,
    val supportsFullScreenAlert: Boolean,
    val supportsSystemSoundPicker: Boolean,
)

data class ReminderScheduleReport(
    val scheduledCount: Int,
    val coverageHorizon: Instant?,      // null = unbounded (Android)
    val dropped: List<ReminderKey>,
)

interface ReminderScheduler {
    /** The ONLY mutating entry point. Idempotent: apply(x) twice == apply(x) once. */
    suspend fun apply(specs: List<ReminderSpec>): ReminderScheduleReport
    suspend fun cancelAll()
    val capabilities: ReminderCapabilities
}
```

```kotlin
// shared/domain/…/alarm/ReminderPlanner.kt — PURE. No platform types, no I/O.
object ReminderPlanner {
    fun plan(
        specs: List<ReminderSpec>,
        now: LocalDateTime,
        zone: TimeZone,
        budget: SlotBudget,
    ): ReminderPlan
}

data class SlotBudget(
    val total: Int = 64,
    val dailyPlan: Int = 1,
    val repeating: Int = 28,
    val window: Int = 30,
    val sentinel: Int = 1,
    val reserve: Int = 4,
)

data class ReminderPlan(
    val repeating: List<RepeatingTrigger>,   // month/day/weekday/hour/minute; -1 = unset
    val oneShots: List<OneShotTrigger>,      // absolute Instant
    val sentinel: OneShotTrigger?,
    val dropped: List<ReminderKey>,
    val coverageHorizon: Instant,
)
```

```kotlin
// shared/data/iosMain/…/IosReminderScheduler.kt
class IosReminderScheduler(
    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
    private val clock: Clock,
    private val zoneProvider: () -> TimeZone,
) : ReminderScheduler {

    override val capabilities = ReminderCapabilities(
        maxPendingRequests = 64,
        supportsSelfRearming = false,
        supportsFullScreenAlert = false,
        supportsSystemSoundPicker = false,
    )

    override suspend fun apply(specs: List<ReminderSpec>): ReminderScheduleReport {
        val plan = ReminderPlanner.plan(specs, clock.now().toLocalDateTime(zoneProvider()), zoneProvider(), SlotBudget())
        val desired = plan.toRequests()
        val pending = center.pendingRequests().associateBy { it.identifier }
        // Diff, never clear-and-re-add: clearing opens a window where nothing is armed.
        (pending.keys - desired.keys).forEach { center.removePendingNotificationRequestsWithIdentifiers(listOf(it)) }
        (desired.keys - pending.keys).forEach { center.addNotificationRequest(desired.getValue(it)) }
        return ReminderScheduleReport(desired.size, plan.coverageHorizon, plan.dropped)
    }

    override suspend fun cancelAll() { center.removeAllPendingNotificationRequests() }
}
```

```swift
// iosApp/AppDelegate.swift — Swift keeps only what it must
func userNotificationCenter(_ c: UNUserNotificationCenter,
                            willPresent n: UNNotification,
                            withCompletionHandler h: @escaping (UNNotificationPresentationOptions) -> Void) {
    h([.banner, .sound, .list])
}

func userNotificationCenter(_ c: UNUserNotificationCenter,
                            didReceive r: UNNotificationResponse,
                            withCompletionHandler h: @escaping () -> Void) {
    // Route into the same deep-link path MainViewModel.onPushIntent already uses.
    h()
}
```

## 7. Acceptance

- [ ] `ReminderScheduler` replaces `AlarmScheduler`; `BuildReminderSpecsUseCase` is pure and shared
- [ ] `ReminderPlannerTest` covers **all** of: slot budget exhaustion · precedence order · every row of the expressibility table · **MONTHLY day-31 clamping** · **YEARLY Feb-29** · `until`-bounded rules · idempotence (`apply(x); apply(x)` → empty second diff) · horizon calculation
- [ ] `AlarmRequestCodesTest` and `TaskAlarmLifecycleTest` pass **unchanged**
- [ ] Android behaviour is bit-identical: same request codes, same fire times, same re-arm
- [ ] On device, iOS: `getPendingNotificationRequests().count <= 64` after any `apply`
- [ ] iOS: a daily routine reminder fires correctly for 3 consecutive days without opening the app
- [ ] iOS: a "31st monthly" routine fires on 28 February
- [ ] iOS: reminders survive a device restart
- [ ] Settings shows the live coverage row: **"Scheduled through 27 Aug · 4 later reminders will be set the next time you open DoneBot"**, driven by `coverageHorizon` + `dropped.size`, localized EN + TR
- [ ] Settings explains Time Sensitive notifications with a button to `UIApplication.openNotificationSettingsURLString`
- [ ] Tapping a notification opens the right task on both platforms

## 8. Pitfalls

- **`.critical` will not be granted.** Do not design around it. `.timeSensitive` is the ceiling.
- **Never clear-and-re-add.** It opens a window with nothing armed. Diff.
- **The `slot` column must stay stable.** It seeds the request code; deriving it from a list index orphans an armed `PendingIntent` on every mid-list delete. Already documented in `CLAUDE.md`.
- **Group and personal ids collide.** Server ids and local Room ids both start at 1. The `t.`/`g.` prefix is the string equivalent of the `0x4000_0000` range and is load-bearing.
- **MONTHLY day-29..31 and YEARLY Feb-29 must be materialized.** A repeating trigger simply does not fire in the missing month, which silently drops reminders for months at a time.
- **`UNCalendarNotificationTrigger(repeats: true)` has no end date.** Any rule with `until` must be materialized or it fires forever.
- **Implement in Kotlin/Native, not Swift.** Diffing logic in Swift is untestable from `commonTest`.
- **Do not rely on `BGAppRefreshTask`.** It is one of five triggers and the least reliable. Foreground reconcile is the primary path.
- **Weekly costs `byDay.size` slots, not 1.** A 5-day weekly routine with 2 reminder times is 10 slots. The budget must account for it or a handful of routines exhaust the pool.
- **Test with the clock, not with waiting.** `ReminderPlanner` is pure and takes `now` — that is what makes February testable in August.

## 9. Verification

```bash
# 1. The pure planner — the real gate
./gradlew :shared:domain:testDebugUnitTest --tests '*ReminderPlanner*'

# 2. Android behaviour unchanged
./gradlew :app:testDebugUnitTest --tests '*AlarmRequestCodes*' --tests '*TaskAlarmLifecycle*'

# 3. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 4. On a real iPhone — the simulator's notification behaviour is not representative
#    create 5 daily routines with 2 reminders each  → 10 repeating slots
#    create 20 one-off dated tasks                  → window fills, sentinel appears
#    check Settings: "Scheduled through <date>" and the dropped count are correct
#    background the app 3 days               → daily reminders still fire
#    restart the device                      → reminders survive
#    create a "31st monthly" routine, set the device clock to late Feb → fires on the 28th
#    call apply() twice                      → second produces no change
```
