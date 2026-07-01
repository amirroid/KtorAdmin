plugins {
    id("git-version")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    version = rootProject.version
}
