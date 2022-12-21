import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "json4k"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    registerFeature("jackson") {
        usingSourceSet(sourceSets["main"])
    }

    registerFeature("vertx") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {

    "jacksonImplementation"("com.fasterxml.jackson.core:jackson-core:2.14.1")
    "jacksonImplementation"("com.fasterxml.jackson.jr:jackson-jr-stree:2.14.1")
    "vertxImplementation"("io.vertx:vertx-json-schema:4.3.7")

    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}