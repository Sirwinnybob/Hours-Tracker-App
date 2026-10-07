package com.example.timecard.ui.theme

import android.content.Context

/**
 * Remembers each employee's equipped theme (profile accentColor) on this tablet, so a KKC
 * auto-login launch can apply the right theme on the very first frame. The real profile only loads
 * after login (network/SAF), and without this the app drew the default theme first, then switched.
 * The loaded profile always wins once it arrives; this only covers the gap before it.
 */
class LaunchAccentCache(context: Context) {

    /** A remembered theme. [accent] null = the standard theme (no immersive theme equipped). */
    data class Cached(val accent: String?)

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** null when nothing is remembered for [login] (first launch for that employee on this tablet). */
    fun get(login: String): Cached? {
        val k = key(login)
        if (!prefs.contains(k)) return null
        return Cached(prefs.getString(k, "").orEmpty().ifEmpty { null })
    }

    /** Stores [accent] under every identity the auto-login may be given (PIN / id and name). */
    fun put(logins: List<String>, accent: String?) {
        val editor = prefs.edit()
        logins.filter { it.isNotBlank() }.forEach { editor.putString(key(it), accent.orEmpty()) }
        editor.apply()
    }

    companion object {
        private const val PREFS = "launch_accent_cache"

        /** Case/whitespace-insensitive, matching how TimecardApp resolves the auto-login input. */
        fun key(login: String): String = "accent:" + login.trim().lowercase()
    }
}
