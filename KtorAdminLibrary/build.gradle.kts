plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

group = "ir.amirreza"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // KSP
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")

    // Kotlin Poet
    implementation("com.squareup:kotlinpoet:1.11.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
}

tasks.test {
    useJUnitPlatform()
}