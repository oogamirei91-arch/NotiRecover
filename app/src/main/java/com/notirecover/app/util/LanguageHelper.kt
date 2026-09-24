package com.notirecover.app.util

object LanguageHelper {

    fun get(key: String, lang: String = "ID"): String {
        val isId = lang == "ID"
        return when (key) {
            "app_subtitle" -> if (isId) "Universal Chat & Media Recovery" else "Universal Chat & Media Recovery"
            "tab_messages" -> if (isId) "Pesan" else "Messages"
            "tab_gallery" -> if (isId) "Galeri" else "Gallery"
            "tab_status" -> if (isId) "Status" else "Status"
            "tab_web" -> if (isId) "Web" else "Web"
            "tab_direct" -> if (isId) "Direct WA" else "Direct WA"

            // Settings
            "settings_title" -> if (isId) "Pengaturan" else "Settings"
            "theme_title" -> if (isId) "Tampilan & Tema" else "Theme & Appearance"
            "theme_system" -> if (isId) "Ikuti Tema HP (Sistem)" else "System Default"
            "theme_light" -> if (isId) "Mode Terang (Light)" else "Light Mode"
            "theme_dark" -> if (isId) "Mode Gelap (Dark)" else "Dark Mode"
            "lang_title" -> if (isId) "Pilihan Bahasa" else "Language"
            "lang_id" -> if (isId) "Bahasa Indonesia (IN)" else "Indonesian (IN)"
            "lang_en" -> if (isId) "English (ENG)" else "English (ENG)"
            "about_app" -> if (isId) "Tentang Aplikasi" else "About Application"
            "privacy_safe" -> if (isId) "🔒 100% Data Tersimpan Lokal di HP Anda" else "🔒 100% Data Stored Locally on Your Device"

            // Status
            "monitoring_active" -> if (isId) "🟢 Monitoring Aktif" else "🟢 Monitoring Active"
            "monitoring_inactive" -> if (isId) "⚠️ Izin Notifikasi Dibutuhkan" else "⚠️ Notification Permission Needed"
            "btn_enable" -> if (isId) "Aktifkan" else "Enable"

            else -> key
        }
    }
}
