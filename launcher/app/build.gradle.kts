plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { buildConfig = true; compose = true; aidl = true }
    sourceSets {
        getByName("main") {
            aidl.srcDir("../../system/mana/interfaces/src")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.11.00"))
    implementation(project(":core"))
    implementation(project(":brain"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-android:2.10.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")
    implementation("io.github.sceneview:sceneview:4.18.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
