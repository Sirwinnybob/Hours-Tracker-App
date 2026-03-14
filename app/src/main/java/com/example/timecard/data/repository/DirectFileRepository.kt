package com.example.timecard.data.repository

import android.util.Log
import com.example.timecard.data.cache.FileCache
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

    override fun loadShopCatalog(): List<ShopItem> {
        // Diagnostic: log exactly where we're looking
        Log.d("ShopCatalog", "baseDir = ${baseDir.absolutePath}")
        Log.d("ShopCatalog", "shop_catalog.json exists = ${File(baseDir, "shop_catalog.json").exists()}")
        Log.d("ShopCatalog", "baseDir contents = ${baseDir.listFiles()?.joinToString { it.name } ?: "null (no permission?)"}")

        val json = loadGlobalFile("shop_catalog.json", useCache = false)
        if (json != null) {
            // Try wrapped {"items":[...]} format first
            try {
                val mapType = object : TypeToken<Map<String, List<ShopItem>>>() {}.type
                val result: Map<String, List<ShopItem>> = Gson().fromJson(json, mapType)
                val items = result["items"]
                if (!items.isNullOrEmpty()) return items.filter { it.inShop != false }
            } catch (e: Exception) { /* not wrapped format */ }
            // Fall back to bare [...] format
            try {
                val type = object : TypeToken<List<ShopItem>>() {}.type
                val items: List<ShopItem>? = Gson().fromJson(json, type)
                if (!items.isNullOrEmpty()) return items.filter { it.inShop != false }
            } catch (e: Exception) {
                Log.e("DirectFileRepo", "Error parsing shop_catalog.json", e)
            }
        }
        // Fallback default catalog
        Log.w("ShopCatalog", "Using hardcoded defaults — catalog not loaded from disk")
        return listOf(
            ShopItem("accent_sunrise", "Sunrise", "A warm morning gradient.", 250, "Accent", "🌅"),
            ShopItem("accent_twilight", "Twilight", "Evening purple hues.", 250, "Accent", "🌆"),
            ShopItem("accent_isle", "Isle", "Tropical island colors.", 250, "Accent", "🏝️"),
            ShopItem("accent_daybreak", "Daybreak", "Early bright sky.", 250, "Accent", "🌤️"),
            ShopItem("accent_red", "Red Alert", "High visibility red.", 750, "Accent", "🚨"),
            ShopItem("accent_sunset", "Sunset", "Vibrant evening sun.", 750, "Accent", "🌇"),
            ShopItem("accent_midnight", "Midnight", "Deep dark blues.", 1000, "Accent", "🌌"),
            ShopItem("accent_ocean", "Ocean", "Deep sea vibes.", 1000, "Accent", "🌊"),
            ShopItem("accent_royal", "Royal", "Premium purple.", 1500, "Accent", "👑"),
            ShopItem("accent_hacker", "Hacker", "Terminal green.", 1500, "Accent", "💻"),

            ShopItem("feature_custom_avatar", "Custom Avatar", "Upload your own photo as an avatar.", 2000, "Feature", "🖼️"),
            ShopItem("feature_display_name", "Custom Name", "Set a custom display name.", 2500, "Feature", "✏️"),
            ShopItem("consumable_send_note", "Send a Note", "Send an alert note to another employee.", 30, "Consumable", "✉️"),
            ShopItem("consumable_send_anonymous_note", "Send Anonymous Note", "Send a note without revealing your identity.", 60, "Consumable", "👻")
        )
    }

    override fun loadFile(name: String, date: String): String? {
        val cacheKey = "$name/$date.json"
        FileCache.get(cacheKey)?.let {
            Log.d("FileCache", "Cache HIT: $cacheKey")
            return it
        }
        return try {
            val file = File(File(baseDir, name), "$date.json")
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
        val cacheKey = "$name/$date.json"
        return try {
            val empDir = File(baseDir, name)
            if (!empDir.exists() && !empDir.mkdirs()) {
                return "Error: Could not create directory for $name"
            }
            val filename = "$date.json"
            val file = File(empDir, filename)
            val tempFile = File(empDir, "$filename.tmp")

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
        val cacheKey = "$name/$filename"
        if (useCache) {
            FileCache.get(cacheKey)?.let {
                Log.d("FileCache", "Cache HIT: $cacheKey")
                return it
            }
        }
        return try {
            val file = File(File(baseDir, name), filename)
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
        val cacheKey = "$name/$filename"
        return try {
            val empDir = File(baseDir, name)
            if (!empDir.exists() && !empDir.mkdirs()) {
                return "Error: Could not create directory"
            }
            val file = File(empDir, filename)
            val tempFile = File(empDir, "$filename.tmp")

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
        val cacheKey = "global/$filename"
        if (useCache) {
            FileCache.get(cacheKey)?.let {
                Log.d("FileCache", "Cache HIT: $cacheKey")
                return it
            }
        }
        return try {
            val file = File(baseDir, filename)
            if (!file.exists()) return null
            val content = readFileContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading global file: $filename", e)
            null
        }
    }

    override fun loadGlobalBinaryFile(relativePath: String): ByteArray? {
        return try {
            val file = File(baseDir, relativePath.replace("\\", "/"))
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading binary file: $relativePath", e)
            null
        }
    }

    override fun loadEmployeeBinaryFile(name: String, filename: String): ByteArray? {
        return try {
            val empDir = File(baseDir, name)
            // Support wildcard extension: find first file matching prefix
            if (filename.contains("*")) {
                val prefix = filename.substringBefore("*")
                val match = empDir.listFiles()?.firstOrNull { it.name.startsWith(prefix) }
                return match?.readBytes()
            }
            val file = File(empDir, filename)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading employee binary: $name/$filename", e)
            null
        }
    }

    override fun saveEmployeeBinaryFile(name: String, filename: String, data: ByteArray): Boolean {
        return try {
            val empDir = File(baseDir, name)
            if (!empDir.exists() && !empDir.mkdirs()) return false
            // Delete any existing .avatar.* file to avoid stale extension conflicts
            if (filename.startsWith(".avatar")) {
                empDir.listFiles()?.filter { it.name.startsWith(".avatar") }?.forEach { it.delete() }
            }
            File(empDir, filename).writeBytes(data)
            true
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error saving employee binary: $name/$filename", e)
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
            val empDir = File(baseDir, name)
            if (!empDir.exists() || !empDir.isDirectory) return emptyList()

            // Find files matching date pattern "YYYY-MM-DD.json" only
            val files = empDir.listFiles { _, filename ->
                filename.endsWith(".json") &&
                filename.length == 15 && // "YYYY-MM-DD.json" = 15 chars
                filename[4] == '-' && filename[7] == '-'
            } ?: return emptyList()

            files.map { it.name.removeSuffix(".json") }
                 .sortedDescending() // Newest first
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error getting available dates", e)
            emptyList()
        }
    }

    override fun acquireLock(name: String, date: String, deviceId: String): Boolean {
        val empDir = File(baseDir, name)
        if (!empDir.exists()) empDir.mkdirs()
        val lockFile = File(empDir, "$date.json.lock")

        if (lockFile.exists()) {
            val content = readFileContent(lockFile) ?: ""
            val parts = content.split(",")
            if (parts.size == 2) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val ownerDeviceId = parts[1]
                
                if (ownerDeviceId == deviceId) {
                    renewLock(name, date, deviceId) // We already own it, renew implicitly
                    return true
                }

                // If another device owns it, check if it's expired (>5 mins = 300,000 ms)
                if (System.currentTimeMillis() - timestamp < 300_000L) {
                    return false // Lock active & owned by someone else
                }
            }
        }

        // Lock doesn't exist, or is corrupted, or is expired. Acquire it.
        renewLock(name, date, deviceId)
        return true
    }

    override fun renewLock(name: String, date: String, deviceId: String) {
        try {
            val empDir = File(baseDir, name)
            if (!empDir.exists()) empDir.mkdirs()
            val lockFile = File(empDir, "$date.json.lock")
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
        val lockFile = File(File(baseDir, name), "$date.json.lock")
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
            val subDir = File(File(baseDir, name), subdirectory)
            if (!subDir.exists() || !subDir.isDirectory) return emptyList()
            subDir.listFiles { f -> f.isFile }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error listing files in $name/$subdirectory", e)
            emptyList()
        }
    }

    override fun saveInDir(name: String, subdirectory: String, filename: String, json: String): String {
        return try {
            val subDir = File(File(baseDir, name), subdirectory)
            if (!subDir.exists() && !subDir.mkdirs()) {
                return "Error: Could not create directory $name/$subdirectory"
            }
            val file = File(subDir, filename)
            val tempFile = File(subDir, "$filename.tmp")
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
            val file = File(File(File(baseDir, name), subdirectory), filename)
            if (!file.exists()) return null
            readFileContent(file)
        } catch (e: Exception) {
            Log.e("DirectFileRepo", "Error loading $name/$subdirectory/$filename", e)
            null
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
