package com.example.timecard.data.network

import com.google.gson.Gson
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal, dependency-free HTTP client for the one call this app makes to
 * the Hours Tracker backend: pushing an employee's self-set display name
 * into the shared employees.json roster (via a companion backend endpoint
 * that requires no auth -- the endpoint is a best-effort convenience sync,
 * not a security boundary; see its own docstring on the backend for why).
 * Uses java.net.HttpURLConnection rather than pulling in Retrofit/OkHttp
 * for a single, simple POST call.
 */
object HoursTrackerApi {
    /** Default backend host -- matches the host this project's backend
     * (main_v2.py / config.py) is already reachable at elsewhere (see
     * TIMECLOCK_HUB_URL default), on the port main_v2.py listens on
     * (uvicorn.run(..., port=5002)). No in-app settings screen for this
     * yet -- hardcoded default only, consistent with this being a
     * best-effort, non-blocking push (see setEmployeeDisplayName's docs). */
    const val DEFAULT_BASE_URL = "http://192.168.1.15:5002"

    /**
     * POSTs the employee's chosen display name to the backend's shared
     * roster. Best-effort: never throws, returns false on any failure
     * (network down, backend unreachable, non-200 response) so callers can
     * treat this as "nice to have synced" rather than a hard requirement --
     * the employee's local profile.json save (the pre-existing, primary
     * save path) is unaffected either way.
     */
    fun setEmployeeDisplayName(
        id: String,
        displayName: String,
        baseUrl: String = DEFAULT_BASE_URL
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$baseUrl/api/employees/$id/display-name")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            val body = gson.toJson(mapOf("displayName" to displayName))
            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private val gson = Gson()
}
