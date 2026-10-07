package com.example.timecard.kkcnav

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log

data class KkcReturnRequest(
    val packageName: String,
    val className: String,
    val flags: Int,
    val destination: String
)

/**
 * Sends a tap on the mirrored navbar back to KKCSheetTracker. Hours Tracker runs on top of KKC's
 * MainActivity in KKC's task, so CLEAR_TOP|SINGLE_TOP pops Hours Tracker and delivers onNewIntent
 * to the existing KKC activity (state preserved).
 */
object KkcReturnNavigation {

    fun requestFor(route: String): KkcReturnRequest? {
        if (route.isBlank() || route == KkcNavBarContract.DEST_HOURS) return null
        return KkcReturnRequest(
            packageName = KkcNavBarContract.KKC_PACKAGE,
            className = KkcNavBarContract.KKC_ACTIVITY,
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            destination = route
        )
    }

    fun navigate(activity: Activity, route: String, animationsDisabled: Boolean) {
        // A second tap while the first is closing Hours Tracker must not launch KKC twice.
        if (activity.isFinishing) return
        val request = requestFor(route) ?: return
        val intent = Intent().apply {
            setClassName(request.packageName, request.className)
            addFlags(request.flags)
            putExtra(KkcNavBarContract.EXTRA_NAV_DESTINATION, request.destination)
        }
        val enter = if (animationsDisabled) 0 else android.R.anim.fade_in
        val exit = if (animationsDisabled) 0 else android.R.anim.fade_out
        try {
            activity.startActivity(intent, ActivityOptions.makeCustomAnimation(activity, enter, exit).toBundle())
        } catch (e: ActivityNotFoundException) {
            Log.w("KkcNav", "KKCSheetTracker not available; closing Hours Tracker", e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enter, exit)
        }
        activity.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enter, exit)
        }
    }
}
