package com.example.timecard.data.repository

import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileRepositoryBenchmarkTest {
    @Test
    fun benchmarkLoadGlobalDir() {
        // Setup
        val tempDir = createTempDirectory("timecard_benchmark").toFile()
        val limitedDir = File(tempDir, "limited_purchases")
        limitedDir.mkdirs()

        val repo = DirectFileRepository(tempDir)

        val filenames = mutableListOf<String>()
        for (i in 1..100) {
            val filename = "claim_$i.json"
            filenames.add(filename)
            File(limitedDir, filename).writeText("{\"approved\": true}")
        }

        // Benchmark N+1
        val startOld = System.currentTimeMillis()
        val oldResults = mutableListOf<String?>()
        for (f in filenames) {
            oldResults.add(repo.loadGlobalDir("limited_purchases", f))
        }
        val timeOld = System.currentTimeMillis() - startOld

        // Benchmark Optimized
        val startNew = System.currentTimeMillis()
        val newResults = repo.loadGlobalDirFiles("limited_purchases", filenames)
        val timeNew = System.currentTimeMillis() - startNew

        println("Old method (N+1) time for 100 files: $timeOld ms")
        println("New method (optimized) time for 100 files: $timeNew ms")

        // Assertion to ensure they return the same number of valid files
        assert(oldResults.size == 100)
        assert(newResults.size == 100)

        tempDir.deleteRecursively()
    }
}
