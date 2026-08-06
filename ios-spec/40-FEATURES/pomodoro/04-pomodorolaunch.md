---
id: 40-pomodoro-04
title: Pomodoro launcher
layer: ui
status: TODO
depends_on: [40-pomodoro-02]
blocks: []
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/pomodorolaunch/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Pick a timer preset and start a session.

## 2. Why this way

**A recorded UX rule applies directly here:** a "Başlat"-style CTA should auto-trigger the main action, not merely navigate to a screen where the user taps start again. The launcher exists to remove a step, so an implementation that adds one back defeats it.

It is also where a task can be attached to a session, which is what the Live Activity displays as its title.

## 3. Source

| Path | LOC |
|---|---|
| `ui/pomodorolaunch/` (3 files) | 293 |
| `domain/engine/PomodoroEngine.kt` | start |
| `docs/screenshots/pomodorolaunch/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/pomodorolaunch/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify presets list, including the default.
3. **Verify starting a session goes straight to a running timer**, not to an idle screen.
4. Verify attaching a task, and that its title appears in the Live Activity.
5. Verify the empty-preset case falls back to the default.
6. Verify starting while a session is already running behaves sensibly.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Presets list correctly
- [ ] **Start goes directly to a running timer**
- [ ] Attached task title appears in the Live Activity
- [ ] Empty-preset case falls back to the default
- [ ] Starting during a running session is handled
- [ ] Three kits, two themes, two languages
- [ ] Previews cover no presets, one, several

## 8. Pitfalls

- **Do not make the user tap start twice.** The launcher's whole point is removing that step.
- **Starting during a running session must not silently discard it.**
- **The attached task title is what the Live Activity shows.** An empty title makes the Lock Screen presentation look broken.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: pick a preset → running timer in one tap; attach a task and check
# the Live Activity title; start while one is running
```
