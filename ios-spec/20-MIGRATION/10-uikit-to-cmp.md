---
id: 20-10
title: `:uikit` → KMP + CMP
layer: design
status: TODO
depends_on: [20-09]
blocks: [20-11, 50-00]
parallel_safe: false
estimate: 45h
reversible: true
owner_files:
  - uikit/**
  - app/src/main/java/com/todoapp/mobile/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "! grep -rqE '^import android\\.' uikit/src/commonMain"
---

## 1. Goal

Convert `:uikit` (93 files, 16,455 LOC, 80 public `TD*` components) to a KMP + CMP module, and fix its namespace from `com.example.uikit` to `com.todoapp.uikit`.

## 2. Why this way

**This is the cleanest large module in the codebase, which makes it the right place to prove the CMP pattern before the 48k-line UI layer.** Measured: 37 files carry an Android-flavoured import, but **34 of those imports are `androidx.compose.ui.res.*`** (`stringResource` ×27, `painterResource` ×5, `vectorResource` ×2) — already handled by `20-09`'s resource migration. What is genuinely Android-bound is roughly twenty imports across a handful of files:

| Import | Uses | Where it lives |
|---|---|---|
| `android.os.Build` | 4 | version gates |
| `android.content.Context` | 3 | |
| `android.provider.Settings` | 2 | permission cards |
| `android.os.SystemClock` | 2 | |
| `android.content.Intent` | 2 | permission cards |
| `android.app.Activity` | 2 | `TDTheme` edge-to-edge |
| `android.graphics.Paint` + `BlurMaskFilter` | 2 | `NeumorphicShadow` |
| `android.text.format.DateFormat` | 1 | `util/TimeFormat.kt` |
| `android.content.res.Configuration` | 1 | `previews/TDCustomPreviews.kt` |

**The namespace fix is bundled here deliberately.** `com.example.uikit` is Android Studio scaffolding that never got cleaned up, and `:app` currently imports `com.example.uikit.R`. Since `20-09` already rewrote every one of those call sites to `Res`, the R-class references are gone — so the namespace change is now nearly free. Doing it any other time is a separate sweep.

**One thing genuinely regresses: previews.** `TDCustomPreviews.kt` builds its light+dark multipreviews on `android.content.res.Configuration.UI_MODE_NIGHT_*`. CMP's `@Preview` has no `uiMode`. The replacement takes `darkTheme` as an explicit parameter — a real API change across every preview in the module. Budget for it rather than discovering it late.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `uikit/build.gradle.kts` | — | Current namespace `com.example.uikit`, `minSdk 24`, detekt/ktlint wiring |
| `uikit/…/theme/TDTheme.kt` | 125 | `android.app.Activity`, `enableEdgeToEdge`, `LocalView.isInEditMode`. The composition-local plumbing is pure Compose and moves as-is. |
| `uikit/…/modifier/NeumorphicShadow.kt` | — | `android.graphics.Paint` + `BlurMaskFilter` — **no CMP equivalent** |
| `uikit/…/modifier/TdShadow.kt` | — | Branches on `elevationStyle`; SOFT → neumorphic, HARD → offset block |
| `uikit/…/modifier/GridBackground.kt` | — | `drawWithCache` + repeating `ImageShader`. Pure Compose — moves unchanged. |
| `uikit/…/theme/PixelCornerShape.kt` | 119 | `Outline.Generic` stair path. Pure Compose. |
| `uikit/…/util/TimeFormat.kt` | 19 | `DateFormat.is24HourFormat` → the `PlatformFormatting` contract from `20-04` |
| `uikit/…/previews/TDCustomPreviews.kt` | 79 | The 6 multipreview annotations built on `Configuration` |
| `uikit/…/components/TDFullscreenImageViewer.kt`, `TDGroupTaskCard.kt`, `image/PixelPainter.kt` | — | Coil 2 → Coil 3 |
| `uikit/…/extensions/ObscuredTouchGuard.kt` | — | `filterTouchesWhenObscured` — Android-only tapjacking guard |
| `uikit/src/main/res/values-night/themes.xml` | — | **Stays in `androidMain/res/`** — pre-Compose window theme |
| `app/…/theme/PaletteStyleTest.kt`, `PixelIconMapTest.kt` | — | uikit's invariants are tested from `:app`; they must keep passing |

## 4. Target

```
uikit/build.gradle.kts                    KMP + CMP, androidTarget() only
uikit/src/commonMain/kotlin/…             ~16.2k LOC destination
uikit/src/commonMain/composeResources/    (already moved in 20-09)
uikit/src/androidMain/kotlin/…            edge-to-edge actual, ObscuredTouchGuard, blur shadow actual
uikit/src/androidMain/res/values-night/   themes.xml — unmoved
```

## 5. Steps

1. **Convert `uikit/build.gradle.kts`** to KMP + CMP, `androidTarget()` only. Set `namespace = "com.todoapp.uikit"`.

2. **Move sources to `commonMain`.** Most files move untouched — the resource call sites were already rewritten in `20-09`.

3. **Split `TDTheme`.** The composition-local plumbing (7 locals, `remember(palette)` colour/style/typography construction) is pure Compose and stays common. The `enableEdgeToEdge` block becomes a platform call:
   ```kotlin
   @Composable expect fun SystemBarsEffect(darkTheme: Boolean)
   ```
   Android does what it does today; iOS is a no-op (status-bar style is handled by the hosting `UIViewController`).

4. **Handle `NeumorphicShadow`.** `BlurMaskFilter` has no CMP equivalent. Two options — pick one, record it in `DECISIONS.md`:
   - `expect`/`actual` the blur, Android keeping `BlurMaskFilter` and iOS using its own path; or
   - replace with `Modifier.blur()` in common code, accepting a visual difference on Android.

   **The `expect`/`actual` route is recommended** — this shadow is the ORIGINAL kit's signature look and a visual change on Android is a user-visible regression in a task that is supposed to be invisible.

   > Note the inverse constraint disappears on iOS: `PixelCornerShape` produces `Outline.Generic`, which cannot drive a platform elevation shadow below API 29 on Android. That limitation is Android-specific.

5. **`TimeFormat.kt`** → the `PlatformFormatting.uses24HourClock()` contract introduced in `20-04`.

6. **Rebuild the preview annotations.** Replace `uiMode`-based multipreviews with a `darkTheme`-parameterised wrapper. Every preview in the module changes signature. `CLAUDE.md` makes previews mandatory, so this is in-scope work, not cleanup.

7. **Coil 2 → Coil 3** in the three files that use it.

8. **Move `ObscuredTouchGuard` to `androidMain`.** `filterTouchesWhenObscured` is an Android tapjacking guard with no iOS analogue. The common API becomes a no-op modifier on other platforms.

9. **Fix every `com.example.uikit` reference** in `:app`.

10. **Verify purity, run the gate, confirm `PaletteStyleTest` and `PixelIconMapTest` still pass**, and visually check all three palette kits in both themes.

## 6. Code skeleton

```kotlin
// uikit/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget()          // iOS targets arrive in 20-13
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.coil3.compose)
            implementation(projects.shared.core)
        }
    }
}

android {
    namespace = "com.todoapp.uikit"      // was com.example.uikit — Studio scaffolding
    compileSdk = 36
    defaultConfig { minSdk = 24 }
}
```

```kotlin
// uikit/…/theme/TDTheme.kt — the Android-bound half becomes a platform call
@Composable expect fun SystemBarsEffect(darkTheme: Boolean)

@Composable
fun TDTheme(darkTheme: Boolean, palette: PaletteKit, content: @Composable () -> Unit) {
    SystemBarsEffect(darkTheme)
    // Everything below is pure Compose and unchanged:
    val lightColors = remember(palette) { palette.colors(dark = false) }
    val darkColors = remember(palette) { palette.colors(dark = true) }
    val style = remember(palette) { palette.style() }
    // … 7 composition locals
}
```

```kotlin
// previews/TDCustomPreviews.kt — the real API change
// Was: @Preview(uiMode = UI_MODE_NIGHT_YES) + @Preview(uiMode = UI_MODE_NIGHT_NO)
// CMP's @Preview has no uiMode, so the theme becomes explicit.
@Composable
fun TDPreviewContainer(content: @Composable () -> Unit) {
    Column {
        TDTheme(darkTheme = false, palette = PaletteKit.ORIGINAL) { content() }
        TDTheme(darkTheme = true, palette = PaletteKit.ORIGINAL) { content() }
    }
}
```

## 7. Acceptance

- [ ] `! grep -rqE '^import android\.' uikit/src/commonMain`
- [ ] `grep -rn "com.example.uikit" .` returns nothing
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] `PaletteStyleTest` and `PixelIconMapTest` pass unchanged
- [ ] All three palette kits render correctly in light **and** dark — colours, shapes, fonts, borders, shadows, grid
- [ ] The neumorphic shadow looks identical on Android to before (side-by-side screenshot)
- [ ] PIXEL kit: stair-stepped corners, 2dp borders, hard shadows, dither grid, Pixelify Sans all intact
- [ ] Every preview compiles and renders light + dark
- [ ] `values-night/themes.xml` still in `androidMain/res/`
- [ ] 12h/24h time display still follows the device setting
- [ ] `:app:bundleRelease` recorded

## 8. Pitfalls

- **The namespace change touches `:app`.** After `20-09` there should be no `com.example.uikit.R` left, but check — a missed reference is a compile error, which is the good case.
- **`BlurMaskFilter` has no CMP equivalent.** Do not silently swap in `Modifier.blur()` — the neumorphic shadow is ORIGINAL's signature and a change is user-visible. Decide explicitly and record it.
- **`values-night/themes.xml` stays.** It is the window theme that renders *before* Compose exists. `CLAUDE.md` flags it as not-cleanup. Moving it produces a white flash on cold start in dark mode.
- **Preview migration is real work, not a formality.** Six multipreview annotations and every preview in the module. `CLAUDE.md` treats missing previews as an incomplete change.
- **`minSdk` differs.** `:uikit` is 24, `:app` is 26. Keep 24 unless there is a reason.
- **`compose.components.resources`** must be on the module or `Res` will not generate.
- **Do not "fix" `PixelCornerShape`'s `Outline.Generic`.** The comment about platform shadows is an Android API-level constraint, not a bug, and the hard-shadow fallback depends on it.
- **Coil 3 renamed things.** `SuccessResult`, `CachePolicy` and the `ImageRequest` builder moved. Check every call site rather than trusting a global rename.

## 9. Verification

```bash
# 1. Purity + namespace
grep -rnE '^import android\.' uikit/src/commonMain && echo "NOT PURE" || echo "clean"
grep -rn "com.example.uikit" . --include="*.kt" --include="*.kts" --include="*.xml" && echo "OLD NAMESPACE" || echo "clean"

# 2. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Design-system invariants
./gradlew :app:testDebugUnitTest --tests '*PaletteStyleTest*' --tests '*PixelIconMapTest*'

# 4. Manual, on a device — all 3 kits × light/dark
#    Settings → App Colors → ORIGINAL / MONOCHROME / PIXEL
#    check: card corners, borders, shadows, grid background, font face,
#           icon variants (PIXEL swaps them), the theme-change reveal animation
#    compare the neumorphic shadow against a pre-task screenshot
```
