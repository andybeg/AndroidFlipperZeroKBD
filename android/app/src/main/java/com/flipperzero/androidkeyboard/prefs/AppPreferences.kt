package com.flipperzero.androidkeyboard.prefs

import android.content.Context
import com.flipperzero.androidkeyboard.keyboard.LayoutInfo

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var flipperMac: String?
        get() = prefs.getString(KEY_FLIPPER_MAC, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_FLIPPER_MAC, value?.trim()?.uppercase()).apply()
        }

    var currentLayoutId: String?
        get() = prefs.getString(KEY_CURRENT_LAYOUT, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_CURRENT_LAYOUT, value).apply()
        }

    fun enabledLayoutIds(catalog: List<LayoutInfo>): List<String> {
        val raw = prefs.getString(KEY_ENABLED_LAYOUTS, null)
        val catalogIds = catalog.map { it.id }
        if (raw.isNullOrBlank()) {
            return catalogIds
        }
        val saved = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val enabled = saved.filter { it in catalogIds }
        return enabled.ifEmpty { catalogIds }
    }

    fun setEnabledLayoutIds(ids: List<String>) {
        prefs.edit().putString(KEY_ENABLED_LAYOUTS, ids.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_NAME = "akb_prefs"
        private const val KEY_FLIPPER_MAC = "flipper_mac"
        private const val KEY_ENABLED_LAYOUTS = "enabled_layouts"
        private const val KEY_CURRENT_LAYOUT = "current_layout"
    }
}
