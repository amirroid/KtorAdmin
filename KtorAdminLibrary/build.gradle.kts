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

    // Reflection
    implementation("org.reflections:reflections:0.9.12")

    // Ktor Core
    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    // Templating
    implementation("io.ktor:ktor-server-velocity:$ktorVersion")

    // JDBC
    implementation("com.vladsch.kotlin-jdbc:kotlin-jdbc:0.5.0")
    implementation("com.zaxxer:HikariCP:6.2.1")

    // S3
    implementation("software.amazon.awssdk:s3:2.30.2")

    // Authentication
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    // MongoDB Kotlin driver dependency
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.1")
}
tasks.test {
    useJUnitPlatform()
}