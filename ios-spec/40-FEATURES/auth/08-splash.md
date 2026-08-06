---
id: 40-auth-08
title: Splash
layer: ui
status: TODO
depends_on: [20-11, 10-03]
blocks: [40-auth-01]
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/splash/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The animated Compose splash, plus the iOS launch screen that renders before Compose exists.

## 2. Why this way

**There are two splashes, and only one of them is Compose.** Android has the `core-splashscreen` system splash (backed by `values-night/colors.xml` for the dark cold-start background) *and* an animated Compose splash on top of it. iOS has a static launch storyboard or asset, which cannot animate at all.

The Android `values-night/colors.xml` exists precisely because the system splash renders before Compose can theme anything. iOS has the same problem and the same answer: a static launch screen whose background matches the theme, chosen by the system's light/dark setting rather than the app's.

**That means a mismatch is possible on both platforms** — the app's in-app theme toggle is independent of the system setting, so a user in system-light with the app set to dark will see a light launch screen followed by a dark app. Android already lives with this; iOS will too. Do not try to solve it.

## 3. Source

| Path | LOC |
|---|---|
| `ui/splash/SplashScreen.kt` | 161 |
| `app/src/main/res/values-night/colors.xml` | the cold-start background — **stays Android-only** |
| `app/src/main/res/drawable-nodpi/` | splash WebP |
| `MainActivity.kt` | `installSplashScreen()` |

## 4. Target

- `shared/ui/commonMain/…/ui/splash/` — the Compose splash
- `iosApp/Assets.xcassets` — launch screen asset with light and dark variants

## 5. Steps

1. Verify the Compose splash compiles in `commonMain`.
2. Create the iOS launch screen with light and dark variants matching the Android cold-start colours.
3. Verify the handoff: launch screen → Compose splash → first real screen, with no flash between.
4. Verify the splash does not block on network — it must not wait for a sync.
5. Check the logged-in and logged-out branches route correctly.

## 6. Code skeleton

```
iOS launch screen: static, system light/dark only. It cannot follow the app's
in-app theme toggle — the same limitation values-night/colors.xml works around
on Android. A mismatch for users whose in-app theme differs from the system is
expected and is not worth solving.
```

## 7. Acceptance

- [ ] Compose splash compiles in `commonMain`
- [ ] iOS launch screen present with light and dark variants
- [ ] No visible flash between launch screen and Compose splash
- [ ] Splash does not block on network
- [ ] Logged-in routes to Home; logged-out routes to Onboarding
- [ ] `values-night/colors.xml` untouched on Android

## 8. Pitfalls

- **Do not move `values-night/colors.xml`.** It is deliberately pre-Compose; moving it produces a white flash on Android dark cold start.
- **Do not block the splash on a network call.** Cold start against a sleeping Render dyno can take seconds.
- **The iOS launch screen cannot animate.** Do not try.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Cold start on both platforms, light and dark, logged in and out; watch for a flash
```
