---
id: 40-settings-02
title: Profile
layer: ui
status: TODO
depends_on: [40-settings-01, 30-08]
blocks: [40-settings-03]
parallel_safe: true
estimate: 8h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/profile/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The user profile — display name, avatar, and the health-points badge.

## 2. Why this way

**Two recorded lessons converge here.** After `uploadAvatar` or `updateDisplayName`, the code must call `dataStoreHelper.setUser(it)` or the top bar's `observeUser()` chain never sees the change — the avatar updates on this screen and stays stale in the chrome. And the avatar cache-bust token (`avatarVersion`) is **persisted**, not derived per emission; a per-emission value makes every recomposition a cache miss.

**The health badge must match the Activity screen and the widget exactly.** Same `HealthPointsCalculator`, same half-heart rounding. Three surfaces showing three numbers is a trust problem.

## 3. Source

| Path | LOC |
|---|---|
| `ui/profile/` (8 files incl. `avatarcrop/`) | 1,080 |
| `ui/profile/ProfileHealthBadge` | must match Activity and the widget |
| `data/repository/UserRepositoryImpl.kt` | `uploadAvatar`, `updateDisplayName` → `setUser` |
| `ui/topbar/TDTopBar.kt` | `AvatarChip`, `?v=` cache-bust |
| `POST users/me/avatar`, `PUT users/me` | the endpoints |
| `docs/screenshots/profile/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/profile/` — verification.

## 5. Steps

1. Verify the profile files compile in `commonMain`.
2. Verify display-name edit persists **and updates the top bar**.
3. Verify avatar upload, and that both the profile and the top-bar chip refresh.
4. Verify `avatarVersion` is persisted, not per-emission.
5. Verify the health badge matches Activity exactly.
6. Verify the logged-out and guest states.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Profile files compile in `commonMain`
- [ ] Display-name edit persists and **the top bar updates**
- [ ] Avatar upload refreshes both the profile and the top-bar chip
- [ ] `avatarVersion` persisted
- [ ] Health badge matches the Activity screen
- [ ] Logged-out and guest states render
- [ ] Three kits, two themes, two languages
- [ ] Previews cover logged-in, guest, no-avatar

## 8. Pitfalls

- **Call `setUser` after avatar or name changes**, or the top bar stays stale.
- **`avatarVersion` is persisted.** A per-emission value defeats caching entirely.
- **The health badge must match Activity and the widget.** One calculator, three surfaces.
- **`AsyncImage` needs the auth client** to fetch the avatar with a Bearer token.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: change the display name (top bar updates), upload an avatar
# (both surfaces update), compare hearts with Activity and the widget
```
