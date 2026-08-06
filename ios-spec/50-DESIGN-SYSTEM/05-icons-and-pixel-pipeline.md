---
id: 50-05
title: Icons & the pixel-art generation pipeline
layer: design
status: TODO
depends_on: [20-09]
blocks: []
parallel_safe: true
estimate: 12h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/image/**
  - app/src/main/java/com/todoapp/mobile/ui/common/AppPixelIcons.kt
  - tools/genpixelicons.py
  - tools/pixelart/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:testDebugUnitTest --tests '*PixelIconMapTest*'
---

## 1. Goal

Verify the 231 vector icons render correctly on iOS, and keep the pixel-variant generation pipeline working after the resource migration changed the key type from `Int` to `DrawableResource`.

## 2. Why this way

**There are no Material icons in this project.** Neither `material-icons-extended` nor `material-icons-core` is a dependency, and there are zero `androidx.compose.material.icons` imports. Every icon is a project drawable. That is a deliberate design decision and it means the icon set is a real asset with real porting weight — 143 in `:uikit`, 88 in `:app`.

**CMP parses Android VectorDrawable XML on every platform**, so the vectors move as-is. But "parses" is not "renders identically": gradient `aapt:attr` support has edges, and a silently-wrong icon is easy to miss among 231.

**The pixel pipeline is the part that actually breaks.** `LocalPixelIconMap` maps a source drawable id to its 8-bit variant, and `tdPainter(id)` swaps it when the PIXEL kit is active. Under `R` the key was an `Int`; under `Res` it is a `DrawableResource`. That change touches `PixelIcons.kt`, both map files, `tools/genpixelicons.py` and `PixelIconMapTest`.

**The generator has rules that must be respected.** The variants are generated, not hand-drawn. Hand-tuned ASCII grids under `tools/pixelart/<name>.txt` always win and are never overwritten. Icons the quality gate rejects simply get no map entry and fall back to the smooth vector — which is why `tdPainter` on an unmapped drawable is a no-op rather than a crash. And there is a recorded operational trap: the tool must be invoked with **all** drawable directories and all four `--kotlin-*` flags, or it silently deletes entries.

## 3. Source — read before writing

| Path | LOC | What to look for |
|---|---|---|
| `uikit/…/image/PixelIcons.kt` | — | `tdPainter(id)`, `tdIconRes(id)`, `LocalPixelIconMap` |
| `uikit/…/image/UikitPixelIcons.kt` | — | The uikit map — **generated, do not hand-edit** |
| `app/…/ui/common/AppPixelIcons.kt` | — | The app map — same |
| `uikit/…/image/PixelPainter.kt` | 181 | `rememberPixelPainter`, `rememberPixelImageModel`, `rememberPixelBitmap`, `tdPixelFilterQuality()` (nearest-neighbour in PIXEL) |
| `tools/genpixelicons.py` | — | The generator; read its header for the invocation contract |
| `tools/pixelart/*.txt` | 19+ | Hand-tuned grids (`#` on, `.` off) — **never overwritten** |
| `app/…/MainContent.kt` | — | Merges both maps into `LocalPixelIconMap` |
| `app/src/test/…/PixelIconMapTest.kt` | — | Guards the mapping |

## 4. Target

- `uikit/…/image/PixelIcons.kt` — `DrawableResource` keys
- Both map files — regenerated
- `tools/genpixelicons.py` — emits `Res.drawable.*` references
- `PixelIconMapTest` — updated for the new key type

## 5. Steps

1. **Update `tdPainter` and `LocalPixelIconMap`** to `Map<DrawableResource, DrawableResource>`.

2. **Update the generator** to emit `Res.drawable.*` references instead of `R.drawable.*`, with the correct import per module (two `Res` objects, mirroring the two former `R` classes).

3. **Re-run the generator with its full invocation** — all drawable directories and all four `--kotlin-*` flags. A partial invocation silently deletes map entries.

4. **Verify hand-tuned grids still win.** The `tools/pixelart/*.txt` files must not be overwritten and must take precedence.

5. **Update and run `PixelIconMapTest`.**

6. **Render an icon gallery** — all 231, both platforms, both themes — and compare. Look specifically for gradient icons and any using `aapt:attr`.

7. **Verify PIXEL swapping.** In the PIXEL kit, mapped icons show their 8-bit variant; unmapped ones fall back to the smooth vector without error.

8. **Verify nearest-neighbour filtering.** `tdPixelFilterQuality()` must produce hard pixel edges in PIXEL, not blurred upscaling.

9. **Check raster illustrations.** `rememberPixelPainter` downsamples rather than swapping — verify the 24 WebP illustrations render at the right size on both platforms.

## 6. Code skeleton

```kotlin
// Key type changes with the resource migration: Int -> DrawableResource.
val LocalPixelIconMap = staticCompositionLocalOf { emptyMap<DrawableResource, DrawableResource>() }

// An unmapped drawable resolves to itself — that is why wrapping an unmapped icon is a
// no-op rather than a crash or a blank.
@Composable
fun tdPainter(res: DrawableResource): Painter {
    val mapped = if (TDTheme.palette == PaletteKit.PIXEL) LocalPixelIconMap.current[res] ?: res else res
    return painterResource(mapped)
}
```

## 7. Acceptance

- [ ] All 231 vectors render correctly on both platforms, both themes
- [ ] Gradient / `aapt:attr` icons specifically checked
- [ ] `LocalPixelIconMap` keyed by `DrawableResource`; both maps regenerated
- [ ] `tools/genpixelicons.py` runs with its full invocation and produces the same mapping set as before
- [ ] Hand-tuned `tools/pixelart/*.txt` grids are unmodified and still take precedence
- [ ] `PixelIconMapTest` passes
- [ ] PIXEL kit: mapped icons swap; unmapped ones fall back silently
- [ ] Nearest-neighbour filtering gives hard edges in PIXEL — no blur
- [ ] All 24 WebP illustrations render at the correct size on both platforms
- [ ] App icon and launch screen configured on iOS

## 8. Pitfalls

- **Never hand-edit the generated map files.** Re-run the tool.
- **The generator's invocation contract is strict.** All drawable directories, all four `--kotlin-*` flags. A partial run silently deletes entries — a recorded incident.
- **Hand-tuned ASCII grids always win.** Do not let a regeneration overwrite them.
- **`Icons.Default.*` does not resolve in this project.** There are no Material icons. If an icon is missing, add a drawable.
- **Two `Res` objects.** `:uikit` icons come from uikit's `Res`, app icons from `:shared:resources`'. Mixing them is a compile error, which is the good case.
- **Nearest-neighbour matters.** Bilinear filtering on a pixel-art asset destroys the entire look.
- **The app icon is not a drawable.** iOS needs an asset catalog `AppIcon` set — separate work from the icon set.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest --tests '*PixelIconMapTest*'

# Regenerate with the FULL invocation (see the tool's header)
python3 tools/genpixelicons.py <all drawable dirs> --kotlin-... (all four flags)
git diff --stat tools/pixelart/    # must be empty — hand-tuned grids are never overwritten

# Visual: icon gallery, both platforms, both themes, ORIGINAL vs PIXEL
```
