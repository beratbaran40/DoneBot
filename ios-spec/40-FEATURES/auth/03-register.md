---
id: 40-auth-03
title: Register
layer: ui
status: TODO
depends_on: [40-auth-07, 30-10, 50-04]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/register/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Sign-up with a live password-strength meter, plus Google and Apple registration.

## 2. Why this way

Register mirrors login and shares its success path — `RegisterViewModel` runs the same seven-step sequence. The one thing it adds is `TDPasswordStrengthIndicator`, a live-updating component driven by every keystroke, which makes it the second-best place after the chat composer to notice iOS text-input latency.

`redirectAfterRegister` behaves like login's redirect and carries the same persistence requirement.

## 3. Source

| Path | LOC |
|---|---|
| `ui/register/` (5 files) | 971 |
| `ui/register/TDPasswordStrengthIndicator.kt` | the live meter |
| `ui/register/RegisterViewModel.kt` (~187) | the success path |
| `docs/screenshots/register/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/register/` — verification plus the Apple button.

## 5. Steps

1. Verify it compiles in `commonMain`.
2. Verify registration end to end against the live backend.
3. Verify the strength meter updates per keystroke without lag on iOS.
4. Verify Google and Apple registration.
5. Verify the same seven-step success sequence as login.
6. Verify validation errors — email format, weak password, duplicate email — all localized.
7. Verify `redirectAfterRegister`.
8. Both languages.

## 7. Acceptance

- [ ] Compiles in `commonMain`
- [ ] Registration works on both platforms
- [ ] Strength meter updates smoothly per keystroke on iOS
- [ ] Google and Apple registration work
- [ ] The success sequence matches login's exactly
- [ ] All validation errors localized in EN and TR
- [ ] `redirectAfterRegister` works
- [ ] Previews cover idle, typing (each strength level), loading and error

## 8. Pitfalls

- **The strength meter is a per-keystroke recomposition.** If iOS text input lags anywhere, it shows here first.
- **Duplicate-email handling must be specific.** A generic "registration failed" leaves the user stuck.
- **Same success path as login.** Do not fork it.
- **Password rules must match the backend**, or the client accepts something the server rejects.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: register, weak password, duplicate email, Google, Apple, EN + TR
```
