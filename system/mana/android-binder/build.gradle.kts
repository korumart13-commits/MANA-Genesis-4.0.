plugins {
    id("com.android.library")
}

android {
    namespace = "com.mana.androidbinder"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { aidl = true }
    sourceSets.getByName("main").apply {
        java.srcDir(file("../apps/ManaAssistant/src"))
        java.srcDir(file("../apps/ManaLauncher/src"))
        java.include("com/mana/assistant/ManaBrainBinderService.java")
        java.include("com/mana/assistant/ManaAndroidCapabilityExecutor.java")
        java.include("com/mana/launcher/ManaHomeActivity.java")
        aidl.srcDir(file("../brain/aidl"))
    }
}

dependencies {
    implementation(project(":brain"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
