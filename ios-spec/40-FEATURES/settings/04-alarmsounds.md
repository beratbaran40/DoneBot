---
id: 40-settings-04
title: Alarm sounds
layer: ui
status: TODO
depends_on: [40-settings-01, 30-05]
blocks: []
parallel_safe: true
estimate: 5h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/alarmsounds/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

The alarm sound picker, with an honestly shorter list on iOS.

## 2. Why this way

**This is the clearest single-screen degradation in the port.** Android enumerates every system ringtone through `RingtoneManager`; iOS has no equivalent and notification sounds must be bundled files, `.caf`/`.aiff`/`.wav`, ≤30 seconds.

The right handling is not to fake a system list or to show an empty section — it is for `AlarmSoundCatalog.supportsSystemSounds` to be false and for the screen to simply have fewer options. A shorter list needs no apology; a "System" header with nothing under it looks broken.

`20-02` already changed the stored value from a `Uri` to an opaque `String` id, so the domain side is ready.

## 3. Source

| Path | LOC |
|---|---|
| `ui/alarmsounds/` (3 files) | 325 |
| `common/RingtoneHolder.kt` | playback + ringer etiquette |
| `data/repository/AlarmSoundPreferencesImpl.kt` | id ↔ platform resolution |
| `ios-spec/30-PLATFORM/05-audio-and-sounds.md` | the catalog contract |
| `docs/screenshots/alarmsounds/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/alarmsounds/` — verification.

## 5. Steps

1. Verify all 3 files compile in `commonMain`.
2. Verify Android still enumerates system ringtones.
3. Verify iOS shows the 6 bundled sounds and **no "System" section**.
4. Verify preview playback on both platforms, and that it stops.
5. Verify the selection persists across relaunch.
6. Verify a fired reminder actually plays the selected sound.
7. Verify ringer-mode etiquette on Android; on iOS the ringer switch is respected by the system.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 3 files compile in `commonMain`
- [ ] Android system ringtone list unchanged
- [ ] iOS shows 6 bundled sounds; **no empty "System" section**
- [ ] Preview plays and stops on both platforms
- [ ] Selection persists
- [ ] A fired reminder plays the selected sound
- [ ] Three kits, two themes, two languages
- [ ] Previews cover the list and a selected state

## 8. Pitfalls

- **Do not show an empty "System" header on iOS.** Hide the section.
- **Bundled sounds must be ≤30 s**, or iOS silently substitutes the default.
- **They must live in the app bundle**, not `composeResources` — `UNNotificationSound(named:)` reads the bundle only.
- **The stored value is an opaque id**, not a `Uri`. Do not reintroduce platform types.
- **Preview must stop** when the screen is left.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: preview each sound, select one, relaunch (still selected),
# fire a reminder (correct sound); iOS shows no System section
```
