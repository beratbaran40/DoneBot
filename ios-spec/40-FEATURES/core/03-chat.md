---
id: 40-core-03
title: Chat (DoneBot)
layer: ui
status: TODO
depends_on: [40-auth-02, 50-04]
blocks: []
parallel_safe: true
estimate: 14h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/chat/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The AI assistant screen — bubbles, thinking indicator, error banner, and the on-device intent path.

## 2. Why this way

**Almost all of chat is already portable.** The backend proxy is a plain REST call, and `LocalIntentClassifier` (373 LOC) is pure Kotlin that moves to `commonMain` unchanged. The Room-backed history is load-bearing — the backend is stateless and the client sends the last 10 turns per call, so `chat_messages` must not be dropped as "backend migration cleanup."

**The composer is the real iOS risk.** A multiline, growing text field with a send action, and it is the single hardest text-input case in the app. `50-04`'s torture screen should already have settled it; this is where it gets confirmed against the real thing.

**Pomodoro intents must stay client-side.** `CLAUDE.md` is explicit: `PomodoroEngine` state lives on the device, so start/stop/status never go to the backend. That is also what makes them work in guest mode and offline.

**Two rules that are audit surface, not polish:** group tasks are blocked for any chat write (`group_task_blocked`), and the bot must never mention internal numeric task ids.

## 3. Source

| Path | LOC |
|---|---|
| `ui/chat/` (6 files) | 1,979 |
| `data/ai/LocalIntentClassifier.kt` | 373 — pure, moves unchanged |
| `ui/chat/ChatViewModel.kt` (~241-244, ~349-350) | guest gating; forced sync after a mutating turn |
| `POST chat/message`, `POST chat/report` | the endpoints |
| `DONEBOT_CAPABILITIES.md` | the capability catalogue |
| `docs/screenshots/chat/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/chat/` — verification.

## 5. Steps

1. Verify all 6 files compile in `commonMain`, and `LocalIntentClassifier` with them.
2. Verify the composer on iOS: multiline growth, send, keyboard insets, long messages.
3. Verify on-device intents resolve without a network call — today, overdue, weekly, greeting, pomodoro.
4. Verify backend turns work and history persists across relaunch.
5. Verify guest gating: local intents only, with the sign-in banner and pending-prompt resume.
6. Verify the thinking indicator starts and **stops**.
7. Verify a mutating turn triggers a forced sync.
8. Verify group-task writes are blocked.
9. Verify no numeric ids appear in replies.
10. Both languages.

## 7. Acceptance

- [ ] All 6 files plus `LocalIntentClassifier` compile in `commonMain`
- [ ] Composer works on iOS: multiline, growth, send, no keyboard occlusion
- [ ] On-device intents resolve offline
- [ ] Backend turns work; history survives relaunch
- [ ] Guest mode limited to local intents, with the banner and pending-prompt resume
- [ ] Thinking indicator stops when the response arrives
- [ ] A mutating turn forces a sync
- [ ] **Group-task writes blocked**
- [ ] **No numeric task ids in any reply**
- [ ] Pomodoro control works offline and in guest mode
- [ ] Both languages
- [ ] Previews cover empty, conversation, thinking, error and guest-limited

## 8. Pitfalls

- **Do not drop the `chat_messages` table.** The backend is stateless; history is client-side and load-bearing.
- **Pomodoro must stay client-side.**
- **Group-task writes must stay blocked.** Relaxing it needs an audit story.
- **Never surface numeric ids.** Confirm by title and date.
- **The composer is the hardest text-input case.** Verify with long, multiline, pasted content.
- **Prompt rules live in the backend**, not client strings.
- **The thinking indicator is an infinite animation.** It must stop, or it drains battery.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: local intents offline, backend turn, long multiline message,
# guest mode, group-task attempt (blocked), pomodoro start/stop, EN + TR
```
