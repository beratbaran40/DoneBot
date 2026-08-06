---
id: 40-groups-04
title: Group settings
layer: ui
status: TODO
depends_on: [40-groups-02, 30-08]
blocks: [40-groups-09]
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/groups/groupsettings/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Edit a group's name, description and avatar; leave or delete the group.

## 2. Why this way

**Two destructive actions live here**, and the codebase has settled conventions for both: destructive actions go at the **bottom**, in a Danger Zone, and typed confirmations must be **localized** — the Turkish confirmation is `HESABI SİL`, case-sensitive, and comparing against the English string breaks it for half the users.

**Avatar upload** goes through multipart (`POST family-groups/{groupId}/avatar`) and needs the `avatarVersion` cache-bust bump afterwards, or the new image never appears.

## 3. Source

| Path | LOC |
|---|---|
| `ui/groups/groupsettings/` (4 files) | — |
| `PUT family-groups`, `DELETE family-groups/{id}`, `POST …/leave`, `POST …/avatar` | the endpoints |
| `uikit/…/extensions/ObscuredTouchGuard.kt` | tapjacking guard on destructive confirms |
| `ios-spec/30-PLATFORM/08-photos-and-files.md` | the codec |

## 4. Target

`shared/ui/commonMain/…/ui/groups/groupsettings/` — verification.

## 5. Steps

1. Verify all 4 files compile in `commonMain`.
2. Verify name and description edits persist and refresh with `force = true`.
3. Verify avatar upload and that the new image appears (version bumped).
4. Verify leave and delete, each with confirmation.
5. **Verify typed confirmations are localized** and case-sensitive per language.
6. Verify destructive actions sit at the bottom.
7. Verify `ObscuredTouchGuard` is applied — Android only; iOS has no equivalent.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 4 files compile in `commonMain`
- [ ] Name and description edits persist and refresh
- [ ] Avatar uploads and the new image appears
- [ ] Leave and delete work with confirmation
- [ ] **Typed confirmation localized** — TR accepts the Turkish phrase, not the English one
- [ ] Destructive actions at the bottom
- [ ] Three kits, two themes, two languages
- [ ] Previews cover idle, editing, confirm-delete

## 8. Pitfalls

- **Typed confirmations must be localized and compared per language.** Comparing against English breaks TR.
- **Bump `avatarVersion` after upload**, or the old image is cached forever.
- **Destructive actions at the bottom**, in a Danger Zone — the established convention.
- **`ObscuredTouchGuard` is Android-only.** No iOS equivalent; do not fake one.
- **Leaving a group must clear its local rows.**

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms and both languages: edit name, upload an avatar (appears immediately),
# type the confirmation in the device language, leave and delete
```
