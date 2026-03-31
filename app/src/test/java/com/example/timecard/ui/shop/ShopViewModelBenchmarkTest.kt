package com.example.timecard.ui.shop

import com.example.timecard.data.model.Employee
import com.example.timecard.data.model.PlayerProfile
import com.google.gson.Gson
import org.json.JSONObject
import org.junit.Test
import kotlin.system.measureTimeMillis

class ShopViewModelBenchmarkTest {

    @Test
    fun benchmarkNPlusOneVsBulkRead() {
        val numFolders = 1000 // scale it up
        val gson = Gson()

        // Setup mock data
        val validFolders = (1..numFolders).map { "Employee $it" }.toSet()
        val mockProfiles = validFolders.associateWith { folder ->
            """{"displayName":"Test Name $folder", "other_fields":"blah blah blah", "some_data": 1234, "avatar": "none", "runningStats": {}}"""
        }

        var totalCurrentMs = 0L
        val currentIterations = 50
        for (i in 1..currentIterations) {
            val ms = measureTimeMillis {
                val list = mutableListOf<EmployeeRecipient>()
                for (folder in validFolders) {
                    val profileJson = mockProfiles[folder]
                    var dName: String? = null
                    if (profileJson != null) {
                        try {
                            // Full deserialization per item
                            val prof = gson.fromJson(profileJson, PlayerProfile::class.java)
                            dName = prof.displayName
                        } catch (e: Exception) {}
                    }
                    list.add(EmployeeRecipient(folderName = folder, displayName = dName))
                }
            }
            totalCurrentMs += ms
        }
        println("Current approach (N reads, Full GSON obj parse): avg ${totalCurrentMs / currentIterations}ms for $numFolders items")

        // Safe JSONObject Parse
        var totalJsonOptMs = 0L
        for (i in 1..currentIterations) {
            val ms = measureTimeMillis {
                val list = mutableListOf<EmployeeRecipient>()
                for (folder in validFolders) {
                    val profileJson = mockProfiles[folder]
                    var dName: String? = null
                    if (profileJson != null) {
                        try {
                            val jsonObject = JSONObject(profileJson)
                            if (jsonObject.has("displayName")) {
                                dName = jsonObject.optString("displayName", null)
                            }
                        } catch (e: Exception) {}
                    }
                    list.add(EmployeeRecipient(folderName = folder, displayName = dName))
                }
            }
            totalJsonOptMs += ms
        }
        println("JSONObject approach (Safe & Fast string parse): avg ${totalJsonOptMs / currentIterations}ms for $numFolders items")
    }
}
