package com.example.timecard.ui.timesheet

import android.app.Application
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.DateUtils
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeekNavigationTest {
    @Before fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `missing previous card opens a blank previous week and can return`() = runBlocking {
        val files = ConcurrentHashMap<String, String>()
        val repository = Proxy.newProxyInstance(FileRepository::class.java.classLoader, arrayOf(FileRepository::class.java)) { _, method, args ->
            when (method.name) {
                "loadFile" -> files[args!![1] as String]
                "saveJSON" -> { files[args!![2] as String] = args[0] as String; "SUCCESS" }
                "saveInDir" -> "SUCCESS"
                "acquireLock" -> true
                else -> null
            }
        } as FileRepository
        val model = TimesheetViewModel(Application())
        model.initialize("employee", repository)
        val current = model.uiState.value.currentWeekDate
        assertFalse(model.uiState.value.hasPreviousWeek)

        model.togglePrevWeek()
        withTimeout(3000) { while (!model.uiState.value.isDataLoaded) delay(10) }
        assertEquals(DateUtils.getPreviousMonday(current), model.uiState.value.activeWeekDate)
        assertTrue(model.uiState.value.isViewingPrevious)
        assertTrue(model.uiState.value.hours.flatten().all { it.isBlank() })
        model.setJob(1, "123")
        model.saveData()
        withTimeout(3000) { while (files[DateUtils.getPreviousMonday(current)] == null) delay(10) }
        assertTrue(files.getValue(DateUtils.getPreviousMonday(current)).contains("123"))
        withTimeout(3000) { while (model.uiState.value.saveStatus != SaveStatus.SAVED) delay(10) }

        delay(600)
        model.togglePrevWeek()
        withTimeout(3000) { while (!model.uiState.value.isDataLoaded) delay(10) }
        assertEquals(current, model.uiState.value.activeWeekDate)
        assertFalse(model.uiState.value.isViewingPrevious)
    }
}
