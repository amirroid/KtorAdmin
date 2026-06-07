
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}


group = "io.github.amirroid"
version = "0.0.7"

val projectName = "KtorAdmin"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = projectName,
        version = version.toString()
    )
    pom {
        name = projectName
        description =
            "KtorAdmin is a dynamic admin panel for Ktor applications, supporting ORM structures without predefined schemas and simplifying data management with advanced features."
        inceptionYear = "2025"
        url = "https://github.com/Amirroid/KtorAdmin"
        licenses {
            license {
                name = "The MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "Amirroid"
                name = "Amirreza Gholami"
                url = "https://github.com/Amirroid"
            }
        }
        scm {
            url = "https://github.com/Amirroid/KtorAdmin"
            connection = "scm:git:git://github.com/Amirroid/KtorAdmin.git"
            developerConnection = "scm:git:ssh://git@github.com/Amirroid/KtorAdmin.git"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.sessions)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.h2)

    implementation(libs.ksp.api)

    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    implementation(libs.reflections)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.velocity)

    implementation(libs.kotlin.jdbc)
    implementation(libs.hikari)

    implementation(libs.aws.s3)

    implementation(libs.ktor.server.auth)

    implementation(libs.mongodb.coroutine)

    implementation(libs.ktor.server.rate.limit)

    implementation(libs.itext.core)
}

tasks.test {
    useJUnitPlatform()
}