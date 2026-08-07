---
id: 20-11
title: `:shared:ui` + `:composeApp`
layer: ui
status: TODO
depends_on: [20-04, 20-06, 20-07, 20-08, 20-10]
# Also blocks every 40-* feature task — see 40-FEATURES/00-feature-index.md.
# Globs are not valid ids; only real ids belong in this list.
blocks: [20-12, 50-06, 40-index]
parallel_safe: false
estimate: 130h
reversible: false
owner_files:
  - shared/ui/**
  - composeApp/**
  - app/src/main/java/com/todoapp/mobile/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "! grep -rqE '^import android\\.' shared/ui/src/commonMain composeApp/src/commonMain"
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Move the 48,405-line UI layer and the composition root into shared CMP modules. This is the largest task in the migration by a factor of two.

## 2. Why this way

**Everything this task needs already exists.** Resources are `Res` (`20-09`), the design system is CMP (`20-10`), dates are `kotlinx-datetime` (`20-04`), DI is Koin (`20-05`), networking is Ktor (`20-06`), persistence is Room KMP (`20-07`). What remains is the UI itself plus the platform escapes.

**Measured Android coupling in `ui/`** — this is the actual work list:

| Import | Uses | Disposition |
|---|---|---|
| `android.content.Context` | 33 | mostly incidental; remove or route through a contract |
| `android.widget.Toast` | 28 | **replace with the in-app snackbar** — do not `expect`/`actual` it |
| `android.graphics.Bitmap` | 12 | `ImageCodec` contract (`30-PLATFORM/08`) |
| `android.content.Intent` | 11 | `ExternalLinks` contract (`30-PLATFORM/15`) |
| `android.net.Uri` | 10 | plain `String` at the boundary |
| `android.os.Build` | 9 | version gates → capability flags |
| `android.provider.Settings` | 6 | `PermissionController` / `ExternalLinks` |
| `android.util.Log` | 5 | `Logger` contract |
| `android.app.Activity` | 5 | resolved platform-side |
| `android.webkit.*` | 3 | `WebViewHost` contract |
| `android.view.WindowManager` | 2 | `ScreenBehavior` (secure flag, keep-screen-on) |
| `android.util.Patterns` | 2 | replace with a shared regex |

**Toast deserves its own note.** 28 call sites, and the reflex is to `expect`/`actual` it. Do not. Toast is an Android affordance with no iOS equivalent, and iOS users read a floating grey pill as a bug. The codebase already has `TDUndoSnackbar` and an in-app snackbar pattern. Converting these is a small UX improvement on Android and the only correct answer on iOS.

**Burn it down by leverage, not by file order:** `:uikit` (done) → the 5 bottom-bar destinations (Home, Groups, Chat, Calendar, Activity) → the auth flow → the long tail. Track *screens compiling in `commonMain` / 46*. That metric tells you whether you are halfway; "files moved" does not.

## 3. Source — read before writing

| Path | LOC | Note |
|---|---|---|
| `app/…/ui/` | 48,405 | 39 packages, 46 Screen/ViewModel pairs |
| `app/…/navigation/NavGraph.kt` | 1,128 | Largest file. 43 `composable<Screen.X>` blocks, per-route `navEffect` collection, per-app locale switching (~lines 273-285), 12 `hiltViewModel()`→`koinViewModel()` |
| `app/…/navigation/Screen.kt` | 181 | 43 `@Serializable` destinations. **R8-name-sensitive** — renaming makes the bars vanish. |
| `app/…/navigation/AppDestination.kt` | 358 | `bottomBarItems` (5) + `topBarItems` (34) + `hasInfoDialog` |
| `app/…/MainActivity.kt` | — | `calculateWindowSizeClass(this)` → CMP's no-arg form |
| `app/…/MainContent.kt`, `MainViewModel.kt`, `MainContract.kt`, `ThemeViewModel.kt` | 850 | The composition root |
| `app/…/ui/common/` | 3,478 | 33 files of shared UI — `SecureScreen`, `LockScreenOrientation`, `FeedbackIntent`, `TaskLocationIntent`, `ImageCropOverlay`, the task form, the location picker |
| `app/…/ui/journal/camera/LiveCameraPreview.kt` | 176 | The **only** CameraX-bound file; the ~1,900 LOC of polaroid Canvas drawing is pure Compose |
| `app/…/ui/webview/` | 221 | `android.webkit.WebView` |
| `app/…/ui/overlay/OverlayService.kt` | ~460 | **Stays in `:app`** — a Service, not a composable surface |

## 4. Target

```
shared/ui/src/commonMain/kotlin/…      46 Screen/VM pairs + Screen.kt + AppDestination.kt
shared/ui/src/androidMain/kotlin/…     LiveCameraPreview (AndroidView), remaining escapes
composeApp/src/commonMain/kotlin/…     App(), NavGraph, MainViewModel, ThemeViewModel,
                                       TDBottomBar, TDNavigationRail, ThemeChangeReveal,
                                       Koin module aggregation
composeApp/src/androidMain/kotlin/…    androidKoinModule, MainContent entry
```

`Screen.kt` and `AppDestination.kt` go in `:shared:ui`, **below** `NavGraph` — `HomeViewModel` and others import `Screen`, so it cannot live in `:composeApp`.

## 5. Steps

1. **Create `:shared:ui` and `:composeApp`**, both KMP + CMP, `androidTarget()` only.

2. **Move `Screen.kt`, `AppDestination.kt` and `NavigationEffect` first.** Everything else depends on them.

3. **Swap the ViewModel and navigation stacks:**
   - `androidx.lifecycle:lifecycle-viewmodel` 2.10 already publishes KMP artifacts
   - add `lifecycle-viewmodel-compose` for `viewModel()`/`koinViewModel()`
   - add `lifecycle-runtime-compose` for `collectAsStateWithLifecycle` (12 uses)
   - `SavedStateHandle` (17 uses) is KMP via `androidx.savedstate` 1.3+
   - navigation: `org.jetbrains.androidx.navigation:navigation-compose`. `Screen.kt` already uses `@Serializable` type-safe routes and `toRoute` (16 uses), so this ports nearly verbatim.

4. **Move package by package, gate green between each.** Order: `ui/common` → the 5 bottom-bar destinations → auth → the long tail.

5. **Replace all 28 Toasts** with the in-app snackbar.

6. **Route the remaining Android escapes** through the contracts defined in `30-PLATFORM`. Where a contract does not exist yet, put the file in `androidMain` and record the blocking import — that is the ratchet working.

7. **`MainActivity` reduces to** `setContent { App() }`. `calculateWindowSizeClass(this)` becomes CMP's no-arg `calculateWindowSizeClass()`, which makes `LocalWindowSizeClass` platform-free and unblocks the iPad work in `50-06`.

8. **Coil 2 → Coil 3** across the 22 call sites. `Application : coil.ImageLoaderFactory` becomes `setSingletonImageLoaderFactory { }` inside `App()`.

9. **Lottie → compottie** for the confetti animation.

10. **Verify `sh.calvin.reorderable` publishes iOS artifacts** before relying on it (5 call sites: `HomeContent`, `HomeTaskList`, `GroupScreen`). If it does not, that is a `BLOCKED` + `BLOCKERS.md` entry, not a silent substitution.

11. **Full gate after every package.** Track the burn-down metric.

## 6. Code skeleton

```kotlin
// composeApp/src/commonMain/…/App.kt — the shared composition root
@Composable
fun App() {
    KoinContext {
        setSingletonImageLoaderFactory { context -> newImageLoader(context) }   // was Application : ImageLoaderFactory
        val themeViewModel: ThemeViewModel = koinViewModel()
        val theme by themeViewModel.theme.collectAsStateWithLifecycle()
        val palette by themeViewModel.palette.collectAsStateWithLifecycle()
        TDTheme(darkTheme = theme.isDark, palette = palette) {
            DoneBotApp()      // NavGraph.kt
        }
    }
}
```

```kotlin
// app/…/MainActivity.kt — reduced to a host
class MainActivity : FragmentActivity() {          // FragmentActivity: BiometricPrompt needs it
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); forwardPushIntent(intent) }
}
```

```kotlin
// Toast → snackbar. 28 sites. Do NOT expect/actual Toast.
// Before: Toast.makeText(context, R.string.task_created, Toast.LENGTH_SHORT).show()
// After:  effect emits UiEffect.ShowSnackbar(Res.string.task_created); the screen's
//         LaunchedEffect feeds the existing snackbar host.
```

## 7. Acceptance

- [ ] `! grep -rqE '^import android\.' shared/ui/src/commonMain composeApp/src/commonMain`
- [ ] All 46 Screen/ViewModel pairs compile in `commonMain` (or are recorded in `androidMain` with a named blocking import)
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] Zero `Toast` references in `shared/ui`
- [ ] All 43 navigation destinations reachable; deep links still route
- [ ] Bottom bar (5) and top bar (34) render on the right screens; full-bleed screens still have no top bar
- [ ] `LocalWindowSizeClass` is platform-free; tablet two-pane and navigation rail still work
- [ ] All 22 Coil call sites migrated; avatars, task photos, group avatars all load with auth
- [ ] Confetti still plays via compottie
- [ ] Drag-to-reorder still works on all 5 sites
- [ ] `:app:bundleRelease` recorded
- [ ] Manual regression across every screen in **both** languages and all three palette kits

## 8. Pitfalls

- **Do not rename anything in `navigation/Screen.kt`.** R8 matches on route names; renaming a destination makes the top and bottom bars silently vanish in release builds. This has happened before — it is in `CLAUDE.md`'s gotcha list.
- **Do not `expect`/`actual` Toast.** 28 sites, snackbar instead. A floating grey pill on iOS reads as a bug.
- **`MainActivity` must stay a `FragmentActivity`.** `BiometricPrompt` requires it. "Simplifying" to `ComponentActivity` breaks the journal lock and secret mode.
- **`OverlayService` stays in `:app`.** It is a Service that hosts a ComposeView with a manual `LifecycleRegistry` and `SavedStateRegistry`. It is not a shared UI surface, and iOS has no equivalent at all (`40-FEATURES/misc/overlay-replacement`).
- **The polaroid camera is mostly portable.** ~1,900 LOC of Canvas drawing is pure Compose and moves unchanged; only `LiveCameraPreview.kt` (176 LOC) is CameraX-bound. Do not rewrite the drawing.
- **`UiState.Success` must not be clobbered on refresh.** `CLAUDE.md` documents the photo-picker RESUMED→STARTED→RESUMED cycle that triggers `loadData()` and wipes open sheets. Preserve the copy-from-previous-Success pattern wherever it exists.
- **Do not grow oversized files.** `NavGraph.kt` (1,128), `HomeContent.kt` (~1,090), `HomeViewModel.kt` (~1,110) are already over budget. Moving them is fine; adding to them is not. Extract while you are in there.
- **Per-app locale switching is Android-specific.** `NavGraph.kt` lines ~273-285 use `LocaleManager` / `AppCompatDelegate`. Route through the `LocaleController` contract (`30-PLATFORM/14`).
- **`collectAsStateWithLifecycle` needs `lifecycle-runtime-compose`**, a separate artifact from `lifecycle-viewmodel-compose`. Missing it produces 12 unresolved references.
- **Verify third-party iOS artifacts before depending on them.** `sh.calvin.reorderable` in particular.

## 9. Verification

```bash
# 1. Purity
grep -rnE '^import android\.' shared/ui/src/commonMain composeApp/src/commonMain && echo "NOT PURE" || echo "clean"

# 2. Toast is gone
grep -rn "android.widget.Toast\|Toast.makeText" shared/ui && echo "TOASTS REMAIN" || echo "clean"

# 3. Full gate + size
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. Burn-down metric
echo "screens in commonMain: $(ls shared/ui/src/commonMain/kotlin/com/todoapp/mobile/ui/**/*Screen.kt 2>/dev/null | wc -l) / 46"

# 5. Manual — the full regression, both languages, all three kits
#    every one of the 43 destinations; deep links; bottom + top bar;
#    tablet two-pane and rail; image loading with auth; confetti; drag-to-reorder
```
