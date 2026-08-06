---
id: 40-misc-05
title: Update prompt
layer: ui
status: TODO
depends_on: [30-15]
blocks: []
parallel_safe: true
estimate: 4h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/update/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Tell users when a newer version is available, on both stores.

## 2. Why this way

**The mechanism differs completely between platforms.** Android uses Play's in-app update API, which knows authoritatively what is published. iOS has no equivalent — the closest is the iTunes lookup API, which is a public endpoint returning the current App Store version.

**That endpoint is cached and can be stale for hours after a release**, so it is a hint, not a gate. Building a forced-update flow on it would strand users on a version the API has not caught up with.

`domain/update/AppUpdateChecker` already abstracts this, so both implementations sit behind the existing interface and the UI is unchanged.

**A no-op iOS implementation is an acceptable 1.0 outcome.** Record the decision either way.

## 3. Source

| Path | LOC |
|---|---|
| `ui/update/` (3 files) | 199 |
| `domain/update/AppUpdateChecker.kt` | the interface |
| `data/update/PlayAppUpdateChecker.kt` | 61 — Android |
| `uikit/…/TDUpdateAvailableDialog.kt` | the shared dialog |

## 4. Target

`shared/ui/commonMain/…/ui/update/` — verification plus the iOS implementation.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify Android in-app update behaviour is unchanged.
3. Implement the iOS checker against the iTunes lookup API, comparing against `AppInfo.appVersionName`.
4. Verify the dialog appears when a newer version exists and deep-links to the App Store page.
5. Verify dismissal is respected — do not nag on every launch.
6. Verify a network failure fails silently; this is never blocking.
7. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Android unchanged
- [ ] iOS detects a newer App Store version
- [ ] Dialog deep-links to the App Store page
- [ ] Dismissal respected; no nagging
- [ ] Network failure is silent and non-blocking
- [ ] Three kits, two themes, two languages
- [ ] Previews cover the dialog

## 8. Pitfalls

- **The iTunes lookup API is cached.** Never build a forced-update gate on it.
- **Fail silently.** An update check that blocks or errors is worse than no check.
- **Respect dismissal.** Prompting every launch is a reason to uninstall.
- **A no-op iOS implementation is acceptable for 1.0.** Record it rather than half-building it.

## 9. Verification

```bash
curl -s "https://itunes.apple.com/lookup?bundleId=com.todoapp.mobile" | jq '.results[0].version'
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: simulate an older local version, confirm the dialog and the store link;
# dismiss and relaunch (no nag); airplane mode (silent)
```
