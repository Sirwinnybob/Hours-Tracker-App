package com.example.timecard.data.model

data class Employee(
    val id: String,
    val name: String,
    val excluded: Boolean = false
)
