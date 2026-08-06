---
id: 40-pomodoro-02
title: Add pomodoro timer
layer: ui
status: TODO
depends_on: [40-pomodoro-01, 50-02]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/addpomodorotimer/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Create a custom timer preset — focus length, break length, session count.

## 2. Why this way

**The wheel time picker is the gesture surface most likely to feel wrong on iOS.** `TDWheelTimePicker` (325 LOC) uses LazyList snapping with custom `LazyListState` extensions and a bespoke fling; iOS scroll physics differ enough that a wheel tuned on Android can drift past a value or fail to settle. `50-02` verifies the component; this is where it is used in anger.

Presets are stored in the local-only `pomodoro` table — no backend, so no sync concerns.

## 3. Source

| Path | LOC |
|---|---|
| `ui/addpomodorotimer/` (3 files) | 598 |
| `uikit/…/TDWheelTimePicker.kt` | 325 — the snapping |
| `data/model/entity/PomodoroEntity.kt` | local-only |
| `docs/screenshots/addpomodorotimer/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/addpomodorotimer/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. **Verify the wheel picker snaps cleanly on iOS** — no drift, no overshoot, settles on a value.
3. Verify presets save and appear in the launcher.
4. Verify validation — zero-length, absurd values.
5. Verify editing and deleting a preset.
6. Verify `TDTopBar`.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] **Wheel picker snaps cleanly on iOS**
- [ ] Presets save and appear in the launcher
- [ ] Validation prevents invalid durations
- [ ] Edit and delete work
- [ ] `TDTopBar` correct
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, filled, editing

## 8. Pitfalls

- **iOS fling physics differ.** If the wheel drifts, fix the snapping in shared code with a comment — not per platform.
- **Presets are local-only.** No sync, and they are lost on reinstall.
- **Validation must be real.** A zero-length focus session breaks the engine.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: flick the wheel hard and slowly, confirm it settles on a value;
# save a preset, use it, edit it, delete it
```
