package com.todoapp.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Home-list scroll jank (frame durations) for com.todoapp.mobile. The app launch happens in
 * `setupBlock` so it is NOT measured — only the fling frames feed [FrameTimingMetric]
 * (`frameDurationCpuMs` P50/P90/P99 + overrun). Reuses the scroll pattern proven in the
 * :baselineprofile generator.
 *
 * Physical device, Android 13+ (emulator frame timings are unreliable):
 *   ./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ScrollJankBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeScrollJank() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        setupBlock = {
            pressHome()
            startActivityAndWait()
        },
    ) {
        val list = device.wait(Until.findObject(By.scrollable(true)), FIND_TIMEOUT_MS)
            ?: return@measureRepeated
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
        repeat(FLING_COUNT) {
            list.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.todoapp.mobile"
        const val FIND_TIMEOUT_MS = 5_000L
        const val GESTURE_MARGIN_FRACTION = 5
        const val FLING_COUNT = 3
    }
}
