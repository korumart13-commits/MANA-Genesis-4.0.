plugins {
    kotlin("jvm")
}

group = "com.mana.ai"
version = "0.4.0"


kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<JavaExec>("validateEngineeringPatch") {
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.mana.brain.EngineeringPatchPolicyCliKt")
    doFirst {
        val patchFile = System.getenv("MANA_PATCH_FILE")
            ?: throw GradleException("MANA_PATCH_FILE must point to a proposed unified diff.")
        args(patchFile)
    }
}
