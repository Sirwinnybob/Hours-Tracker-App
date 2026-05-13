package com.example.timecard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PathTraversalTest {

    @Test
    fun testPathTraversalInLoadGenericJSON() {
        val rootDir = createTempDirectory("path_traversal_test").toFile()
        val baseDir = File(rootDir, "base").apply { mkdirs() }
        val secretFile = File(rootDir, "secret.json").apply { writeText("secret content") }

        val repo = DirectFileRepository(baseDir)

        // Attempt to read secret.json using path traversal in 'name'
        val content = repo.loadGenericJSON("../", "secret.json", useCache = false)

        // Before fix, this returned "secret content"
        // After fix, it returns null because "../" is sanitized to "" and "base/.secret.json" doesn't exist
        // or actually "../" becomes "" and it looks for "secret.json" inside "base/"
        assertNull(content)

        rootDir.deleteRecursively()
    }
}
