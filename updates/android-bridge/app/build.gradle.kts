plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.mana.updatebridge"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mana.updatebridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "MANA Genesis 4.0 Preview 2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}