package com.notirecover.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notirecover.app.MainActivity
import com.notirecover.app.data.preference.AppPreferences

object SmartAlertHelper {

    private const val CHANNEL_ID = "chatrestore_smart_alerts"
    private const val CHANNEL_NAME = "Smart Keyword Alerts"

    fun checkAndTriggerAlert(
        context: Context,
        senderTitle: String,
        messageText: String,
        isDeleted: Boolean = false
    ) {
        val prefs = AppPreferences(context)
        if (!prefs.isSmartAlertEnabled) return

        val rawKeywords = prefs.smartAlertKeywords
        val keywords = rawKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val lowerMsg = messageText.lowercase()
        val matchedKeyword = keywords.firstOrNull { kw -> lowerMsg.contains(kw) }

        if (matchedKeyword != null) {
            sendAlertNotification(
                context = context,
                senderTitle = senderTitle,
                messageText = messageText,
                keyword = matchedKeyword,
                isDeleted = isDeleted
            )
        }
    }

    private fun sendAlertNotification(
        context: Context,
        senderTitle: String,
        messageText: String,
        keyword: String,
        isDeleted: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi penting saat ada pesan/kata kunci sensitif yang terdeteksi"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isDeleted) {
            "🚨 Peringatan: Pesan Terhapus Berisi '$keyword'!"
        } else {
            "🔔 Peringatan Kata Kunci: '$keyword'"
        }

        val content = "Dari $senderTitle: $messageText"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
