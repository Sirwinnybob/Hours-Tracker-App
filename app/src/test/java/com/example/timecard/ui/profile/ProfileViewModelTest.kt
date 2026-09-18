package com.example.timecard.ui.profile

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Regression coverage for ProfileViewModel.initialize()'s identity guard.
 *
 * The guard `if (employeeName == id && repo == repository) return` exists so that
 * re-calling initialize() with the SAME employee/repo doesn't re-launch the
 * coroutine that reloads the profile and badge config from disk. But the guard
 * used to also silently drop a NEW displayName when id/repo were unchanged
 * (e.g. same login session, employee list refreshed with an admin-edited name).
 * employeeDisplayName feeds gamification records and activity-event badges, so a
 * stale name could get baked into saved records for the rest of the session.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() {
        // viewModelScope dispatches on Dispatchers.Main by default; on a plain JVM
        // unit test (no Robolectric/Android Main looper) that dispatcher is missing
        // and throws synchronously. Swap in a test dispatcher that runs coroutines
        // eagerly so initialize()'s internal viewModelScope.launch{} doesn't crash.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize updates displayName even when identity guard early-returns`() {
        val viewModel = ProfileViewModel(Application())

        // Same id, same repo instance (null == null) on both calls -> the identity
        // guard fires on the second call. Only the displayName differs, simulating
        // the employee list refreshing an admin-edited display name mid-session.
        viewModel.initialize(id = "389", displayName = "Old Name", repo = null)
        viewModel.initialize(id = "389", displayName = "New Name", repo = null)

        assertEquals("New Name", viewModel.employeeDisplayName)
    }

    @Test
    fun `initialize still updates displayName on first call (identity changes)`() {
        val viewModel = ProfileViewModel(Application())

        viewModel.initialize(id = "389", displayName = "First Name", repo = null)

        assertEquals("First Name", viewModel.employeeDisplayName)
        assertEquals("389", viewModel.employeeName)
    }
}
