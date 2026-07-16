package com.flipperzero.androidkeyboard.prefs

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var flipperMac: String?
        get() = prefs.getString(KEY_FLIPPER_MAC, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_FLIPPER_MAC, value?.trim()?.uppercase()).apply()
        }

    companion object {
        private const val PREFS_NAME = "akb_prefs"
        private const val KEY_FLIPPER_MAC = "flipper_mac"
    }
}
