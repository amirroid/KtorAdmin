fun resolveVersionFromGit(): String {
    return try {
        val process = ProcessBuilder(
            "git", "describe", "--tags", "--always", "--dirty"
        )
            .directory(rootProject.projectDir)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val error = process.errorStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode != 0 || output.isEmpty()) {
            logger.warn("git describe failed (exit=$exitCode): $error")
            "0.0.0"
        } else {
            val cleaned = output.removePrefix("v")
            val versionRegex = Regex("^\\d+\\.\\d+\\.\\d+")
            val match = versionRegex.find(cleaned)

            if (match == null) {
                logger.warn("Could not extract semantic version from git describe output: $cleaned")
                "0.0.0"
            } else {
                match.value
            }
        }
    } catch (ex: Exception) {
        logger.warn("git describe threw: ${ex.message}")
        "0.0.0"
    }
}

version = resolveVersionFromGit()
