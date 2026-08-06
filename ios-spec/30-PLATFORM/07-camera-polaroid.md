---
id: 30-07
title: Camera capture — the polaroid viewfinder
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-journal-03]
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - shared/ui/src/androidMain/**/camera/**
  - shared/ui/src/iosMain/**/camera/**
  - shared/ui/src/commonMain/**/journal/camera/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Replace CameraX with `AVCaptureSession` on iOS — **one file**, behind a composable contract. The ~1,900 lines of hand-drawn polaroid camera body are pure Compose and move unchanged.

## 2. Why this way

**This task is far smaller than it looks.** `ui/journal/camera/` is 12 files and roughly 2,000 lines, and the instinct is to treat it as a rewrite. It is not. Measured:

| File | LOC | Nature |
|---|---|---|
| `PolaroidComponentsCanvas.kt` | 577 | pure Compose Canvas |
| `PolaroidBodyCanvas.kt` | 381 | pure Compose Canvas |
| `PolaroidCameraBody.kt` | 245 | pure Compose |
| `LiveCameraPreview.kt` | **176** | **the only CameraX-bound file** |
| `PolaroidCameraControls.kt` | 149 | pure Compose |
| `PolaroidBodyMetrics.kt` | 132 | pure maths |
| `PolaroidPrint.kt`, `PolaroidViewfinder.kt`, + VM/Contract/Previews | ~340 | pure Compose |

So the port is: one composable contract, two implementations, and everything else is a move.

**The contract has to be composable**, not a plain interface — the preview must sit inside the composition, hosted in `AndroidView` on Android and `UIKitView` on iOS.

**Portrait lock is behaviour, not styling.** The polaroid body is drawn to portrait metrics; landscape would break the layout. `LockScreenOrientation` is already used here and needs an iOS equivalent (`30-15`).

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `ui/journal/camera/LiveCameraPreview.kt` | CameraX `PreviewView` + `ImageCapture` in an `AndroidView` — the only thing being replaced |
| `ui/journal/camera/PolaroidCameraViewModel.kt` | Capture state machine, develop animation trigger |
| `ui/journal/camera/PolaroidCameraScreen.kt` | Portrait lock, full-bleed (deliberately excluded from `topBarItems`) |
| `ui/permissions/CameraPermissionRequest.kt` | The permission gate |
| `data/storage/JournalPhotoStorage.kt` | Where captures land |
| `ui/common/components/ImageCropOverlay.kt` (299 LOC) | Pure Canvas — used by avatar crop, ports free |

## 4. Target

- `shared/ui/commonMain/…/journal/camera/CameraCapture.kt` — `@Composable expect`
- `shared/ui/androidMain/…/CameraCapture.android.kt` — the existing CameraX code
- `shared/ui/iosMain/…/CameraCapture.ios.kt` — `AVCaptureSession` in `UIKitView`
- Everything else in `ui/journal/camera/` → `commonMain`, unchanged

## 5. Steps

1. **Move the 11 non-camera files to `commonMain` first**, with the preview stubbed. Verify the polaroid body still renders identically on Android. This proves the drawing is portable before any AVFoundation work.

2. **Define the composable contract** — a preview composable plus a controller with `capture()`.

3. **Android: wrap the existing `LiveCameraPreview`** as the `actual`. No behaviour change.

4. **iOS: `AVCaptureSession`** with `AVCaptureVideoPreviewLayer` hosted in `UIKitView`, and `AVCapturePhotoOutput` for capture.

5. **Configure the session off the main thread.** `startRunning()` blocks; doing it on the main thread stutters the UI.

6. **Match the aspect ratio.** The polaroid viewfinder is a fixed frame; the preview layer's `videoGravity` must match what Android produces or framing differs between platforms.

7. **Handle orientation.** Portrait-locked, but the connection's `videoOrientation` still needs setting or captures come out rotated.

8. **Return the capture as an `ImageBitmap`** through the shared `ImageCodec` contract (`30-08`) so the develop animation and storage path are identical on both platforms.

9. **Release the session** in `onDispose`. A leaked `AVCaptureSession` keeps the camera indicator lit — a visible bug and an App Review flag.

## 6. Code skeleton

```kotlin
// shared/ui/commonMain/…/journal/camera/CameraCapture.kt
@Composable
expect fun CameraPreview(modifier: Modifier, controller: CameraController)

expect class CameraController() {
    suspend fun capture(): ImageBitmap?
    fun release()
}
```

```kotlin
// shared/ui/iosMain/…/CameraCapture.ios.kt
@Composable
actual fun CameraPreview(modifier: Modifier, controller: CameraController) {
    UIKitView(
        factory = {
            UIView().apply {
                val layer = AVCaptureVideoPreviewLayer(session = controller.session)
                layer.videoGravity = AVLayerVideoGravityResizeAspectFill   // must match Android's framing
                this.layer.addSublayer(layer)
            }
        },
        modifier = modifier,
    )
    DisposableEffect(Unit) {
        // A leaked AVCaptureSession keeps the camera indicator lit — visible bug, review flag.
        onDispose { controller.release() }
    }
}
```

## 7. Acceptance

- [ ] All 11 non-camera files compile in `commonMain`
- [ ] The polaroid body renders **identically** on Android before and after the move (screenshot comparison)
- [ ] Android capture behaviour unchanged
- [ ] iOS: preview appears in the viewfinder with the same framing as Android
- [ ] iOS: capture produces an image; the develop animation plays; the photo is stored
- [ ] `NSCameraUsageDescription` present and specific
- [ ] Permission denial shows the existing gate UI, not a crash
- [ ] Portrait lock works on iOS
- [ ] Session released on dispose — camera indicator turns off when leaving the screen
- [ ] Captured photos are correctly oriented, not rotated
- [ ] Session configuration happens off the main thread — no visible stutter on entry

## 8. Pitfalls

- **Do not rewrite the polaroid drawing.** ~1,900 lines of Compose Canvas move unchanged. Rewriting it in SwiftUI would be the single largest wasted effort available in this project.
- **`startRunning()` blocks.** Configure and start on a background queue.
- **A leaked session keeps the camera indicator on.** Users notice, and reviewers do too.
- **Rotation.** Even portrait-locked, `videoOrientation` on the connection must be set or captures are sideways.
- **`videoGravity` changes framing.** `ResizeAspectFill` vs `ResizeAspect` crops differently. Match Android.
- **`NSCameraUsageDescription` must be specific.** "DoneBot uses the camera to take polaroid photos for your journal entries."
- **The simulator has no camera.** Test capture on hardware; the simulator only verifies layout.
- **Photos must not go to `composeResources`.** They are user data — `JournalPhotoStorage` (`30-08`).
- **The journal is local-only and never wiped on logout.** Captures here have no backend copy; losing them is permanent.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# Android first — prove the drawing survived the move
#   open the polaroid camera, screenshot, compare with docs/screenshots/

# iOS, on hardware
#   grant camera permission → preview appears, framing matches Android
#   capture → develop animation → photo saved to the entry
#   deny permission → the gate UI appears, no crash
#   leave the screen → camera indicator turns off
#   rotate the device → stays portrait
```
