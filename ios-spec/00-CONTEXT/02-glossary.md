# Glossary

Domain vocabulary and project-specific terms. When a task file uses one of these words, it means exactly this.

---

## Product concepts

**Task** — the core entity. Personal tasks (`familyGroupId == null`) live in Room and sync bidirectionally. Group tasks are server-authoritative.

**Subtask / step** — child of a task. Its per-day completion state (`SubtaskDailyCompletionEntity`) is **local-only** — there is no endpoint.

**Routine** — a recurring task. Completion is **per-day**, recorded in `task_daily_completions`, never a boolean on the task row. `SetTaskCompletionUseCase` owns this.

**Staged task** — a task whose steps are ordered and completed in sequence, as opposed to a flat checklist.

**Recurrence** — `Recurrence` (NONE/DAILY/WEEKLY/MONTHLY/YEARLY) is a thin shim over `RecurrenceRule` (`interval`, `byDay`, `until`). It models UNTIL and **deliberately never COUNT** — "20 sessions" is resolved to a concrete end date at creation time so `firesOn` stays a pure `(anchor, day) -> Boolean`.

**Reminder slot** — a task may have up to `MAX_REMINDER_SLOTS = 8` reminder times. Slot 0 is the task's own reminder; 1–7 are extras stored in `task_reminders`. **The `slot` column must stay stable** — it seeds the alarm request code, so deriving it from a list index would orphan an armed `PendingIntent` on every mid-list delete.

**Health points / hearts** — 12 hearts tracked in half-heart units. +1 per ended day with ≥1 completion, −1 per idle day, clamped. Only fully-ended days fold into the stored checkpoint; today is applied live on top. A null `lastSettledEpochDay` means "first run — start full." Replaced the old streak mechanic.

**Day mode** — `DayModeCalculator` partitions the day (morning / midday / evening). Some UI is deliberately hidden at MIDDAY.

**Secret mode** — a time- or condition-bounded mode that reveals tasks marked secret. Lives in `domain/security` + `data/security`, auto-ends per `SecretModeEndCondition`.

**Journal** — a **local-only**, biometric-gated diary with a skeuomorphic polaroid camera. No backend, no sync, and deliberately **not wiped on logout**. The single documented data-loss surface.

**DoneBot** — the in-app AI assistant. Trivial intents resolve on-device via `LocalIntentClassifier`; everything else goes through the backend proxy to Vertex AI. Pomodoro control must stay client-side because engine state lives on the device.

**Guest mode** — a first-class path, not an afterthought. No account required: tasks are created locally as `PENDING_CREATE` and never leave the device; chat is limited to on-device intents.

**Palette kit** — a user-selectable design language: `ORIGINAL` (default, purple/blue), `MONOCHROME` (Notion-style neutrals, keeps the four semantic accents), `PIXEL` (NES palette, Pixelify Sans, stair-stepped corners, hard shadows). A kit carries **colour *and* geometry/motion/typeface**. Enum entry names are persisted DataStore values — renaming one silently resets every user who selected it.

**Creation Hub** — the swipeable multi-step task creator (type → scope → core → details).

**Overlay** — on Android, a `SYSTEM_ALERT_WINDOW` full-screen alarm card drawn over other apps. **Has no iOS equivalent.**

---

## Architecture terms

**MVI three-file rule** — every feature has `*Contract.kt` (UiState / UiAction / UiEffect), `*ViewModel.kt`, `*Screen.kt`. Extracted composables live in same-package files named with the screen prefix. These three always remain.

**`UiEffect` vs `NavigationEffect`** — one-time UI effects (toasts, dialogs) go through `_uiEffect`; navigation goes through a separate `_navEffect` collected in `NavGraph.kt` via `NavigationEffectController`. Composables never touch `NavController`.

**`SyncStatus`** — `PENDING_CREATE` | `PENDING_UPDATE` | `PENDING_DELETE` | `SYNCED`. `afterEdit()` never downgrades a pending create/delete. **Updating a `SYNCED` row must flip it to `PENDING_UPDATE`** or the next reconciliation silently overwrites the edit.

**`comparableFields()`** — the explicit 21-field list that decides whether a remote task "differs" from a clean local one. A field missing from it means off-device edits for that field die silently.

**`clientTaskId`** — client-generated UUID used as the idempotency key so a retried create does not duplicate.

**`BaseResponse<T?>`** — the universal envelope `{ code, message, data, errorCode }`. Unit-returning endpoints need `handleEmptyRequest`; plain `handleRequest` rejects a 2xx whose `data` is null.

**TTL cache (groups)** — `GroupRepositoryImpl` keeps in-memory caches that mutations do **not** invalidate. Reload with `force = true` after writes.

---

## KMP / CMP terms used in this spec

**`commonMain` / `androidMain` / `iosMain`** — Kotlin Multiplatform source sets. `commonMain` compiles for every target and may not reference `java.*`, `javax.*` or `android.*`.

**The `androidMain` ratchet** — the migration technique: a file moves into a shared module's `androidMain` first (green build, unchanged imports), then to `commonMain` once its JVM dependencies are removed. Gated by a grep. See README §4.2.

**KMP-shaped, Android-only** — a KMP module declaring only `androidTarget()`. Byte-equivalent to `com.android.library`, so the whole restructure happens before any iOS target exists. See README §4.1.

**`expect` / `actual`** — Kotlin's platform-declaration mechanism. In this spec, **prefer an interface + platform implementation** over `expect class`: interfaces are Koin-injectable and mockable in `commonTest`. `expect fun` is reserved for leaf value-returning helpers.

**`Res`** — the Compose Multiplatform generated resource accessor, replacing Android's `R`. Two `Res` objects exist, mirroring today's two `R` classes: one for `:uikit`, one for `:shared:resources`.

**One-way door** (`reversible: false`) — a task whose revert is expensive or impossible: schema changes, DI framework swap, sweeps across hundreds of files. Requires a clean, committed tree before starting.

---

## iOS terms

**Live Activity / ActivityKit** — the lock-screen and Dynamic Island surface. `Text(timerInterval:)` counts down without the app waking, which is what replaces the Pomodoro foreground service.

**`UNCalendarNotificationTrigger(repeats: true)`** — a repeating local notification. Costs **1** of the 64 pending requests forever, but supports **no end date** — so any rule with `until` must be materialized instead.

**Materialize** — to expand a recurrence into concrete one-shot notification requests inside a bounded time window, because the rule cannot be expressed as a repeating trigger.

**Rolling window / coverage horizon** — the period ahead for which reminders are actually scheduled. Surfaced to the user as "Scheduled through &lt;date&gt;".

**Sentinel** — a scheduled notification near the end of the coverage horizon telling the user to open the app so reminders can be extended.

**`.timeSensitive`** — a notification interruption level that breaks through Focus. Available to all apps and user-revocable. Distinct from `.critical`, which needs an Apple entitlement that will not be granted here.

**App Group** — the shared container that lets the main app and the widget/Live Activity extensions read the same data.

---

## Units and encodings — the expensive ones

| Thing | Encoding |
|---|---|
| Personal task times | **minutes** of day (Room) |
| Group / backend task times | **seconds** of day (wire) |
| `date`, `finishedOn`, `recurrenceUntil` | **epoch days** |
| `createdAt`, `dueDate`, `timestamp` | **epoch millis** |
| `UserData.createdAt` | **String** |

Mixing minute-of-day with second-of-day is a 60× error and has bitten this codebase before.
