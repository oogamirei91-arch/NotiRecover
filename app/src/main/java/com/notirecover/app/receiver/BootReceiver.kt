package com.notirecover.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.i("BootReceiver", "Perangkat baru saja dinyalakan atau aplikasi di-update.")
            // Android akan secara otomatis meregistrasikan NotificationListenerService jika izin sudah diberikan pengguna
        }
    }
}
