pluginManagement {
    repositories {
        google()
        mavenCentral()
        // ktlint-gradle is only published to the Gradle Plugin Portal.
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Minlauncher"
include(":app")
