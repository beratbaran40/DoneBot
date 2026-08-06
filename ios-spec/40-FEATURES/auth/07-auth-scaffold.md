---
id: 40-auth-07
title: Auth scaffold (shared chrome)
layer: ui
status: TODO
depends_on: [20-11, 50-00]
blocks: [40-auth-01, 40-auth-02, 40-auth-03, 40-auth-04, 40-auth-05, 40-auth-06]
parallel_safe: false
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/auth/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Verify `AuthScaffold` and `AuthConsentFooter` on iOS. Every auth screen sits inside this, so it goes first.

## 2. Why this way

Auth screens live **outside** the root Scaffold in `NavGraph.kt`, which is why `AuthScaffold` applies `gridBackground` itself — it is one of only three places in the app that does. If that is wrong, every auth screen loses its palette texture at once.

It is also where `SecureScreenEffect()` is applied, and where the ToS/privacy consent footer lives — the latter is a store-listing requirement, not decoration.

## 3. Source

| Path | LOC |
|---|---|
| `ui/auth/AuthScaffold.kt` | ~180 |
| `ui/auth/AuthConsentFooter.kt` | ~80 |
| `uikit/…/modifier/GridBackground.kt` | the texture |
| `ui/common/SecureScreen.kt` → `30-15` | `blocksScreenshots` |
| `docs/screenshots/login/`, `register/` | references |

## 4. Target

`shared/ui/src/commonMain/…/ui/auth/` — verification after `20-11`.

## 5. Steps

1. Verify both files compile in `commonMain`.
2. Verify `gridBackground` renders — MONOCHROME graph paper, PIXEL dither, ORIGINAL plain.
3. Verify `SecureScreenEffect()` applies; on iOS that means app-switcher hiding only.
4. Verify the consent footer's links open the web view (`30-15`).
5. Verify keyboard insets — the footer must not be pushed off-screen when a field is focused.
6. Check both languages.

## 6. Code skeleton

```kotlin
// Auth screens live outside the root Scaffold, so this is one of only three places
// that applies the palette texture itself. Losing it here loses it on every auth screen.
Box(Modifier.fillMaxSize().gridBackground(TDTheme.colors.background, TDTheme.colors.gridLine)) {
    SecureScreenEffect()
    content()
    AuthConsentFooter()
}
```

## 7. Acceptance

- [ ] Both files compile in `commonMain`
- [ ] Grid texture correct in all three kits, light and dark, both platforms
- [ ] `SecureScreenEffect()` applied; iOS hides content in the app switcher
- [ ] Consent footer links open the web view
- [ ] Footer stays visible with the keyboard open
- [ ] Both languages
- [ ] Previews cover the scaffold with and without the footer

## 8. Pitfalls

- **Do not paint an opaque background here.** It occludes the grid — a documented sweep in this codebase.
- **The consent footer is a listing requirement**, not decoration. It must be present and its links must work.
- **Keyboard insets on iOS** need the same care as `imePadding` on Android; a footer pushed off-screen is a real bug on small devices.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: login and register, 3 kits, light + dark, EN + TR, keyboard open and closed
```
