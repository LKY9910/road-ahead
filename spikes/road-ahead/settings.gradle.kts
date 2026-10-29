pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://nexus-playground.pleos.ai/repository/maven-releases/") }
            filter {
                includeGroup("ai.pleos.playground")
                includeGroup("ai.pleos.playground.security")
            }
        }
    }
}
rootProject.name = "RoadAheadSpike"
include(":app")
