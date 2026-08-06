---
id: 30-05
title: Ambience audio & the alarm sound catalog
layer: platform
status: TODO
depends_on: [20-13, 30-00]
blocks: [40-pomodoro-01, 40-settings-04]
parallel_safe: true
estimate: 18h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/ambience/**
  - shared/data/src/androidMain/**/ambience/**
  - shared/data/src/iosMain/**/audio/**
  - tools/prep_ambience.sh
  - shared/resources/src/commonMain/composeResources/files/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Play the three Pomodoro ambience loops in the background on iOS, and replace the system-ringtone alarm picker with a bundled sound catalog.

## 2. Why this way

**Background audio is one of the few places where iOS is easier than Android.** The Android app fought this: the git history shows background ambience being tied to a foreground service and then rolled back because the app could not reliably deliver it (`ede5f8c fix(pomodoro): stop offering background ambience the app cannot deliver`). On iOS, `UIBackgroundModes: audio` plus an `AVAudioSession` in `.playback` is a supported, first-class capability. The feature that was *withdrawn* on Android can ship on iOS.

**But the files are wrong.** The three loops are Ogg Vorbis, and **iOS cannot play Ogg**. `tools/prep_ambience.sh` needs an AAC/`.m4a` output alongside the existing `.ogg`. Both ship; each platform loads its own.

**The alarm sound picker genuinely degrades.** Android enumerates the system ringtone list via `RingtoneManager`. iOS has no equivalent — notification sounds must be bundled files, `.caf`/`.aiff`/`.wav`, ≤30 s. The screen loses its "System" section and becomes a short curated list. This is a real, user-visible reduction and it belongs in the degradation table.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `domain/ambience/AmbiencePlayer.kt` | The existing interface — already platform-neutral, keep it |
| `data/ambience/AmbiencePlayerImpl.kt` | `MediaPlayer` with `AudioAttributes` and audio-focus etiquette (deliberately **not** media3) |
| `data/ambience/AmbienceCoordinator.kt` | Which loop plays for which focus mode |
| `data/ambience/AmbienceAssets.kt` | The id → resource map |
| `app/src/main/res/raw/ambience_{fireplace,rain,handpan}.ogg` | 96 KB / 80 KB / 300 KB |
| `tools/prep_ambience.sh` | Seam-matching and −23 LUFS levelling. Add an `.m4a` output. |
| `pomodoro_background_noises/` | Unprocessed sources (gitignored) |
| `common/RingtoneHolder.kt` | Ringtone playback + ringer-mode etiquette |
| `data/repository/AlarmSoundPreferencesImpl.kt` | Resolves the sound id (a `String` since `20-02`) |
| `ui/alarmsounds/` (3 files, 325 LOC) | The picker |
| `ui/pomodoro/ambience/` | `FireplaceScene`, `RainScene`, `HandpanScene` — pure Compose Canvas, port free |

## 4. Target

- `tools/prep_ambience.sh` — emits `.ogg` **and** `.m4a`
- `shared/resources/…/composeResources/files/` — both formats
- `shared/data/androidMain/…/AndroidAmbiencePlayer.kt` — the existing implementation
- `shared/data/iosMain/…/AvAudioAmbiencePlayer.kt` — `AVAudioPlayer` + `AVAudioSession`
- `shared/domain/…/ambience/AlarmSoundCatalog.kt` — the new contract
- `iosApp/Resources/Sounds/*.caf` — 6 bundled alarm sounds
- `iosApp/Info.plist` — `UIBackgroundModes: audio`

## 5. Steps

1. **Extend `prep_ambience.sh`** to emit AAC `.m4a` alongside `.ogg`, from the same seam-matched, −23 LUFS master. Keep the `.ogg` — Android ships it and it is smaller.

2. **Move both formats** into `composeResources/files/`; select by platform at load time.

3. **iOS: configure the audio session** as `.playback` with `.mixWithOthers`. Mixing matters — a focus app that stops the user's music is a bad app. Activate on play, deactivate on stop so other audio resumes.

4. **Loop with `numberOfLoops = -1`.** The files are seam-matched, so gapless looping is already handled at the asset level.

5. **Handle interruptions.** `AVAudioSession.interruptionNotification` — pause on a call, resume after if the option says so. Android's audio-focus handling is the direct analogue.

6. **Add `UIBackgroundModes: audio`.** Required, and only justified because playback is genuine — App Review rejects the declaration otherwise.

7. **Build the alarm sound catalog contract.** Android enumerates `RingtoneManager`; iOS returns a fixed bundled list.

8. **Bundle 6 alarm sounds** as `.caf`, ≤30 s. `afconvert` produces them. They must live in the app bundle, not `composeResources` — `UNNotificationSound` reads from the bundle.

9. **Update the picker UI** to drop the "System" section when the catalog reports it is unsupported.

## 6. Code skeleton

```bash
# tools/prep_ambience.sh — added alongside the existing .ogg output.
# iOS cannot play Ogg Vorbis; AAC in an .m4a container is the portable choice.
ffmpeg -i "$SEAMED" -c:a aac -b:a 96k -movflags +faststart "app/../shared/resources/.../files/ambience_${name}.m4a"
```

```kotlin
// shared/data/iosMain/…/AvAudioAmbiencePlayer.kt
class AvAudioAmbiencePlayer : AmbiencePlayer {
    private var player: AVAudioPlayer? = null

    override fun play(assetId: String) {
        val session = AVAudioSession.sharedInstance()
        // .mixWithOthers: a focus app must not stop the user's music.
        session.setCategory(AVAudioSessionCategoryPlayback, withOptions = AVAudioSessionCategoryOptionMixWithOthers, error = null)
        session.setActive(true, error = null)

        player = AVAudioPlayer(contentsOfURL = bundleUrl(assetId), error = null).apply {
            numberOfLoops = -1        // files are seam-matched, so this is gapless
            prepareToPlay()
            play()
        }
    }

    override fun stop() {
        player?.stop()
        player = null
        // Deactivate so other audio resumes.
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }
}
```

```kotlin
// shared/domain/…/ambience/AlarmSoundCatalog.kt
data class AlarmSoundOption(val id: String, val displayName: String, val isSystem: Boolean)

interface AlarmSoundCatalog {
    suspend fun options(): List<AlarmSoundOption>
    suspend fun preview(id: String)
    suspend fun stopPreview()
    val supportsSystemSounds: Boolean   // Android true, iOS false
}
```

## 7. Acceptance

- [ ] `prep_ambience.sh` emits `.m4a`; all three exist in both formats
- [ ] iOS plays all three loops, gapless
- [ ] Audio continues with the app backgrounded and with the screen locked
- [ ] `.mixWithOthers` — the user's music keeps playing alongside
- [ ] A phone call pauses ambience; it resumes afterwards
- [ ] Stopping deactivates the session and other audio resumes
- [ ] `UIBackgroundModes: audio` declared and justified by real playback
- [ ] `AlarmSoundCatalog` in `:shared:domain`; both implementations registered
- [ ] Android enumerates system ringtones exactly as before
- [ ] iOS ships 6 bundled `.caf` sounds, each ≤30 s, previewable
- [ ] The Alarm Sounds screen hides the "System" section when unsupported
- [ ] A notification actually plays the selected bundled sound on iOS
- [ ] The three ambience Canvas scenes render identically on both platforms

## 8. Pitfalls

- **iOS cannot play Ogg.** Not a codec to add — an unsupported container. Ship `.m4a`.
- **Notification sounds must be in the app bundle**, not `composeResources`. `UNNotificationSound(named:)` reads from the bundle only.
- **>30 s notification sounds are silently replaced by the default.** No error.
- **`UIBackgroundModes: audio` without genuine audio is a rejection.** DoneBot qualifies — but the feature must actually work.
- **Without `.mixWithOthers`, starting a pomodoro kills the user's music.** For a focus app that is close to a one-star review.
- **Deactivate the session on stop**, or other apps' audio stays ducked.
- **Do not port the Android foreground-service dance.** iOS background audio needs the mode and an active session — nothing else. The Android complexity is Android's problem.
- **Keep `MediaPlayer` on Android.** The choice over media3 was deliberate for size and simplicity.
- **`afconvert` for `.caf`.** `ffmpeg` will not produce the format `UNNotificationSound` expects.

## 9. Verification

```bash
# Both formats present
ls -la shared/resources/src/commonMain/composeResources/files/ambience_*

./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a real iPhone
#   start a pomodoro with ambience → plays
#   background the app, lock the screen → still playing
#   start music in another app → both play
#   receive a call → pauses, then resumes
#   stop the pomodoro → session deactivates, other audio resumes
#   Settings → Alarm Sounds → 6 bundled sounds, each previews, no "System" section
#   fire a reminder → the selected sound plays
```
