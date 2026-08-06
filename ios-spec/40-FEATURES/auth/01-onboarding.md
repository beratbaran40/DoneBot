---
id: 40-auth-01
title: Onboarding
layer: ui
status: TODO
depends_on: [40-auth-07, 40-auth-08]
blocks: [40-auth-02]
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/onboarding/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The first-run pager, and the guest-mode entry that makes an account optional.

## 2. Why this way

**Onboarding is the start destination when logged out**, and its "Get Started" action navigates straight to Home with no account — guest mode is a first-class path, not an afterthought. Breaking that would gate the entire app behind sign-up, which is a product change, not a port.

The four illustrations are nodpi WebP that moved to `composeResources` in `20-09`; verify they render at the right size on both platforms.

## 3. Source

| Path | LOC |
|---|---|
| `ui/onboarding/` (4 files) | 441 |
| `ui/onboarding/OnboardingViewModel.kt` (~35-39) | `OnGetStartedClick` → Home, popping Onboarding |
| `app/src/main/res/drawable-nodpi/onboarding{1..4}.webp` | the illustrations |
| `docs/screenshots/onboarding/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/onboarding/` — verification.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify the pager swipes and the indicator tracks on both platforms — iOS swipe physics differ.
3. Verify all four illustrations render at the right size.
4. Verify "Get Started" enters guest mode: Home, with Onboarding popped from the back stack.
5. Verify the sign-in path also works.
6. Verify onboarding does not reappear after being seen.
7. Both languages, three kits.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] Pager swipes smoothly on iOS; indicator correct
- [ ] All four illustrations at correct size and aspect
- [ ] **Guest mode works** — Home reachable with no account, Onboarding popped
- [ ] Sign-in path reaches login
- [ ] Seen-state persists; onboarding does not reappear
- [ ] Both languages, three kits, light and dark
- [ ] Previews cover all four pages

## 8. Pitfalls

- **Guest mode is load-bearing.** Tasks, pomodoro, journal and on-device chat all work without an account. Gating the app behind sign-up is a product change.
- **iOS pager physics differ.** Verify the swipe feels right rather than assuming.
- **The illustrations are large WebP.** Check they are not being decoded at full size for a small view.
- **Popping Onboarding matters.** Leaving it on the back stack means back from Home exits to onboarding.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Fresh install on both platforms: swipe all four, Get Started → Home, back → exits app not onboarding
```
