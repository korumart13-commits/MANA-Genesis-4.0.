pluginManagement {
    plugins { id("org.jetbrains.kotlin.jvm") version "2.2.21" }
    repositories { gradlePluginPortal(); mavenCentral(); google() }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = "mana-brain-engine"
