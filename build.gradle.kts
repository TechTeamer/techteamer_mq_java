import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
    signing
    application
    id("org.sonarqube") version "3.3"
    id("jacoco")
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.owasp.dependencycheck") version "8.0.1"
    id("com.github.ben-manes.versions") version "0.42.0"
}

group = "com.facekom"
version = "1.4.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.rabbitmq:amqp-client:5.22.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.5.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    useJUnitPlatform {
        includeEngines("junit-jupiter")
        excludeEngines("junit-vintage")
    }
    finalizedBy(tasks.jacocoTestReport)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}

tasks.withType<JavaCompile>().all {
    targetCompatibility = JavaVersion.VERSION_11.toString()
    sourceCompatibility = JavaVersion.VERSION_11.toString()
}

application {
    mainClass.set("MainKt")
}

buildscript {
}

tasks.named("check") {
    dependsOn(tasks.named("dependencyCheckAnalyze"))
}
// For reproducible builds, see: https://docs.gradle.org/7.0/userguide/dependency_locking.html
dependencyLocking {
    lockAllConfigurations()
}

tasks.getByName<Zip>("distZip").archiveFileName.set("${project.name}.zip")
tasks.getByName<Tar>("distTar").enabled = false

dependencyCheck {
    // Specifies if the build should be failed if a CVSS score above a specified level is identified.
    // See: https://nvd.nist.gov/vuln-metrics/cvss
    failBuildOnCVSS = 6.6f
    scanConfigurations = mutableListOf("runtimeClasspath")
}

java {
    withJavadocJar()
    withSourcesJar()
}

val sonatypeUsername: String? = System.getenv("SONATYPE_USERNAME")
val sonatypePassword: String? = System.getenv("SONATYPE_PASSWORD")
val artifactName: String = "mq"

publishing {
    publications {
        repositories {
            maven {
                name = artifactName
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = sonatypeUsername
                    password = sonatypePassword
                }
            }
        }
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = artifactName
            version = version

            from(components["java"])
            pom {
                name.set(artifactName)
                description.set("A RabbitMQ wrapper for java written in Kotlin")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                url.set("https://github.com/TechTeamer/techteamer_mq_java")
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/TechTeamer/techteamer_mq_java/issues")
                }
                scm {
                    connection.set("https://github.com/TechTeamer/techteamer_mq_java.git")
                    url.set("https://github.com/TechTeamer/techteamer_mq_java")
                }
                developers {
                    developer {
                        name.set("Zoltan Nagy")
                        email.set("zoltan.nagy@facekom.com")
                    }
                    developer {
                        name.set("Bence Tihanyi")
                        email.set("bence.tihanyi@facekom.com")
                    }
                }
            }
        }
        create<MavenPublication>("local") {
            from(components["java"])
            groupId = group.toString()
            artifactId = artifactName
            version = version
        }
    }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    onlyIf {
        (publication == publishing.publications["local"])
    }
}

signing {
    setRequired {
        // signing is required if this is a release version and the artifacts are to be published
        // do not use hasTask() as this require realization of the tasks that maybe are not necessary
        gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY_ID"),
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PRIVATE_PASSWORD")
    )
    sign(publishing.publications)
}

sonarqube {
    properties {
        property("sonar.projectKey", "TechTeamer_techteamer_mq_java")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "src/main/")
        property("sonar.tests", "src/test/")
        property("sonar.core.codeCoveragePlugin", "jacoco")
        property("sonar.verbose", "true")
        property("sonar.binaries", "build/classes/kotlin")
        property("sonar.dynamicAnalysis", "reuseReports")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
    }
}

tasks.named("sonarqube") {
    dependsOn(tasks.named("jacocoTestReport"))
}

jacoco {
    toolVersion = "0.8.7"
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "mq"
    manifest {
        attributes["Implementation-Title"] = "Mq fat jar"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "com.facekom.mq.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
