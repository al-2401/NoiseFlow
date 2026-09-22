pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "NoiseFlow"

include(":app")

// Pure Kotlin/JVM. Deliberately not an Android library -- see its build file.
include(":core:audio")

include(":core:playback")
include(":core:data")
include(":core:designsystem")
include(":core:i18n")

include(":feature:player")
include(":feature:mixer")
include(":feature:presets")
include(":feature:settings")
include(":feature:paywall")

include(":billing:api")
include(":billing:play")
include(":billing:rustore")
