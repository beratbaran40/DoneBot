---
id: 40-pomodoro-03
title: Pomodoro summary
layer: ui
status: TODO
depends_on: [40-pomodoro-01]
blocks: []
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/pomodorosummary/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The end-of-session summary — focus sessions, focus minutes, break minutes.

## 2. Why this way

A small screen whose route carries three primitive arguments (`focusSessions`, `totalFocusMinutes`, `totalBreakMinutes`), which makes it another check that type-safe navigation survived.

**It is reached on session completion**, including when the app was backgrounded during the session — so the arguments must survive the app being brought forward from a notification tap, not just an in-app transition.

## 3. Source

| Path | LOC |
|---|---|
| `ui/pomodorosummary/` (3 files) | 376 |
| `navigation/Screen.kt` | `PomodoroSummary(focusSessions, totalFocusMinutes, totalBreakMinutes)` |
| `uikit/…/TDStatisticCard.kt` (282) | the count-up animation |

## 4. Target

`shared/ui/commonMain/…/ui/pomodorosummary/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify all three arguments arrive correctly.
3. Verify it is reached on completion, including from a backgrounded session.
4. Verify the statistic count-up animates and honours Reduce Motion.
5. Verify minute formatting is localized.
6. Verify the dismiss path.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] All three route arguments correct
- [ ] Reached on completion, including from a backgrounded session
- [ ] Count-up animates; Reduce Motion honoured
- [ ] Minute formatting localized
- [ ] Dismiss returns correctly
- [ ] Three kits, two themes, two languages
- [ ] Previews cover zero, typical and long sessions

## 8. Pitfalls

- **A session completing in the background must still reach this screen** when the user opens the app or taps the notification.
- **Reduce Motion applies to the count-up.**
- **`Screen.PomodoroSummary` is R8-name-sensitive.**
- **Minute formatting is localized**, not string concatenation.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: complete a session in-app; complete one with the app backgrounded
# and tap the notification; enable Reduce Motion; EN + TR
```
