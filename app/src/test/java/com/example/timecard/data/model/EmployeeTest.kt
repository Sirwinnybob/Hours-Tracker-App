package com.example.timecard.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class EmployeeTest {
    @Test
    fun `parses legacy entries without new fields using safe defaults`() {
        val json = """[{"id":"389","name":"Winston Ferguson","excluded":false}]"""
        val type = object : TypeToken<List<Employee>>() {}.type
        val employees: List<Employee> = Gson().fromJson(json, type)

        assertEquals("389", employees[0].id)
        assertEquals("Winston Ferguson", employees[0].name)
        assertEquals("", employees[0].displayName)
        assertEquals(null, employees[0].rtcId)
        assertEquals("hours_tracker", employees[0].addedBy)
    }

    @Test
    fun `parses new-format entries with all fields`() {
        val json = """[{"id":"389","name":"Winston Ferguson","displayName":"Fergy","rtcId":12345,"excluded":false,"addedBy":"rtc"}]"""
        val type = object : TypeToken<List<Employee>>() {}.type
        val employees: List<Employee> = Gson().fromJson(json, type)

        assertEquals("Fergy", employees[0].displayName)
        assertEquals(12345, employees[0].rtcId)
        assertEquals("rtc", employees[0].addedBy)
    }

    @Test
    fun `parses timeclockInactive and defaults it to false when missing`() {
        val json = """[{"id":"067","name":"Jared Rosenburg","timeclockInactive":true},{"id":"389","name":"Winston Ferguson"}]"""
        val type = object : TypeToken<List<Employee>>() {}.type
        val employees: List<Employee> = Gson().fromJson(json, type)

        assertEquals(true, employees[0].timeclockInactive)
        assertEquals(false, employees[1].timeclockInactive)
    }
}
