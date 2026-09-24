package com.notirecover.app.data.preference

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notirecover_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val LANG_ID = "ID"
        const val LANG_EN = "EN"
    }

    var themeMode: String
        get() = prefs.getString("theme_mode", THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var language: String
        get() = prefs.getString("language", LANG_ID) ?: LANG_ID
        set(value) = prefs.edit().putString("language", value).apply()
}
