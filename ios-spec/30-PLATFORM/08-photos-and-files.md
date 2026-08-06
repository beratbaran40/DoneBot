---
id: 30-08
title: Photo storage & image codec
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-journal-02, 40-settings-03, 40-core-04]
parallel_safe: true
estimate: 16h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/storage/**
  - shared/data/src/androidMain/**/storage/**
  - shared/data/src/iosMain/**/storage/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Two contracts: `PhotoStorage` (where user images live on disk) and `ImageCodec` (decode, encode, crop) — replacing the 12 `android.graphics.Bitmap` uses in `ui/`.

## 2. Why this way

`Bitmap` is the single most viral Android type in the UI layer. It appears in avatar cropping, journal photos, task photo attachments and the polaroid develop animation. Left unabstracted it blocks four feature areas.

**Two contracts, not one, because the concerns are genuinely separate.** Storage is about paths and bytes and survives across launches; the codec is about pixels and is transient. Merging them produces an interface where half the methods are irrelevant to any given caller.

**Bitmap lifetime is a known hazard in this codebase.** `CLAUDE.md` has an explicit anti-pattern entry: never decode a `Bitmap` into `remember { }` without a `DisposableEffect` that recycles it — and there is a matching memory about `DisposableEffect` needing to capture the value, not the state. The preferred fix is to route through Coil's `AsyncImage` so Coil manages native memory. The `ImageCodec` contract exists for the cases where that is not possible (cropping, the develop animation), and it returns Compose's `ImageBitmap` rather than a platform bitmap so lifetime is the runtime's problem.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/storage/JournalPhotoStorage.kt`, `AvatarPhotoStorage.kt` | 148 LOC combined — path conventions, filenames |
| `ui/common/components/ImageCropOverlay.kt` (299 LOC) | Pure Canvas; the crop *maths* is portable, only the pixel operation is not |
| `ui/profile/avatarcrop/` | The crop screen; `SuccessResult` / `CachePolicy` usage (renamed in Coil 3) |
| `ui/journal/entry/` | Photo strip, polaroid photo rendering |
| `data/repository/PendingPhotoRepositoryImpl.kt` + `PendingPhotoEntity` | The offline upload queue |
| `Application.newImageLoader()` | Coil wired with the auth `OkHttpClient` so `AsyncImage` sends the Bearer token |
| `CLAUDE.md` anti-patterns | The `Bitmap` recycling rule |

## 4. Target

- `shared/domain/…/storage/PhotoStorage.kt`, `ImageCodec.kt`
- `shared/data/androidMain/…/` — `filesDir` + `BitmapFactory`
- `shared/data/iosMain/…/` — `NSFileManager` Documents + `UIImage`

## 5. Steps

1. **Define both contracts.** `PhotoStorage` deals in `ByteArray` and opaque path strings; `ImageCodec` deals in `ImageBitmap`.

2. **Android: wrap the existing storage classes.** Path conventions must not change — existing journal photos have to keep resolving.

3. **iOS: use the Documents directory**, not Caches. Journal photos are irreplaceable user data with no backend copy; the system may purge Caches at any time.

4. **Exclude photos from iCloud backup or include them deliberately.** Journal photos are local-only, so backup is arguably desirable — but it is a size and privacy decision. Record it in `DECISIONS.md`.

5. **Implement `ImageCodec`.** `decode` must take a `maxDim` and downsample — decoding a 12 MP photo at full size to display a thumbnail is how the codebase previously produced a 6 MB decode and an ANR.

6. **Keep the crop maths shared.** Only the final pixel operation is platform-specific.

7. **Route display images through Coil 3's `AsyncImage`** wherever possible, so the codec is only used where a raw bitmap is genuinely required.

## 6. Code skeleton

```kotlin
// shared/domain/…/storage/PhotoStorage.kt
interface PhotoStorage {
    suspend fun save(bytes: ByteArray, id: String): String   // returns an opaque path
    suspend fun load(path: String): ByteArray?
    suspend fun delete(path: String)
    fun displayUri(path: String): String                     // for AsyncImage
}

// shared/domain/…/storage/ImageCodec.kt
interface ImageCodec {
    // maxDim is not optional: decoding a 12MP photo for a thumbnail is how this
    // codebase previously produced a 6 MB decode and an ANR.
    suspend fun decode(bytes: ByteArray, maxDim: Int): ImageBitmap?
    suspend fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray
    suspend fun crop(image: ImageBitmap, rect: IntRect): ImageBitmap
}
```

```kotlin
// shared/data/iosMain/…/IosPhotoStorage.kt
// Documents, not Caches: journal photos are local-only user data with no backend copy,
// and the system may purge Caches at any time.
class IosPhotoStorage : PhotoStorage {
    private val root = NSFileManager.defaultManager
        .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
    // …
}
```

## 7. Acceptance

- [ ] Both contracts in `:shared:domain`; both platforms registered
- [ ] Zero `android.graphics.Bitmap` references in `shared/ui/commonMain`
- [ ] Android: existing journal photos still resolve after the change — **verify by upgrade, not by unit test**
- [ ] iOS: photos land in Documents, survive relaunch and app update
- [ ] Avatar crop works on both platforms and produces the same output dimensions
- [ ] Task photo attach/upload works, including the offline `PendingPhoto` queue
- [ ] Journal photos display, and the polaroid develop animation runs
- [ ] `decode` honours `maxDim` — no full-size decode for a thumbnail
- [ ] No bitmap leak: repeatedly opening and closing the crop screen does not grow memory
- [ ] `AsyncImage` still sends the Bearer token for remote images on both platforms
- [ ] The iCloud-backup decision is recorded in `DECISIONS.md`

## 8. Pitfalls

- **Do not change Android path conventions.** Existing installs have photos at the current paths; changing them orphans real user data with no backend copy.
- **Documents, not Caches, on iOS.** Caches can be purged without warning.
- **`maxDim` is mandatory.** This codebase has an ANR in its history from a full-size decode.
- **`ImageBitmap`, not a platform bitmap, in common signatures.** It lets the Compose runtime own the lifetime and sidesteps the recycling anti-pattern.
- **`DisposableEffect` must capture the value, not the state.** `val toRecycle = bitmap` inside the effect — capturing the state reads the *new* value at dispose and recycles the wrong bitmap.
- **Coil 3 renamed things.** `SuccessResult` and `CachePolicy` moved; check each call site rather than trusting a global rename.
- **Do not put user photos in `composeResources`.** That is for bundled assets.
- **iOS file paths change between installs.** The app container path is not stable across reinstalls — **store relative paths and resolve at read time**. Storing absolute paths produces broken images after an update.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
grep -rn "android.graphics.Bitmap" shared/ui/src/commonMain && echo "BITMAP LEAKED" || echo "clean"

# Android — the upgrade path
#   install the previous build, add a journal photo, install this build over it → photo still there

# iOS, on hardware
#   journal photo, avatar crop, task photo attach
#   force-quit and reopen → all still there
#   open/close the crop screen 20 times → memory flat
```
