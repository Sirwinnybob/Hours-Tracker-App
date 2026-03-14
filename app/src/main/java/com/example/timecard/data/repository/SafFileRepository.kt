package com.example.timecard.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.timecard.data.cache.FileCache
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SafFileRepository(
    private val context: Context,
    private val treeUri: Uri
) : FileRepository {

    override fun loadFile(name: String, date: String): String? {
        val cacheKey = "$name/$date.json"
        FileCache.get(cacheKey)?.let {
            Log.d("FileCache", "Cache HIT: $cacheKey")
            return it
        }
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(name) ?: return null
            val file = empDir.findFile("$date.json") ?: return null
            val content = readDocumentContent(file)
            if (content != null) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading: $cacheKey", e)
            null
        }
    }

    override fun saveJSON(json: String, name: String, date: String): String {
        val cacheKey = "$name/$date.json"
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(name)
            if (empDir == null) empDir = root.createDirectory(name)
            if (empDir == null) return "Error: Could not create directory for $name"

            val filename = "$date.json"
            val tempFilename = "$date.tmp"

            var tempFile = empDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = empDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            context.contentResolver.openOutputStream(tempFile.uri)?.use { os ->
                os.write(json.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val finalFile = empDir.findFile(filename)
            if (finalFile != null) finalFile.delete()

            val result = if (tempFile.renameTo(filename)) "SUCCESS" else "Error: SAF Rename Failed"
            if (result == "SUCCESS") {
                FileCache.put(cacheKey, json)
                Log.d("FileCache", "Cache updated after save: $cacheKey")
            }
            result
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
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val file = root.findFile("employees.json") ?: return null
            readDocumentContent(file)
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading employee list", e)
            null
        }
    }

    override fun loadShopCatalog(): List<com.example.timecard.data.model.ShopItem> {
        // Diagnostic: log SAF tree URI and root contents
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
            Log.d("ShopCatalog", "SAF treeUri = $treeUri")
            Log.d("ShopCatalog", "SAF root exists = ${root?.exists()}")
            val rootFiles = root?.listFiles()?.joinToString { it.name ?: "<unnamed>" } ?: "null"
            Log.d("ShopCatalog", "SAF root contents = $rootFiles")
            Log.d("ShopCatalog", "shop_catalog.json in root = ${root?.findFile("shop_catalog.json") != null}")
        } catch (e: Exception) {
            Log.e("ShopCatalog", "SAF diagnostic error", e)
        }

        val defaultItems = listOf(
            com.example.timecard.data.model.ShopItem("accent_sunrise", "Sunrise", "Warm orange and pink", 250, "theme", "🌅"),
            com.example.timecard.data.model.ShopItem("accent_twilight", "Twilight", "Deep purple and blue", 250, "theme", "🌃"),
            com.example.timecard.data.model.ShopItem("accent_isle", "Isle", "Teal and green", 250, "theme", "🏝️"),
            com.example.timecard.data.model.ShopItem("accent_daybreak", "Daybreak", "Bright yellow and orange", 250, "theme", "🌄"),
            com.example.timecard.data.model.ShopItem("accent_red", "Ruby", "Deep red", 750, "theme", "🔴"),
            com.example.timecard.data.model.ShopItem("accent_sunset", "Sunset", "Fiery magenta", 750, "theme", "🌇"),
            com.example.timecard.data.model.ShopItem("accent_midnight", "Midnight", "Starry blue", 1000, "theme", "🌙"),
            com.example.timecard.data.model.ShopItem("accent_ocean", "Ocean", "Deep aqua", 1000, "theme", "🌊"),
            com.example.timecard.data.model.ShopItem("accent_royal", "Royal", "Rich purple", 1500, "theme", "👑"),
            com.example.timecard.data.model.ShopItem("accent_hacker", "Terminal", "Cyber green", 1500, "theme", "💻"),

            com.example.timecard.data.model.ShopItem("feature_custom_avatar", "Custom Avatar", "Upload your own profile picture", 2000, "feature", "🖼️"),
            com.example.timecard.data.model.ShopItem("feature_display_name", "Display Name", "Unlock the ability to change your display name", 2500, "feature", "📝"),
            com.example.timecard.data.model.ShopItem("consumable_send_note", "Send a Note", "Send an alert note to another employee", 30, "consumable", "✉️")
        )
        
        val json = loadGlobalFile("shop_catalog.json", useCache = false) ?: run {
            Log.w("ShopCatalog", "SAF: shop_catalog.json returned null — using defaults")
            return defaultItems
        }
        // Try wrapped {"items":[...]} format first
        try {
            val mapType = object : com.google.gson.reflect.TypeToken<Map<String, List<com.example.timecard.data.model.ShopItem>>>() {}.type
            val result: Map<String, List<com.example.timecard.data.model.ShopItem>> = com.google.gson.Gson().fromJson(json, mapType)
            val items = result["items"]
            if (!items.isNullOrEmpty()) return items.filter { it.inShop != false }
        } catch (e: Exception) { /* not wrapped format */ }
        // Fall back to bare [...] format
        return try {
            val listType = object : com.google.gson.reflect.TypeToken<List<com.example.timecard.data.model.ShopItem>>() {}.type
            val items: List<com.example.timecard.data.model.ShopItem>? = com.google.gson.Gson().fromJson(json, listType)
            if (!items.isNullOrEmpty()) items.filter { it.inShop != false } else defaultItems
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error parsing shop catalog", e)
            defaultItems
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
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(name) ?: return null
            val file = empDir.findFile(filename) ?: return null
            val content = readDocumentContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading: $cacheKey", e)
            null
        }
    }

    override fun saveGenericJSON(name: String, filename: String, json: String): String {
        val cacheKey = "$name/$filename"
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(name)
            if (empDir == null) empDir = root.createDirectory(name)
            if (empDir == null) return "Error: Could not create directory"

            val tempFilename = "$filename.tmp"
            var tempFile = empDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = empDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            context.contentResolver.openOutputStream(tempFile.uri)?.use { os ->
                os.write(json.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val finalFile = empDir.findFile(filename)
            if (finalFile != null) finalFile.delete()

            val result = if (tempFile.renameTo(filename)) "SUCCESS" else "Error: SAF Rename Failed"
            if (result == "SUCCESS") {
                FileCache.put(cacheKey, json)
                Log.d("FileCache", "Cache updated after save: $cacheKey")
            }
            result
        } catch (e: Exception) {
            "Error: ${e.message}"
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
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val file = root.findFile(filename) ?: return null
            val content = readDocumentContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading global file: $filename", e)
            null
        }
    }

    override fun loadGlobalBinaryFile(relativePath: String): ByteArray? {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val segments = relativePath.replace("\\", "/").split("/").filter { it.isNotEmpty() }
            var doc: DocumentFile = root
            for (segment in segments) {
                doc = doc.findFile(segment) ?: return null
            }
            context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading binary file: $relativePath", e)
            null
        }
    }

    override fun loadEmployeeBinaryFile(name: String, filename: String): ByteArray? {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(name) ?: return null
            // Support wildcard extension: find first file matching prefix
            if (filename.contains("*")) {
                val prefix = filename.substringBefore("*")
                val match = empDir.listFiles().firstOrNull { it.name?.startsWith(prefix) == true }
                    ?: return null
                return context.contentResolver.openInputStream(match.uri)?.use { it.readBytes() }
            }
            val file = empDir.findFile(filename) ?: return null
            context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading employee binary: $name/$filename", e)
            null
        }
    }

    override fun saveEmployeeBinaryFile(name: String, filename: String, data: ByteArray): Boolean {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            var empDir = root.findFile(name)
            if (empDir == null) empDir = root.createDirectory(name)
            if (empDir == null) return false
            // Delete any existing .avatar.* files to avoid stale extension conflicts
            if (filename.startsWith(".avatar")) {
                empDir.listFiles().filter { it.name?.startsWith(".avatar") == true }.forEach { it.delete() }
            }
            val newFile = empDir.createFile("image/jpeg", filename) ?: return false
            context.contentResolver.openOutputStream(newFile.uri)?.use { os ->
                os.write(data)
                os.flush()
            }
            true
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error saving employee binary: $name/$filename", e)
            false
        }
    }

    override fun listEmployeeFolders(): List<String> {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            root.listFiles()
                .filter { it.isDirectory && it.name?.startsWith(".") == false }
                .mapNotNull { it.name }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error listing employee folders", e)
            emptyList()
        }
    }

    override fun listFilesInDir(name: String, subdirectory: String): List<String> {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val empDir = root.findFile(name) ?: return emptyList()
            val subDir = empDir.findFile(subdirectory) ?: return emptyList()
            subDir.listFiles().filter { it.isFile }.mapNotNull { it.name }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error listing files in $name/$subdirectory", e)
            emptyList()
        }
    }

    override fun saveInDir(name: String, subdirectory: String, filename: String, json: String): String {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(name)
            if (empDir == null) empDir = root.createDirectory(name)
            if (empDir == null) return "Error: Could not create directory $name"
            var subDir = empDir.findFile(subdirectory)
            if (subDir == null) subDir = empDir.createDirectory(subdirectory)
            if (subDir == null) return "Error: Could not create directory $name/$subdirectory"

            val tempFilename = "$filename.tmp"
            var tempFile = subDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = subDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            context.contentResolver.openOutputStream(tempFile.uri)?.use { os ->
                os.write(json.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val finalFile = subDir.findFile(filename)
            if (finalFile != null) finalFile.delete()

            if (tempFile.renameTo(filename)) "SUCCESS" else "Error: SAF Rename Failed"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadFromDir(name: String, subdirectory: String, filename: String): String? {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(name) ?: return null
            val subDir = empDir.findFile(subdirectory) ?: return null
            val file = subDir.findFile(filename) ?: return null
            readDocumentContent(file)
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading $name/$subdirectory/$filename", e)
            null
        }
    }

    override fun getAvailableDates(name: String): List<String> {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val empDir = root.findFile(name) ?: return emptyList()

            // Filter to date-pattern files only: "YYYY-MM-DD.json"
            val files = empDir.listFiles().filter { file ->
                val filename = file.name ?: ""
                filename.endsWith(".json") &&
                filename.length == 15 && // "YYYY-MM-DD.json" = 15 chars
                filename[4] == '-' && filename[7] == '-'
            }

            files.mapNotNull { it.name?.removeSuffix(".json") }
                 .sortedDescending()
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error getting available dates", e)
            emptyList()
        }
    }

    override fun acquireLock(name: String, date: String, deviceId: String): Boolean {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return true
        var empDir = root.findFile(name)
        if (empDir == null) empDir = root.createDirectory(name)
        if (empDir == null) return true // Failsafe, don't block if FS is broken

        val lockFile = empDir.findFile("$date.json.lock")
        if (lockFile != null) {
            val content = readDocumentContent(lockFile) ?: ""
            val parts = content.split(",")
            if (parts.size == 2) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val ownerDeviceId = parts[1]
                
                if (ownerDeviceId == deviceId) {
                    renewLock(name, date, deviceId)
                    return true
                }
                
                if (System.currentTimeMillis() - timestamp < 300_000L) {
                    return false // Another device actively owns the lock
                }
            }
        }
        
        renewLock(name, date, deviceId)
        return true
    }

    override fun renewLock(name: String, date: String, deviceId: String) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
            var empDir = root.findFile(name)
            if (empDir == null) empDir = root.createDirectory(name)
            if (empDir == null) return
            
            val filename = "$date.json.lock"
            var lockFile = empDir.findFile(filename)
            if (lockFile == null) {
                lockFile = empDir.createFile("application/octet-stream", filename)
            }
            if (lockFile == null) return
            
            val timestamp = System.currentTimeMillis()
            context.contentResolver.openOutputStream(lockFile.uri)?.use { os ->
                os.write("$timestamp,$deviceId".toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error renewing lock", e)
        }
    }

    override fun releaseLock(name: String, date: String, deviceId: String) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val empDir = root.findFile(name) ?: return
        val lockFile = empDir.findFile("$date.json.lock") ?: return
        
        val content = readDocumentContent(lockFile) ?: ""
        val parts = content.split(",")
        if (parts.size == 2 && parts[1] == deviceId) {
            lockFile.delete()
        }
    }

    private fun readDocumentContent(file: DocumentFile): String? {
        return context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                sb.toString()
            }
        }
    }
}
