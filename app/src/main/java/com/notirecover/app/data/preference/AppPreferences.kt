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

    // ==========================================
    // FITUR PRO & MONETISASI GOOGLE PLAY
    // ==========================================
    var isProUser: Boolean
        get() = prefs.getBoolean("is_pro_user", true)
        set(value) = prefs.edit().putBoolean("is_pro_user", value).apply()

    // Kunci Aplikasi & Biometrik
    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var appLockPin: String
        get() = prefs.getString("app_lock_pin", "1234") ?: "1234"
        set(value) = prefs.edit().putString("app_lock_pin", value).apply()

    var useBiometric: Boolean
        get() = prefs.getBoolean("use_biometric", true)
        set(value) = prefs.edit().putBoolean("use_biometric", value).apply()

    // Status Saver PRO: Auto-Save Status Favorit
    var autoSaveFavoriteStatus: Boolean
        get() = prefs.getBoolean("auto_save_favorite_status", false)
        set(value) = prefs.edit().putBoolean("auto_save_favorite_status", value).apply()

    // Smart Keyword Alert: Deteksi Kata Kunci Sensitif
    var smartAlertKeywords: String
        get() = prefs.getString("smart_alert_keywords", "transfer, bukti, pinjam, uang, rahasia, password, rekening") ?: "transfer, bukti, pinjam, uang, rahasia, password, rekening"
        set(value) = prefs.edit().putString("smart_alert_keywords", value).apply()

    var isSmartAlertEnabled: Boolean
        get() = prefs.getBoolean("smart_alert_enabled", true)
        set(value) = prefs.edit().putBoolean("smart_alert_enabled", value).apply()

    // Floating Bubble / Direct Chat Shortcut
    var isDirectChatFloatingEnabled: Boolean
        get() = prefs.getBoolean("direct_chat_floating_enabled", false)
        set(value) = prefs.edit().putBoolean("direct_chat_floating_enabled", value).apply()
}
