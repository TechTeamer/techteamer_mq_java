import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
    application
    id("org.sonarqube") version "3.4.0.2513"
}

group = "org.TechTeamer"
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
    implementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.20")
    testImplementation("junit:junit:4.12")
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
            groupId = "org.TechTeamer"
            artifactId = "Kotlin_MQ"
            version = "1.0"

            from(components["java"])
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "TechTeamer_techteamer_mq_java")
    }
}
