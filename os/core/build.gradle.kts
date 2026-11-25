plugins {
    kotlin("jvm")
}
group = "com.mana.os"
version = "4.0.0"
kotlin { jvmToolchain(17) }
tasks.test { useJUnitPlatform() }
dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
