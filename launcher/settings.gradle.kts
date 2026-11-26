pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "MANA-Launcher"
include(":app", ":core", ":brain")
project(":core").projectDir = file("../os/core")
project(":brain").projectDir = file("../engine/mana-brain")
