package com.example.timecard.data.model

data class Employee(
    val id: String,
    val name: String,
    val excluded: Boolean = false,
    val displayName: String = "",
    val rtcId: Int? = null,
    val addedBy: String = "hours_tracker"
) {
    // Gson allocates instances via Unsafe (bypassing the primary constructor) unless a
    // no-arg constructor is present, which means Kotlin default values are otherwise
    // silently ignored for fields missing from JSON (they come back null/0/false instead).
    // This no-arg constructor gives Gson one to find, so legacy JSON without the newer
    // fields still gets the intended defaults.
    constructor() : this(
        id = "",
        name = "",
        excluded = false,
        displayName = "",
        rtcId = null,
        addedBy = "hours_tracker"
    )
}
