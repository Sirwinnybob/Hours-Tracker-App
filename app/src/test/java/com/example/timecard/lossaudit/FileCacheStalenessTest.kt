package com.example.timecard.lossaudit

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FileCache is backed by android.util.LruCache, which is a no-op stub in JVM unit tests
 * (isReturnDefaultValues), so the production cache never engages under the normal test classpath.
 * This test loads the real FileCache + DirectFileRepository in an isolated child-first class loader
 * whose android.util.LruCache is a working map (compiled at test time), leaving every other test's
 * classpath untouched.
 */
class FileCacheStalenessTest {

    private val shimSource = """
        package android.util;
        public class LruCache<K, V> {
            private final java.util.LinkedHashMap<K, V> map = new java.util.LinkedHashMap<>();
            public LruCache(int maxSize) {}
            public final synchronized V get(K key) { return map.get(key); }
            public final synchronized V put(K key, V value) { return map.put(key, value); }
            public final synchronized void evictAll() { map.clear(); }
        }
    """.trimIndent()

    private fun compileShim(): ByteArray {
        val dir = Files.createTempDirectory("lru-shim").toFile()
        val src = File(dir, "android/util/LruCache.java").apply { parentFile.mkdirs(); writeText(shimSource) }
        // javax.tools is absent from the android.jar compile classpath; reach it reflectively at runtime.
        val javac = Class.forName("javax.tools.ToolProvider").getMethod("getSystemJavaCompiler").invoke(null)
            ?: error("JDK compiler not available")
        val run = Class.forName("javax.tools.Tool").getMethod(
            "run", java.io.InputStream::class.java, java.io.OutputStream::class.java,
            java.io.OutputStream::class.java, Array<String>::class.java
        )
        val rc = run.invoke(javac, null, null, null, arrayOf("-d", dir.path, src.path)) as Int
        check(rc == 0) { "shim compile failed" }
        return File(dir, "android/util/LruCache.class").readBytes().also { dir.deleteRecursively() }
    }

    private class ChildFirst(parent: ClassLoader, private val shim: ByteArray) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(this) {
            findLoadedClass(name)?.let { return it }
            val bytes = when {
                name == "android.util.LruCache" -> shim
                name.startsWith("com.example.timecard.") && !name.startsWith("com.example.timecard.lossaudit.") ->
                    parent.getResourceAsStream(name.replace('.', '/') + ".class")?.use { it.readBytes() }
                else -> null
            } ?: return super.loadClass(name, resolve)
            defineClass(name, bytes, 0, bytes.size)
        }
    }

    @Test fun `tablet re-reads its own cached copy and overwrites a newer file from another tablet or the server`() {
        val loader = ChildFirst(javaClass.classLoader!!, compileShim())
        val repoClass = loader.loadClass("com.example.timecard.data.repository.DirectFileRepository")
        val loadFile = repoClass.getMethod("loadFile", String::class.java, String::class.java)
        val saveJSON = repoClass.getMethod("saveJSON", String::class.java, String::class.java, String::class.java)
        val week = "2026-11-02"
        val root = Files.createTempDirectory("loss-cache").toFile()
        try {
            val onDisk = root.resolve("101/$week.json").apply { parentFile.mkdirs() }
            val v1 = card("101", week, """{"job":"SHOP","mon":"9.00"}""")
            onDisk.writeText(v1)

            // Monday on tablet A: employee opens the card (cached for the life of the process).
            val tabletA = repoClass.getConstructor(File::class.java).newInstance(root)
            assertEquals(v1, loadFile.invoke(tabletA, "101", week))

            // Tuesday on tablet B (or an office edit via /manual-timecard): Syncthing delivers v2 to A's disk.
            val v2 = card("101", week, """{"job":"SHOP","mon":"9.00","tue":"9.00"}""")
            onDisk.writeText(v2)

            // Wednesday back on tablet A (same process; even a new repository instance shares FileCache).
            val tabletAagain = repoClass.getConstructor(File::class.java).newInstance(root)
            val seen = loadFile.invoke(tabletAagain, "101", week) as String
            assertEquals("tablet A shows the stale Monday-only card", v1, seen)
            assertFalse(seen.contains("tue"))

            // Employee adds Wednesday; the VM serializes what it loaded + the edit, overwriting v2.
            val v3 = card("101", week, """{"job":"SHOP","mon":"9.00","wed":"9.00"}""")
            assertEquals("SUCCESS", saveJSON.invoke(tabletAagain, v3, "101", week))
            val final = onDisk.readText()
            assertTrue(final.contains("\"wed\""))
            assertFalse("Tuesday's 9h from tablet B is gone: $final", final.contains("\"tue\""))

            // Sanity: the cache really is the production FileCache object.
            val cache = loader.loadClass("com.example.timecard.data.cache.FileCache").getField("INSTANCE").get(null)
            assertNotNull(cache.javaClass.getMethod("get", String::class.java).invoke(cache, "101/$week.json"))
        } finally { root.deleteRecursively() }
    }
}
