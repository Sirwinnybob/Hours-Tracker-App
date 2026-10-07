package com.example.timecard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.timecard.data.cache.FileCache
import com.example.timecard.kkcnav.KkcInterFontFamily
import com.example.timecard.kkcnav.KkcNavBarPayload
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.update.NotificationWorker
import com.example.timecard.update.UpdateManager
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTO_LOGIN = "extra_auto_login"
        const val EXTRA_JOB_NUMBER = "extra_job_number"
        const val EXTRA_HOURS = "extra_hours"
        const val EXTRA_LAUNCHED_BY_KKC = "extra_launched_by_kkc"
    }

    private lateinit var updateManager: UpdateManager
    private lateinit var themeState: ThemeState

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide system status and navigation bars for immersive fullscreen
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        updateManager = UpdateManager(this)
        themeState = ThemeState.create(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        scheduleReminders()

        val autoLoginInput = intent?.getStringExtra(EXTRA_AUTO_LOGIN)
        val jobNumberInput = intent?.getStringExtra(EXTRA_JOB_NUMBER)
        val hoursInput = intent?.getStringExtra(EXTRA_HOURS)?.toDoubleOrNull()
        val launchedByKkc = intent?.getBooleanExtra(EXTRA_LAUNCHED_BY_KKC, false) == true || autoLoginInput != null
        @Suppress("DEPRECATION") // Bundle.get(key): one generic read for every extra type in the contract
        val kkcNavBar = if (launchedByKkc) KkcNavBarPayload.parse { key -> intent?.extras?.get(key) } else null
        if (launchedByKkc) {
            Log.i("KkcNav", "navbar payload=${if (kkcNavBar == null) "none" else "v${com.example.timecard.kkcnav.KkcNavBarContract.VERSION} animDisabled=${kkcNavBar.animDisabled} blurDisabled=${kkcNavBar.blurDisabled}"}")
        }

        if (kkcNavBar != null) {
            // Warm the async Google Font so the mirrored navbar labels don't flash the fallback font.
            lifecycleScope.launch {
                try {
                    createFontFamilyResolver(this@MainActivity).preload(KkcInterFontFamily)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("TimecardMain", "Inter font preload failed; navbar uses the fallback font", e)
                }
            }
        }

        setContent {
            TimecardApp(
                themeState = themeState,
                autoLoginInput = autoLoginInput,
                jobNumberInput = jobNumberInput,
                hoursInput = hoursInput,
                launchedByKkc = launchedByKkc,
                kkcNavBar = kkcNavBar,
                onReinstallLatest = { updateManager.reinstallLatest() }
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
