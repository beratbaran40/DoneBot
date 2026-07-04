# Analytics event taxonomy (§7.17)

Minimal product-behavior taxonomy so we can answer funnel / activation / retention questions after launch.
All events go through `AnalyticsHelper` (`domain/analytics/`), implemented by `FirebaseAnalyticsHelper`
(`data/analytics/`). **Never call `FirebaseAnalytics.logEvent` directly** — add a method to `AnalyticsHelper`
so every event stays in one auditable place.

## Consent / opt-out

Collection is gated by the **§7.3 opt-out** (`CrashAnalyticsPreferences`, default ON). When the user turns
"Share usage & crash data" off, `Application` calls `FirebaseAnalytics.setAnalyticsCollectionEnabled(false)`
and the SDK drops every event below natively — so `AnalyticsHelper` methods carry no per-call guard.
Analytics is also always off in debug builds and defaults off in the manifest until the collector runs.

Enabling analytics means the Play **Data Safety** form must declare *App activity → App interactions*.

## Events

| Event | Params (type) | Fired from | Meaning |
|---|---|---|---|
| `sign_up` | — | `RegisterViewModel.handleSuccessfulRegister` | Account created (email or Google) |
| `login` | — | `LoginViewModel.handleSuccessfulLogin` | Returning sign-in (email or Google) |
| `task_created` | `has_due` (long 1/0), `recurrence` (string enum) | `TaskRepositoryImpl.insert` / `insertWithPhotos` | A personal task was created. Activation = first one |
| `task_completed` | — | `SetTaskCompletionUseCase` (on complete only) | A task instance was marked done |
| `pomodoro_completed` | `duration` (long, minutes) | `PomodoroViewModel.onSessionFinished` (focus) | A focus session ran to completion |
| `chat_message_sent` | `local_intent` (long 1/0), `refused` (long 1/0), `round_trips` (long) | `ChatViewModel` (local-intent + server paths) | A DoneBot message was sent |
| `group_created` | — | `CreateNewGroupViewModel.createGroup` (success) | A family/group was created |
| `screen_view` | `screen_name` (string, route) | `MainViewModel.updateCurrentRoute` | Navigation to a destination |

`sign_up` / `login` / `screen_view` use the Firebase reserved event + param names
(`FirebaseAnalytics.Event.*`, `Param.SCREEN_NAME`); the rest are custom.

## Caveats (read before analysing)

- **`sign_up` vs `login` on Google is by screen intent, not truth.** The backend returns no `isNewUser`
  flag, so a first-time Google user on the login screen logs `login`, not `sign_up`. Treat the split as
  "which screen they used", not "new vs returning".
- **`task_created` is personal tasks only.** Group tasks go through a different path
  (`groupRepository.createGroupTask`) and are intentionally excluded here; `group_created` covers groups.
- **`has_due` = `!isAllDay`** — the closest signal for "has a concrete due time". An all-day task logs
  `has_due = 0`.
- **`task_completed` can over-count.** A non-recurring toggle done→undone→done logs twice, and recurring
  tasks log once per day-instance. It measures completion *actions*, not distinct tasks.
- **`chat_message_sent` skips the guest-blocked and hard-failure paths on purpose** — only actually-sent
  messages are logged. `round_trips` is 0 for local-intent replies (no backend call).
