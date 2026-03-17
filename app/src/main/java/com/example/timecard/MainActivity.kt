package com.example.timecard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.timecard.data.cache.FileCache
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.update.NotificationWorker
import com.example.timecard.update.UpdateManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: UpdateManager
    private lateinit var themeState: ThemeState

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateManager = UpdateManager(this)
        themeState = ThemeState.create(this)

        updateManager.checkForUpdates()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        scheduleReminders()

        setContent {
            TimecardApp(
                themeState = themeState,
                onReinstallLatest = { updateManager.reinstallLatest() },
                pendingUpdate = updateManager.pendingUpdateApk,
                onInstallUpdate = { updateManager.installPendingUpdate() }
            )
        }
    }

    /**
     * Schedules two daily hour-reminder notifications:
     *   - 11:50 AM  "Halfway through the day — don't forget to log your hours!"
     *   - 4:15 PM   "Almost time to clock out — log your hours before you leave!"
     *
     * Uses KEEP policy so existing schedules aren't disrupted on subsequent app launches.
     */
    private fun scheduleReminders() {
        val wm = WorkManager.getInstance(this)

        scheduleReminder(
            wm, "reminder_midday",
            targetHour = 11, targetMinute = 50,
            message = "🕐 Halfway through the day — don't forget to log your hours!",
            notifId = 1001
        )
        scheduleReminder(
            wm, "reminder_endofday",
            targetHour = 16, targetMinute = 15,
            message = "⏰ Almost time to clock out — log your hours before you leave!",
            notifId = 1002
        )
    }

    private fun scheduleReminder(
        wm: WorkManager,
        workName: String,
        targetHour: Int,
        targetMinute: Int,
        message: String,
        notifId: Int
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val inputData = Data.Builder()
            .putString(NotificationWorker.KEY_MESSAGE, message)
            .putInt(NotificationWorker.KEY_NOTIF_ID, notifId)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        wm.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun onResume() {
        super.onResume()
        FileCache.clear()
    }
}
