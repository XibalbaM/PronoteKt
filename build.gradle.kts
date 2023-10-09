import java.net.URI

plugins {
    kotlin("jvm") version "1.9.0"
    id("maven-publish")
    id("signing")
}

group = "io.github.xibalbam"
version = "1.1"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.xibalbam"
            artifactId = "pronoteKt"
            version = "1.1"

            from(components["java"])

            pom {
                name = "Pronote Kt"
                description = "A Kotlin library to interact with Pronote"
                url = "https://github.com/XibalbaM/PronoteKt"
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("Xibalba")
                        name.set("Xibalba")
                        email.set("maelporret@outlook.fr")
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/XibalbaM/PronoteKt.git"
                    developerConnection = "scm:git:ssh://github.com/XibalbaM/PronoteKt.git"
                    url = "https://github.com/XibalbaM/PronoteKt"
                }
            }
        }
    }
    repositories {
        maven {
            credentials {
                if (!(project.hasProperty("NEXUS_USERNAME") && project.hasProperty("NEXUS_PASSWORD")))
                    throw IllegalStateException("NEXUS_USERNAME and NEXUS_PASSWORD must be set as project properties")
                username = project.properties["NEXUS_USERNAME"].toString()
                password = project.properties["NEXUS_PASSWORD"].toString()
            }

            name = "pronoteKt"
            url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-cio-jvm:2.3.4")
    implementation("io.ktor:ktor-client-logging-jvm:2.3.4")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-gson:2.3.4")
    implementation("io.ktor:ktor-client-logging:2.3.4")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

java {
    withJavadocJar()
    withSourcesJar()
}