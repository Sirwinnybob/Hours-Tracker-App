package com.example.timecard.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val message = inputData.getString(KEY_MESSAGE)
            ?: "Don't forget to log your hours!"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the channel (safe to call repeatedly — no-op if already exists)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hour Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders to log your hours"
        }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hours Tracker")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(inputData.getInt(KEY_NOTIF_ID, NOTIF_ID_DEFAULT), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "hours_reminders"
        const val KEY_MESSAGE = "message"
        const val KEY_NOTIF_ID = "notif_id"
        const val NOTIF_ID_DEFAULT = 1001
    }
}
