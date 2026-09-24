package com.notirecover.app

import android.app.Application
import com.notirecover.app.data.AppDatabase
import com.notirecover.app.security.NativeSecurity

class NotiRecoverApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Verifikasi Integritas & Anti-Tamper Native saat aplikasi pertama kali jalan
        val isSecure = NativeSecurity.checkIntegrity(this)
        if (!isSecure) {
            // Jika terdeteksi debugger / Frida / hooking modifikasi, tutup aplikasi secara diam-diam
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
