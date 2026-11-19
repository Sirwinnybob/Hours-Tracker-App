package com.example.timecard.ui.timesheet.standard

/**
 * Which timesheet rows the standard grid shows. The data always has at least 9 rows (row 0 = SHOP);
 * the grid shows only rows that hold something — a job or any hours — plus the row being added.
 * A row with hours but no job is still shown, so no logged time is ever hidden.
 */
fun visibleRowIndices(jobs: List<String>, hours: List<List<String>>, rowCount: Int, pendingRow: Int?): List<Int> =
    (0 until rowCount).filter { i ->
        i == pendingRow ||
            jobs.getOrElse(i) { "" }.isNotBlank() ||
            hours.getOrNull(i).orEmpty().any { it.isNotBlank() }
    }

/** First row with no job and no hours (the one "Add job" fills), or null when every row is used. */
fun firstEmptyRow(jobs: List<String>, hours: List<List<String>>, rowCount: Int): Int? =
    (0 until rowCount).firstOrNull { i ->
        jobs.getOrElse(i) { "" }.isBlank() && hours.getOrNull(i).orEmpty().all { it.isBlank() }
    }
