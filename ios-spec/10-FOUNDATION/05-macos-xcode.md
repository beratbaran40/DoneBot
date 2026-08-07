---
id: 10-05
title: macOS upgrade & Xcode 26
layer: foundation
status: TODO
depends_on: []
blocks: [10-03, 20-13]
parallel_safe: true
estimate: 8h (mostly waiting — downloads, installs, reboots)
reversible: true
owner_files: []
verify:
  - sw_vers -productVersion
  - xcodebuild -version
  - xcrun simctl list devices available
---

## 1. Goal

Get the machine onto a toolchain that can compile Kotlin/Native for iOS and build an Xcode project. When this task is done, `xcodebuild -version` reports Xcode 26.x and at least one iPhone simulator runtime is installed.

**This task needs a human.** An agent records it in `BLOCKERS.md`, marks it `BLOCKED`, and continues — nothing in `20-MIGRATION/01` … `20-MIGRATION/12` depends on it.

## 2. Why this way

**This is deliberately not on the migration's critical path.** It used to be folded into `10-00`, which blocks `20-01` and therefore the entire restructure. That was wrong: 504 of the migration's 544 hours never touch an iOS SDK. Splitting it means the port can run for months on the current OS while the upgrade happens whenever it is convenient.

**But it is a hard prerequisite for exactly two tasks, and both are load-bearing:**

- **`20-13`** runs `linkDebugFrameworkIosSimulatorArm64`. Kotlin/Native compiles and links against the real iOS SDK and shells out to `xcrun` — **it cannot produce an iOS framework without Xcode installed.** Command Line Tools alone are not enough. This dependency was missing from the graph and is the reason this task exists as a blocker rather than a footnote.
- **`10-03`** is the Xcode project itself.

**When to actually do it.** Any time before `20-12` completes. Two constraints shape the choice:

1. **Do it on a clean tree with a green gate on both sides.** Run `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` immediately before the upgrade and immediately after, before any further code change. That is what keeps OS-induced breakage from being mistaken for migration breakage — the original reason `10-00` wanted it first.
2. **Do not leave it to the day `20-12` lands.** A failed upgrade, a full disk, or an Xcode version that needs a *further* OS bump turns into days of dead time at the exact moment the migration is ready to switch on iOS. Treat "before `20-11` starts" as the comfortable deadline.

**Enrolment is a different clock.** `10-01` (Apple Developer Program, 99 USD/yr) takes weeks of identity verification and is unrelated to this task — start it on day one regardless of when Xcode gets installed. Simulator development needs no paid membership.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `00-CONTEXT/04-constraints.md` §1.1 | The version matrix this task satisfies |
| `90-STATE/BLOCKERS.md` | The existing `10-00 — macOS upgrade required` entry; append the resolution here rather than rewriting it |

## 4. Target

No files in this repository change. This task modifies the machine.

## 5. Steps

1. **Record the starting state.**
   ```bash
   sw_vers
   xcodebuild -version 2>&1 | head -2
   df -h / | tail -1
   ```

2. **Green gate before touching anything.**
   ```bash
   ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
   ```
   If it is already red, fix that first. Upgrading on top of a red tree destroys the ability to attribute the next failure.

3. **Take a bootable backup.** Time Machine plus a bootable clone. This is the one step in the whole spec whose absence cannot be recovered from.

4. **Check disk.** Xcode 26 is ~10–15 GB, plus ~8 GB per simulator runtime, plus the OS installer. Budget 45 GB free.

5. **Upgrade macOS.** Current machine is **14.5 (Sonoma)**. Xcode 26 needs **15.6 (Sequoia)** minimum; Xcode 26.4 needs **macOS Tahoe 26.2**. Pick the target version from which Xcode you intend to install and record it in `DECISIONS.md`.

6. **Re-run the gate before any other change.**
   ```bash
   ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
   ```
   Anything red here is the OS, not the migration. That attribution is the entire point of running it now.

7. **Install Xcode 26** from the App Store or the developer portal, then point the command-line tools at it:
   ```bash
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   sudo xcodebuild -license accept
   xcodebuild -version
   ```

8. **Install an iOS simulator runtime** and confirm a device exists:
   ```bash
   xcodebuild -downloadPlatform iOS
   xcrun simctl list devices available | head
   ```

9. **Prove Kotlin/Native can see the SDK** — this is what `20-13` will need:
   ```bash
   xcrun --sdk iphonesimulator --show-sdk-path
   ```

10. **Append the resolution to the existing `BLOCKERS.md` entry** rather than deleting it, and record the chosen macOS + Xcode versions in `DECISIONS.md`.

## 6. Code skeleton

None — no repository files change.

## 7. Acceptance

- [ ] `sw_vers -productVersion` ≥ **15.6**
- [ ] `xcodebuild -version` reports **Xcode 26.x**
- [ ] `xcrun simctl list devices available` lists at least one iPhone simulator
- [ ] `xcrun --sdk iphonesimulator --show-sdk-path` resolves
- [ ] The Android gate was green **before** the upgrade and is green **after**, with no code change in between
- [ ] A bootable backup was taken before the OS upgrade
- [ ] `BLOCKERS.md` entry for the macOS upgrade has a `**RESOLVED**` line appended
- [ ] Chosen macOS and Xcode versions recorded in `DECISIONS.md`

## 8. Pitfalls

- **Command Line Tools are not Xcode.** `xcode-select -p` pointing at `/Library/Developer/CommandLineTools` gives you `git` and `clang` but no iOS SDK. `20-13` fails with a linker error that does not say "install Xcode".
- **Do not upgrade mid-task.** Finish and commit whatever migration task is in flight first. A half-applied `reversible: false` task plus an OS that now behaves differently is the worst debugging position in this project.
- **Xcode 26.4 needs a newer OS than Xcode 26.0.** Decide which Xcode you want *before* choosing the macOS version, not after.
- **The simulator runtime is a separate download.** A fresh Xcode often ships with none, and `xcodebuild -destination 'platform=iOS Simulator,name=iPhone 17'` then fails with a confusing "unavailable destination" error.
- **Building with the iOS 26 SDK does not force users onto iOS 26.** The deployment target is independent and is set in `10-03`.
- **This task does not need the paid Apple Developer account.** Simulator development works with no membership at all. `10-01` runs on its own clock; do not couple them.
- **Free disk is the most common failure.** An interrupted OS installer on a full disk is exactly the scenario the bootable backup exists for.

## 9. Verification

```bash
# 1. The OS
sw_vers -productVersion              # >= 15.6

# 2. Xcode, not just Command Line Tools
xcode-select -p                      # must be inside Xcode.app
xcodebuild -version                  # Xcode 26.x

# 3. A simulator exists and the SDK resolves
xcrun simctl list devices available | head
xcrun --sdk iphonesimulator --show-sdk-path

# 4. Android is unaffected by the upgrade
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
```
