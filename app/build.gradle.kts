import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.roborazzi)
}

// version.properties holds the released version as a literal committed by
// scripts/tag-release.sh as part of the commit it tags, derived from the tag:
//   tag v1.2.3 -> versionName=1.2.3, versionCode=10203 (major*10000 + minor*100 + patch)
// A literal in source (rather than only in the tag) is what lets F-Droid's build
// server and its tag-based update checker resolve the version without any
// injected build properties. Passing -PappVersionName/-PappVersionCode still
// overrides it, e.g. for local testing (see docs/development.md).
val versionProps = Properties()
file("$rootDir/version.properties").inputStream().use { versionProps.load(it) }
val appVersionName =
    (
        project.findProperty("appVersionName") ?: versionProps.getProperty("versionName")
            ?: error("version.properties is missing versionName")
    ).toString()
val appVersionCode =
    (
        project.findProperty("appVersionCode") ?: versionProps.getProperty("versionCode")
            ?: error("version.properties is missing versionCode")
    ).toString().toInt()

android {
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "io.github.ouj4k2q5.minlauncher"
        minSdk {
            version = release(30)
        }
        targetSdk {
            version = release(36)
        }
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += setOf("en", "ja")
    }

    testOptions {
        unitTests {
            // Lets Robolectric tests read res/ assets (strings, themes, etc.).
            isIncludeAndroidResources = true
        }
        managedDevices {
            localDevices {
                // Headless ATD image at minSdk for the on-device smoke tests:
                // ./gradlew pixel2api30DebugAndroidTest (first run downloads
                // the system image, ~1.2 GB).
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    // Credentials come from the environment rather than being written here, so nothing
    // secret ends up in the repository. The release workflow decodes the keystore from a
    // secret and points KEYSTORE_PATH at it. Passing them as environment variables rather
    // than -P properties also keeps them out of the process list.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    // Pinned rather than whatever AGP bundles: JaCoCo 0.8.13+ records line
    // coverage for Kotlin inline functions, and pinning keeps report semantics
    // stable across AGP updates.
    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a keystore the APK is left unsigned, so assembleRelease still works
            // for anyone building from source.
            if (System.getenv("KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Generates createDebugUnitTestCoverageReport (report only; no threshold gate).
            enableUnitTestCoverage = true
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    namespace = "app.minlauncher"
}

ktlint {
    // Kotlin formatting/lint via .editorconfig at the repo root.
    // ktlintFormat rewrites, ktlintCheck is what CI runs.
    android = true
}

detekt {
    // Start from detekt's defaults and layer config/detekt/detekt.yml on top,
    // so the yml only records deliberate deviations.
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.datastore.preferences)

    // Android lifecycle (runtime-compose provides collectAsStateWithLifecycle
    // for the Compose screens; the fragments keep owning their ViewModels).
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)

    // Compose (BOM 2026.11.01 / material3 1.5.0, which requires compileSdk 37,
    // now satisfied). All three screens — home, drawer, settings — consume
    // these through their fragments' Compose content.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi.compose)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // The on-device smoke tests drive the Compose home, drawer, and settings
    // screens.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

// Robolectric loads app classes in a sandbox classloader without a code
// location, so JaCoCo skips them unless they are included explicitly; without
// this, coverage of the app's own classes reads 0%. jdk.internal.* cannot be
// instrumented on JDK 17. AGP applies the JaCoCo plugin (which creates the
// jacoco extension on Test tasks) during its own afterEvaluate, so this must
// be registered after that, or the findByType below is a silent no-op.
afterEvaluate {
    tasks.withType<Test>().configureEach {
        val jacoco = extensions.findByType<JacocoTaskExtension>()
        // Fail loudly rather than silently producing a ~0% report if a future
        // AGP change moves JaCoCo extension creation behind this block again.
        if (name == "testDebugUnitTest") {
            requireNotNull(jacoco) { "JaCoCo extension missing on $name; coverage would silently read 0%" }
        }
        jacoco?.apply {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}
