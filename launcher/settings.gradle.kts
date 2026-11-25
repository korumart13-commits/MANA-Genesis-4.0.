pluginManagement {
    plugins { id("org.jetbrains.kotlin.jvm") version "2.2.21" }
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "MANA-Launcher"
include(":app", ":core")
project(":core").projectDir = file("../os/core")
