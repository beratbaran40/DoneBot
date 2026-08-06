---
id: 40-core-06
title: Notification centre
layer: ui
status: TODO
depends_on: [40-core-01, 30-03]
blocks: []
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/notifications/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The in-app notification inbox — sections, cards, skeleton, swipe actions and undo.

## 2. Why this way

This screen was redesigned recently (One UI style, shipped with swipe plus two undo affordances), so it is well-specified and the port should preserve that work rather than reinterpret it.

**Swipe-to-dismiss is the iOS risk.** Fling velocity differs; a threshold tuned on Android can feel wrong. Fix it in shared code with a comment, not per platform.

**The unread count feeds a badge**, so it must stay consistent with the server: `GET notifications/unread-count`, `PUT notifications/{id}/read`, `PUT notifications/read-all`.

## 3. Source

| Path | LOC |
|---|---|
| `ui/notifications/` (11 files) | 1,412 |
| `uikit/…/TDUndoSnackbar.kt` | the undo affordance |
| `GET notifications`, `PUT …/read`, `PUT read-all`, `DELETE …`, `GET unread-count` | the endpoints |
| `data/source/remote/fcm/PushPayload.kt` | the 10 types the inbox renders |
| `docs/screenshots/notifications/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/notifications/` — verification.

## 5. Steps

1. Verify all 11 files compile in `commonMain`.
2. Verify paging (`?limit=50&before=`).
3. Verify swipe-to-dismiss on iOS and the undo path.
4. Verify mark-read, mark-all-read and delete, and that the badge follows.
5. Verify all 10 payload types render with the right copy and icon.
6. Verify tapping routes to the right screen.
7. Verify the skeleton and the empty state.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 11 files compile in `commonMain`
- [ ] Paging works
- [ ] Swipe-to-dismiss feels right on iOS; undo works
- [ ] Mark-read, mark-all-read and delete work; the badge stays consistent
- [ ] All 10 payload types render correctly
- [ ] Tapping routes correctly
- [ ] Skeleton and empty state render
- [ ] Three kits, two themes, two languages
- [ ] Previews cover loading, empty, populated and error

## 8. Pitfalls

- **Do not tune the swipe threshold per platform.** Shared code, with a comment.
- **The badge must match the server count.** A local-only decrement drifts.
- **Undo must actually undo**, including server-side. A local-only undo reappears on refresh.
- **Zero-count needs its own string.** A recorded rule here: `count == 0` gets a dedicated key, not a plural.
- **The skeleton shell should render unconditionally**, with the `when` scoped inside — a recorded skeleton-design rule.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: receive each payload type, swipe-dismiss, undo, mark all read,
# check the badge, tap through to each target, EN + TR
```
