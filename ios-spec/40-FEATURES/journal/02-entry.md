---
id: 40-journal-02
title: Journal entry editor
layer: ui
status: TODO
depends_on: [40-journal-01, 30-08, 50-04]
blocks: [40-journal-03]
parallel_safe: true
estimate: 10h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/journal/entry/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The entry editor — handwriting-style text, photo strip, polaroid photos, washi-tape decoration.

## 2. Why this way

**A long-form multiline editor with a custom typeface** (`journalHandwritingStyle`, 16sp with 28sp line height) is a different text-input case from the short fields in auth — it is the one where line-height behaviour and scrolling-while-typing show up. Compose's iOS text metrics differ slightly, and this is where that is most visible.

**Two recorded traps apply.** Auto-generating a title from the first line duplicates that line in the preview unless the preview does `drop(1)`. And a save-and-exit path plus a double tap produces two entries unless the exit path is re-entrancy-guarded.

**Photos here are local-only**, stored via `JournalPhotoStorage`. They have no backend copy and no upload queue.

## 3. Source

| Path | LOC |
|---|---|
| `ui/journal/entry/` (7 files) | part of 4,075 |
| `uikit/…/theme/Type.kt` | `journalHandwritingStyle` |
| `data/storage/JournalPhotoStorage.kt` | local-only storage |
| `uikit/…/theme/PolaroidColors.kt` | the 30-field polaroid palette |

## 4. Target

`shared/ui/commonMain/…/ui/journal/entry/` — verification.

## 5. Steps

1. Verify all 7 files compile in `commonMain`.
2. Verify the editor: long multiline text, correct line height, scroll-while-typing.
3. Verify auto-title does not duplicate the first line in the preview.
4. **Verify a double tap on save produces one entry.**
5. Verify photo attach from the library and from the polaroid camera.
6. Verify washi-tape decoration renders.
7. Verify polaroid photo rendering uses `TDTheme.colors.polaroid`.
8. Verify entries and photos survive relaunch.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 7 files compile in `commonMain`
- [ ] Long multiline editing works; line height correct; scroll-while-typing smooth
- [ ] Auto-title does not duplicate the first line
- [ ] **Double tap on save → one entry**
- [ ] Photos attach from library and camera
- [ ] Washi tape and polaroid rendering correct
- [ ] Entries and photos survive relaunch
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, text-only, with-photos

## 8. Pitfalls

- **Auto-title duplicates the first line** unless the preview drops it.
- **Save-and-exit plus double tap equals two entries.** Guard the exit path.
- **Photos are local-only.** Losing them is permanent.
- **`journalHandwritingStyle` has an explicit line height.** If baselines look wrong on iOS, that is the place to look — do not change the value for one platform.
- **Do not clobber `Success`** — the editor holds form state and the photo picker triggers a reload.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: write a long entry, attach photos, tap save twice quickly (one entry),
# relaunch (everything present), 3 kits, EN + TR
```
