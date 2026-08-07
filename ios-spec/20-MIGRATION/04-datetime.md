---
id: 20-04
title: `java.time` → `kotlinx-datetime`
layer: domain
status: TODO
depends_on: [20-03b]
blocks: [20-11]
parallel_safe: false
estimate: 70h
reversible: false
owner_files:
  - shared/domain/src/**
  - shared/core/src/**
  - app/src/main/java/com/todoapp/mobile/**
  - uikit/src/main/java/com/todoapp/uikit/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "! grep -rqE '^import java\\.time' shared/domain/src/commonMain shared/core/src/commonMain"
---

## 1. Goal

Replace `java.time` with `kotlinx-datetime` across the codebase — **93 files** — and introduce a platform contract for the one thing `kotlinx-datetime` cannot do: locale-aware month and weekday names.

## 2. Why this way

`java.time` does not exist in `commonMain`. It is the single largest blocker to domain purity, and it is spread through every layer.

Measured distribution:

| Layer | Files | | Type | Uses |
|---|---|---|---|---|
| `ui/` | 52 | | `LocalDate` | 62 |
| `domain/` | 16 | | `LocalTime` | 37 |
| `uikit/` | 13 | | `DateTimeFormatter` | 19 |
| `data/` | 8 | | `DayOfWeek` | 17 |
| `common/` | 3 | | `ZoneId` | 15 |
| `di/` | 1 | | `YearMonth` | 15 |
| | | | `LocalDateTime` | 11 |
| | | | `Instant` | 10 |
| | | | `Clock` | 10 |
| | | | `TextStyle` | 7 |
| | | | `ChronoUnit` | 6 |

**Most of this is mechanical. One part is not.** `DateTimeFormatter` (19 files) and `TextStyle` (7 files) provide *localized* month and weekday names. `kotlinx-datetime` has no equivalent — it formats, but it does not localize. That gap needs a platform contract, and it is the only genuinely new design in this task.

**Do it per-layer with a green build between each:** domain → core → data → uikit → ui. A 93-file sweep in one commit is unbisectable.

**The regression shields are the point.** Seven test files encode date arithmetic that must not change. Treat any semantic difference as a bug in the migration, never as a test that needs updating.

**That is also why this task depends on `20-03b` rather than `20-03`.** The shields have to be running in `:shared:domain`'s `commonTest` before the sweep starts — otherwise this task's own `verify:` line (`:shared:domain:testDebugUnitTest --tests '*Recurrence*'`) matches nothing and passes without testing anything, on the largest semantic-risk sweep in the migration.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `shared/domain/…/model/Recurrence.kt` | `firesOn` + `clampedDayOfMonth`. **Port this first.** It is the shared predicate behind the task list *and* both platforms' schedulers. |
| `shared/domain/…/alarm/AlarmScheduler.kt` | `LocalDate` in the interface signature |
| `app/…/data/alarm/AlarmSchedulerImpl.kt` | `computeNextFire` walks up to 400 days; month-end clamping lives here too |
| `app/…/ui/home/HomeViewModel.kt` | Injected `Clock` (the fake-clock seam the tests rely on) + `YearMonth` |
| `app/…/ui/calendar/CalendarViewModel.kt` | `YearMonth`, week maths |
| `uikit/…/components/TDMonthNavigator.kt`, `TDMonthlyDatePicker.kt`, `TDDatePicker.kt` | `YearMonth` + localized month names |
| `uikit/…/util/TimeFormat.kt` | `DateFormat.is24HourFormat` — Android-only, needs a contract |
| `shared/core/…/TimeFormat.kt`, `DayModeCalculator.kt` | Cross-cutting time helpers |
| Tests: `RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest`, `CalendarGridTest`, `DayTapOutcomeTest`, `SubtaskTest`, `HealthPointsCalculatorTest` | The shields |

## 4. Target

- `gradle/libs.versions.toml` — `kotlinx-datetime` **0.7.x**
- All 93 files, per-layer
- `shared/domain/…/platform/PlatformFormatting.kt` *(new)* — the localization contract
- Android implementation of that contract in `:app` (moves to `:shared:data/androidMain` later)

## 5. Steps

1. **Pin `kotlinx-datetime` 0.7.x, not 0.6.x.** 0.6 has no `YearMonth`, and this codebase uses it in 15 places. On 0.6 you would hand-roll it — do not.

2. **Port `Recurrence.kt` first, alone, and run its three tests.**
   ```bash
   ./gradlew :shared:domain:testDebugUnitTest --tests '*Recurrence*'
   ```
   Do not touch anything else until they are green. This file is the highest-consequence date logic in the project.

3. **Map the types:**

   | `java.time` | `kotlinx-datetime` / stdlib |
   |---|---|
   | `LocalDate`, `LocalTime`, `LocalDateTime`, `DayOfWeek`, `Month`, `YearMonth` | same names in `kotlinx.datetime` |
   | `Instant` | `kotlin.time.Instant` (stdlib since Kotlin 2.1) |
   | `Clock` | `kotlin.time.Clock` — **keep it injected** |
   | `ZoneId` | `kotlinx.datetime.TimeZone` |
   | `ChronoUnit.DAYS.between(a, b)` | `a.daysUntil(b)` |
   | `ChronoUnit.MONTHS.between(a, b)` | `a.periodUntil(b).months` (+ years) |
   | `Duration` | `kotlin.time.Duration` |
   | `DateTimeFormatter` (fixed patterns) | `LocalDate.Format { … }` builders |
   | `DateTimeFormatter` (localized) / `TextStyle` | **`PlatformFormatting` contract — see step 5** |

4. **Keep `Clock` injected.** It is a deliberate test seam: `HealthPointsCalculatorTest` and `TaskAlarmLifecycleTest` drive it with a fake. Replacing injection with `Clock.System` breaks both and removes the ability to test date-boundary behaviour.

5. **Introduce the localization contract.** `kotlinx-datetime` cannot produce "Ağustos" or "Pzt". Define:

   ```kotlin
   // shared/domain/…/platform/PlatformFormatting.kt
   enum class NameStyle { FULL, SHORT, NARROW }

   expect fun monthName(month: Month, style: NameStyle, locale: AppLocale): String
   expect fun dayOfWeekName(day: DayOfWeek, style: NameStyle, locale: AppLocale): String
   expect fun uses24HourClock(): Boolean
   ```

   Android implements with `java.time.format.TextStyle` + `java.util.Locale` and `DateFormat.is24HourFormat`. iOS implements with `NSDateFormatter.monthSymbols` / `shortWeekdaySymbols` and an `NSLocale` 12/24-hour probe. Full spec: `30-PLATFORM/14`.

   > Since only `androidTarget()` exists right now, `expect`/`actual` compiles with a single actual. That is fine and is the ratchet working as intended.

6. **Sweep per layer**, gate green between each: domain → core → data → uikit → ui.

7. **Watch the week-start convention.** `CalendarGrid.kt` builds a **Monday-first** 6×7 grid. `kotlinx.datetime.DayOfWeek` has the same ISO ordinals as `java.time`, so this should carry over — but `CalendarGridTest` is the proof, not the assumption.

8. **Run the full gate**, then re-run every shield test individually and compare output to the pre-migration run.

## 6. Code skeleton

```kotlin
// Before
import java.time.LocalDate
import java.time.temporal.ChronoUnit
val days = ChronoUnit.DAYS.between(anchor, target)

// After
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
val days = anchor.daysUntil(target)
```

```kotlin
// Before — localized month name
import java.time.format.TextStyle
month.getDisplayName(TextStyle.FULL, Locale.getDefault())

// After — via the platform contract
monthName(month, NameStyle.FULL, currentLocale)
```

```kotlin
// Before — fixed pattern
DateTimeFormatter.ofPattern("HH:mm").format(time)

// After
val hhmm = LocalTime.Format { hour(); char(':'); minute() }
hhmm.format(time)
```

```kotlin
// Clock stays injected — this is a test seam, not an implementation detail.
class HomeViewModel @Inject constructor(
    private val clock: Clock,          // kotlin.time.Clock
    private val timeZone: TimeZone,
) {
    private fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date
}
```

## 7. Acceptance

- [ ] `! grep -rqE '^import java\.time' shared/domain/src/commonMain shared/core/src/commonMain`
- [ ] No `java.time` import anywhere in `:app` or `:uikit`
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] All seven shield tests pass **with unchanged assertions**
- [ ] `Clock` is still injected everywhere it was before — no `Clock.System` call sites introduced
- [ ] `PlatformFormatting` exists with an Android actual; month and weekday names still render correctly in **both EN and TR**
- [ ] Manual: calendar month navigation, week strip, 12h/24h time display, and a recurring task's next-fire date all behave as before
- [ ] `:app:bundleRelease` recorded in the ledger

## 8. Pitfalls

- **`YearMonth` requires 0.7.x.** Choosing 0.6 means hand-rolling it in 15 places. Check the version before writing a line.
- **`kotlinx-datetime` does not localize.** Any attempt to produce month or weekday names without the platform contract will silently emit English on a Turkish device. This is the single most likely user-visible regression in this task — TR is half the user base.
- **Do not replace injected `Clock` with `Clock.System`.** It reads as a simplification and destroys two test suites plus the ability to test date boundaries.
- **`Instant` moved to the stdlib.** `kotlin.time.Instant`, not `kotlinx.datetime.Instant`, on current versions. Mixing the two produces confusing type errors.
- **Month-end clamping is real behaviour, not a rounding detail.** `clampedDayOfMonth` makes a "31st of every month" rule fire on 28 February. `30-PLATFORM/01` depends on this being preserved exactly — iOS has to work around it, which it cannot do if the semantics drift here.
- **Epoch-day and epoch-milli fields must not shift.** `date`, `finishedOn`, `recurrenceUntil` are epoch **days**; `createdAt`, `dueDate`, `timestamp` are epoch **millis**. An off-by-one from a timezone-aware conversion where the original was timezone-naive corrupts stored data.
- **Time zone must be explicit.** `java.time.LocalDate.now()` uses the system zone implicitly; `kotlinx-datetime` forces you to pass one. Pass the *injected* zone, not `TimeZone.currentSystemDefault()` inline, or you lose testability the same way `Clock.System` would.
- **Do not batch the sweep.** Per-layer commits with a green gate between each. 93 files in one commit cannot be bisected.

## 9. Verification

```bash
# 1. No java.time anywhere
grep -rn "^import java\.time" --include="*.kt" app/src uikit/src shared/ && echo "STILL PRESENT" || echo "clean"

# 2. Shields, individually
./gradlew :shared:domain:testDebugUnitTest --tests '*Recurrence*'
./gradlew :app:testDebugUnitTest --tests '*CalendarGrid*' --tests '*DayTapOutcome*' \
                                 --tests '*HealthPoints*' --tests '*Subtask*'

# 3. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 4. Manual, on a device, in BOTH languages
#    Calendar: navigate months → correct localized month names
#    Home: week strip → correct localized weekday abbreviations
#    Settings → Language → Turkish → month/weekday names switch
#    A 12h-locale device shows 12h times; a 24h device shows 24h
#    Create a "31st monthly" routine → next fire in February is the 28th
```
