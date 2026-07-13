import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.util.Properties

// Read debug BASE_URL from local.properties (not in git). Defaults to the deployed Render backend.
// Emulator dev: add `debugBaseUrl=http://10.0.2.2:8080/` to local.properties.
private val localProps: Properties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

// Release signing — read from keystore.properties at the repo root (gitignored, never committed).
// Absent on CI and fresh clones, so the release signingConfig below is wired only when it exists.
private val keystorePropsFile = rootProject.file("keystore.properties")
private val keystoreProps: Properties =
    Properties().apply {
        if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
    }
val debugBaseUrl: String = localProps.getProperty("debugBaseUrl", "https://donebot-backend.onrender.com/")
// Google Maps Platform API key. Add `MAPS_API_KEY=…` to local.properties (not in git).
// Empty string is acceptable at build-time — Maps SDK will fail at runtime which is fine
// for CI/local-only-no-key dev. Production releases must set the key.
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.androidx.baselineprofile)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.todoapp.mobile"
    compileSdk {
        version = release(36)
    }

    // Pinned so release builds actually package native debug symbols (debugSymbolLevel FULL below):
    // without an installed NDK, AGP silently skips symbol extraction and every locally-built AAB
    // uploads with Play's "contains native code / no debug symbols" warning. r27c is an LTS release;
    // CI installs the same version (see .github/workflows/ci.yml size-budget job).
    ndkVersion = "27.2.12479018"

    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        applicationId = "com.todoapp.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Restrict packaged locales to what we actually translate. Drops AppCompat/Material
        // resource tables for ~50 unused locales (~500 KB APK savings).
        @Suppress("UnstableApiUsage")
        androidResources.localeFilters += listOf("en", "tr")

        // Surface the Maps API key into AndroidManifest's `${MAPS_API_KEY}` placeholder
        // (read by `<meta-data android:name="com.google.android.geo.API_KEY" .../>`)
        // and to BuildConfig for Places SDK initialization in Application.onCreate.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        create("release") {
            // Populated from keystore.properties; the ?.let leaves the config empty when the file is
            // absent so non-release tasks (and CI without secrets) still configure cleanly.
            keystoreProps.getProperty("storeFile")?.let { path ->
                storeFile = file(path)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Lets the debug build install side-by-side with release (com.todoapp.mobile.debug).
            // NOTE: social login (Google/Facebook) + FCM are bound to the package name, so they
            // only work on debug once com.todoapp.mobile.debug + the debug SHA-1 are registered in
            // Firebase/Google Cloud. Email/password login works regardless. A src/debug/google-services.json
            // placeholder keeps the build green until then.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField(
                "String",
                "PRIVACY_POLICY_URL",
                "\"https://donebot-backend.onrender.com/legal/privacy.html\"",
            )
            buildConfigField(
                "String",
                "TERMS_OF_SERVICE_URL",
                "\"https://donebot-backend.onrender.com/legal/terms.html\"",
            )
            buildConfigField("String", "SUPPORT_EMAIL", "\"donebotapp@gmail.com\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Bundle native debug symbols so Play can symbolicate native crash/ANR stack traces
            // (from third-party .so libs). Without this Play shows the "native code, no debug
            // symbols" upload warning. FULL = full symbol table (best traces; only in the AAB,
            // not shipped to devices, so no install-size cost).
            ndk {
                debugSymbolLevel = "FULL"
            }
            // Sign with the release key when keystore.properties is present; never fall back to the
            // debug key for a release build (Play rejects debug-signed uploads).
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("String", "BASE_URL", "\"https://donebot-backend.onrender.com/\"")
            buildConfigField(
                "String",
                "PRIVACY_POLICY_URL",
                "\"https://donebot-backend.onrender.com/legal/privacy.html\"",
            )
            buildConfigField(
                "String",
                "TERMS_OF_SERVICE_URL",
                "\"https://donebot-backend.onrender.com/legal/terms.html\"",
            )
            buildConfigField("String", "SUPPORT_EMAIL", "\"donebotapp@gmail.com\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // AAB: let Play serve per-device splits (smaller installs than one universal APK).
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Stubbed android.jar methods (android.util.Log.*, etc.) return default values instead of
            // throwing "Method ... not mocked"; Robolectric tests additionally need real resources.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    // MigrationTestHelper reads the exported Room schema JSONs from androidTest assets.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

// Compose compiler metrics/reports — gated behind `-PcomposeCompilerReports=true`.
// Strong skipping is enabled by default for Kotlin 2.2.x with the kotlin-compose plugin.
composeCompiler {
    if (project.findProperty("composeCompilerReports") == "true") {
        metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
    ignoreFailures.set(false)
    additionalEditorconfig.set(
        // not supported until ktlint 0.49
        mapOf(
            "ktlint_standard_function-naming" to "disabled",
        ),
    )
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}
detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/config/baseline.xml")
}
tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

dependencies {
    implementation(project(":uikit"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.sizeclass)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.appcompat)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)

    // Unit tests (JVM host)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)

    // Instrumented tests (device/emulator)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Network (Retrofit & KotlinX Serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Local Storage (Room)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.sqlite)
    ksp(libs.room.compiler)

    // Utils
    implementation(libs.timber)
    detektPlugins(libs.detekt.formatting)
    implementation(libs.reorderable)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)

    // Google Maps + Places (location feature)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.google.places)
    implementation(libs.maps.compose)

    // Camera (CameraX) — skeuomorphic Polaroid capture for Journal entries
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Generated baseline profile is merged into app/src/main/baseline-prof.txt.
    "baselineProfile"(project(":baselineprofile"))
}

baselineProfile {
    // Merge any newly generated profile into src/main/baseline-prof.txt so it ships
    // with the APK without a per-build regeneration step.
    mergeIntoMain = true
}
