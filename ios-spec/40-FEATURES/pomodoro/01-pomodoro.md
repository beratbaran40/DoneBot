---
id: 40-pomodoro-01
title: Pomodoro
layer: ui
status: TODO
depends_on: [40-core-01, 30-04, 30-05]
blocks: [40-pomodoro-03, 40-misc-03]
parallel_safe: false
estimate: 16h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/pomodoro/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The timer screen — portrait and landscape, ring, session dots, controls, finish-early dialog, ambience sheet and the three animated ambience scenes.

## 2. Why this way

**This is the one feature where iOS is straightforwardly better.** Android fought background ambience and eventually withdrew it (`ede5f8c fix(pomodoro): stop offering background ambience the app cannot deliver`). On iOS, `UIBackgroundModes: audio` plus an `AVAudioSession` makes it a supported capability. And `Text(timerInterval:)` in a Live Activity counts down with no app wakeups at all, which is a better experience than the foreground-service notification.

**The engine stays authoritative and shared.** `PomodoroEngine` owns the state on both platforms; the Live Activity and the banner are sinks. That is also what lets chat control the timer offline.

**One recorded trap:** a singleton engine plus a screen that resets on entry clash — the ViewModel's `init` must gate on `!isRunning`, or opening the screen kills a running session.

**Landscape needs a scrollable container.** A recorded lesson: tall UI clips in landscape unless it scrolls.

## 3. Source

| Path | LOC |
|---|---|
| `ui/pomodoro/` (20 files) | 2,737 |
| `ui/pomodoro/PomodoroTimerRing.kt` | 141 — pure Canvas |
| `ui/pomodoro/ambience/` | `FireplaceScene` 209, `RainScene` 157, `HandpanScene` 125 — pure Canvas |
| `domain/engine/PomodoroEngine.kt`, `data/engine/PomodoroEngineImpl.kt` (290) | the state machine |
| `thesis/figures/fig12_pomodoro_state_machine.mmd` | the diagram |

## 4. Target

`shared/ui/commonMain/…/ui/pomodoro/` — verification.

## 5. Steps

1. Verify all 20 files compile in `commonMain`.
2. Verify the ring and session dots render identically.
3. Verify the three ambience scenes animate correctly.
4. **Verify entering the screen does not reset a running session.**
5. Verify start, pause, resume, skip and finish-early.
6. Verify the Live Activity appears and counts down with the app killed (`30-04`).
7. Verify ambience plays in the background and mixes with other audio (`30-05`).
8. Verify landscape scrolls rather than clipping.
9. Verify chat can start and stop the timer.
10. Verify the summary is reached on completion.
11. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 20 files compile in `commonMain`
- [ ] Ring and dots render identically on both platforms
- [ ] Three ambience scenes animate correctly
- [ ] **Entering the screen does not reset a running session**
- [ ] All controls work
- [ ] Live Activity counts down with the app killed
- [ ] Ambience plays backgrounded and with the screen locked; mixes with other audio
- [ ] Landscape scrolls, nothing clipped
- [ ] Chat control works offline
- [ ] Summary reached on completion
- [ ] Three kits, two themes, two languages
- [ ] Previews cover idle, running, paused, break

## 8. Pitfalls

- **Gate the ViewModel `init` on `!isRunning`.** A singleton engine plus a resetting screen kills live sessions.
- **Landscape clips without a scroll container.**
- **`PomodoroEngineImpl` holds a singleton `CoroutineScope`** and needs its `shutdown()` path — the reference case in `CLAUDE.md`.
- **Ambience must mix with other audio.** Killing the user's music is a one-star review for a focus app.
- **The engine is authoritative.** Do not let the Live Activity hold its own notion of remaining time.
- **`.ogg` will not play on iOS** — `30-05` ships `.m4a`.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# iOS on hardware: start a session, kill the app (Live Activity still counts),
# play music (both audible), lock the screen (ambience continues), rotate to landscape,
# leave and re-enter the screen mid-session (not reset), start/stop via chat
```
