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
        versionCode = 3
        versionName = "4.0.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets { getByName("main").java.srcDir("../../engine/mana-brain/src/main/kotlin") }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":core"))
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
