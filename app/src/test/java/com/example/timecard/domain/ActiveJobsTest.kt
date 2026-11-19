package com.example.timecard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveJobsTest {

    // Real Ready Jobs root listing (2026-11-19), including the non-job folders that must be skipped.
    private val folders = listOf(
        "00 INSTALLATION DRAWINGS (FILLERS, CROWN)",
        "106b - WEAVER FOYER",
        "591 - BHC 2555 MOON MOUNTAIN",
        "614 - KLUMP",
        "679 - LIZ NAYADLEY 235 PORTLAND ST",
        "690r - TRUNORTH 892 CHATEAU MEADOWS",
        "711 - WIECHERT 88141 WYMORE",
        "723 - WIECHERT 88129 WYMORE",
        "725 - WIECHERT 34 CHURCHVIEW",
        ".time_cards"
    )
    private val jobs = ActiveJobs.fromFolderNames(folders)

    @Test
    fun `parses job folders, skips everything else, sorts by number`() {
        assertEquals(listOf("106b", "591", "614", "679", "690r", "711", "723", "725"), jobs.map { it.number })
        assertEquals(ActiveJob("725", "WIECHERT 34 CHURCHVIEW"), ActiveJobs.parseFolderName("725 - WIECHERT 34 CHURCHVIEW"))
        assertNull(ActiveJobs.parseFolderName("00 INSTALLATION DRAWINGS (FILLERS, CROWN)"))
        assertNull(ActiveJobs.parseFolderName("701-PULLS.pdf"))
    }

    @Test
    fun `number prefix matches come first, then names`() {
        assertEquals(listOf("723", "725"), ActiveJobs.search("72", jobs).map { it.number })
        assertEquals(listOf("711", "723"), ActiveJobs.search("wymore", jobs).map { it.number })
        // "7" matches numbers 711/723/725 first; nothing by name is needed to fill the limit.
        assertEquals(listOf("711", "723", "725"), ActiveJobs.search("7", jobs, limit = 3).map { it.number })
        assertTrue(ActiveJobs.search("   ", jobs).isEmpty())
        assertTrue(ActiveJobs.search("9999", jobs).isEmpty())
    }

    @Test
    fun `exact number decides whether the not-on-the-floor choice is offered`() {
        assertTrue(ActiveJobs.isExactNumber("725", jobs))
        assertTrue(ActiveJobs.isExactNumber("106B", jobs))
        assertFalse(ActiveJobs.isExactNumber("72", jobs))
        assertFalse(ActiveJobs.isExactNumber("684", jobs))
        assertFalse(ActiveJobs.isExactNumber("", jobs))
    }

    @Test
    fun `find resolves stored entries including the delivery suffix`() {
        assertEquals("WIECHERT 34 CHURCHVIEW", ActiveJobs.find("725", jobs)?.name)
        assertEquals("WIECHERT 34 CHURCHVIEW", ActiveJobs.find("725D", jobs)?.name)
        assertEquals("WEAVER FOYER", ActiveJobs.find("106B", jobs)?.name)
        assertNull(ActiveJobs.find("SHOP", jobs))
        assertNull(ActiveJobs.find("684", jobs))
    }
}
