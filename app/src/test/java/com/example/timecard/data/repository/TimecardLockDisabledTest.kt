package com.example.timecard.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TimecardLockDisabledTest {
    @Test fun legacyLocksDoNotBlockOrCreateLeases() {
        val root = Files.createTempDirectory("timecard-lock-test").toFile()
        try {
            val employee = root.resolve("1234").apply { mkdirs() }
            val legacy = employee.resolve("2026-11-02.json.lock")
            legacy.writeText("${System.currentTimeMillis()},another-device")
            val repo = DirectFileRepository(root)
            assertTrue(repo.acquireLock("1234", "2026-11-02", "this-device"))
            repo.renewLock("1234", "2026-11-09", "this-device")
            assertFalse(employee.resolve("2026-11-09.json.lock").exists())
            assertTrue(legacy.readText().endsWith(",another-device"))
        } finally { root.deleteRecursively() }
    }
}
