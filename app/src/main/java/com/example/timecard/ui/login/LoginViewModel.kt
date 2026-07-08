package com.example.timecard.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.Employee
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.data.repository.FileRepositoryFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LoginViewModel : ViewModel() {

    companion object {
        private const val PREFS_NAME = "TimecardPrefs"
        private const val KEY_URI = "sync_folder_uri"
        private const val KEY_EMPLOYEES = "cached_employees"
        private val JOB_FOLDER_NAMES = arrayOf("Ready Jobs", "Jobs", "JOBS")

        private val DEFAULT_EMPLOYEES = listOf(
            Employee("023", "Jonathan Thornton"),
            Employee("067", "Jared Rosenburg"),
            Employee("101", "Chris Tennent"),
            Employee("189", "Kevin Leafdale"),
            Employee("223", "Barry Roper"),
            Employee("345", "Donald McEdward"),
            Employee("389", "Winston Ferguson"),
            Employee("423", "Michael Diekotto"),
            Employee("467", "Montgomery Blackburn"),
            Employee("501", "Cameron Baker"),
            Employee("623", "Tye Lewin"),
            Employee("701", "Nate Hoseteetter"),
            Employee("901", "Kevin Olson"),
            Employee("989", "Kevin Palmer")
        )
    }

    var loginInput by mutableStateOf("")
    var employees by mutableStateOf<List<Employee>>(emptyList())
        private set
    var syncFolderUri by mutableStateOf<Uri?>(null)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var isDebugBuild by mutableStateOf(false)

    private lateinit var prefs: SharedPreferences
    private var repository: FileRepository? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDebugBuild = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        restoreUri()
        if (syncFolderUri == null) {
            autoDetectSyncFolder(context)
        } else {
            updateConnectionStatus()
            loadEmployees(context)
        }
    }

    fun getRepository(context: Context): FileRepository? {
        val uri = syncFolderUri ?: return null
        if (repository == null) {
            repository = FileRepositoryFactory.create(context, uri)
        }
        return repository
    }

    fun onSyncFolderSelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        saveUri(uri)
        syncFolderUri = uri
        repository = null // Force recreate
        updateConnectionStatus()
        Toast.makeText(context, "Connected: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        loadEmployees(context)
    }

    fun attemptLogin(): Employee? {
        val val_ = loginInput.trim()
        if (val_.isEmpty()) return null

        // Try match by ID first
        var employee = employees.find { it.id == val_ }

        // If no ID match, try name (case-insensitive)
        if (employee == null) {
            employee = employees.find { it.name.equals(val_, ignoreCase = true) }
        }

        return employee
    }

    fun checkAutoLogin(): Employee? {
        val val_ = loginInput.trim()
        if (Regex("^\\d{3}$").matches(val_)) {
            val match = employees.find { it.id == val_ }
            if (match != null) {
                return match
            }
        }
        return null
    }

    suspend fun getDisplayName(employee: Employee, context: Context): String {
        val repo = getRepository(context) ?: return employee.name
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = repo.loadGenericJSON(employee.name, "profile.json", useCache = false)
                if (json != null) {
                    val jsonObj = com.google.gson.JsonParser.parseString(json).asJsonObject
                    if (jsonObj.has("displayName") && !jsonObj.get("displayName").isJsonNull) {
                        val displayName = jsonObj.get("displayName").asString
                        if (displayName.isNotBlank()) return@withContext displayName
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            employee.name
        }
    }

    val filteredEmployees: List<String>
        get() {
            if (loginInput.isBlank()) return emptyList()
            return employees
                .filter { it.name.lowercase().contains(loginInput.lowercase()) }
                .map { it.name }
        }

    private fun restoreUri() {
        val uriStr = prefs.getString(KEY_URI, null)
        if (uriStr != null) {
            syncFolderUri = Uri.parse(uriStr)
        }
    }

    private fun saveUri(uri: Uri) {
        prefs.edit().putString(KEY_URI, uri.toString()).apply()
    }

    private fun updateConnectionStatus() {
        isConnected = syncFolderUri != null
    }

    private fun autoDetectSyncFolder(context: Context) {
        viewModelScope.launch {
            val detectionResult = withContext(Dispatchers.IO) {
                val storageRoot = Environment.getExternalStorageDirectory()
                for (jobFolder in JOB_FOLDER_NAMES) {
                    val jobDir = File(storageRoot, jobFolder)
                    if (jobDir.exists() && jobDir.isDirectory) {
                        val timeCardsDir = File(jobDir, ".time_cards")
                        if (timeCardsDir.exists() && timeCardsDir.isDirectory) {
                            try {
                                return@withContext Uri.fromFile(timeCardsDir) to jobFolder
                            } catch (e: Exception) {
                                Log.e("LoginVM", "Failed to auto-connect: ${e.message}")
                            }
                        }
                    }
                }
                null
            }

            if (detectionResult != null) {
                val (fileUri, jobFolder) = detectionResult
                saveUri(fileUri)
                syncFolderUri = fileUri
                repository = null
                updateConnectionStatus()
                Toast.makeText(context, "Auto-connected: $jobFolder/.time_cards", Toast.LENGTH_SHORT).show()
                Log.d("LoginVM", "Auto-detected time_cards at: ${fileUri.path}")
                loadEmployees(context)
            } else {
                Log.d("LoginVM", "Could not auto-detect .time_cards folder")
                updateConnectionStatus()
                loadEmployees(context)
            }
        }
    }

    private fun parseName(fullName: String): String {
        val parts = fullName.split(",").map { it.trim() }
        return if (parts.size == 2) {
            "${parts[1]} ${parts[0]}"
        } else {
            fullName
        }
    }

    private fun parseEmployeesJson(json: String): List<Employee> {
        val type = object : TypeToken<List<Employee>>() {}.type
        return Gson().fromJson<List<Employee>>(json, type)
            .map { it.copy(name = parseName(it.name)) }
    }

    private fun loadEmployees(context: Context) {
        if (!prefs.contains(KEY_EMPLOYEES)) {
            val defaultJson = Gson().toJson(DEFAULT_EMPLOYEES)
            prefs.edit().putString(KEY_EMPLOYEES, defaultJson).apply()
        }

        if (syncFolderUri != null) {
            Thread {
                try {
                    val fileContent = getRepository(context)?.loadEmployeeList()
                    if (fileContent != null) {
                        val cachedContent = prefs.getString(KEY_EMPLOYEES, "")
                        if (fileContent != cachedContent) {
                            Log.d("LoginVM", "Employee list changed, updating cache")
                            prefs.edit().putString(KEY_EMPLOYEES, fileContent).apply()
                        }
                        employees = parseEmployeesJson(fileContent)
                        return@Thread
                    }
                } catch (e: Exception) {
                    Log.e("LoginVM", "Error in employee sync", e)
                }

                val cachedJson = prefs.getString(KEY_EMPLOYEES, "[]")!!
                try {
                    employees = parseEmployeesJson(cachedJson)
                } catch (e: Exception) {
                    Log.e("LoginVM", "Failed to parse cached employees", e)
                    employees = DEFAULT_EMPLOYEES
                }
            }.start()
            return
        }

        val cachedJson = prefs.getString(KEY_EMPLOYEES, "[]")!!
        try {
            employees = parseEmployeesJson(cachedJson)
        } catch (e: Exception) {
            Log.e("LoginVM", "Failed to parse cached employees", e)
            employees = DEFAULT_EMPLOYEES
        }
    }
}
