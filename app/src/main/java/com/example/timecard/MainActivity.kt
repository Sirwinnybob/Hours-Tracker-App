package com.example.timecard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.timecard.data.cache.FileCache
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.update.UpdateManager

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: UpdateManager
    private lateinit var themeState: ThemeState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateManager = UpdateManager(this)
        themeState = ThemeState.create(this)

        updateManager.checkForUpdates()

        setContent {
            TimecardApp(
                themeState = themeState,
                onReinstallLatest = { updateManager.reinstallLatest() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        FileCache.clear()
    }
}
