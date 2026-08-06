---
id: 40-settings-07
title: Open-source licenses
layer: ui
status: TODO
depends_on: [40-settings-01]
blocks: []
parallel_safe: true
estimate: 3h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/licenses/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The open-source attribution screen, updated for the new dependency set.

## 2. Why this way

**The dependency list changes substantially in this migration**, and the license screen has to follow. Out: Hilt, Retrofit, Coil 2, Lottie, `maps-compose`. In: Koin, Ktor, Coil 3, compottie, and the iOS-side SDKs (Firebase iOS, GoogleSignIn iOS). Attribution is a licence obligation, not a courtesy.

**One nuance:** the Google Maps license text stays even after `maps-compose` is removed, because the Places SDK still ships Google code.

## 3. Source

| Path | LOC |
|---|---|
| `ui/licenses/LicensesScreen.kt` | 134 |
| `gradle/libs.versions.toml` | the source of truth for what ships |
| `ios-spec/20-MIGRATION/01-dead-deps-and-detektall.md` | what was removed and why the Maps text stays |

## 4. Target

`shared/ui/commonMain/…/ui/licenses/` — verification plus a content update.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. **Audit the list against `libs.versions.toml`** — remove what no longer ships, add what does.
3. Add the iOS-only SDKs.
4. Keep the Google Maps text — Places still ships Google code.
5. Verify the fonts are attributed: Poppins and Pixelify Sans are both OFL.
6. Verify scrolling and readability on both platforms.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] List matches the shipped dependency set
- [ ] Hilt, Retrofit, Coil 2, Lottie, maps-compose removed
- [ ] Koin, Ktor, Coil 3, compottie added
- [ ] iOS SDKs attributed
- [ ] Google Maps text retained
- [ ] Poppins and Pixelify Sans (OFL) attributed
- [ ] Scrolls and reads well on both platforms
- [ ] Three kits, two themes, two languages
- [ ] Preview present

## 8. Pitfalls

- **Attribution is a licence obligation.** Removing a library without removing its text is untidy; shipping a library without its text is a breach.
- **The Maps text stays** — Places still ships Google code.
- **Both fonts need OFL attribution.**
- **The iOS SDKs are shipped code too**, even though they come in via SPM.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Diff the license list against libs.versions.toml and the SPM manifest
```
