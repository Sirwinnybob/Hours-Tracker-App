package com.example.timecard.data.repository

import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.ShopItem

interface FileRepository {
    fun loadShopCatalog(): List<ShopItem>
    fun loadFile(name: String, date: String): String?
    fun saveJSON(json: String, name: String, date: String): String
    fun loadAlerts(name: String): String?
    fun loadAcknowledgements(name: String): String?
    fun saveAcknowledgements(name: String, json: String): String
    fun loadEmployeeList(): String?
    fun loadGenericJSON(name: String, filename: String, useCache: Boolean = true): String?
    fun saveGenericJSON(name: String, filename: String, json: String): String
    fun getAvailableDates(name: String): List<String>
    /** Load a file from the root of the shared folder (not employee-specific). */
    fun loadGlobalFile(filename: String, useCache: Boolean = true): String?
    /** Load a binary file relative to the root of the shared folder (e.g. badge images). */
    fun loadGlobalBinaryFile(relativePath: String): ByteArray?
    /** Load a binary file from an employee's folder (e.g. custom avatar). */
    fun loadEmployeeBinaryFile(name: String, filename: String): ByteArray?
    /** Save a binary file to an employee's folder (e.g. self-uploaded avatar). */
    fun saveEmployeeBinaryFile(name: String, filename: String, data: ByteArray): Boolean
    /** List all employee folder names in the root directory (for leaderboard). */
    fun listEmployeeFolders(): List<String>
    /** Load activity events for a specific employee. Returns empty list if file absent. */
    fun loadEmployeeActivityEvents(name: String): List<ActivityEvent>
    /** Save activity events for the logged-in employee (overwrites atomically, max 50). */
    fun saveEmployeeActivityEvents(name: String, events: List<ActivityEvent>)
    /** Load challenges.json from the root of the shared folder. Returns empty list if absent. */
    fun loadChallenges(): List<com.example.timecard.data.model.Challenge>

    // Lock Management
    fun acquireLock(name: String, date: String, deviceId: String): Boolean
    fun renewLock(name: String, date: String, deviceId: String)
    fun releaseLock(name: String, date: String, deviceId: String)

    // Subdirectory operations (write-once per-file pattern for conflict-free Syncthing)
    /** Lists filenames directly inside [name]/[subdirectory]/. Returns empty list if absent. */
    fun listFilesInDir(name: String, subdirectory: String): List<String>
    /** Writes [json] to [name]/[subdirectory]/[filename] atomically. Creates dirs as needed. */
    fun saveInDir(name: String, subdirectory: String, filename: String, json: String): String
    /** Reads [name]/[subdirectory]/[filename], or null if absent. Never uses FileCache. */
    fun loadFromDir(name: String, subdirectory: String, filename: String): String?
}
