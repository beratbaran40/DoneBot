---
id: 40-journal-03
title: Polaroid camera
layer: ui
status: TODO
depends_on: [40-journal-02, 30-07]
blocks: []
parallel_safe: true
estimate: 12h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/journal/camera/**
  - shared/ui/src/iosMain/**/camera/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The skeuomorphic polaroid camera: hand-drawn body, viewfinder, capture, develop animation, print.

## 2. Why this way

**This looks like the biggest feature in the port and is one of the smaller ones.** Of ~2,000 lines across 12 files, only `LiveCameraPreview.kt` (176 LOC) is CameraX-bound. The rest — 577 lines of component canvas, 381 of body canvas, plus metrics, controls, viewfinder and print — is pure Compose that moves unchanged.

`30-07` builds the capture contract; this task is the screen around it.

**Portrait lock is behaviour, not styling.** The body is drawn to portrait metrics and landscape breaks the layout.

**It is also full-bleed** — deliberately excluded from `topBarItems`, with its own floating chrome. Adding it to the top-bar list would break the illusion.

## 3. Source

| Path | LOC |
|---|---|
| `ui/journal/camera/` (12 files) | ~2,000 |
| `PolaroidComponentsCanvas.kt` / `PolaroidBodyCanvas.kt` | 577 / 381 — pure Compose |
| `LiveCameraPreview.kt` | **176 — the only platform-bound file** |
| `ui/common/LockScreenOrientation.kt` → `30-15` | portrait lock |
| `navigation/AppDestination.kt` | excluded from `topBarItems` |

## 4. Target

`shared/ui/commonMain/…/ui/journal/camera/` (11 files) + the platform preview.

## 5. Steps

1. Verify the 11 non-camera files compile in `commonMain`.
2. **Screenshot the polaroid body on Android before and after** — the drawing must be identical.
3. Verify the iOS preview appears with matching framing.
4. Verify capture, develop animation and print.
5. Verify the captured photo attaches to the entry.
6. Verify portrait lock on iOS.
7. Verify the camera-permission gate.
8. Verify the session releases on exit — camera indicator off.
9. Verify full-bleed chrome: no top bar.
10. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] 11 files compile in `commonMain`
- [ ] Polaroid body renders **identically** on Android before and after
- [ ] iOS preview framing matches Android
- [ ] Capture → develop → print works
- [ ] Photo attaches to the entry
- [ ] Portrait lock works on iOS
- [ ] Permission gate works; denial does not crash
- [ ] Camera indicator turns off on exit
- [ ] No top bar; floating chrome intact
- [ ] Three kits, two themes, two languages
- [ ] Previews cover idle, capturing, developing

## 8. Pitfalls

- **Do not rewrite the drawing.** ~1,900 lines of Compose Canvas move unchanged.
- **A leaked `AVCaptureSession` keeps the camera indicator lit.** Visible bug, review flag.
- **Framing must match.** `videoGravity` changes the crop.
- **Portrait lock is layout-critical.**
- **Do not add this to `topBarItems`.** It is deliberately full-bleed.
- **The simulator has no camera.** Layout can be checked there; capture cannot.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Android first: screenshot the body, compare with docs/screenshots/
# iOS on hardware: preview, capture, develop, attach; rotate (stays portrait);
# leave the screen (indicator off); deny permission (no crash)
```
