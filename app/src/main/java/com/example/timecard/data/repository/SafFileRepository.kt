package com.example.timecard.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.timecard.util.PathSanitizer
import androidx.documentfile.provider.DocumentFile
import com.example.timecard.data.cache.FileCache
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SafFileRepository(
    private val context: Context,
    private val treeUri: Uri
) : FileRepository {

    /**
     * Writes [bytes] to the content at [uri], requiring a non-null OutputStream.
     * Returns true only if the stream was obtained and the write completed.
     * A null stream (a real, documented possible SAF outcome) returns false instead
     * of silently no-op'ing, so callers never mistake an unwritten temp file for success.
     */
    private fun writeAllBytes(uri: Uri, bytes: ByteArray): Boolean {
        val os = context.contentResolver.openOutputStream(uri) ?: return false
        os.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
        return true
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
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(sName) ?: return null
            var file = empDir.findFile("$sDate.json")
            if (file == null) {
                val tempFile = empDir.findFile("$sDate.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo("$sDate.json")) {
                        Log.d("SafFileRepo", "Recovered timecard temp file: $sDate.tmp")
                        file = empDir.findFile("$sDate.json")
                    }
                }
            }
            if (file == null) return null
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
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val cacheKey = "$sName/$sDate.json"
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return "Error: Could not create directory for $sName"

            val sFilenameFinal = "$sDate.json"
            val tempFilename = "$sDate.tmp"

            var tempFile = empDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = empDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            if (!writeAllBytes(tempFile.uri, json.toByteArray(StandardCharsets.UTF_8))) {
                tempFile.delete()
                return "Error: Could not open output stream for write"
            }

            val finalFile = empDir.findFile(sFilenameFinal)
            if (finalFile != null) finalFile.delete()

            val result = if (tempFile.renameTo(sFilenameFinal)) "SUCCESS" else "Error: SAF Rename Failed"
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

    override fun loadShopCatalog(): List<com.example.timecard.data.model.ShopItem> = loadShopCatalogInternal(filterPool = true)

    override fun loadFullShopCatalog(): List<com.example.timecard.data.model.ShopItem> = loadShopCatalogInternal(filterPool = false)

    private fun loadShopCatalogInternal(filterPool: Boolean): List<com.example.timecard.data.model.ShopItem> {
        val json = loadGlobalFile("shop_catalog.json", useCache = false) ?: return emptyList()
        // Try wrapped {"items":[...]} format first
        try {
            val mapType = object : com.google.gson.reflect.TypeToken<Map<String, List<com.example.timecard.data.model.ShopItem>>>() {}.type
            val result: Map<String, List<com.example.timecard.data.model.ShopItem>> = com.google.gson.Gson().fromJson(json, mapType)
            val items = result["items"]
            if (!items.isNullOrEmpty()) return if (filterPool) items.filter { it.inShop } else items
        } catch (e: Exception) { /* not wrapped format */ }
        // Fall back to bare [...] format
        return try {
            val listType = object : com.google.gson.reflect.TypeToken<List<com.example.timecard.data.model.ShopItem>>() {}.type
            val items: List<com.example.timecard.data.model.ShopItem>? = com.google.gson.Gson().fromJson(json, listType)
            val list = items ?: emptyList()
            if (filterPool) list.filter { it.inShop } else list
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error parsing shop catalog", e)
            emptyList()
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
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(sName) ?: return null
            var file = empDir.findFile(sFilename)
            if (file == null) {
                val tempFile = empDir.findFile("$sFilename.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo(sFilename)) {
                        Log.d("SafFileRepo", "Recovered generic temp file: $sFilename.tmp")
                        file = empDir.findFile(sFilename)
                    }
                }
            }
            if (file == null) return null
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
        val sName = PathSanitizer.sanitize(name)
        val sFilename = PathSanitizer.sanitize(filename)
        val cacheKey = "$sName/$sFilename"
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return "Error: Could not create directory"

            val tempFilename = "$sFilename.tmp"
            var tempFile = empDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = empDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            if (!writeAllBytes(tempFile.uri, json.toByteArray(StandardCharsets.UTF_8))) {
                tempFile.delete()
                return "Error: Could not open output stream for write"
            }

            val finalFile = empDir.findFile(sFilename)
            if (finalFile != null) finalFile.delete()

            val result = if (tempFile.renameTo(sFilename)) "SUCCESS" else "Error: SAF Rename Failed"
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
        val sFilename = PathSanitizer.sanitize(filename)
        val cacheKey = "global/$sFilename"
        if (useCache) {
            FileCache.get(cacheKey)?.let {
                Log.d("FileCache", "Cache HIT: $cacheKey")
                return it
            }
        }
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            var file = root.findFile(sFilename)
            if (file == null) {
                val tempFile = root.findFile("$sFilename.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo(sFilename)) {
                        Log.d("SafFileRepo", "Recovered global temp file: $sFilename.tmp")
                        file = root.findFile(sFilename)
                    }
                }
            }
            if (file == null) return null
            val content = readDocumentContent(file)
            if (content != null && useCache) {
                FileCache.put(cacheKey, content)
                Log.d("FileCache", "Cache MISS, loaded: $cacheKey")
            }
            content
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading global file: $sFilename", e)
            null
        }
    }

    override fun loadGlobalBinaryFile(relativePath: String): ByteArray? {
        val sPath = PathSanitizer.sanitizePath(relativePath)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val segments = sPath.split("/").filter { it.isNotEmpty() }
            var doc: DocumentFile = root
            for (segment in segments) {
                doc = doc.findFile(segment) ?: return null
            }
            context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading binary file: $sPath", e)
            null
        }
    }

    override fun loadEmployeeBinaryFile(name: String, filename: String): ByteArray? {
        val sName = PathSanitizer.sanitize(name)
        val sFilename = PathSanitizer.sanitize(filename)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(sName) ?: return null
            // Support wildcard extension: find first file matching prefix
            if (sFilename.contains("*")) {
                val prefix = sFilename.substringBefore("*")
                val match = empDir.listFiles().firstOrNull { it.name?.startsWith(prefix) == true }
                    ?: return null
                return context.contentResolver.openInputStream(match.uri)?.use { it.readBytes() }
            }
            val file = empDir.findFile(sFilename) ?: return null
            context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading employee binary: $sName/$sFilename", e)
            null
        }
    }

    override fun saveEmployeeBinaryFile(name: String, filename: String, data: ByteArray): Boolean {
        val sName = PathSanitizer.sanitize(name)
        val sFilename = PathSanitizer.sanitize(filename)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return false
            // Delete any existing .avatar.* files to avoid stale extension conflicts
            if (sFilename.startsWith(".avatar")) {
                empDir.listFiles().filter { it.name?.startsWith(".avatar") == true }.forEach { it.delete() }
            }
            val newFile = empDir.createFile("image/jpeg", sFilename) ?: return false
            if (!writeAllBytes(newFile.uri, data)) {
                newFile.delete()
                return false
            }
            true
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error saving employee binary: $sName/$sFilename", e)
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
        val sName = PathSanitizer.sanitize(name)
        val sSub = PathSanitizer.sanitize(subdirectory)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val empDir = root.findFile(sName) ?: return emptyList()
            val subDir = empDir.findFile(sSub) ?: return emptyList()
            subDir.listFiles().filter { it.isFile }.mapNotNull { it.name }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error listing files in $sName/$sSub", e)
            emptyList()
        }
    }

    override fun saveInDir(name: String, subdirectory: String, filename: String, json: String): String {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return "Error: Could not create directory $sName"
            var subDir = empDir.findFile(sSub)
            if (subDir == null) subDir = empDir.createDirectory(sSub)
            if (subDir == null) return "Error: Could not create directory $sName/$sSub"

            val tempFilename = "$sFilename.tmp"
            var tempFile = subDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = subDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            if (!writeAllBytes(tempFile.uri, json.toByteArray(StandardCharsets.UTF_8))) {
                tempFile.delete()
                return "Error: Could not open output stream for write"
            }

            val finalFile = subDir.findFile(sFilename)
            if (finalFile != null) finalFile.delete()

            if (tempFile.renameTo(sFilename)) "SUCCESS" else "Error: SAF Rename Failed"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadFromDir(name: String, subdirectory: String, filename: String): String? {
        val sName = PathSanitizer.sanitize(name)
        val sSub = PathSanitizer.sanitize(subdirectory)
        val sFilename = PathSanitizer.sanitize(filename)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val empDir = root.findFile(sName) ?: return null
            val subDir = empDir.findFile(sSub) ?: return null
            var file = subDir.findFile(sFilename)
            if (file == null) {
                val tempFile = subDir.findFile("$sFilename.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo(sFilename)) {
                        Log.d("SafFileRepo", "Recovered dir temp file: $sFilename.tmp")
                        file = subDir.findFile(sFilename)
                    }
                }
            }
            if (file == null) return null
            readDocumentContent(file)
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading $sName/$sSub/$sFilename", e)
            null
        }
    }

    override fun getAvailableDates(name: String): List<String> {
        return try {
            val sName = PathSanitizer.sanitize(name)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val empDir = root.findFile(sName) ?: return emptyList()

            // Filter to date-pattern files only: "YYYY-MM-DD.json"
            val files = empDir.listFiles().filter { file ->
                val fName = file.name ?: ""
                fName.endsWith(".json") &&
                fName.length == 15 && // "YYYY-MM-DD.json" = 15 chars
                fName[4] == '-' && fName[7] == '-'
            }

            files.mapNotNull { it.name?.removeSuffix(".json") }
                 .sortedDescending()
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error getting available dates", e)
            emptyList()
        }
    }

    override fun acquireLock(name: String, date: String, deviceId: String): Boolean {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return true
        var empDir = root.findFile(sName)
        if (empDir == null) empDir = root.createDirectory(sName)
        if (empDir == null) return true // Failsafe, don't block if FS is broken

        val lockFile = empDir.findFile("$sDate.json.lock")
        if (lockFile != null) {
            val content = readDocumentContent(lockFile) ?: ""
            val parts = content.split(",")
            if (parts.size == 2) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val ownerDeviceId = parts[1]
                
                if (ownerDeviceId == deviceId) {
                    renewLock(sName, sDate, deviceId)
                    return true
                }
                
                if (System.currentTimeMillis() - timestamp < 300_000L) {
                    return false // Another device actively owns the lock
                }
            }
        }
        
        renewLock(sName, sDate, deviceId)
        // Best-effort narrowing of the check-then-act race: SAF has no atomic create-if-absent
        // primitive, so two devices can still both reach this point concurrently. Writing then
        // reading back means only whichever write physically landed last will see its own
        // deviceId on read-back, so at most one caller returns true. This does not make the
        // operation atomic (a device could still read back its own id before a concurrent
        // writer's write lands), it only shrinks the window versus trusting the pre-write state.
        return verifyLockOwnership(empDir, sDate, deviceId)
    }

    private fun verifyLockOwnership(empDir: DocumentFile, sDate: String, deviceId: String): Boolean {
        val lockFile = empDir.findFile("$sDate.json.lock") ?: return false
        val content = readDocumentContent(lockFile) ?: return false
        val parts = content.split(",")
        return parts.size == 2 && parts[1] == deviceId
    }

    override fun renewLock(name: String, date: String, deviceId: String) {
        try {
            val sName = PathSanitizer.sanitize(name)
            val sDate = PathSanitizer.sanitize(date)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return
            
            val sLockFilename = "$sDate.json.lock"
            var lockFile = empDir.findFile(sLockFilename)
            if (lockFile == null) {
                lockFile = empDir.createFile("application/octet-stream", sLockFilename)
            }
            if (lockFile == null) return
            
            val timestamp = System.currentTimeMillis()
            if (!writeAllBytes(lockFile.uri, "$timestamp,$deviceId".toByteArray(StandardCharsets.UTF_8))) {
                Log.e("SafFileRepo", "Error renewing lock: could not open output stream for $sLockFilename")
            }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error renewing lock", e)
        }
    }

    override fun releaseLock(name: String, date: String, deviceId: String) {
        val sName = PathSanitizer.sanitize(name)
        val sDate = PathSanitizer.sanitize(date)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val empDir = root.findFile(sName) ?: return
        val lockFile = empDir.findFile("$sDate.json.lock") ?: return
        
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

    override fun loadEmployeeActivityEvents(name: String): List<com.example.timecard.data.model.ActivityEvent> {
        val sName = PathSanitizer.sanitize(name)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val empDir = root.findFile(sName) ?: return emptyList()
            var file = empDir.findFile("activity_events.json")
            if (file == null) {
                val tempFile = empDir.findFile("activity_events.json.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo("activity_events.json")) {
                        Log.d("SafFileRepo", "Recovered activity events temp file")
                        file = empDir.findFile("activity_events.json")
                    }
                }
            }
            if (file == null) return emptyList()
            val json = readDocumentContent(file) ?: return emptyList()
            com.google.gson.Gson().fromJson(json, com.example.timecard.data.model.ActivityFeed::class.java)?.events ?: emptyList()
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading activity events for $sName", e)
            emptyList()
        }
    }

    override fun saveEmployeeActivityEvents(name: String, events: List<com.example.timecard.data.model.ActivityEvent>) {
        val sName = PathSanitizer.sanitize(name)
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
            var empDir = root.findFile(sName)
            if (empDir == null) empDir = root.createDirectory(sName)
            if (empDir == null) return

            val feed = com.example.timecard.data.model.ActivityFeed(events.take(50))
            val json = com.google.gson.Gson().toJson(feed)
            val bytes = json.toByteArray(StandardCharsets.UTF_8)

            val tempFilename = "activity_events.json.tmp"
            var tempFile = empDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = empDir.createFile("application/json", tempFilename) ?: return

            if (!writeAllBytes(tempFile.uri, bytes)) {
                Log.e("SafFileRepo", "Error saving activity events for $sName: could not open output stream")
                tempFile.delete()
                return
            }

            val finalFile = empDir.findFile("activity_events.json")
            if (finalFile != null) finalFile.delete()

            tempFile.renameTo("activity_events.json")
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error saving activity events for $sName", e)
        }
    }

    override fun saveGlobalDir(subdirectory: String, filename: String, json: String): String {
        return try {
            val sSub = PathSanitizer.sanitize(subdirectory)
            val sFilename = PathSanitizer.sanitize(filename)
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return "Error: Access to folder lost"
            var subDir = root.findFile(sSub)
            if (subDir == null) subDir = root.createDirectory(sSub)
            if (subDir == null) return "Error: Could not create directory $sSub"

            val tempFilename = "$sFilename.tmp"
            var tempFile = subDir.findFile(tempFilename)
            if (tempFile != null) tempFile.delete()
            tempFile = subDir.createFile("application/json", tempFilename)
                ?: return "Error: Could not create temp file"

            if (!writeAllBytes(tempFile.uri, json.toByteArray(StandardCharsets.UTF_8))) {
                tempFile.delete()
                return "Error: Could not open output stream for write"
            }

            val finalFile = subDir.findFile(sFilename)
            if (finalFile != null) finalFile.delete()

            if (tempFile.renameTo(sFilename)) "SUCCESS" else "Error: SAF Rename Failed"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun loadGlobalDir(subdirectory: String, filename: String): String? {
        val sSub = PathSanitizer.sanitize(subdirectory)
        val sFilename = PathSanitizer.sanitize(filename)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val subDir = root.findFile(sSub) ?: return null
            var file = subDir.findFile(sFilename)
            if (file == null) {
                val tempFile = subDir.findFile("$sFilename.tmp")
                if (tempFile != null) {
                    if (tempFile.renameTo(sFilename)) {
                        Log.d("SafFileRepo", "Recovered global dir temp file: $sFilename.tmp")
                        file = subDir.findFile(sFilename)
                    }
                }
            }
            if (file == null) return null
            readDocumentContent(file)
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error loading $sSub/$sFilename", e)
            null
        }
    }

    override fun loadGlobalDirFiles(subdirectory: String, filenames: List<String>): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        val sSub = PathSanitizer.sanitize(subdirectory)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyMap()
            val subDir = root.findFile(sSub) ?: return emptyMap()
            for (f in filenames) {
                val sFilename = PathSanitizer.sanitize(f)
                try {
                    var file = subDir.findFile(sFilename)
                    if (file == null) {
                        val tempFile = subDir.findFile("$sFilename.tmp")
                        if (tempFile != null) {
                            if (tempFile.renameTo(sFilename)) {
                                Log.d("SafFileRepo", "Recovered global dir temp file: $sFilename.tmp")
                                file = subDir.findFile(sFilename)
                            }
                        }
                    }
                    if (file != null && file.isFile) {
                        result[f] = readDocumentContent(file)
                    }
                } catch (e: Exception) {
                    Log.e("SafFileRepo", "Error loading file $sFilename from $sSub", e)
                }
            }
            result
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error accessing directory $sSub", e)
            emptyMap()
        }
    }

    override fun listGlobalDir(subdirectory: String): List<String> {
        val sSub = PathSanitizer.sanitize(subdirectory)
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val subDir = root.findFile(sSub) ?: return emptyList()
            subDir.listFiles().filter { it.isFile }.mapNotNull { it.name }
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error listing files in $sSub", e)
            emptyList()
        }
    }

    override fun loadChallenges(): List<com.example.timecard.data.model.Challenge> {
        val json = loadGlobalFile("challenges.json", useCache = false) ?: return emptyList()
        return try {
            com.google.gson.Gson().fromJson(json, com.example.timecard.data.model.ChallengeCatalog::class.java)?.challenges ?: emptyList()
        } catch (e: Exception) {
            Log.e("SafFileRepo", "Error parsing challenges.json", e)
            emptyList()
        }
    }
}
