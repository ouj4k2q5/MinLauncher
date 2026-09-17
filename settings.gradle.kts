pluginManagement {
    repositories {
        google()
        mavenCentral()
        // ktlint-gradle is only published to the Gradle Plugin Portal.
        gradlePluginPortal()
    }
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
