import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
    application
    id ("org.sonarqube") version "3.3"
}

group = "com.facekom"
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
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")
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

buildscript {

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.facekom"
            artifactId = "mq_kotlin"
            version = "1.0"

            from(components["java"])
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "TechTeamer_techteamer_mq_java")
        property("sonar.organization", "techteamer")
    }
}
