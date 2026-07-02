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

tasks.register("publishableModules") {
    description = "Lists subproject paths that have the Maven publish plugin applied"
    doLast {
        subprojects
            .filter { it.plugins.hasPlugin("com.vanniktech.maven.publish") }
            .forEach { println(it.path) }
    }
}
