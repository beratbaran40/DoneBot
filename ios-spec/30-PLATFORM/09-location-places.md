---
id: 30-09
title: Place search
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-shared-02]
parallel_safe: true
estimate: 12h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/location/**
  - shared/data/src/androidMain/**/location/**
  - shared/data/src/iosMain/**/location/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Implement place search on iOS with **MapKit**, behind the existing `PlaceSearchRepository` contract.

## 2. Why this way

**The contract already exists and is already platform-neutral.** `domain/location/PlaceSearchRepository` returns `PlacePrediction` and `PickedPlace` — no Android types. This is the port working as designed.

**MapKit over the Google Places iOS SDK, for three concrete reasons.**

1. **No API key.** `MKLocalSearch` is free and requires no key, no quota and no billing. The Google path would need a *separate* iOS-restricted key in the same GCP project — and this repo's git history is a known exposure (`google-services.json` is deliberately committed), so every additional key is another thing to restrict and monitor.
2. **Smaller.** No third-party SDK.
3. **Better on the platform.** iOS users expect Apple Maps results, and tapping through opens Apple Maps.

The cost is a **different result set** — Apple and Google disagree about places, especially outside major cities. For a task app where location is a note attached to a task, that is acceptable. Record it in `DECISIONS.md`.

**No location permission on either platform.** The Android manifest deliberately `tools:node="remove"`s `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` — the picker is text-search only, which keeps the Play Data Safety declaration simple. iOS must match: **do not add `NSLocationWhenInUseUsageDescription`.** `MKLocalSearch` works without it; adding it means declaring location collection on the App Store listing for no feature gain.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `domain/location/PlaceSearchRepository.kt` | The contract — already clean |
| `domain/location/model/PickedPlace.kt`, `PlacePrediction.kt` | The value types |
| `domain/usecase/location/GetPlacePredictionsUseCase.kt`, `GetPlaceDetailsUseCase.kt` | The two operations |
| `data/location/PlaceSearchRepositoryImpl.kt` (74 LOC) | Places SDK autocomplete; the missing-key path logs a warning and disables autocomplete |
| `di/PlacesModule.kt` | Deliberately lazy `@Provides` |
| `ui/common/locationpicker/` (5 files) | The picker UI |
| `ui/common/TaskLocationIntent.kt` | Opens the platform maps app |
| `app/src/main/AndroidManifest.xml` lines 21-25 | The `tools:node="remove"` permissions and why |

## 4. Target

- `shared/data/androidMain/…/PlacesSearchRepository.kt` — the existing implementation
- `shared/data/iosMain/…/MapKitSearchRepository.kt` — `MKLocalSearchCompleter` + `MKLocalSearch`

## 5. Steps

1. **Move the existing implementation to `androidMain`** behind the contract. No change.

2. **iOS: `MKLocalSearchCompleter`** for predictions as the user types (`resultTypes = .address | .pointOfInterest`), and `MKLocalSearch` to resolve a completion into coordinates.

3. **Debounce.** The completer fires on every keystroke; the picker already debounces on Android — mirror it.

4. **Map both platforms' results onto the same `PlacePrediction` shape.** Any divergence here shows up as different UI behaviour between platforms for no reason.

5. **`TaskLocationIntent` on iOS** opens Apple Maps via `maps://?q=...&ll=lat,lng`, routed through the `ExternalLinks` contract (`30-15`).

6. **Do not add any location permission.** The picker never asks for the user's location.

7. **Handle no results and no network** the same way Android does — the picker already has an empty state.

## 6. Code skeleton

```kotlin
// shared/data/iosMain/…/MapKitSearchRepository.kt
// MKLocalSearch needs no API key, no quota and no billing — which also means one
// fewer credential to restrict, and this repo's git history is a known exposure.
class MapKitSearchRepository : PlaceSearchRepository {

    private val completer = MKLocalSearchCompleter().apply {
        resultTypes = MKLocalSearchCompleterResultTypeAddress or
            MKLocalSearchCompleterResultTypePointOfInterest
    }

    override suspend fun predictions(query: String): List<PlacePrediction> =
        suspendCancellableCoroutine { cont ->
            completer.queryFragment = query
            // delegate → map MKLocalSearchCompletion into PlacePrediction
        }

    override suspend fun details(id: String): PickedPlace? {
        val request = MKLocalSearchRequest().apply { naturalLanguageQuery = id }
        // MKLocalSearch(request).startWithCompletionHandler { … } → PickedPlace
        return null
    }
}
```

## 7. Acceptance

- [ ] `PlaceSearchRepository` unchanged; both implementations registered
- [ ] Android behaviour unchanged
- [ ] iOS: typing in the picker returns predictions
- [ ] Selecting a prediction resolves name, address and coordinates
- [ ] The location is stored on the task and displayed on the detail screen
- [ ] Tapping the location opens Apple Maps at the right coordinates
- [ ] **No location permission requested on iOS**; no `NSLocation*UsageDescription` in `Info.plist`
- [ ] No Google Places SDK on the iOS side
- [ ] Empty state and offline behaviour match Android
- [ ] Search is debounced — no request per keystroke
- [ ] The result-set difference is recorded in `DECISIONS.md`

## 8. Pitfalls

- **Do not add a location permission.** It would change the App Store privacy declaration for a feature that never uses the device's location. Android deliberately removes the same permissions.
- **`MKLocalSearchCompleter` is delegate-based and fires repeatedly.** Bridge it to a `Flow` or a `Channel`, not a one-shot continuation — a `suspendCancellableCoroutine` will resume on the first callback and lose the rest.
- **Debounce, or you will hammer MapKit** and the UI will flicker.
- **Results differ from Google's.** Expected. Do not try to normalise them into agreement.
- **`maps://` for Apple Maps.** `http://maps.apple.com/` also works and is a safer fallback if the scheme is unavailable.
- **The location columns are already a known trap.** They were once missing from `comparableFields()` in `TaskRepositoryImpl` and off-device edits to them died silently. Verify a location set on one device reaches another.
- **Do not add the Google Places iOS SDK "for parity".** It costs a key, a quota, an SDK and a monitoring obligation.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
grep -rn "NSLocation" iosApp/ && echo "LOCATION PERMISSION DECLARED" || echo "clean"

# On a device
#   task form → add location → type → predictions appear
#   select one → name, address, coordinates stored
#   task detail → tap the location → Apple Maps opens at the right place
#   set a location on iOS, sync, open on Android → it is there
#   airplane mode → graceful empty state
```
