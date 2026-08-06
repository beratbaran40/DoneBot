---
id: 40-shared-02
title: Location picker
layer: ui
status: TODO
depends_on: [30-09, 50-04]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/common/locationpicker/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The full-screen place picker used by the task form.

## 2. Why this way

**It never asks for the user's location**, on either platform. The Android manifest deliberately removes both location permissions and the picker is text-search only, which keeps the Play Data Safety declaration free of location claims. iOS must match — no `NSLocation*UsageDescription`.

`30-09` swaps the Places SDK for MapKit on iOS. Results will differ between platforms; that is expected and recorded.

**The four location columns have a recorded history.** They were once missing from `comparableFields()` in `TaskRepositoryImpl`, so off-device edits to them died silently. Verify a location set on one platform reaches the other.

## 3. Source

| Path | LOC |
|---|---|
| `ui/common/locationpicker/` (5 files) | part of `ui/common`'s 3,478 |
| `ui/common/LocationPickerLauncher.kt`, `TaskLocationIntent.kt` | entry and hand-off to maps |
| `uikit/…/TDLocationPicker.kt` | the field |
| `data/repository/TaskRepositoryImpl.kt` | `comparableFields()` — the four location columns |

## 4. Target

`shared/ui/commonMain/…/ui/common/locationpicker/` — verification.

## 5. Steps

1. Verify all 5 files compile in `commonMain`.
2. Verify search returns predictions on both platforms.
3. Verify selecting one stores name, address, latitude and longitude.
4. **Verify a location set on iOS appears on Android** and vice versa — the `comparableFields()` check.
5. Verify clearing a location works (and, for group tasks, uses `clearLocation`).
6. Verify tapping the location opens the platform maps app.
7. **Verify no location permission is requested** on either platform.
8. Verify search is debounced and the empty state renders.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 5 files compile in `commonMain`
- [ ] Search returns predictions on both platforms
- [ ] Selection stores all four fields
- [ ] **Locations sync in both directions**
- [ ] Clearing works, including the group `clearLocation` flag
- [ ] Tapping opens the platform maps app
- [ ] **No location permission requested; no `NSLocation*` key in `Info.plist`**
- [ ] Search debounced; empty state renders
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, results, selected

## 8. Pitfalls

- **Do not add a location permission.** It changes the store privacy declaration for a feature that never uses the device's location.
- **The four location columns must be in `comparableFields()`.** They were once missing, and edits died silently.
- **Group tasks need `clearLocation`** — an omitted field means unchanged.
- **Debounce the search.**
- **Results differ between MapKit and Places.** Expected.

## 9. Verification

```bash
grep -rn "NSLocation" iosApp/ && echo "PERMISSION DECLARED" || echo "clean"
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Cross-platform: set a location on iOS, sync, confirm on Android; clear it,
# confirm the clear propagates; tap to open maps
```
