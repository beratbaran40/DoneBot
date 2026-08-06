---
id: 40-journal-01
title: Journal timeline
layer: ui
status: TODO
depends_on: [40-core-02, 30-06]
blocks: [40-journal-02]
parallel_safe: false
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/journal/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The date-grouped journal timeline, behind a biometric lock.

## 2. Why this way

**The journal is the app's only irreplaceable data.** It is local-only — no backend, no sync — and it is deliberately **not wiped on logout**. `MainViewModel.clearLocalSession()` clears everything else and skips this on purpose. A bug that deletes journal entries destroys them permanently, which is why `20-07`'s migration gate matters so much.

**Its privacy story changes on iOS and the copy must change with it.** Android applies `FLAG_SECURE`, blocking screenshots and screen recording. iOS **cannot**. The biometric lock still works; screenshot blocking does not. Any string implying screenshot protection has to be conditional on `blocksScreenshots` or reworded — `80-02` audits this, but the strings live here.

**The entry point is the Creation Hub carousel**, not a top-level nav item — a non-obvious connection that is easy to lose.

## 3. Source

| Path | LOC |
|---|---|
| `ui/journal/` root (10 files) | part of 4,075 |
| `ui/journal/JournalViewModel.kt` | `UiState.Locked` |
| `domain/repository/JournalBiometricPreferences.kt` | the lock flag |
| `data/repository/JournalRepositoryImpl.kt` | DAO + photo storage + DataStore; **no remote source** |
| `MainViewModel.kt` (~289-293) | why logout does not wipe it |
| `data/model/entity/JournalEntryEntity.kt` | `owner_user_id`; 0 = unclaimed |

## 4. Target

`shared/ui/commonMain/…/ui/journal/` (root) — verification.

## 5. Steps

1. Verify the root files compile in `commonMain`.
2. Verify the biometric gate: `UiState.Locked` until authenticated (`30-06`).
3. Verify Face ID unlocks and cancel keeps it locked.
4. Verify the timeline groups by date correctly.
5. Verify search within the journal.
6. Verify the entry point from the Creation Hub carousel.
7. **Verify entries survive logout** — the deliberate exception.
8. Verify `owner_user_id` scoping: a second account does not see the first's entries.
9. **Verify no string claims screenshot protection on iOS.**
10. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Root files compile in `commonMain`
- [ ] Biometric gate works; cancel keeps it locked
- [ ] Timeline groups by date
- [ ] Search works
- [ ] Reachable from the Creation Hub carousel
- [ ] **Entries survive logout**
- [ ] Per-user scoping enforced
- [ ] **No screenshot-protection claim in any iOS string**
- [ ] iOS hides content in the app switcher
- [ ] Three kits, two themes, two languages
- [ ] Previews cover locked, empty, populated

## 8. Pitfalls

- **Journal data is irreplaceable.** No backend copy. A migration or scoping bug destroys it permanently.
- **Do not wipe it on logout.** The exception is deliberate and documented.
- **`FLAG_SECURE` has no iOS equivalent.** Do not claim screenshot protection there.
- **`owner_user_id = 0` means unclaimed** — pre-v20 or created while logged out. `claimOrphansForCurrentUser()` backfills at startup.
- **The Creation Hub carousel is the only entry point.**

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: unlock with biometrics, cancel (stays locked), create entries,
# log out and back in (entries survive), log in as another account (not visible),
# grep the strings for screenshot claims
```
