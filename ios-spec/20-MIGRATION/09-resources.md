---
id: 20-09
title: `:shared:resources` + `R` → `Res`
layer: ui
status: TODO
depends_on: [20-03]
blocks: [20-10, 20-11]
parallel_safe: false
estimate: 75h
reversible: false
owner_files:
  - shared/resources/**
  - uikit/src/**
  - app/src/main/res/**
  - app/src/main/java/com/todoapp/mobile/**
  - .github/workflows/ci.yml
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "! grep -rqE 'R\\.(string|drawable|font|raw)\\.' app/src/main/java uikit/src/main/java"
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Move every user-facing resource into Compose Multiplatform `composeResources` and rewrite all ~2,000 call sites from Android `R` to the generated `Res`. Two `Res` objects, mirroring today's two `R` classes.

## 2. Why this way

Android `R` does not exist in `commonMain`. This blocks both `:uikit` and the 48k-line UI layer, so it must precede them.

**Measured volume:**

| Sweep | Count |
|---|---|
| `R.string.*` references | **1,346** (1,024 unique keys) |
| `R.drawable.*` references | **647** (244 unique) |
| `stringResource(` calls | 844 |
| `tdPainter(` / `painterResource(` | 291 / 20 |
| `pluralStringResource(` | 5 |
| String keys defined | 1,135 app + 73 uikit, × EN/TR |

Most of it is a codemod. Three things are not:

1. **Plurals.** Android `<plurals>` and CMP's plural handling differ. Only 7 call sites, but each needs checking — and Turkish uses only the `other` category, which is easy to get subtly wrong.
2. **Two `Res` objects, not one.** `:uikit` keeps its own 73 strings; `:shared:resources` holds the app's 1,135. This mirrors today's two `R` classes exactly, preserves the "uikit never references app strings" boundary that `CLAUDE.md` enforces, and turns the diff into a 1:1 rename instead of a merge.
3. **`values-night/` does not move.** `app/…/values-night/colors.xml` (cold-start splash background) and `uikit/…/values-night/themes.xml` (the window theme that renders before Compose does) are deliberately pre-Compose Android resources. `CLAUDE.md` calls them out as not-cleanup. They stay in `androidMain/res/`.

**There is a known AAB regression here, and it should be accepted knowingly.** CMP packages resources under `assets/composeResources/`, and `bundle { language { enableSplit = true } }` does not split assets. Per-language stripping is lost for these strings — roughly 100 KB with only `en` and `tr`. Small, but it is why the ceiling is raised in this task.

**Bonus cleanup:** 1,208 keys are defined but only 1,024 are referenced — about **184 dead keys**. Delete them here, in both languages.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `app/src/main/res/values/strings.xml` · `values-tr/strings.xml` | 1,135 strings + 9 plurals each |
| `uikit/src/main/res/values{,-tr}/strings.xml` | 73 each |
| `app/src/main/res/drawable{,-mdpi,-nodpi}/` | 88 `ic_*` vectors + `logo_text.xml` + 9 WebP |
| `uikit/src/main/res/drawable{,-mdpi,-nodpi}/` | 143 `ic_*` vectors + 15 WebP |
| `uikit/src/main/res/font/` | 6 TTFs (Poppins ×4, Pixelify Sans ×2) |
| `uikit/src/main/res/raw/confetti` | Lottie JSON, **extension-less** |
| `app/src/main/res/raw/ambience_*.ogg` | 3 loops — see Pitfalls |
| `uikit/…/image/PixelIcons.kt`, `UikitPixelIcons.kt`, `app/…/ui/common/AppPixelIcons.kt` | The pixel-icon maps keyed by drawable id |
| `tools/genpixelicons.py` | Regenerates the maps; must be updated for the new id type |
| `.claude/skills/check-l10n/SKILL.md` | The EN/TR parity audit; extend it |
| `app/src/main/res/values-night/`, `uikit/src/main/res/values-night/` | **Do not move these** |
| `.github/workflows/ci.yml` | `AAB_MAX_BYTES` — raised in this task |

## 4. Target

```
shared/resources/build.gradle.kts                             new
shared/resources/src/commonMain/composeResources/
    values/strings.xml            ← app values/strings.xml (minus dead keys)
    values-tr/strings.xml         ← app values-tr/strings.xml
    drawable/                     ← app drawables
    files/                        ← ambience audio
uikit/src/commonMain/composeResources/
    values{,-tr}/strings.xml      ← uikit strings
    drawable/                     ← uikit drawables
    font/                         ← 6 TTFs
    files/confetti.json           ← Lottie (note: gains an extension)
app/src/main/res/values-night/                                stays
uikit/src/main/res/values-night/                              stays
```

## 5. Steps

1. **Find the dead keys** and delete them from both languages:
   ```bash
   comm -23 \
     <(grep -oE 'name="[a-z0-9_]+"' app/src/main/res/values/strings.xml | sed 's/name="//;s/"//' | sort -u) \
     <(grep -rhoE 'R\.string\.[a-zA-Z0-9_]+' app/src/main/java uikit/src/main/java | sed 's/R\.string\.//' | sort -u)
   ```
   Review before deleting — a key referenced only from XML or built dynamically will not appear in that grep.

2. **Create `:shared:resources`** as a KMP + CMP module, `androidTarget()` only.

3. **Move `:uikit` resources first** — smaller, self-contained, and it proves the pattern. Then run the gate.

4. **Move app resources** into `:shared:resources`.

5. **Codemod the call sites.** Mechanical and scriptable, but review the diff:
   - `R.string.foo` → `Res.string.foo`
   - `R.drawable.ic_foo` → `Res.drawable.ic_foo`
   - `androidx.compose.ui.res.stringResource` → `org.jetbrains.compose.resources.stringResource`
   - `painterResource` → the CMP equivalent
   - `:app` files referencing uikit drawables move from `com.example.uikit.R.drawable.*` to the uikit `Res`

6. **Handle the 7 plural sites by hand.** Verify Turkish output for count 0, 1 and 2 in each.

7. **Update the pixel-icon maps.** `LocalPixelIconMap` is keyed by drawable id — an `Int` under `R`, a `DrawableResource` under `Res`. Update `PixelIcons.kt`, both map files, `tools/genpixelicons.py`, and `PixelIconMapTest`.

8. **Rename the Lottie file.** `uikit/res/raw/confetti` has no extension; under `composeResources/files/` give it `.json`. Update the loader.

9. **Extend the `check-l10n` skill** to diff `composeResources/values/strings.xml` against `values-tr/strings.xml`, and wire it into CI.

10. **Measure, then raise the ceiling deliberately.** Record the AAB before and after. Then set `AAB_MAX_BYTES` to 24 MiB with a comment recording the measured deltas (decision D-09). One deliberate change with a rationale — not a reaction to a red build.

## 6. Code skeleton

```kotlin
// Before
import com.todoapp.mobile.R
Text(stringResource(R.string.task_created))
Icon(painter = tdPainter(R.drawable.ic_check), contentDescription = null)

// After
import com.todoapp.mobile.resources.Res
import com.todoapp.mobile.resources.task_created
import org.jetbrains.compose.resources.stringResource
Text(stringResource(Res.string.task_created))
Icon(painter = tdPainter(Res.drawable.ic_check), contentDescription = null)
```

```kotlin
// The pixel-icon map changes key type: Int -> DrawableResource
val LocalPixelIconMap = staticCompositionLocalOf { emptyMap<DrawableResource, DrawableResource>() }

@Composable
fun tdPainter(res: DrawableResource): Painter =
    painterResource(
        if (TDTheme.palette == PaletteKit.PIXEL) LocalPixelIconMap.current[res] ?: res else res,
    )
```

```diff
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
-# Measured 18.17 MiB on 2026-08-05.
-AAB_MAX_BYTES: 20971520   # 20 MiB
+# Raised deliberately for the KMP/CMP migration (decision D-09). Measured deltas:
+#   Ktor replacing Retrofit              +X.XX MiB
+#   CMP resources (no asset lang-split)  +X.XX MiB
+#   Koin replacing Hilt                  -X.XX MiB
+# Baseline before migration was 18.17 MiB. Update this comment with each measurement.
+AAB_MAX_BYTES: 25165824   # 24 MiB
```

## 7. Acceptance

- [ ] `! grep -rqE 'R\.(string|drawable|font|raw)\.' app/src/main/java uikit/src/main/java`
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] All 7 plural sites verified in EN **and** TR at counts 0, 1, 2
- [ ] `PixelIconMapTest` passes; `tools/genpixelicons.py` runs and produces the same mappings
- [ ] Dead keys removed from **both** language files; counts still match exactly
- [ ] `check-l10n` reports EN/TR parity on the new layout and runs in CI
- [ ] `values-night/` still present in both modules, unmoved
- [ ] Confetti animation still plays on task completion
- [ ] Ambience audio still plays (all three loops)
- [ ] All three palette kits render correct icons, including PIXEL's swapped variants
- [ ] Measured AAB recorded; `AAB_MAX_BYTES` raised to 24 MiB with the rationale comment
- [ ] Manual sweep of every screen in **both** languages — no missing or English-in-Turkish strings

## 8. Pitfalls

- **Do not move `values-night/`.** Both files are deliberately pre-Compose: the cold-start splash background and the window theme that renders before Compose exists. `CLAUDE.md` names them explicitly as not-cleanup.
- **Two `Res` objects, not one.** Merging uikit's strings into the app's breaks the module boundary and turns a rename into a merge conflict across 1,200 keys.
- **CMP parses Android VectorDrawable XML on every platform**, so all 231 vectors move as-is — but **diff them visually anyway**. Gradient `aapt:attr` support has edges, and a silently-wrong icon is easy to miss.
- **Turkish plurals use only `other`.** Verify counts 0, 1 and 2 render correctly; `_zero` variants in this codebase are separate keys, not plural categories.
- **The Lottie file has no extension.** `raw/confetti` must become `files/confetti.json` and the loader updated, or the animation silently fails to load.
- **`.ogg` is unplayable on iOS.** Move the three loops now, but adding the `.m4a` siblings is `30-PLATFORM/05` — do not solve it here. Note it so nobody "fixes" it twice.
- **Alarm sounds are system ringtones, not bundled assets**, so nothing to move — but iOS needs bundled `.caf` files ≤30 s. Also `30-PLATFORM`, also not here.
- **`Res` accessors are compile-checked**, so a *missing* key fails the build. The real risks are silent value drift and a wrong-but-valid key. Review the codemod diff; do not just trust a green build.
- **Do not raise `AAB_MAX_BYTES` before measuring.** The number in the comment must be a measurement, not a guess.
- **Dead-key deletion needs review.** A key used only from XML, or assembled dynamically, will not show in the grep. Check before deleting.

## 9. Verification

```bash
# 1. No Android R left in Kotlin
grep -rnE 'R\.(string|drawable|font|raw)\.' app/src/main/java uikit/src/main/java && echo "R REMAINS" || echo "clean"

# 2. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Localization parity
#    run the check-l10n skill against the new composeResources layout

# 4. Size — measure, then set the ceiling
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 5. Pixel icons
./gradlew :app:testDebugUnitTest --tests '*PixelIconMapTest*'
python3 tools/genpixelicons.py   # with the flags documented in the tool's header

# 6. Manual, on a device, in BOTH languages
#    every screen; confetti on completion; all three ambience loops;
#    all three palette kits (ORIGINAL / MONOCHROME / PIXEL) with correct icons
```
