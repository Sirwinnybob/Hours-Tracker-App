package com.example.timecard.data.repository

import android.util.Log
import com.example.timecard.util.PathSanitizer
import com.example.timecard.data.cache.FileCache
import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.ActivityFeed
import com.example.timecard.data.model.Challenge
import com.example.timecard.data.model.ChallengeCatalog
import com.example.timecard.data.model.ShopItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class DirectFileRepository(private val baseDir: File) : FileRepository {

    override fun loadShopCatalog(): List<ShopItem> = loadShopCatalogInternal(filterPool = true)

    override fun loadFullShopCatalog(): List<ShopItem> = loadShopCatalogInternal(filterPool = false)

    private fun loadShopCatalogInternal(filterPool: Boolean): List<ShopItem> {
        val json = loadGlobalFile("shop_catalog.json", useCache = false)
        if (json != null) {
            // Try wrapped {"items":[...]} format first
            try {
                val mapType = object : TypeToken<Map<String, List<ShopItem>>>() {}.type
                val result: Map<String, List<ShopItem>> = Gson().fromJson(json, mapType)
                val items = result["items"]
                if (!items.isNullOrEmpty()) return if (filterPool) items.filter { it.inShop } else items
            } catch (e: Exception) { /* not wrapped format */ }
            // Fall back to bare [...] format
            try {
                val type = object : TypeToken<List<ShopItem>>() {}.type
                val items: List<ShopItem>? = Gson().fromJson(json, type)
                if (!items.isNullOrEmpty()) return if (filterPool) items.filter { it.inShop } else items
            } catch (e: Exception) {
                Log.e("DirectFileRepo", "Error parsing shop_catalog.json", e)
            }
        }
        return emptyList()
    }

    override fun loadFile(name: String, date: String): String? {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val cacheKey = "$sName/$sDate.json"
        FileCache.get(cacheKey)?.let {
            Log.d("FileCache", "Cache HIT: $cacheKey")
            return it
        }
        return try {
            val file = File(File(baseDir, sName), "$sDate.json")
            if (!file.exists()) return null
            val content = readFileContent(file)
            if (content != null) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading file: $cacheKey", e)
            null
        }
    }

    override fun saveJSON(json: String, name: String, date: String): String {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val cacheKey = "$sName/$sDate.json"
        return try {
            val empDir = File(baseDir, sName)
            if (!empDir.exists() && !empDir.mkdirs()) {
                return "Error: Could not create directory for $sName"
            }
            val sFilenameFinal = "$sDate.json"
            val file = File(empDir, sFilenameFinal)
            val tempFile = File(empDir, "$sDate.tmp")

            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }

            val renamed = atomicRename(tempFile, file)
            if (renamed) {
                FileCache.put(cacheKey, json)
                Log.d("FileCache", "Cache updated after save: $cacheKey")
                "SUCCESS"
            } else {
                "Error: Could not rename temp file"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadAlerts(name: String): String? = loadGenericJSON(name, "alerts.json")

    override fun loadAcknowledgements(name: String): String? = loadGenericJSON(name, "acknowledgements.json")

    override fun saveAcknowledgements(name: String, json: String): String =
        saveGenericJSON(name, "acknowledgements.json", json)

    override fun loadEmployeeList(): String? {
        return try {
            val file = File(baseDir, "employees.json")
            if (!file.exists()) return null
            readFileContent(file)
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading employee list", e)
            null
        }
    }

    override fun loadGenericJSON(name: String, filename: String, useCache: Boolean): String? {
        val sName = PathSanitizer.sanitize(name)
        val sFilename = PathSanitizer.sanitize(filename)
        val cacheKey = "$sName/$sFilename"
        if (useCache) {
            FileCache.get(cacheKey)?.let {
                Log.d("FileCache", "Cache HIT: $cacheKey")
                return it
            }
        }
        return try {
            val file = File(File(baseDir, sName), sFilename)
            if (!file.exists()) return null
            val content = readFileContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading: $cacheKey", e)
            null
        }
    }

    override fun saveGenericJSON(name: String, filename: String, json: String): String {
        val sName = PathSanitizer.sanitize(name)
        val sFilename = PathSanitizer.sanitize(filename)
        val cacheKey = "$sName/$sFilename"
        return try {
            val empDir = File(baseDir, sName)
            if (!empDir.exists() && !empDir.mkdirs()) {
                return "Error: Could not create directory"
            }
            val file = File(empDir, sFilename)
            val tempFile = File(empDir, "$sFilename.tmp")

            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }

            val renamed = atomicRename(tempFile, file)
            if (renamed) {
                FileCache.put(cacheKey, json)
                Log.d("FileCache", "Cache updated after save: $cacheKey")
                "SUCCESS"
            } else {
                "Error: Could not rename temp file"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun readFileContent(file: File): String? {
        return FileInputStream(file).use { fis ->
            BufferedReader(InputStreamReader(fis)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                sb.toString()
            }
        }
    }

    override fun loadGlobalFile(filename: String, useCache: Boolean): String? {
        val sFilename = PathSanitizer.sanitize(filename)
        val cacheKey = "global/$sFilename"
        if (useCache) {
            FileCache.get(cacheKey)?.let {
                Log.d("FileCache", "Cache HIT: $cacheKey")
                return it
            }
        }
        return try {
            val file = File(baseDir, sFilename)
            if (!file.exists()) return null
            val content = readFileContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading global file: $sFilename", e)
            null
        }
    }

    override fun loadGlobalBinaryFile(relativePath: String): ByteArray? {
        return try {
            val sPath = PathSanitizer.sanitizePath(relativePath)
            val file = File(baseDir, sPath)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading binary file: $sPath", e)
            null
        }
    }

    override fun loadEmployeeBinaryFile(name: String, filename: String): ByteArray? {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sFilename = PathSanitizer.sanitize(filename)
            val empDir = File(baseDir, sName)
            // Support wildcard extension: find first file matching prefix
            if (sFilename.contains("*")) {
                val prefix = sFilename.substringBefore("*")
                val match = empDir.listFiles()?.firstOrNull { it.name.startsWith(prefix) }
                return match?.readBytes()
            }
            val file = File(empDir, sFilename)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading employee binary: $sName/$sFilename", e)
            null
        }
    }

    override fun saveEmployeeBinaryFile(name: String, filename: String, data: ByteArray): Boolean {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sFilename = PathSanitizer.sanitize(filename)
            val empDir = File(baseDir, sName)
            if (!empDir.exists() && !empDir.mkdirs()) return false
            // Delete any existing .avatar.* file to avoid stale extension conflicts
            if (sFilename.startsWith(".avatar")) {
                empDir.listFiles()?.filter { it.name.startsWith(".avatar") }?.forEach { it.delete() }
            }
            File(empDir, sFilename).writeBytes(data)
            true
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error saving employee binary: $sName/$sFilename", e)
            false
        }
    }

    override fun listEmployeeFolders(): List<String> {
        return try {
            baseDir.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
                ?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error listing employee folders", e)
            emptyList()
        }
    }

    override fun getAvailableDates(name: String): List<String> {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val empDir = File(baseDir, sName)
            if (!empDir.exists() || !empDir.isDirectory) return emptyList()

            // Find files matching date pattern "YYYY-MM-DD.json" only
            val files = empDir.listFiles { _, f ->
                f.endsWith(".json") &&
                f.length == 15 && // "YYYY-MM-DD.json" = 15 chars
                f[4] == '-' && f[7] == '-'
            } ?: return emptyList()

            files.map { it.name.removeSuffix(".json") }
                 .sortedDescending() // Newest first
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error getting available dates", e)
            emptyList()
        }
    }

    override fun acquireLock(name: String, date: String, deviceId: String): Boolean {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val empDir = File(baseDir, sName)
        if (!empDir.exists()) empDir.mkdirs()
        val lockFile = File(empDir, "$sDate.json.lock")

        if (lockFile.exists()) {
            val content = readFileContent(lockFile) ?: ""
            val parts = content.split(",")
            if (parts.size == 2) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val ownerDeviceId = parts[1]
                
                if (ownerDeviceId == deviceId) {
                    renewLock(sName, sDate, deviceId) // We already own it, renew implicitly
                    return true
                }

                // If another device owns it, check if it's expired (>5 mins = 300,000 ms)
                if (System.currentTimeMillis() - timestamp < 300_000L) {
                    return false // Lock active & owned by someone else
                }
            }
        }

        // Lock doesn't exist, or is corrupted, or is expired. Acquire it.
        renewLock(sName, sDate, deviceId)
        return true
    }

    override fun renewLock(name: String, date: String, deviceId: String) {
        try {
            val sName = PathSanitizer.sanitize(name)
            val sDate = PathSanitizer.sanitize(date)
            val empDir = File(baseDir, sName)
            if (!empDir.exists()) empDir.mkdirs()
            val lockFile = File(empDir, "$sDate.json.lock")
            val timestamp = System.currentTimeMillis()
            
            FileOutputStream(lockFile).use { fos ->
                fos.write("$timestamp,$deviceId".toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error renewing lock", e)
        }
    }

    override fun releaseLock(name: String, date: String, deviceId: String) {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val lockFile = File(File(baseDir, sName), "$sDate.json.lock")
        if (lockFile.exists()) {
            val content = readFileContent(lockFile) ?: ""
            val parts = content.split(",")
            if (parts.size == 2 && parts[1] == deviceId) {
                lockFile.delete()
            }
        }
    }

    override fun listFilesInDir(name: String, subdirectory: String): List<String> {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sSub = PathSanitizer.sanitize(subdirectory)
            val subDir = File(File(baseDir, sName), sSub)
            if (!subDir.exists() || !subDir.isDirectory) return emptyList()
            subDir.listFiles { f -> f.isFile }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error listing files in $sName/$sSub", e)
            emptyList()
        }
    }

    override fun saveInDir(name: String, subdirectory: String, filename: String, json: String): String {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val subDir = File(File(baseDir, sName), sSub)
            if (!subDir.exists() && !subDir.mkdirs()) {
                return "Error: Could not create directory $sName/$sSub"
            }
            val file = File(subDir, sFilename)
            val tempFile = File(subDir, "$sFilename.tmp")
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (atomicRename(tempFile, file)) "SUCCESS"
            else "Error: Could not rename temp file"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadFromDir(name: String, subdirectory: String, filename: String): String? {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val file = File(File(File(baseDir, sName), sSub), sFilename)
            if (!file.exists()) return null
            readFileContent(file)
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading $sName/$sSub/$sFilename", e)
            null
        }
    }

    override fun loadEmployeeActivityEvents(name: String): List<ActivityEvent> {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val file = File(File(baseDir, sName), "activity_events.json")
            if (!file.exists()) return emptyList()
            val json = readFileContent(file) ?: return emptyList()
            Gson().fromJson(json, ActivityFeed::class.java)?.events ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading activity events for $sName", e)
            emptyList()
        }
    }

    override fun saveEmployeeActivityEvents(name: String, events: List<ActivityEvent>) {
        try {
            val sName = PathSanitizer.sanitize(name)
            val empDir = File(baseDir, sName)
            if (!empDir.exists()) empDir.mkdirs()
            val file = File(empDir, "activity_events.json")
            val tempFile = File(empDir, "activity_events.json.tmp")
            val json = Gson().toJson(ActivityFeed(events.take(50)))
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            atomicRename(tempFile, file)
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error saving activity events for $sName", e)
        }
    }

    override fun loadChallenges(): List<Challenge> {
        val json = loadGlobalFile("challenges.json", useCache = false) ?: return emptyList()
        return try {
            Gson().fromJson(json, ChallengeCatalog::class.java)?.challenges ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error parsing challenges.json", e)
            emptyList()
        }
    }

    override fun saveGlobalDir(subdirectory: String, filename: String, json: String): String {
        return try {
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val subDir = File(baseDir, sSub)
            if (!subDir.exists() && !subDir.mkdirs()) {
                return "Error: Could not create directory $sSub"
            }
            val file = File(subDir, sFilename)
            val tempFile = File(subDir, "$sFilename.tmp")
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (atomicRename(tempFile, file)) "SUCCESS"
            else "Error: Could not rename temp file"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadGlobalDir(subdirectory: String, filename: String): String? {
        return try {
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val file = File(File(baseDir, sSub), sFilename)
            if (!file.exists()) return null
            readFileContent(file)
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading $sSub/$sFilename", e)
            null
        }
    }

    override fun loadGlobalDirFiles(subdirectory: String, filenames: List<String>): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        try {
            val sSub = PathSanitizer.sanitize(subdirectory)
            val subDir = File(baseDir, sSub)
            if (!subDir.exists() || !subDir.isDirectory) return emptyMap()
            for (f in filenames) {
                try {
                    val sFilename = PathSanitizer.sanitize(f)
                    val file = File(subDir, sFilename)
                    if (file.exists() && file.isFile) {
                        result[f] = readFileContent(file)
                    }
                } catch (e: Exception) {
                    Log.e("DirectFileRepo", "Error loading file $sFilename from $sSub", e)
                }
            }
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error accessing directory $sSub", e)
        }
        return result
    }

    override fun listGlobalDir(subdirectory: String): List<String> {
        return try {
            val sSub = PathSanitizer.sanitize(subdirectory)
            val subDir = File(baseDir, sSub)
            if (!subDir.exists() || !subDir.isDirectory) return emptyList()
            subDir.listFiles { f -> f.isFile }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error listing files in $sSub", e)
            emptyList()
        }
    }

    private fun atomicRename(from: File, to: File): Boolean {
        for (i in 0 until 3) {
            if (from.renameTo(to)) return true
            if (to.exists() && to.delete() && from.renameTo(to)) return true
            try { Thread.sleep(50) } catch (_: InterruptedException) {}
        }
        return false
    }
}
