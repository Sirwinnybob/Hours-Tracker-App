package com.example.timecard.testutil

import java.io.File

/** Locates a file under `app/src/main/java/` by its path relative to that root, walking up from the working directory. */
object SourceFiles {
    fun mainSource(relativePath: String): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidate = File(dir, "app/src/main/java/$relativePath")
            if (candidate.exists()) return candidate
            val direct = File(dir, "src/main/java/$relativePath")
            if (direct.exists()) return direct
            dir = dir.parentFile ?: return@repeat
        }
        error("Unable to locate $relativePath from ${System.getProperty("user.dir")}")
    }
}
