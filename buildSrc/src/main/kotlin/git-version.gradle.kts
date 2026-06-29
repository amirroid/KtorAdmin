
fun resolveVersionFromGit(): String {
    return try {
        val process = ProcessBuilder(
            "git", "describe", "--tags", "--always", "--dirty"
        )
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode != 0 || output.isEmpty()) {
            "0.0.0"
        } else {
            output.removePrefix("v")
        }
    } catch (ex: Exception) {
        "0.0.0"
    }
}

version = resolveVersionFromGit()
