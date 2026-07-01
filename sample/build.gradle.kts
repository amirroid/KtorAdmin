plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jpa)
}

group = "ir.amirreza"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.h2)

    implementation(libs.ktor.server.thymeleaf)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback)
    implementation(libs.ktor.server.config.yaml)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.status.pages)

    implementation(libs.postgresql)

    implementation(project(":KtorAdminLibrary"))
    ksp(project(":KtorAdminLibrary"))

    implementation(project(":templates:fluent"))

    implementation(libs.jcodec)
    implementation(libs.jcodec.javase)

    implementation(libs.mongodb.coroutine)
    implementation(libs.mongodb.bson.kotlinx)

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.hibernate.core)
    implementation(libs.javax.persistence.api)
    implementation(libs.hibernate.validator)
}
