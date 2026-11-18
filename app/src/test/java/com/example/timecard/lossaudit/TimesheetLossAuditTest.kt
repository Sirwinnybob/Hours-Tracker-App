package com.example.timecard.lossaudit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.timecard.data.repository.DirectFileRepository
import com.example.timecard.domain.DateUtils
import com.example.timecard.ui.timesheet.SaveStatus
import com.example.timecard.ui.timesheet.TimesheetViewModel
import com.example.timecard.ui.timesheet.getDayTotal
import com.example.timecard.ui.timesheet.getGrandTotal
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Data-loss audit of the tablet timecard flow (TimesheetViewModel + repositories).
 * Each test reproduces a scenario where hours an employee typed or saw never reach the
 * week file (or are later overwritten) without any error being shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimesheetLossAuditTest {
    private val week = DateUtils.getWeekStartingMonday()
    private val key = "101/$week"
    private val gson = Gson()

    @Before fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun rows(json: String) = gson.fromJson(json, JsonObject::class.java).getAsJsonArray("rows")

    // ── 1. Debounced autosave dropped when the Activity finishes (← KKC / KKC nav bar) ─────────
    @Test fun `pending autosave is cancelled when the activity finishes within 2s of an edit`() {
        // Control: with the VM alive, the autosave lands ~2s after the edit.
        val controlShare = FakeShare()
        val control = newModel(controlShare)
        control.setJob(1, "123"); control.setHours(1, 0, "7")
        waitUntil(4000, "control autosave") { controlShare.files[key]?.contains("7.00") == true }

        // Same edit, then the user taps "← KKC" (activity.finish()) → ViewModelStore cleared.
        val share = FakeShare()
        val store = ViewModelStore()
        val model = newModel(share)
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = model as T
        })[TimesheetViewModel::class.java]
        model.setJob(1, "123"); model.setHours(1, 0, "7")
        assertEquals(SaveStatus.SYNCING, model.uiState.value.saveStatus)
        store.clear() // what Activity.finish() → onDestroy does to activity-scoped ViewModels
        Thread.sleep(3000)
        assertTrue("edit never written: saves=${share.saves}", share.saves.isEmpty())
        assertTrue(share.files[key] == null)
    }

    // ── 2. Logout within the autosave window ────────────────────────────────────────────────
    @Test fun `logout within 2s of an edit discards the edit without saving`() {
        val share = FakeShare()
        share.files[key] = card("101", week, """{"job":"SHOP","mon":"8.00"}""")
        val model = newModel(share)
        model.setHours(0, 1, "8") // Tuesday 8h
        model.logout()
        Thread.sleep(3000)
        assertTrue("no save attempted after logout", share.saves.isEmpty())
        assertFalse(share.files.getValue(key).contains("\"tue\":\"8.00\""))
    }

    // ── 3. KKC clock-out prefill overwrites earlier hours on the same job/day ────────────────
    @Test fun `KKC prefill replaces existing hours for the same job today instead of adding`() {
        assumeTrue(LocalDate.now().dayOfWeek != DayOfWeek.SUNDAY)
        val today = LocalDate.now().dayOfWeek.value - 1 // Mon=0
        val day = listOf("mon", "tue", "wed", "thu", "fri", "sat")[today]
        val share = FakeShare()
        share.files[key] = card("101", week, """{"job":"SHOP"},{"job":"123","$day":"3.00"}""")
        val model = newModel(share)
        assertEquals("3.00", model.uiState.value.hours[1][today])

        model.prefillJobEntry("123", 2.0) // afternoon clock-out of the same job: 2h
        model.saveData()
        waitUntil(what = "save") { share.saves.isNotEmpty() && model.uiState.value.saveStatus == SaveStatus.SAVED }
        val saved = rows(share.files.getValue(key))[1].asJsonObject
        assertEquals("morning 3h replaced, not 3+2", "2.00", saved.get(day).asString)
    }

    @Test fun `KKC prefill is silently dropped when every row already has a job`() {
        assumeTrue(LocalDate.now().dayOfWeek != DayOfWeek.SUNDAY)
        val share = FakeShare()
        val nine = (0 until 9).joinToString(",") { i -> """{"job":"${if (i == 0) "SHOP" else (100 + i).toString()}"}""" }
        share.files[key] = card("101", week, nine)
        val model = newModel(share)
        val before = model.uiState.value
        model.prefillJobEntry("999", 2.5)
        val after = model.uiState.value
        assertEquals(before.jobs, after.jobs)
        assertEquals(before.hours, after.hours)
        assertEquals(SaveStatus.SAVED, after.saveStatus) // no error, no new row
        assertTrue(share.saves.isEmpty())
    }

    // ── 4. "No lunch" half hour is display-only ─────────────────────────────────────────────
    @Test fun `no-lunch half hour counts in the shown day total but is never persisted`() {
        val share = FakeShare()
        val model = newModel(share)
        model.setHours(0, 0, "8.5")          // SHOP Monday 8.5
        model.toggleNoLunch(0)               // employee unchecks Lunch for Monday
        assertEquals(9.0, model.uiState.value.getDayTotal(0), 0.0) // UI: 9.0 (green, target met)
        model.saveData()
        waitUntil(what = "save") { model.uiState.value.saveStatus == SaveStatus.SAVED && share.files[key] != null }
        val json = share.files.getValue(key)
        assertEquals("8.50", rows(json)[0].asJsonObject.get("mon").asString)
        assertFalse("no lunch flag in file: $json", json.contains("lunch", ignoreCase = true))

        val reloaded = newModel(share)
        assertEquals(8.5, reloaded.uiState.value.getDayTotal(0), 0.0) // the +0.5 is gone after reload
        assertTrue(reloaded.uiState.value.noLunchDays.isEmpty())
    }

    @Test fun `fill-to-target with lunch unchecked writes 8_5 while showing 9`() {
        val share = FakeShare()
        val model = newModel(share)
        model.toggleNoLunch(1)   // Tuesday: no lunch
        model.fillShopHours(1)   // tap "+" on Tuesday's daily total (target 9)
        assertEquals(9.0, model.uiState.value.getDayTotal(1), 0.0)
        model.saveData()
        waitUntil(what = "save") { model.uiState.value.saveStatus == SaveStatus.SAVED && share.files[key] != null }
        assertEquals("8.50", rows(share.files.getValue(key))[0].asJsonObject.get("tue").asString)
    }

    // ── 5. Unreadable file → blank grid, no error → next edit overwrites the week ────────────
    @Test fun `truncated week file loads as a blank grid with no error and the next edit overwrites it`() {
        val share = FakeShare()
        share.files[key] = card("101", week, """{"job":"SHOP","mon":"9.00"},{"job":"123","tue":"8.00"}""").dropLast(3)
        val model = newModel(share)
        val s = model.uiState.value
        assertTrue(s.isDataLoaded)
        assertEquals(SaveStatus.SAVED, s.saveStatus) // nothing tells the employee the load failed
        assertTrue(s.hours.flatten().all { it.isBlank() })
        model.setHours(0, 2, "4")
        model.saveData()
        waitUntil(what = "save") { share.saves.isNotEmpty() && model.uiState.value.saveStatus == SaveStatus.SAVED }
        val after = share.files.getValue(key)
        assertFalse("job 123 / Mon 9h overwritten: $after", after.contains("123") || after.contains("9.00"))
    }

    @Test fun `file whose rows are null or missing also loads blank and is overwritten`() {
        // (A UTF-8 BOM prefix was also tried: Gson tolerates it, so that variant is NOT a loss.)
        for (content in listOf(
            """{"employeeName":"101","weekStarting":"$week","updatedAt":"x","rows":null,"job":"123"}""",
            """{"employeeName":"101","weekStarting":"$week","updatedAt":"x","Rows":[{"job":"123","mon":"8.00"}]}"""
        )) {
            val share = FakeShare()
            share.files[key] = content
            val model = newModel(share)
            assertTrue(model.uiState.value.hours.flatten().all { it.isBlank() })
            assertEquals(SaveStatus.SAVED, model.uiState.value.saveStatus)
            model.setHours(0, 3, "1"); model.saveData()
            waitUntil(what = "save") { share.saves.isNotEmpty() && model.uiState.value.saveStatus == SaveStatus.SAVED }
            assertFalse(share.files.getValue(key).contains("123"))
        }
    }

    // ── 6. Gson model round-trip drops fields another writer added ───────────────────────────
    @Test fun `saving drops unknown top-level and row fields written by another writer`() {
        val share = FakeShare()
        share.files[key] = card(
            "101", week,
            """{"job":"123","mon":"8.00","note":"left early, approved","approved":true}""",
            extraTopLevel = """"approvedBy":"office","reviewNote":"keep""""
        )
        val model = newModel(share)
        model.setHours(1, 1, "8"); model.saveData()
        waitUntil(what = "save") { share.saves.isNotEmpty() && model.uiState.value.saveStatus == SaveStatus.SAVED }
        val after = share.files.getValue(key)
        for (k in listOf("approvedBy", "reviewNote", "note", "approved")) assertFalse("$k survived? $after", after.contains("\"$k\""))
    }

    // ── 7. Hours on a row with no job number: counted on tablet, skipped by server sync ──────
    @Test fun `hours on a blank-job row are in the tablet total and saved under an empty job`() {
        val share = FakeShare()
        val model = newModel(share)
        model.setHours(0, 0, "4")       // SHOP 4
        model.setHours(2, 0, "4")       // row 2 has no job number
        assertEquals(8.0, model.uiState.value.getGrandTotal(), 0.0)
        model.saveData()
        waitUntil(what = "save") { share.files[key] != null && model.uiState.value.saveStatus == SaveStatus.SAVED }
        val row2 = rows(share.files.getValue(key))[2].asJsonObject
        assertEquals("", row2.get("job").asString)
        assertEquals("4.00", row2.get("mon").asString) // backend db.py sync_digital_timecard skips job=="" rows
    }

    // ── 8. Default-locale number formatting ──────────────────────────────────────────────────
    @Test fun `comma-decimal locale zeroes every loaded hour on the next save`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val share = FakeShare()
            share.files[key] = card("101", week, """{"job":"SHOP","mon":"8.5","tue":"9"},{"job":"123","wed":"4"}""")
            val model = newModel(share)
            assertEquals("8,50", model.uiState.value.hours[0][0]) // loadGrid uses String.format without Locale
            model.setHours(1, 3, "2")
            model.saveData()
            waitUntil(what = "save") { share.saves.isNotEmpty() && model.uiState.value.saveStatus == SaveStatus.SAVED }
            val r = rows(share.files.getValue(key))
            assertEquals("", r[0].asJsonObject.get("mon").asString)
            assertEquals("", r[0].asJsonObject.get("tue").asString)
            assertEquals("", r[1].asJsonObject.get("wed").asString)
        } finally {
            Locale.setDefault(saved)
        }
    }

    // ── 9. Saves are not serialized: a slow older save can land last ─────────────────────────
    @Test fun `a slow earlier autosave finishing after a later one leaves the older snapshot on disk`() {
        val share = FakeShare()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        share.beforeSave = { idx -> if (idx == 0) { firstEntered.countDown(); releaseFirst.await(15, TimeUnit.SECONDS) } }
        val model = newModel(share)

        model.setJob(1, "111"); model.setHours(1, 0, "1")
        assertTrue(firstEntered.await(5, TimeUnit.SECONDS)) // autosave #1 snapshot taken, write stalled
        model.setJob(2, "222"); model.setHours(2, 0, "2")
        waitUntil(5000, "autosave #2") { share.saves.size >= 2 && share.files[key]?.contains("222") == true }
        releaseFirst.countDown()
        waitUntil(what = "save #1 landing") { share.files[key]?.contains("222") == false }
        Thread.sleep(100)
        assertEquals(SaveStatus.SAVED, model.uiState.value.saveStatus)
        assertEquals("222", model.uiState.value.jobs[2]) // employee still sees 222 and "SAVED"
        assertFalse(share.files.getValue(key).contains("222"))
    }

    // ── 10. Syncthing conflict copies are invisible to the app ───────────────────────────────
    @Test fun `syncthing conflict copy of a week is ignored by the tablet`() {
        val root = Files.createTempDirectory("loss-conflict").toFile()
        try {
            val dir = root.resolve("101").apply { mkdirs() }
            dir.resolve("2026-11-02.json").writeText(card("101", "2026-11-02", """{"job":"123","mon":"8.00"}"""))
            dir.resolve("2026-11-02.sync-conflict-20261103-101500-ABCDEFG.json")
                .writeText(card("101", "2026-11-02", """{"job":"123","mon":"8.00","tue":"8.00"},{"job":"456","wed":"6.00"}"""))
            val repo = DirectFileRepository(root)
            assertEquals(listOf("2026-11-02"), repo.getAvailableDates("101"))
            val loaded = repo.loadFile("101", "2026-11-02")!!
            assertFalse(loaded.contains("456"))
            assertNotEquals(null, loaded)
        } finally { root.deleteRecursively() }
    }
}
