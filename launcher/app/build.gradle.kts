plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.mana.launcher"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mana.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "4.0.0"
    }
    buildFeatures { buildConfig = true }
}
