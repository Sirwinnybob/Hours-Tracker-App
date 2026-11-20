package com.example.timecard.lossaudit

import android.app.Application
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.ui.timesheet.TimesheetViewModel
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * In-memory stand-in for the share. Files are keyed "employee/week". Every saveJSON is recorded
 * in [saves] so tests can assert whether a save happened at all. [beforeSave] lets a test block
 * or slow a specific save call (index is 0-based call order).
 */
class FakeShare {
    val files = ConcurrentHashMap<String, String>()
    val saves: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile var beforeSave: (callIndex: Int) -> Unit = {}

    val repository: FileRepository = Proxy.newProxyInstance(
        FileRepository::class.java.classLoader,
        arrayOf(FileRepository::class.java)
    ) { _, method, args ->
        when (method.name) {
            "loadFile" -> files["${args!![0]}/${args[1]}"]
            "employeeFileExists" -> files.containsKey("${args!![0]}/${(args[1] as String).removeSuffix(".json")}")
            "saveJSON" -> {
                val idx = synchronized(saves) { saves.add(args!![0] as String); saves.size - 1 }
                beforeSave(idx)
                files["${args!![1]}/${args[2]}"] = args[0] as String
                "SUCCESS"
            }
            "saveInDir" -> "SUCCESS"
            "acquireLock" -> true
            "getAvailableDates" -> emptyList<String>()
            "listFilesInDir", "listEmployeeFolders", "listGlobalDir" -> emptyList<String>()
            else -> null
        }
    } as FileRepository
}

fun newModel(share: FakeShare, employee: String = "101"): TimesheetViewModel {
    val model = TimesheetViewModel(Application())
    runBlocking { model.initialize(employee, share.repository) }
    return model
}

fun waitUntil(timeoutMs: Long = 5000, what: String = "condition", cond: () -> Boolean) = runBlocking {
    try {
        withTimeout(timeoutMs) { while (!cond()) delay(10) }
    } catch (e: Exception) {
        throw AssertionError("Timed out waiting for $what", e)
    }
}

fun card(employee: String, week: String, rowsJson: String, extraTopLevel: String = ""): String =
    """{"employeeName":"$employee","weekStarting":"$week","updatedAt":"2026-11-03T12:00:00Z",""" +
        (if (extraTopLevel.isNotEmpty()) "$extraTopLevel," else "") +
        """"rows":[$rowsJson]}"""
