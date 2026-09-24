package com.notirecover.app.security

import android.content.Context

object NativeSecurity {
    init {
        System.loadLibrary("security-core")
    }

    /**
     * Memeriksa integritas APK dan memastikan tidak sedang di-hook oleh debugger / Frida.
     */
    external fun checkIntegrity(context: Context): Boolean

    /**
     * Mengambil Secure Salt dari binary C++ untuk enkripsi / hashing lokal.
     */
    external fun getSecureSalt(): String
}
