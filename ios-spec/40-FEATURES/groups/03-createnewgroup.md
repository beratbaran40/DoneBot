---
id: 40-groups-03
title: Create new group
layer: ui
status: TODO
depends_on: [40-groups-01]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/createnewgroup/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Create a group — name, description, and the transition into the first-invite flow.

## 2. Why this way

**This screen produced a shipped bug class: duplicate groups.** The fix was a mutex plus a unique `remote_id` index, with `MIGRATION_25_26` deduping existing rows. Two causes converge here — a double tap on the create button, and a retried request after a slow response against a cold Render dyno.

The recorded lesson is broader than this screen: a save-and-exit path plus a double tap equals a double INSERT unless the exit path is re-entrancy-guarded.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/createnewgroup/` (3 files) | — |
| `POST family-groups` | `CreateGroupRequest(name, description)` |
| `data/repository/GroupRepositoryImpl.kt` | the mutex |
| `data/source/local/Migrations.kt` | `MIGRATION_25_26` — the dedup |

## 4. Target

`shared/ui/commonMain/…/ui/groups/createnewgroup/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify creation works and routes into the first-invite dialog.
3. **Verify a double tap creates one group**, not two.
4. Verify a slow-network retry does not duplicate.
5. Verify validation — empty name, over-length.
6. Verify the list refreshes with `force = true`.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Creation works and routes to first-invite
- [ ] **Double tap → one group**
- [ ] Slow-network retry → one group
- [ ] Validation errors localized
- [ ] List refreshes immediately
- [ ] Three kits, two themes, two languages
- [ ] Previews cover idle, loading and error

## 8. Pitfalls

- **Double tap on save-and-exit is the recorded cause of duplicates.** Guard re-entrancy on the exit path, not just the button.
- **Cold Render dynos make slow responses normal**, which makes retries normal.
- **`force = true`** after creating.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: create a group; tap create twice as fast as possible → one group;
# throttle the network to 3G and retry → one group
```
