---
id: 60-01
title: WidgetKit widgets
layer: ios-native
status: TODO
depends_on: [10-03, 30-04]
blocks: []
parallel_safe: true
estimate: 30h
reversible: true
owner_files:
  - iosApp/DoneBotWidget/**
  - shared/data/src/iosMain/**/widget/**
verify:
  - "xcodebuild -scheme DoneBotWidgetExtension build"
---

## 1. Goal

Home Screen and Lock Screen widgets showing today's tasks, the health-points hearts, and pomodoro state. **Android has no widgets at all**, so this is net-new capability and a genuine iOS differentiator.

## 2. Why this way

**Widgets cannot run Compose.** The extension runs in a separate process with a restricted runtime — this is SwiftUI, and there is no alternative. That is acceptable because widget UI is small and mostly text and shapes.

**The widget must not open the Room database.** It is a separate process with its own memory limit (a few tens of MB) and a short execution budget. Opening a 15-table database and running queries there is slow, fragile, and risks contention with the main app.

**Instead, the app writes a small JSON snapshot to a shared App Group container on every relevant mutation.** The widget reads that file. It is a few kilobytes, it cannot fail in interesting ways, and the timeline provider becomes trivial. This is the single most important design decision in this task.

**The health-points hearts are the most distinctive widget.** `TDHealthBar`'s half-heart model is unusual and instantly recognisable, and it is the kind of thing people put on a Lock Screen. It also shares the `ActivityAttributes` infrastructure built in `30-04`.

**This is the first thing to cut if the schedule slips** (decision D-05).

## 3. Source — read before writing

| Path | Why |
|---|---|
| `iosApp/DoneBotWidget/` | The stub target from `10-03` |
| `iosApp/DoneBotWidget/PomodoroAttributes.swift` | From `30-04` — shared, not duplicated |
| `domain/usecase/ComputeHealthPointsUseCase` + `HealthPointsCalculator` | The hearts model: 12 hearts in half-heart units |
| `common/HeartsFormat.kt` | The formatting rules |
| `ui/profile/ProfileHealthBadge` | The visual reference |
| `uikit/…/components/TDHealthBar.kt` (176 LOC) | The Compose rendering to mirror in SwiftUI |
| `data/repository/TaskRepositoryImpl.kt` | Every mutation site that must refresh the snapshot |
| `docs/screenshots/home/`, `activity/` | Visual references |

## 4. Target

```
iosApp/DoneBotWidget/
├── DoneBotWidgetBundle.swift
├── TodayTasksWidget.swift          small + medium
├── HealthWidget.swift              small + Lock Screen circular/rectangular
├── PomodoroWidget.swift            + the Live Activity from 30-04
├── WidgetSnapshot.swift            the shared model
└── WidgetTheme.swift               token subset, SwiftUI
shared/data/src/iosMain/…/widget/WidgetSnapshotWriter.kt
```

## 5. Steps

1. **Define the snapshot model** — a small, stable JSON contract between app and widget. Keep it minimal: what the widget renders, nothing more.

2. **Write the snapshot on every relevant mutation:** task create/update/delete/complete, health-points recompute, pomodoro state change, sync completion. Then call `WidgetCenter.shared.reloadAllTimelines()`.

3. **Build the timeline provider** to read the snapshot file. Refresh cadence: hourly, plus explicit reloads from the app. Do not request aggressive refreshes — the system throttles them anyway.

4. **Build three widgets:**
   - *Today* — small (count + next task) and medium (up to 4 tasks)
   - *Health* — small, plus Lock Screen circular and rectangular
   - *Pomodoro* — current session, sharing `30-04`'s attributes

5. **Port a token subset to SwiftUI.** Widgets cannot read `TDTheme`. Hand-port the handful of colours and the type ramp needed. **Keep it minimal and document that it is a mirror** — a full parallel design system is not the goal.

6. **Support light and dark**, and the widget-rendering modes (`.accented`, `.fullColor`).

7. **Deep link into the app** via `widgetURL` using the same `todoapp://` scheme `30-13` handles.

8. **Handle the empty state.** No tasks, or not logged in, must render something sensible — not a blank rectangle.

## 6. Code skeleton

```swift
// iosApp/DoneBotWidget/WidgetSnapshot.swift
// A small JSON contract. The widget NEVER opens the Room database: a separate process
// with a few tens of MB and a short budget is the wrong place for a 15-table database.
struct WidgetSnapshot: Codable {
    struct Task: Codable { let id: Int64; let title: String; let time: String?; let isDone: Bool }
    let tasks: [Task]
    let halfHearts: Int          // 0...24, twelve hearts in half units
    let pomodoroEndsAt: Date?
    let generatedAt: Date
}
```

```kotlin
// shared/data/iosMain/…/widget/WidgetSnapshotWriter.kt
class WidgetSnapshotWriter(private val appGroupId: String = "group.com.todoapp.mobile") {
    fun write(snapshot: WidgetSnapshot) {
        val url = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier(appGroupId)
            ?.URLByAppendingPathComponent("widget-snapshot.json") ?: return
        // … write JSON atomically, then:
        WidgetCenter.sharedCenter.reloadAllTimelines()
    }
}
```

## 7. Acceptance

- [ ] The widget extension builds and the widgets appear in the gallery
- [ ] Today widget shows today's tasks in small and medium
- [ ] Health widget shows the correct half-heart count, including Lock Screen forms
- [ ] Pomodoro widget shows the current session
- [ ] The snapshot updates on every relevant mutation and the widget refreshes
- [ ] **The widget never opens the Room database**
- [ ] Light and dark, plus `.accented` rendering
- [ ] Tapping a widget deep-links into the right screen
- [ ] Empty and logged-out states render sensibly
- [ ] The App Group id matches across app and extension entitlements
- [ ] Memory stays within the extension budget
- [ ] Hearts match `ProfileHealthBadge` exactly — same count, same half-heart rounding

## 8. Pitfalls

- **Do not open the database from the widget.** Separate process, tiny memory budget, short deadline, contention risk.
- **App Group id must match everywhere.** A mismatch means the widget silently reads an empty container with no error.
- **Widgets cannot run Compose.** Do not attempt it.
- **Timeline refreshes are throttled.** Requesting aggressive refreshes wastes budget; drive updates from the app with `reloadAllTimelines()`.
- **Lock Screen widgets are monochrome-tinted.** Colour-dependent designs disappear there — the hearts need a shape that reads without colour.
- **Half-heart rounding must match `HeartsFormat`.** A widget showing a different count than the app is a correctness bug, not a rendering one.
- **Write the snapshot atomically.** A partial write read mid-update produces a broken widget.
- **The token mirror is a mirror.** Document it as such so nobody grows it into a second design system.
- **Test on hardware.** Widget rendering and memory limits differ from the simulator.

## 9. Verification

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme DoneBotWidgetExtension \
  -destination 'platform=iOS Simulator,name=iPhone 17' build

# On a real iPhone
#   add each widget in every size
#   create/complete a task → widget updates
#   complete a task → hearts change and match the app exactly
#   start a pomodoro → widget shows the session
#   Lock Screen widgets in circular and rectangular
#   light and dark
#   tap each widget → correct screen
#   log out → sensible empty state
```
