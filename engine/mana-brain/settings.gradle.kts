pluginManagement {
    plugins { id("org.jetbrains.kotlin.jvm") version "2.2.21" }
    repositories { gradlePluginPortal(); mavenCentral(); google() }
}
dependencyResolutionManagement {
    repositories {
        // Google-hosted Maven Central mirror reduces transient central throttling.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}
rootProject.name = "mana-brain-engine"
