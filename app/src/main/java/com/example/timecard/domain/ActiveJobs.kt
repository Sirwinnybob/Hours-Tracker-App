package com.example.timecard.domain

/** A job on the floor: one `NNN - NAME` folder in the Ready Jobs root. */
data class ActiveJob(val number: String, val name: String)

/**
 * Active-job lookup for the job picker. Ready Jobs (owned by the Ready Jobs worker / KKCSheetTracker)
 * holds one folder per job named `"<number> - <name>"`; Hours Tracker only reads the folder names.
 */
object ActiveJobs {

    private val FOLDER = Regex("^\\s*([0-9]+[A-Za-z]?)\\s+-\\s+(.+?)\\s*$")

    /** `"725 - WIECHERT 34 CHURCHVIEW"` → ActiveJob("725", "WIECHERT 34 CHURCHVIEW"); anything else → null. */
    fun parseFolderName(folderName: String): ActiveJob? {
        val match = FOLDER.matchEntire(folderName) ?: return null
        return ActiveJob(number = match.groupValues[1], name = match.groupValues[2])
    }

    /** Parses folder names, drops non-job folders and duplicate numbers, sorts by job number. */
    fun fromFolderNames(folderNames: List<String>): List<ActiveJob> =
        folderNames.mapNotNull(::parseFolderName)
            .distinctBy { it.number.uppercase() }
            .sortedWith(compareBy<ActiveJob>({ leadingNumber(it.number) }, { it.number.uppercase() }))

    /**
     * Jobs matching [query]: job numbers starting with it first (in number order), then jobs whose
     * name contains it. Case-insensitive. A blank query matches nothing.
     */
    fun search(query: String, jobs: List<ActiveJob>, limit: Int = 4): List<ActiveJob> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val byNumber = jobs.filter { it.number.lowercase().startsWith(q) }
        val byName = jobs.filter { it !in byNumber && it.name.lowercase().contains(q) }
        return (byNumber + byName).take(limit)
    }

    /** True when [query] is exactly an active job's number (so no "not on the floor" choice is needed). */
    fun isExactNumber(query: String, jobs: List<ActiveJob>): Boolean {
        val q = query.trim()
        return q.isNotEmpty() && jobs.any { it.number.equals(q, ignoreCase = true) }
    }

    /**
     * The active job a timesheet entry refers to. Entries are stored uppercase and a Delivery entry
     * carries a trailing `D` (`725D`), so that suffix is ignored when the bare number is a job.
     */
    fun find(entry: String, jobs: List<ActiveJob>): ActiveJob? {
        val e = entry.trim()
        if (e.isEmpty()) return null
        jobs.firstOrNull { it.number.equals(e, ignoreCase = true) }?.let { return it }
        if (JobValidator.isDeliveryJob(e)) {
            val bare = e.dropLast(1)
            return jobs.firstOrNull { it.number.equals(bare, ignoreCase = true) }
        }
        return null
    }

    private fun leadingNumber(number: String): Int =
        number.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
}
