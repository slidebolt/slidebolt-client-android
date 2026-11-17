pluginManagement {
    repositories {
        google()
        mavenCentral()
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

rootProject.name = "slidebolt-client-android"

include(
    ":app-phone",
    ":app-tv",
    ":core-model",
    ":core-client",
    ":core-session",
    ":platform-android",
    ":feature-browser",
    ":acceptance",
)

