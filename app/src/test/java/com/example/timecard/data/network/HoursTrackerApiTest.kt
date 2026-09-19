package com.example.timecard.data.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * This app has never made a network call before, so there's no HTTP-mocking
 * infrastructure (no MockWebServer, no Retrofit test doubles) in the project.
 * Rather than add a new test dependency or thread an injectable
 * HttpURLConnection seam through production code for a single call site,
 * the success path here spins up a tiny stdlib-only ServerSocket that
 * speaks just enough HTTP/1.1 to capture the real request
 * HoursTrackerApi.setEmployeeDisplayName sends and respond 200. This gives
 * real confidence (actual socket I/O, actual HttpURLConnection) without a
 * new dependency or a production-code seam that only exists for tests.
 */
class HoursTrackerApiTest {

    /** Minimal captured shape of the HTTP request the fake server received. */
    private class CapturedRequest(
        val requestLine: String,
        val headers: Map<String, String>,
        val body: String
    )

    private fun startFakeServer(
        responseStatusLine: String = "HTTP/1.1 200 OK"
    ): Pair<ServerSocket, () -> CapturedRequest> {
        val serverSocket = ServerSocket(0)
        serverSocket.soTimeout = 5000
        val latch = CountDownLatch(1)
        var captured: CapturedRequest? = null
        var error: Exception? = null

        val thread = Thread {
            try {
                serverSocket.accept().use { client ->
                    val input = BufferedReader(InputStreamReader(client.getInputStream()))
                    val requestLine = input.readLine() ?: ""
                    val headers = mutableMapOf<String, String>()
                    var line = input.readLine()
                    while (!line.isNullOrEmpty()) {
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) {
                            headers[parts[0].trim()] = parts[1].trim()
                        }
                        line = input.readLine()
                    }
                    val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
                    val bodyChars = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val n = input.read(bodyChars, read, contentLength - read)
                        if (n == -1) break
                        read += n
                    }
                    val body = String(bodyChars, 0, read)
                    captured = CapturedRequest(requestLine, headers, body)

                    val response = "$responseStatusLine\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    client.getOutputStream().write(response.toByteArray())
                    client.getOutputStream().flush()
                }
            } catch (e: Exception) {
                error = e
            } finally {
                latch.countDown()
            }
        }
        thread.isDaemon = true
        thread.start()

        return serverSocket to {
            latch.await(5, TimeUnit.SECONDS)
            error?.let { throw it }
            captured ?: throw IllegalStateException("Fake server never received a request")
        }
    }

    @Test
    fun `setEmployeeDisplayName posts JSON body and returns true on HTTP 200`() {
        val (serverSocket, getCaptured) = startFakeServer()
        try {
            val port = serverSocket.localPort
            val result = HoursTrackerApi.setEmployeeDisplayName(
                id = "7",
                displayName = "Winnie",
                baseUrl = "http://127.0.0.1:$port"
            )

            assertTrue("Expected setEmployeeDisplayName to return true on HTTP 200", result)

            val captured = getCaptured()
            assertEquals("POST /api/employees/7/display-name HTTP/1.1", captured.requestLine)
            assertEquals("application/json", captured.headers["Content-Type"])
            assertEquals("{\"displayName\":\"Winnie\"}", captured.body)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun `setEmployeeDisplayName returns false and does not throw on connection failure`() {
        // Port 1 is a reserved, unroutable port -- nothing listens there, so the
        // connection attempt fails fast (connection refused / unreachable).
        val result = HoursTrackerApi.setEmployeeDisplayName(
            id = "7",
            displayName = "Winnie",
            baseUrl = "http://127.0.0.1:1"
        )

        assertFalse("Expected setEmployeeDisplayName to return false on connection failure", result)
    }
}
