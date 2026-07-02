plugins {
    id("git-version")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    version = rootProject.version
}


tasks.register("printVersions") {
    description = "Prints resolved version for each subproject (debug helper)"
    doLast {
        allprojects.forEach {
            println("[${it.path}] version = ${it.version}")
        }
    }
}
