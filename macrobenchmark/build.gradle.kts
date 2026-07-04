// §3.8 — Macrobenchmark module (jank + startup / Android Vitals proxy).
// A `com.android.test` module that instruments the :app process on a connected device.
// Sibling to :baselineprofile, but a MEASUREMENT module — it does NOT generate a profile,
// so it deliberately does NOT apply the androidx.baselineprofile plugin.
//
// Run on a PHYSICAL device (Android 13+ recommended — emulator frame timings are unreliable):
//   ./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
// See donebot prod/MACROBENCHMARK_RUN.md for the full runbook.
plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.todoapp.macrobenchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Match :app's plugin-generated `benchmarkRelease` variant (R8-minified, debug-signed,
    // non-debuggable, profileable). Targeting it needs NO release keystore and keeps
    // applicationId = com.todoapp.mobile so google-services.json still matches. Keeping minify
    // ON = Android Vitals fidelity (we measure the real R8 + baseline-profile path users run).
    // A `com.android.test` module has no `testBuildType`; `beforeVariants` below leaves only
    // `benchmarkRelease` enabled, which matches :app's plugin-generated variant of the same name.
    buildTypes {
        create("benchmarkRelease") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

// Only configure the benchmarkRelease variant; the default `debug` test variant is unused.
androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmarkRelease"
    }
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
}
