import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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
        version = release(36)
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
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
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

    // Android lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)

    testImplementation(libs.junit)
}
