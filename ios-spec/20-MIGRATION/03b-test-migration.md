---
id: 20-03b
title: Move the regression shields to `commonTest`
layer: data
status: TODO
depends_on: [20-03]
blocks: [20-04]
parallel_safe: false
estimate: 30h
reversible: true
owner_files:
  - shared/core/src/commonTest/**
  - shared/domain/src/commonTest/**
  - shared/core/build.gradle.kts
  - shared/domain/build.gradle.kts
  - app/src/test/java/com/todoapp/mobile/**
  - gradle/libs.versions.toml
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :shared:domain:testDebugUnitTest :shared:core:testDebugUnitTest
  - "! grep -rqE '^import (org\\.junit|org\\.robolectric)' shared/*/src/commonTest"
---

## 1. Goal

Move the tests that cover `domain/` and `common/` into `:shared:domain` / `:shared:core` `commonTest`, rewriting their assertions onto the multiplatform test API. Every test that moves must assert **the same behaviours** it asserted before.

This task exists because the test suite is the safety net for everything after it, and moving it is not a `git mv`.

## 2. Why this way

**The whole migration is justified by "the regression shields keep passing".** `20-00` §8 names fifteen test files as the encoding of behaviour that must not change; five of them (`RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest`, `HealthPointsCalculatorTest`, `SubtaskTest`) cover pure domain logic that will live in `commonMain` from `20-03` onward. A shield that only runs on the JVM cannot guard the iOS build — and `30-01`'s `ReminderPlannerTest`, the gate for the highest-risk platform task, is specified as a `commonTest`. Something has to make `commonTest` real, with a working stack, before any of that is possible.

**It is not a move, it is an assertion rewrite.** The current stack is JUnit4 + `org.junit.Assert` + MockK + Turbine + `kotlinx-coroutines-test` + Robolectric. Of those:

| Library | `commonTest`? | Disposition |
|---|---|---|
| `org.junit.Assert`, `@Test` (JUnit4) | ❌ JVM only | → `kotlin.test` (`assertEquals`, `assertTrue`, `assertFailsWith`, `@Test`) |
| Robolectric | ❌ JVM/Android only | test **stays** in `app/src/test` — it needs an Android `Context` |
| MockK | ✅ multiplatform artifact exists | verify the version publishes for `iosArm64`/`iosSimulatorArm64` before relying on it |
| Turbine | ✅ | — |
| `kotlinx-coroutines-test` | ✅ | — |
| `MainDispatcherRule` (JUnit4 `TestRule`) | ❌ rules are JUnit4 | → an explicit `Dispatchers.setMain` / `resetMain` pair, or keep the rule for tests that stay |

**Only what belongs in the shared modules moves.** A ViewModel test does not move here — the UI layer is still in `:app` until `20-11`. A Robolectric test does not move at all. The rule is: *the test moves if and only if its subject moved in `20-03`.* Roughly 8–10 of the 37 files qualify.

**Why it blocks `20-04` rather than sitting anywhere in the phase.** `20-04` is a 93-file date-arithmetic sweep whose entire safety argument is "the shields pass with unchanged assertions". If the shields are still in `app/src/test` while `Recurrence.kt` lives in `:shared:domain`, they do still run — but `20-04`'s own `verify:` line invokes `:shared:domain:testDebugUnitTest --tests '*Recurrence*'`, which finds nothing and passes vacuously. Doing this first makes that command mean what it says.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `app/src/test/java/com/todoapp/mobile/` | 37 files, ~4,900 LOC. The list recorded in `20-03`'s notes says which cover `domain/` and `common/`. |
| `app/src/test/…/util/MainDispatcherRule.kt` | The shared JUnit4 rule. Its `commonTest` replacement is the one piece of new infrastructure. |
| `app/src/test/…/RecurrenceTest.kt`, `RecurrenceProgressTest.kt`, `GroupTaskRecurrenceTest.kt` | Pure domain, no Android — **the primary candidates** |
| `app/src/test/…/HealthPointsCalculatorTest.kt`, `SubtaskTest.kt` | Pure domain |
| `app/src/test/…/SyncWorkerTest.kt` | `@Config(sdk = [34], application = android.app.Application::class)` — Robolectric, **does not move** |
| `app/src/test/…/PaletteStyleTest.kt`, `PixelIconMapTest.kt` | Test `:uikit` invariants from `:app`; they move in `20-10`, not here |
| `app/build.gradle.kts` | The current test dependency block |
| `20-00` §8 | The regression-shield table |

## 4. Target

```
shared/core/build.gradle.kts       + commonTest dependencies
shared/domain/build.gradle.kts     + commonTest dependencies
shared/domain/src/commonTest/kotlin/com/todoapp/mobile/domain/…   ← the moved shields
shared/core/src/commonTest/kotlin/com/todoapp/mobile/common/…     ← common helpers' tests
shared/core/src/commonTest/kotlin/…/util/MainDispatcher.kt        (new) rule replacement
app/src/test/…                     everything that did not qualify, unchanged
gradle/libs.versions.toml          + kotlin-test, mockk multiplatform coordinates
```

## 5. Steps

1. **Classify all 37 files** into three buckets and write the classification into the task notes. This is the actual design work; the rest is mechanical.
   - **MOVE** — subject is in `:shared:domain` or `:shared:core`, no Android/Robolectric dependency.
   - **STAY (for now)** — subject is still in `:app` (ViewModels, workers, repositories). They move with their subject in `20-05` … `20-11`.
   - **STAY (forever)** — needs Robolectric or an Android `Context`. Never becomes a `commonTest`.

2. **Wire `commonTest` dependencies** on both new modules (skeleton below). Add `kotlin("test")`, `kotlinx-coroutines-test`, Turbine, and MockK **only if** its multiplatform artifact resolves:
   ```bash
   ./gradlew :shared:domain:dependencies --configuration commonTestImplementation | grep -i mockk
   ```
   If MockK does not publish for the targets you will declare in `20-13`, that is a `BLOCKED` note in `BLOCKERS.md`, not a silent substitution — but check whether the moved tests need mocking at all first. The pure ones (`RecurrenceTest`, `HealthPointsCalculatorTest`) do not.

3. **Move one file first — `RecurrenceTest`.** It is the highest-value shield and the simplest rewrite. Get it green in `:shared:domain:commonTest` before touching another file.

4. **Rewrite assertions onto `kotlin.test`.** The mapping is mechanical but must not change semantics:

   | `org.junit.Assert` | `kotlin.test` |
   |---|---|
   | `assertEquals(expected, actual)` | `assertEquals(expected, actual)` — **argument order is the same** |
   | `assertTrue(msg, cond)` | `assertTrue(cond, msg)` — **message moves to the end** |
   | `assertNull` / `assertNotNull` | same names |
   | `assertThrows(X::class.java) { }` | `assertFailsWith<X> { }` |
   | `@Test(expected = X::class)` | `assertFailsWith<X> { }` |
   | `org.junit.Test` | `kotlin.test.Test` |
   | `@Before` / `@After` | `kotlin.test.BeforeTest` / `AfterTest` |

   > **`assertTrue`'s message parameter moves.** JUnit4 puts it first, `kotlin.test` puts it last. A mechanical import swap compiles and silently asserts the *message string* instead of the condition. This is the single most likely way to turn a shield into a test that passes unconditionally.

5. **Replace `MainDispatcherRule`** for the moved tests with an explicit setup/teardown pair (skeleton below). Leave the JUnit4 rule in `app/src/test` for the tests that stay.

6. **Prove semantic equivalence, do not assume it.** For each moved file, run the original in `:app` and the moved one, and compare the test-case names and count:
   ```bash
   ./gradlew :app:testDebugUnitTest --tests '*RecurrenceTest*'
   # note the test count from build/reports/tests/…, then after the move:
   ./gradlew :shared:domain:testDebugUnitTest --tests '*RecurrenceTest*'
   ```
   **Same number of test cases, same names.** A rewrite that drops a case is worse than no move.

7. **Delete the original only after the moved one is green.** `git mv` then edit, in two commits, per `20-00` §6.

8. **Repeat for the remaining MOVE files**, one file per commit, gate green between each.

9. **Run the full gate.** `testDebugUnitTest` at the root must now execute both `:app`'s remaining tests and the new modules' — confirm the total case count across all modules is unchanged from before this task.

## 6. Code skeleton

```kotlin
// shared/domain/build.gradle.kts
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            // MockK only if it resolves for every target declared in 20-13.
            // The pure shields (Recurrence, HealthPoints) need no mocking at all.
        }
    }
}
```

```kotlin
// Before — app/src/test/…/RecurrenceTest.kt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    @Test
    fun `monthly rule on the 31st fires on 28 February`() {
        assertTrue("should fire", rule.firesOn(anchor, feb28))   // message FIRST
    }
}

// After — shared/domain/src/commonTest/…/RecurrenceTest.kt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceTest {
    @Test
    fun `monthly rule on the 31st fires on 28 February`() {
        assertTrue(rule.firesOn(anchor, feb28), "should fire")   // message LAST
    }
}
```

```kotlin
// shared/core/src/commonTest/…/util/MainDispatcher.kt
// JUnit4 TestRule has no commonTest equivalent. Explicit setup/teardown instead.
class MainDispatcherSetup(val dispatcher: TestDispatcher = StandardTestDispatcher()) {
    fun install() = Dispatchers.setMain(dispatcher)
    fun uninstall() = Dispatchers.resetMain()
}

// Usage:
class SomeTest {
    private val main = MainDispatcherSetup()
    @BeforeTest fun setUp() = main.install()
    @AfterTest fun tearDown() = main.uninstall()

    @Test fun something() = runTest(main.dispatcher.scheduler) { /* … */ }
}
```

## 7. Acceptance

- [ ] All 37 test files classified MOVE / STAY-for-now / STAY-forever, with the classification in the task notes
- [ ] Every MOVE file lives in `commonTest` of the module that owns its subject
- [ ] `! grep -rqE '^import (org\.junit|org\.robolectric)' shared/*/src/commonTest`
- [ ] `./gradlew :shared:domain:testDebugUnitTest :shared:core:testDebugUnitTest` passes
- [ ] **Total test-case count across all modules is unchanged** from before this task — no case silently dropped
- [ ] Each moved file has the same test-case *names* as its original
- [ ] No `assertTrue(condition, message)` inversion — spot-check every `assertTrue`/`assertFalse` call that carried a message
- [ ] `RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest`, `HealthPointsCalculatorTest` run from `:shared:domain`
- [ ] `MainDispatcherRule` still exists in `app/src/test` for the tests that stayed
- [ ] `SyncWorkerTest` and every other Robolectric test is untouched and still passing
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] MockK's multiplatform availability confirmed, or recorded in `BLOCKERS.md` with the exact coordinate tried

## 8. Pitfalls

- **`assertTrue`'s message argument moves from first to last.** JUnit4: `assertTrue(msg, cond)`. `kotlin.test`: `assertTrue(cond, msg)`. Swapping the import alone still compiles — a non-empty `String` is truthy nowhere in Kotlin, so this actually becomes a type error in most cases, but `assertTrue(cond)` with a dropped message compiles silently and `assertEquals` with swapped expected/actual reverses every failure message. Read each call, do not sed it.
- **Do not move a test whose subject has not moved.** A ViewModel test in `:shared:domain` cannot see the ViewModel. The rule is subject-follows, not name-follows.
- **Robolectric tests never become `commonTest`.** `SyncWorkerTest`'s `@Config(application = android.app.Application::class)` trick is what lets it skip Firebase/Hilt init — it is Android-specific by construction and correct where it is.
- **Do not "fix" a failing moved test by relaxing the assertion.** `20-00` §8 is explicit: a semantic difference is a bug in the migration, not a test that needs updating. If a moved shield fails, the move broke something.
- **Test-case count is the real acceptance.** A rewrite that quietly drops three parameterised cases still shows green. Compare counts from the HTML reports, not from the console summary.
- **`kotlin.test` has no `@Ignore` reason parameter and no `assertThrows`.** `assertFailsWith<X> { }` returns the exception, so assertions about the message still work.
- **`runTest` needs the scheduler.** `CLAUDE.md` documents `runTest(mainDispatcherRule.dispatcher.scheduler)` so `advanceUntilIdle()` drives coroutines. The replacement must pass the scheduler the same way or ViewModel-shaped tests hang.
- **Do not add a UI-test harness.** `CLAUDE.md` is explicit that `ComposeTestRule` is a declared-but-unused dependency and that bootstrapping UI tests is out of scope. This task moves existing tests; it does not add a category.

## 9. Verification

```bash
# 1. The moved tests run in their new home
./gradlew :shared:domain:testDebugUnitTest :shared:core:testDebugUnitTest

# 2. No JVM-only test API leaked into commonTest
grep -rnE '^import (org\.junit|org\.robolectric)' shared/*/src/commonTest \
  && echo "JVM TEST API IN commonTest" || echo "clean"

# 3. Case count unchanged — count <testcase> elements across every module's XML results
find . -path '*/test-results/*' -name 'TEST-*.xml' -exec grep -ho '<testcase ' {} + | wc -l

# 4. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 5. The shields, individually
./gradlew :shared:domain:testDebugUnitTest --tests '*Recurrence*' --tests '*HealthPoints*'
```
