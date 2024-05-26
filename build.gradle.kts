import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    id("maven-publish")
}

group = "io.github.xibalbam"
version = "1.1"

repositories {
    mavenCentral()
    maven ("https://jitpack.io")
}

dependencies {
    implementation("io.ktor:ktor-client-cio-jvm:2.3.11")
    implementation("io.ktor:ktor-client-logging-jvm:2.3.11")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-gson:2.3.11")
    implementation("io.ktor:ktor-client-logging:2.3.11")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("jitpack") {
                from(components["java"])
                groupId = "fr.xibalba"
                artifactId = "pronotekt"
                version = project.version.toString()
            }
        }
    }
}