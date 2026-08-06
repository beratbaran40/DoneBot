---
id: 30-04
title: Live Activity — the Pomodoro session presenter
layer: platform
status: TODO
depends_on: [20-13, 10-03, 30-00]
blocks: [40-pomodoro-01, 60-01]
parallel_safe: true
estimate: 25h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/engine/**
  - shared/data/src/androidMain/**/notification/**
  - shared/data/src/iosMain/**/activity/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Replace `PomodoroForegroundService` on iOS with an ActivityKit Live Activity that shows a live countdown on the Lock Screen and in the Dynamic Island — without the app running.

## 2. Why this way

**`Text(timerInterval:)` is the whole trick.** Given a start and end date, iOS renders a self-updating countdown with **zero app wakeups**. That is a better fit for a Pomodoro timer than Android's foreground-service notification, which the app must actively update.

So the contract is not "keep the app alive" — it is "hand the system a snapshot and let it render." Updates are only needed on *state transitions* (pause, resume, skip, phase change), not per second.

**The engine stays shared and authoritative.** `PomodoroEngine` lives in `commonMain` and owns the session state on both platforms. The presenter is a dumb sink. This matters because DoneBot's chat can start and stop pomodoros, and `CLAUDE.md` is explicit that pomodoro must stay client-side for exactly this reason.

**Live Activity UI is Swift, and cannot be Compose.** Widget and Live Activity extensions run in a separate process with a restricted runtime; CMP cannot render there. That is fine — the Lock Screen presentation is small, and it shares an `ActivityAttributes` model with `60-01`'s widgets.

**Android is unchanged.** `PomodoroForegroundService` keeps its `specialUse|mediaPlayback` type and its live countdown notification.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `domain/engine/PomodoroEngine.kt` | — | The shared state machine — the source of truth on both platforms |
| `data/engine/PomodoroEngineImpl.kt` | 290 | The singleton `CoroutineScope` and its `shutdown()` path. `CLAUDE.md` calls this out as the reference example. |
| `data/notification/PomodoroForegroundService.kt` | 176 | `specialUse\|mediaPlayback`, `foregroundTypeMask()` |
| `data/notification/PomodoroNotificationBuilder.kt` | 147 | Live countdown + pause/skip actions — the feature set to match |
| `data/notification/PomodoroServiceController.kt` | 80 | start/stop bridging |
| `data/notification/PomodoroSessionAlarmScheduler.kt`, `PomodoroSessionEndReceiver.kt` | 56 / 48 | Session-end alarm |
| `ui/pomodoro/` | 2,737 | The screen; `PomodoroTimerRing.kt` (141) is pure Canvas and ports free |
| `ui/banner/` | 296 | The in-app floating banner — separate from the Live Activity |
| `thesis/figures/fig12_pomodoro_state_machine.mmd` | — | The state machine, already diagrammed |

## 4. Target

- `shared/domain/…/engine/OngoingSessionPresenter.kt` — the contract
- `shared/data/androidMain/…/ForegroundServicePresenter.kt` — wraps the existing service
- `shared/data/iosMain/…/LiveActivityPresenter.kt` — ActivityKit bridge
- `iosApp/DoneBotWidget/PomodoroActivity.swift` — the Live Activity UI (SwiftUI)
- `iosApp/DoneBotWidget/PomodoroAttributes.swift` — shared with `60-01`

## 5. Steps

1. **Define the contract** as start / update / stop over an immutable snapshot.

2. **Android: wrap the existing service.** No behaviour change.

3. **Define `ActivityAttributes`** with the static session identity plus a `ContentState` holding `endDate`, phase, session index and paused-ness.

4. **Build the Live Activity UI in SwiftUI** — Lock Screen presentation plus compact/minimal/expanded Dynamic Island. Use `Text(timerInterval:)` so the countdown runs without app updates.

5. **Update only on transitions**: pause, resume, skip, phase change, finish. Not on a timer.

6. **Handle the paused state.** `Text(timerInterval:)` always counts; when paused, switch to a static remaining-time label instead.

7. **Fall back gracefully.** Live Activities need iOS 16.1+ and can be disabled by the user. When unavailable, `capabilities.supportsLiveCountdown = false` and the app posts a single end-of-session notification instead. **Do not fail; degrade.**

8. **End the activity when the session ends**, with a short dismissal delay so the user sees the final state.

9. **Background audio is separate** — `30-05`. The Live Activity does not keep the app alive, and it does not need to.

## 6. Code skeleton

```kotlin
// shared/domain/…/engine/OngoingSessionPresenter.kt
data class SessionSnapshot(
    val phase: Phase,                 // FOCUS | SHORT_BREAK | LONG_BREAK
    val endsAt: Instant,
    val isPaused: Boolean,
    val remainingWhilePaused: Duration?,
    val sessionIndex: Int,
    val totalSessions: Int,
    val taskTitle: String?,
)

interface OngoingSessionPresenter {
    fun start(snapshot: SessionSnapshot)
    fun update(snapshot: SessionSnapshot)
    fun stop()
    val capabilities: SessionPresenterCapabilities
}

data class SessionPresenterCapabilities(
    val supportsLiveCountdown: Boolean,   // Android: always true. iOS: iOS 16.1+ and user-enabled.
    val supportsActions: Boolean,         // pause/skip from the presentation
)
```

```swift
// iosApp/DoneBotWidget/PomodoroActivity.swift
struct PomodoroAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var endsAt: Date
        var phase: String
        var isPaused: Bool
        var remainingWhilePaused: TimeInterval?
        var sessionIndex: Int
        var totalSessions: Int
    }
    var taskTitle: String?
}

struct PomodoroLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: PomodoroAttributes.self) { context in
            VStack {
                Text(context.attributes.taskTitle ?? "Focus")
                if context.state.isPaused, let remaining = context.state.remainingWhilePaused {
                    // Text(timerInterval:) always counts; a paused session needs a static label.
                    Text(formatted(remaining)).monospacedDigit()
                } else {
                    // Renders a live countdown with ZERO app wakeups — the whole point.
                    Text(timerInterval: Date()...context.state.endsAt, countsDown: true)
                        .monospacedDigit()
                }
            }
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.center) { /* … */ }
            } compactLeading: { Image(systemName: "timer") }
              compactTrailing: { Text(timerInterval: Date()...context.state.endsAt, countsDown: true) }
              minimal: { Image(systemName: "timer") }
        }
    }
}
```

## 7. Acceptance

- [ ] `OngoingSessionPresenter` in `:shared:domain`; both implementations registered
- [ ] Android behaviour unchanged — foreground service, live notification, pause/skip actions
- [ ] iOS: starting a pomodoro shows a Live Activity on the Lock Screen
- [ ] Countdown advances with the app **force-quit**
- [ ] Dynamic Island shows compact, minimal and expanded presentations correctly
- [ ] Pause shows a static remaining time, not a running countdown
- [ ] Skip and phase change update the presentation
- [ ] Session end dismisses the activity after a short delay
- [ ] With Live Activities **disabled** in Settings, the app still works and posts an end-of-session notification
- [ ] On a device below iOS 16.1, the fallback path is used and nothing crashes
- [ ] `PomodoroEngine` remains the single source of truth on both platforms
- [ ] DoneBot chat can still start/stop a pomodoro on iOS

## 8. Pitfalls

- **`Text(timerInterval:)` cannot be paused.** It always counts. A paused session must render a static label or it shows a wrong, still-decreasing time.
- **Live Activity updates are rate-limited.** Update on transitions only. A per-second update loop gets throttled and drains battery.
- **Live Activity UI cannot be Compose.** The extension runtime does not support it. This is SwiftUI, and that is not negotiable.
- **iOS 16.1+ and user-revocable.** Both need a graceful fallback — degrade, never fail.
- **The Live Activity does not keep the app alive.** Background audio (`30-05`) is what does, and only while audio is actually playing.
- **`ActivityAttributes` must be shared with the widget extension**, not duplicated. `60-01` uses the same model.
- **Do not move state into the presenter.** `PomodoroEngine` owns it. A presenter with its own notion of remaining time will disagree with the app.
- **`PomodoroEngineImpl`'s singleton scope needs its `shutdown()` path** — `CLAUDE.md` names it as the reference case for this anti-pattern.
- **Do not start a Live Activity from the background.** iOS requires the app to be foreground when it begins.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone (Dynamic Island needs iPhone 14 Pro or later)
#   start a pomodoro → Live Activity on the Lock Screen
#   force-quit the app → countdown still advances
#   pause → static remaining time; resume → counting again
#   skip → phase updates
#   let it finish → activity dismisses after a short delay
#   Settings → disable Live Activities → app still works, end-of-session notification arrives
#   ask DoneBot chat to start a pomodoro → works
```
