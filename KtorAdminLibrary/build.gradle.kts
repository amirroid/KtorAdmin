import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

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
    testImplementation(kotlin("test"))

    // KSP
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")

    // Kotlin Poet
    implementation("com.squareup:kotlinpoet:1.11.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")

    // Reflection
    implementation("org.reflections:reflections:0.10.2")

    // Ktor Core
    val ktorVersion = "3.2.2"
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

    // Rate Limiting
    implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")

    // IText
    implementation("com.itextpdf:itext7-core:7.2.3")
}
tasks.test {
    useJUnitPlatform()
}