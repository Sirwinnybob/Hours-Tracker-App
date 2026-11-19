package com.example.timecard.ui.timesheet.standard

import org.junit.Assert.assertEquals
import org.junit.Test

class JobPickerSelectionTest {
    @Test
    fun `delivery mode saves a numbered selection with the delivery suffix`() {
        assertEquals(JobPick.Entry("725D"), resolveJobPick("725", delivery = true, emptyMap()))
        assertEquals(JobPick.Entry("725d"), resolveJobPick("725d", delivery = true, emptyMap()))
        assertEquals(JobPick.Entry("725d"), resolveJobPick("725d", delivery = false, emptyMap()))
        assertEquals(JobPick.Entry("725"), resolveJobPick("725", delivery = false, emptyMap()))
    }

    @Test
    fun `delivery and regular work on the same job can use separate rows`() {
        assertEquals(JobPick.Entry("725D"), resolveJobPick("725", delivery = true, mapOf("725" to 2)))
        assertEquals(JobPick.ExistingRow(3), resolveJobPick("725", delivery = true, mapOf("725D" to 3)))
        assertEquals(JobPick.Entry("725"), resolveJobPick("725", delivery = false, mapOf("725D" to 3)))
    }

    @Test
    fun `delivery mode preserves other entries including historical leave codes`() {
        for (job in listOf("SHOP", "PTO", "SICK", "HOLIDAY", "")) {
            assertEquals(JobPick.Entry(job), resolveJobPick(job, delivery = true, emptyMap()))
        }
    }
}
