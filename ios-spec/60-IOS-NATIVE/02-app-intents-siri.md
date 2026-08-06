---
id: 60-02
title: App Intents & Siri
layer: ios-native
status: TODO
depends_on: [10-03]
blocks: []
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - iosApp/DoneBotIntents/**
  - shared/data/src/iosMain/**/intents/**
verify:
  - "xcodebuild -scheme DoneBotIntents build"
---

## 1. Goal

Expose DoneBot's core actions to Siri, Shortcuts and Spotlight through App Intents: create a task, complete a task, start a pomodoro, check today.

## 2. Why this way

**This is the native counterpart to DoneBot chat.** The app already has an on-device intent classifier (`LocalIntentClassifier`, 373 LOC) handling exactly these actions — today, overdue, weekly, greeting, pomodoro start/stop/status — without a backend round trip. App Intents surface the same capabilities through the system, which is what iOS users reach for.

**Pomodoro is the strongest case.** `CLAUDE.md` is explicit that pomodoro must stay client-side because `PomodoroEngine` state lives on the device — which makes it a perfect App Intent: no network, instant, and it pairs with the Live Activity from `30-04`.

**Reuse the use cases, do not reimplement them.** An intent that creates a task through its own path will drift from the app's: wrong `syncStatus`, missing `clientTaskId`, no alarm scheduling. Intents call the same shared code the UI calls.

**Intents run in a separate process with a short budget.** Keep each one small: one operation, one result. Anything requiring a long sync should open the app instead.

**This is the second thing to cut if the schedule slips** (decision D-05), after widgets.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `data/ai/LocalIntentClassifier.kt` (373 LOC) | The intent vocabulary already defined for chat — mirror it |
| `DONEBOT_CAPABILITIES.md` | The full capability catalogue |
| `domain/usecase/SetTaskCompletionUseCase.kt` | Completion must go through this — per-day for recurring tasks |
| `domain/engine/PomodoroEngine.kt` | Start/stop/status |
| `data/repository/TaskRepositoryImpl.kt` | Task creation: `clientTaskId`, `syncStatus`, reminder scheduling |
| `ios-spec/30-PLATFORM/04-live-activity-pomodoro.md` | The Live Activity that a started pomodoro shows |

## 4. Target

```
iosApp/DoneBotIntents/
├── CreateTaskIntent.swift
├── CompleteTaskIntent.swift
├── StartPomodoroIntent.swift
├── TodayTasksIntent.swift
└── DoneBotShortcuts.swift        AppShortcutsProvider — the phrases
shared/data/src/iosMain/…/intents/IntentBridge.kt
```

## 5. Steps

1. **Build the Kotlin bridge first.** Each intent maps to one shared use case; the bridge is the only place Swift touches app logic.

2. **`CreateTaskIntent`** — title, optional date, optional time. Must produce the same row the UI produces: `clientTaskId` set, `syncStatus = PENDING_CREATE`, reminders scheduled.

3. **`CompleteTaskIntent`** — takes an entity query so Siri can disambiguate. **Must go through `SetTaskCompletionUseCase`**: completion for a recurring task is per-day, recorded in `task_daily_completions`, not a boolean on the row.

4. **`StartPomodoroIntent`** — no parameters for the default timer. Starts the engine and the Live Activity.

5. **`TodayTasksIntent`** — returns a spoken and displayed summary. Reuse the phrasing `LocalIntentClassifier` already produces so Siri and chat agree.

6. **Register `AppShortcuts` with phrases in EN and TR.** Phrases must include the app name — Apple requires it.

7. **Add `EntityQuery` for tasks** so "complete *the report*" resolves. Scope it to incomplete tasks in a sensible window; querying everything is slow and ambiguous.

8. **Donate intents** after in-app actions so Siri learns and Spotlight surfaces them.

9. **Never expose group-task writes.** Chat blocks them deliberately (`group_task_blocked`); intents must match, or the audit story diverges.

10. **Handle the logged-out and guest cases.** Guests have local tasks, so creation should still work; anything needing the backend should say so.

## 6. Code skeleton

```swift
// iosApp/DoneBotIntents/CompleteTaskIntent.swift
struct CompleteTaskIntent: AppIntent {
    static var title: LocalizedStringResource = "Complete a task"
    @Parameter(title: "Task") var task: TaskEntity

    func perform() async throws -> some IntentResult & ProvidesDialog {
        // Goes through SetTaskCompletionUseCase: completion for a recurring task is
        // per-day (task_daily_completions), never a boolean on the row.
        try await IntentBridge.shared.completeTask(id: task.id)
        return .result(dialog: "Done.")
    }
}
```

```swift
// Phrases must include the app name — Apple requires it. Provide EN and TR.
struct DoneBotShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(intent: StartPomodoroIntent(),
                    phrases: ["Start a pomodoro in \(.applicationName)",
                              "\(.applicationName) ile pomodoro başlat"],
                    shortTitle: "Start Pomodoro", systemImageName: "timer")
    }
}
```

## 7. Acceptance

- [ ] The intents extension builds; all four appear in Shortcuts
- [ ] "Hey Siri, start a pomodoro in DoneBot" works and shows the Live Activity
- [ ] Creating a task via intent produces a row **identical** to one created in the UI — `clientTaskId`, `syncStatus`, reminders
- [ ] Completing via intent goes through `SetTaskCompletionUseCase`; a recurring task completes **for that day only**
- [ ] Today's summary matches what chat produces
- [ ] Entity query resolves tasks by title; Siri disambiguates sensibly
- [ ] Phrases work in EN **and** TR
- [ ] Intents appear in Spotlight after donation
- [ ] **Group-task writes are blocked**, matching chat
- [ ] Guest mode: local creation works; backend-dependent intents explain themselves
- [ ] Each intent completes within its execution budget

## 8. Pitfalls

- **Do not reimplement task creation.** Bypassing `TaskRepositoryImpl` produces rows with no `clientTaskId` (duplicates on retry), the wrong `syncStatus` (never synced), and no scheduled reminders.
- **Recurring completion is per-day.** Setting a boolean is wrong and corrupts routine tracking.
- **Group-task writes must stay blocked.** Chat blocks them for a documented audit reason; a Siri backdoor would defeat it.
- **Phrases must include the app name.** Apple rejects shortcuts without it.
- **Extensions have a short budget.** A long sync belongs in the app, not an intent.
- **Entity queries must be scoped.** Querying every task is slow and produces poor disambiguation.
- **Do not surface internal numeric ids.** `CLAUDE.md` is explicit: the app shows no ids anywhere. Confirm by title and date.
- **Test with Siri, not only Shortcuts.** Voice recognition of the phrases is a separate failure mode, especially in Turkish.

## 9. Verification

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme DoneBotIntents \
  -destination 'platform=iOS Simulator,name=iPhone 17' build

# On a real iPhone
#   Shortcuts app → all four intents present and runnable
#   "Hey Siri, start a pomodoro in DoneBot" → starts, Live Activity appears
#   create a task by voice → verify the row matches a UI-created one
#   complete a recurring task by voice → only that day is marked
#   Turkish phrases with the device in Turkish
#   attempt a group task → blocked
#   Spotlight search after donation
```
