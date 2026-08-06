---
id: 40-core-02
title: Creation Hub
layer: ui
status: TODO
depends_on: [40-core-01, 40-shared-01]
blocks: []
parallel_safe: true
estimate: 14h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/creationhub/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The swipeable multi-step task creator: type → scope → core → details/group, for one-time, routine, staged and group tasks.

## 2. Why this way

**This is the most designed feature in the app** — `.design/creation-hub/` holds its brief, information architecture and task checklist, produced through the full design pipeline. Read those before touching it; the interaction model is intentional.

**It is also the entry point to the journal**, via the carousel — a non-obvious connection that is easy to break.

**Two recorded lessons apply directly.** A "Başlat"-style CTA should auto-trigger the main action rather than just navigating (single-tap start), and any smart default that is auto-set must be symmetrically auto-reverted on deselect. Both are easy to lose in a port because they look like extra behaviour.

## 3. Source

| Path | LOC |
|---|---|
| `ui/creationhub/` (10 files) | 2,118 |
| `.design/creation-hub/DESIGN_BRIEF.md`, `INFORMATION_ARCHITECTURE.md`, `TASKS.md` | 439 — **read first** |
| `ui/common/taskform/` → `40-shared-01` | the shared form sections |
| `domain/model/Recurrence.kt` | routine configuration |

## 4. Target

`shared/ui/commonMain/…/ui/creationhub/` — verification.

## 5. Steps

1. Read the three design documents.
2. Verify all 10 files compile in `commonMain`.
3. Verify the swipe between steps on iOS — physics differ.
4. Verify each of the four task types creates correctly: one-time, routine, staged, group.
5. Verify the journal entry point from the carousel.
6. Verify auto-set defaults revert symmetrically on deselect.
7. Verify single-tap start behaviour on the CTA.
8. Verify created tasks have `clientTaskId`, correct `syncStatus` and scheduled reminders.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 10 files compile in `commonMain`
- [ ] Step swiping works on iOS
- [ ] All four task types create correctly
- [ ] Journal reachable from the carousel
- [ ] Auto-set defaults auto-revert on deselect
- [ ] CTA triggers the main action, not just navigation
- [ ] Created tasks carry `clientTaskId`, `PENDING_CREATE`, and reminders
- [ ] Group tasks use seconds, personal tasks minutes
- [ ] Three kits, two themes, two languages
- [ ] Previews cover each step and each task type

## 8. Pitfalls

- **Read the design documents first.** The interaction model is deliberate.
- **The journal entry point is here.** Losing it hides a whole feature.
- **Auto-set needs auto-revert.** Asymmetry leaves stale state.
- **Minutes vs seconds** — personal tasks store minutes, group tasks seconds. This screen creates both.
- **Do not skip `clientTaskId`.** Without it a retried create duplicates.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: create one of each type; check the row matches an equivalent
# created elsewhere; journal from the carousel; 3 kits, EN + TR
```
