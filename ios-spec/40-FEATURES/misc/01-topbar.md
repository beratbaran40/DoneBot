---
id: 40-misc-01
title: Top bar
layer: ui
status: TODO
depends_on: [20-11, 50-04]
blocks: [40-core-01]
parallel_safe: false
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/topbar/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

`TDTopBar` — the shared chrome for 34 screens, including the avatar chip and the info-dialog system.

## 2. Why this way

**This is one of the hardest rules in the codebase**: never build a custom top bar or an in-screen back/title row. A screen gets its top bar by adding an `AppDestination` entry and appearing in `topBarItems`; `ShowTopBar` then renders back-arrow and title automatically. Thirty-four screens depend on it, and hiding it is done by *removing* the destination from `topBarItems`, not by conditional rendering.

**Despite the `TD` prefix it lives in `:app`, not `:uikit`** — along with `TDBottomBar` and `TDNavigationRail`. That is deliberate: they depend on `:app` types.

**The avatar chip has a recorded trap.** It cache-busts with `?v=` from a *persisted* `avatarVersion`; a per-emission value makes every recomposition a cache miss. It also observes the user through `observeUser()`, which is why profile changes must call `setUser`.

**Top-bar real estate is limited** — another recorded lesson: count the slots before adding anything, because Home is already crowded.

## 3. Source

| Path | LOC |
|---|---|
| `ui/topbar/TDTopBar.kt` + VM | 607 |
| `navigation/AppDestination.kt` | 358 — `topBarItems` (34), `hasInfoDialog` |
| `ui/common/ScreenInfoDialog.kt` | the info-dialog system |
| `ui/topbar/` → `AvatarChip` | the `?v=` cache-bust |

## 4. Target

`shared/ui/commonMain/…/ui/topbar/` — verification.

## 5. Steps

1. Verify the files compile in `commonMain`.
2. Verify the top bar renders on all 34 `topBarItems` screens.
3. Verify it is **absent** on the three full-bleed screens (JournalEntry, PolaroidCamera, AvatarCrop).
4. Verify back-arrow and title behaviour.
5. Verify the info dialog appears where `hasInfoDialog` is true.
6. Verify the avatar chip loads with auth and refreshes after an avatar change.
7. Verify safe-area handling on iOS — notch and Dynamic Island.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Files compile in `commonMain`
- [ ] Top bar renders on all 34 screens
- [ ] Absent on the three full-bleed screens
- [ ] Back and title correct
- [ ] Info dialog works where flagged
- [ ] Avatar chip loads with the Bearer token and refreshes after a change
- [ ] Safe area correct on iOS, including the Dynamic Island
- [ ] Three kits, two themes, two languages
- [ ] Previews cover with/without back, with/without info, with/without avatar

## 8. Pitfalls

- **Never build a custom top bar.** `AppDestination` + `topBarItems` is the only mechanism.
- **Hide by removing from `topBarItems`**, not by conditional rendering.
- **`avatarVersion` is persisted.**
- **`TDTopBar` lives in `:app`**, not `:uikit`, despite the prefix.
- **Top-bar space is limited.** Do not add slots without counting.
- **iOS safe area differs by device.** Check on a notched device and one with the Dynamic Island.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: visit all 34 top-bar screens; confirm the three full-bleed screens
# have none; change the avatar and watch the chip refresh; check the Dynamic Island
```
