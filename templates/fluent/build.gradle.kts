plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

group = "ir.amirroid.ktoradmin.templates"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.javaV.map { it.toInt() }.get()))
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "fluent",
        version = version.toString(),
    )

    pom {
        name = "KtorAdmin Fluent Template"
        description = "A Microsoft Fluent UI themed admin template for KtorAdmin."
        inceptionYear = "2026"
        url = "https://github.com/amirroid/KtorAdmin"
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
                url = "https://github.com/amirroid"
            }
        }
        scm {
            url = "https://github.com/amirroid/KtorAdmin"
            connection = "scm:git:git://github.com/amirroid/KtorAdmin.git"
            developerConnection = "scm:git:ssh://git@github.com/amirroid/KtorAdmin.git"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":KtorAdminLibrary"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.velocity)
}

tasks.jar {
    from(
        rootProject.files("KtorAdminLibrary/src/main/kotlin/ir/amirroid/ktoradmin/template/AdminTemplate.kt"),
        rootProject.files("KtorAdminLibrary/src/main/kotlin/ir/amirroid/ktoradmin/template/TemplateModel.kt"),
    )
}
