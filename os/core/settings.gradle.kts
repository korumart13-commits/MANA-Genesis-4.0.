pluginManagement {
    plugins { id("org.jetbrains.kotlin.jvm") version "2.2.21" }
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}
rootProject.name = "mana-os-core"
