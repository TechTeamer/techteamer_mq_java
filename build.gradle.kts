import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
    application
}

group = "org.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.rabbitmq:amqp-client:5.14.2")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.12.4")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.gradle.sample"
            artifactId = "library"
            version = "1.1"

            from(components["java"])
        }
    }
}

