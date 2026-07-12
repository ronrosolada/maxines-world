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

rootProject.name = "MaxinesWorld"

include(":app")
include(":core-model")
include(":core-network")
include(":core-database")
include(":core-design-system")
include(":core-content")
include(":feature-auth")
include(":feature-child-home")
include(":feature-lesson-player")
include(":feature-progress")
include(":feature-parent")
include(":feature-rewards")
include(":engine-activity")
include(":engine-assessment")
include(":engine-mastery")
include(":engine-sync")
include(":engine-minigame")
include(":game-cat-cafe")
include(":game-pawprint-parkour")
