import java.io.File
import java.net.URL
import java.security.MessageDigest

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
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "4.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            assets.srcDir(layout.buildDirectory.dir("generated/manaVoiceAssets"))
        }
    }
    androidResources { noCompress += "onnx" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.11.00"))
    implementation(project(":core"))
    implementation(project(":brain"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-android:2.10.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")
    implementation("io.github.sceneview:sceneview:4.18.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
}


// Bundles the Russian Piper voice into the APK so speech works offline from first launch.
// The upstream sherpa-onnx release archive is pinned by its published GitHub SHA-256 digest.
val prepareManaOfflineVoice by tasks.registering {
    val archive = layout.buildDirectory.file("downloads/vits-piper-ru_RU-irina-medium.tar.bz2")
    val unpackDir = layout.buildDirectory.dir("tmp/mana-offline-voice")
    val generatedAssets = layout.buildDirectory.dir("generated/manaVoiceAssets")
    outputs.dir(generatedAssets)
    doLast {
        val archiveFile = archive.get().asFile
        archiveFile.parentFile.mkdirs()
        if (!archiveFile.isFile) {
            URL("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-ru_RU-irina-medium.tar.bz2")
                .openStream().use { input -> archiveFile.outputStream().use { output -> input.copyTo(output) } }
        }
        val archiveDigest = MessageDigest.getInstance("SHA-256")
        val actualArchiveSha256 = archiveFile.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                archiveDigest.update(buffer, 0, count)
            }
            archiveDigest.digest().joinToString("") { "%02x".format(it) }
        }
        check(actualArchiveSha256 == "1fc0f54e5e084fe287c07909f2f6e0ba6d857864cf800e3ab80286a4e8233008") {
            "Pinned sherpa-onnx Piper archive SHA-256 mismatch: $actualArchiveSha256"
        }
        val unpack = unpackDir.get().asFile
        project.delete(unpack)
        unpack.mkdirs()
        ant.withGroovyBuilder {
            "untar"("src" to archiveFile, "dest" to unpack, "compression" to "bzip2")
        }
        val model = unpack.walkTopDown().firstOrNull { it.isFile && it.name == "ru_RU-irina-medium.onnx" }
            ?: error("Russian Piper ONNX voice model is missing from the pinned archive")
        check(model.length() in 50_000_000L..80_000_000L) {
            "Russian Piper model has an unexpected size: ${model.length()} bytes"
        }
        val voiceDir = model.parentFile
        check(File(voiceDir, "tokens.txt").isFile && File(voiceDir, "espeak-ng-data").isDirectory) {
            "Russian Piper model archive is missing tokens or eSpeak language data"
        }
        val target = generatedAssets.get().dir("mana_voice").asFile
        project.delete(target)
        target.mkdirs()
        project.copy { from(voiceDir); into(target) }
    }
}
tasks.named("preBuild").configure { dependsOn(prepareManaOfflineVoice) }
