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
        version = release(37)
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

configurations.all {
    resolutionStrategy {
        // The Compose graph drags the androidx.lifecycle atomic group to 2.11.0,
        // and of the lifecycle 2.11.0 artifacts only lifecycle-runtime-compose
        // requires compileSdk 37 (the rest are minCompileSdk 34, AAR metadata).
        // Pin just that one coordinate to 2.10.0 (minCompileSdk 35, verified via
        // AAR metadata) and leave the rest of the family at 2.11.0: navigation
        // 2.10.2 bytecode references ViewModelStoreProvider from 2.11.0, so
        // forcing the whole family down breaks 17 Robolectric tests with
        // NoClassDefFoundError in NavHostFragment. Caution: force silently
        // overrides explicit declarations too — if you ever add a direct
        // dependency on lifecycle-runtime-compose (e.g. for
        // collectAsStateWithLifecycle), revisit this force first.
        // Remove this force when the project moves to compileSdk 37.
        force("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.datastore.preferences)

    // Android lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)

    // Compose (Stage 0 foundation — BOM 2026.06.01 is the newest that builds
    // against compileSdk 36; 2026.08.00+ requires 37. Compose screens are still
    // zero; Stage 8a settings will be the first consumer of these.)
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
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.roborazzi.compose)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
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
