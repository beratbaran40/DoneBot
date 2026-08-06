---
id: 40-settings-08
title: Security & secret mode
layer: ui
status: TODO
depends_on: [40-settings-01, 30-06]
blocks: []
parallel_safe: true
estimate: 6h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/security/**
  - shared/ui/src/commonMain/**/settings/SecretModeSettingsScreen.kt
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The biometric authenticator wrapper and the secret-mode settings.

## 2. Why this way

**Secret mode gates content across five surfaces** — home, calendar, search, filtered tasks and settings — so a bug here is not local to one screen. It ends automatically per `SecretModeEndCondition`, which is what makes it safe to leave enabled.

**Its privacy story changes on iOS, exactly like the journal's.** Android applies `FLAG_SECURE`; iOS cannot. The biometric gate still works, screenshot blocking does not, and the copy must reflect that.

`30-06` builds the platform contract; this is the settings surface around it.

## 3. Source

| Path | LOC |
|---|---|
| `ui/security/biometric/BiometricAuthenticator.kt` | 51 |
| `ui/settings/SecretModeSettingsScreen.kt` | the settings surface |
| `domain/security/` | `Authenticator`, `SecretModeConditionFactory`, `SecretModeEndEvent`, `SecretModeReopenOption` |
| `domain/usecase/security/IsSecretModeActiveUseCase.kt`, `OnSecretModeEventUseCase.kt` | the gates |
| `ui/common/SecureScreen.kt` → `30-15` | `blocksScreenshots` |

## 4. Target

`shared/ui/commonMain/…/ui/security/` and the secret-mode settings screen — verification.

## 5. Steps

1. Verify the files compile in `commonMain`.
2. Verify enabling secret mode requires authentication.
3. Verify secret content appears on all five surfaces when active.
4. Verify each `SecretModeEndCondition` ends it correctly.
5. Verify the journal biometric toggle.
6. **Verify no string claims screenshot protection on iOS.**
7. Verify iOS hides content in the app switcher on these surfaces.
8. Verify the no-biometrics-enrolled case falls back sensibly.
9. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] Files compile in `commonMain`
- [ ] Enabling secret mode requires authentication
- [ ] Secret content appears on **all five** surfaces
- [ ] Every end condition works
- [ ] Journal biometric toggle works
- [ ] **No screenshot-protection claim in any iOS string**
- [ ] App-switcher hiding works on iOS
- [ ] No-biometrics case falls back to passcode
- [ ] Three kits, two themes, two languages
- [ ] Previews cover enabled, disabled, unavailable

## 8. Pitfalls

- **Five surfaces, not one.** A gate missed on one screen leaks secret content.
- **`FLAG_SECURE` has no iOS equivalent.** Do not promise screenshot protection.
- **End conditions must actually fire.** A secret mode that never ends is a privacy hazard.
- **No enrolled biometrics must not lock the user out.** Fall back to passcode.
- **`MainActivity` must stay a `FragmentActivity`** for `BiometricPrompt`.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: enable secret mode, check all five surfaces, trigger each end
# condition, disable biometrics on the device and confirm the passcode fallback,
# grep the strings for screenshot claims
```
