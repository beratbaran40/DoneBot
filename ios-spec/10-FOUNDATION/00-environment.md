---
id: 10-00
title: Environment & toolchain
layer: foundation
status: TODO
depends_on: []
blocks: [10-02, 10-03, 10-04, 20-01]
parallel_safe: false
estimate: 6h (plus a multi-hour OS upgrade that is mostly waiting)
reversible: true
owner_files:
  - gradle.properties
  - .github/workflows/ci.yml
verify:
  - JAVA_HOME= ./gradlew --version
  - ./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug
---

## 1. Goal

Make every Gradle invocation in this repository work from a plain shell, and get the machine onto a toolchain that can build for iOS at all. When this task is done, an agent can run the verification gate without knowing anything about JDK paths, and `xcodebuild -version` reports Xcode 26.

## 2. Why this way

**The JDK problem is the single highest-leverage fix in the whole spec.** Right now, running `./gradlew` from a shell selects JDK 24 and every build dies with `Type T not present`. An autonomous agent hits this on its *first* verification command and the entire chain stops. Android Studio works because it silently uses its own bundled runtime; the shell does not.

We pin it in `gradle.properties` rather than relying on an exported `JAVA_HOME` because the pin must survive a fresh shell, a CI runner, a cron job and an agent that does not read its own environment. It is one line, it is committed, and it is impossible to forget.

**The macOS upgrade goes first, before any code change**, so that if the upgrade breaks something, the cause is unambiguous and the rollback costs nothing. Doing it mid-migration would entangle OS breakage with migration breakage.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `gradle.properties` | Current JVM args (4 GB Gradle heap, 2 GB Kotlin daemon), parallel/cache/configuration-cache flags. You are adding one property, not restructuring. |
| `.github/workflows/ci.yml` | The JDK setup step. CI already pins Temurin 21 and must stay that way — do not change CI's JDK here. |
| `/CLAUDE.md` § "Build, Test & CI" | The existing statement of the JDK requirement. |

## 4. Target

- `gradle.properties` — add `org.gradle.java.home`
- No other file changes in this task.

## 5. Steps

1. **Confirm the current state.** Record the output; you will assert against it later.
   ```bash
   sw_vers -productVersion
   uname -m
   java -version 2>&1 | head -1
   /usr/libexec/java_home -V 2>&1 | head -20
   ls -d "/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home" 2>/dev/null
   ```

2. **Pin the JDK.** Append to `gradle.properties`:
   ```properties
   # Gradle requires JDK/JBR 21. JDK 24 fails with "Type T not present".
   # Pinned so a plain shell, CI and automated agents all resolve the same runtime
   # that Android Studio uses. Bytecode target stays Java 17.
   org.gradle.java.home=/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home
   ```
   If that directory does not exist, find the real one:
   ```bash
   ls -d /Applications/Android\ Studio*.app/Contents/jbr/Contents/Home
   ```
   and use the path that reports `openjdk version "21."`.

3. **Prove the pin works from a shell that has the wrong JDK active.**
   ```bash
   JAVA_HOME= ./gradlew --version    # must report JVM 21.x
   ```

4. **Run the full gate.** It must pass before you touch anything else in this repo.
   ```bash
   ./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug
   ```
   Note `detektMain` here, not `detektAll` — `detektAll` does not exist until `20-01`.

5. **Record the pre-migration AAB baseline** in `90-STATE/PROGRESS.md`:
   ```bash
   ./gradlew :app:bundleRelease
   ls -l app/build/outputs/bundle/release/*.aab
   ```
   Expect ≈ 18.17 MiB. If it differs materially, note it — the whole size ledger is keyed to this number.

6. **macOS upgrade — human action.** Requires macOS 15.6 minimum for Xcode 26; Xcode 26.4 requires macOS Tahoe 26.2. Take a bootable backup first. If you are an agent: write this to `BLOCKERS.md`, mark the task `BLOCKED`, and move on to `20-01`, which needs none of it.

7. **After the upgrade, re-run step 4 before any other change.** Isolating OS-induced breakage from migration breakage is the entire point of doing this first.

8. **Install Xcode 26** from the App Store or the developer portal, then:
   ```bash
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   xcodebuild -version
   xcrun simctl list devices available | head
   ```

## 6. Code skeleton

The complete diff for this task:

```diff
--- a/gradle.properties
+++ b/gradle.properties
@@
+# Gradle requires JDK/JBR 21. JDK 24 fails with "Type T not present".
+# Pinned so a plain shell, CI and automated agents all resolve the same runtime
+# that Android Studio uses. Bytecode target stays Java 17.
+org.gradle.java.home=/Applications/Android Studio Panda.app/Contents/jbr/Contents/Home
```

## 7. Acceptance

- [ ] `JAVA_HOME= ./gradlew --version` reports JVM **21.x**
- [ ] `./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug` passes from a plain shell
- [ ] Pre-migration AAB size recorded in `90-STATE/PROGRESS.md`
- [ ] `gradle.properties` change is committed, with the explanatory comment intact
- [ ] `sw_vers -productVersion` ≥ 15.6 *(or `BLOCKED` with a `BLOCKERS.md` entry)*
- [ ] `xcodebuild -version` reports Xcode 26.x *(or `BLOCKED`)*
- [ ] `xcrun simctl list devices available` lists at least one iPhone simulator *(or `BLOCKED`)*

## 8. Pitfalls

- **The path contains a space.** `Android Studio Panda.app`. In `gradle.properties` it is *not* quoted and *not* escaped — Java properties take the raw value to end of line. Quoting it breaks the lookup.
- **Do not change CI's JDK.** CI pins Temurin 21 via `actions/setup-java` and is correct. `org.gradle.java.home` is a local-machine convenience; if it ever conflicts on a runner, CI's `JAVA_HOME` wins and that is fine.
- **Do not "fix" the JDK by uninstalling 24.** Other tooling on this machine may need it. Pin, do not remove.
- **Do not pipe the Gradle output through `grep` or `tail`** to check the result. The pipe masks the exit code and a failed build reads as success. Redirect to a file and check the exit status.
- **The Android Studio app name may change** across updates (`Android Studio.app`, `Android Studio Panda.app`, …). If a future update breaks the pin, re-run the `ls -d` discovery in step 2. Consider this the known maintenance cost of the pin.
- **Xcode 26 is ~10–15 GB** plus simulator runtimes. Check free disk before starting.

## 9. Verification

```bash
# 1. The pin resolves even when the ambient JDK is wrong
JAVA_HOME= ./gradlew --version

# 2. The full Android gate (note: detektMain, not detektAll, until 20-01)
./gradlew ktlintCheck detektMain testDebugUnitTest assembleDebug

# 3. Size baseline
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. iOS toolchain (only after the OS upgrade + Xcode install)
sw_vers -productVersion
xcodebuild -version
xcrun simctl list devices available | head
```
