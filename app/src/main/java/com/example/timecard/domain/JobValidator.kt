package com.example.timecard.domain

object JobValidator {

    private val ALLOWED_JOB_WORDS = listOf("SHOP", "DELIVERY", "VACATION", "HOLIDAY", "SICK", "PERSONAL")

    fun isValidJobEntry(value: String): Boolean {
        if (value.isBlank()) return true
        val v = value.trim().uppercase()
        if (v in ALLOWED_JOB_WORDS) return true
        return Regex("^\\d+D?$").matches(v)
    }

    fun isDeliveryJob(value: String): Boolean {
        if (value.isBlank()) return false
        return Regex("^\\d+D$", RegexOption.IGNORE_CASE).matches(value.trim())
    }

    fun snapToQuarter(value: Double): Double {
        if (value.isNaN()) return 0.0
        val snapped = Math.round(value * 4.0) / 4.0
        return if (snapped > 0) snapped else 0.0
    }

    fun snapToQuarter(value: String): String {
        if (value.isBlank()) return ""
        val num = value.toDoubleOrNull() ?: return ""
        val snapped = snapToQuarter(num)
        return if (snapped > 0) String.format("%.2f", snapped) else ""
    }
}
