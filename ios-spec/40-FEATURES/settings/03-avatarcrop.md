---
id: 40-settings-03
title: Avatar crop
layer: ui
status: TODO
depends_on: [40-settings-02, 30-08]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/profile/avatarcrop/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The in-app avatar crop screen.

## 2. Why this way

**The crop overlay is pure Canvas** (`ImageCropOverlay.kt`, 299 LOC) and ports unchanged; only the final pixel operation goes through `ImageCodec` (`30-08`).

**This is the screen where the bitmap-lifetime rule matters most.** `CLAUDE.md` has an explicit anti-pattern: never decode a `Bitmap` into `remember { }` without a `DisposableEffect` that recycles it — and a companion lesson that the effect must capture the *value* (`val toRecycle = bitmap`), not the state, or it recycles the wrong bitmap at dispose. Opening and closing this screen repeatedly is the standard leak test.

**It is full-bleed**, deliberately excluded from `topBarItems`.

## 3. Source

| Path | LOC |
|---|---|
| `ui/profile/avatarcrop/` | part of 1,080 |
| `ui/common/components/ImageCropOverlay.kt` | 299 — pure Canvas |
| `ui/common/ImageCropUtils` | the crop maths |
| `navigation/Screen.kt` | `AvatarCrop(source)`; excluded from `topBarItems` |

## 4. Target

`shared/ui/commonMain/…/ui/profile/avatarcrop/` — verification.

## 5. Steps

1. Verify the files compile in `commonMain`.
2. Verify the overlay renders identically on both platforms.
3. Verify pan and zoom gestures on iOS.
4. Verify the crop produces the same output dimensions on both platforms.
5. **Verify no bitmap leak** — open and close 20 times, watch memory.
6. Verify the upload path and the `avatarVersion` bump.
7. Verify full-bleed chrome: no top bar.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Files compile in `commonMain`
- [ ] Overlay renders identically
- [ ] Pan and zoom work on iOS
- [ ] Crop output dimensions match across platforms
- [ ] **No memory growth over 20 open/close cycles**
- [ ] Upload works; version bumped
- [ ] No top bar
- [ ] Three kits, two themes, two languages
- [ ] Previews cover the crop overlay at a couple of zoom levels

## 8. Pitfalls

- **`DisposableEffect` must capture the value, not the state.** `val toRecycle = bitmap` inside the effect.
- **Decode with a `maxDim`.** A full-size decode here is the documented ANR path.
- **Do not add this to `topBarItems`.** Full-bleed by design.
- **Crop maths is shared; only the pixel operation is platform-specific.**

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: pick a large photo, pan/zoom, crop, upload; repeat 20 times
# while watching memory
```
