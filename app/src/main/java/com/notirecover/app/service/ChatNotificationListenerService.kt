package com.notirecover.app.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notirecover.app.data.AppDatabase
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.model.MessageEntity
import com.notirecover.app.util.DeletedMessageClassifier
import com.notirecover.app.util.MediaHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    companion object {
        private const val TAG = "NotiRecoverService"

        val SUPPORTED_PACKAGES = setOf(
            "com.whatsapp",                  // WhatsApp
            "com.whatsapp.w4b",              // WhatsApp Business
            "com.instagram.android",          // Instagram
            "org.telegram.messenger",        // Telegram Official
            "org.telegram.messenger.web",    // Telegram Web/Plus
            "org.thunderdog.challegram",      // Telegram X
            "com.facebook.orca",             // Messenger
            "jp.naver.line.android"          // LINE
        )
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        Log.i(TAG, "NotificationListenerService Berhasil Dimulai")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!SUPPORTED_PACKAGES.contains(packageName)) return

        val notification = sbn.notification ?: return

        // 1. Abaikan notifikasi Ringkasan Grup Android (misal: "3 new messages from 2 chats")
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(TAG, "Mengabaikan notifikasi Group Summary dari $packageName")
            return
        }

        val extras = notification.extras ?: return

        val title = extractTitle(extras) ?: return
        val rawMessageText = extractMessageText(extras)

        // 2. Abaikan notifikasi sistem internal
        if (DeletedMessageClassifier.isSystemNotification(title, rawMessageText)) {
            Log.d(TAG, "Mengabaikan notifikasi sistem: $title - $rawMessageText")
            return
        }

        // Coba ekstrak foto dari notifikasi (jika pengirim mengirim gambar)
        val savedMediaPath = MediaHelper.saveNotificationMedia(applicationContext, notification)

        // 3. Abaikan placeholder "sent a photo" / "3 new messages" jika tidak ada teks asli atau media yang tersimpan
        if (DeletedMessageClassifier.isPlaceholderOrSummary(title, rawMessageText, savedMediaPath != null)) {
            Log.d(TAG, "Mengabaikan teks placeholder / counter: '$rawMessageText' dari '$title'")
            return
        }

        val messageText = rawMessageText ?: if (savedMediaPath != null) "📷 [Foto]" else return

        serviceScope.launch {
            processIncomingNotification(packageName, title, messageText, savedMediaPath)
        }
    }

    private suspend fun processIncomingNotification(
        packageName: String,
        chatTitle: String,
        messageText: String,
        savedMediaPath: String?
    ) {
        val chatDao = database.chatDao()

        // 1. Cari atau buat percakapan
        var conversation = chatDao.getConversation(packageName, chatTitle)
        if (conversation == null) {
            val newConversation = ConversationEntity(
                packageName = packageName,
                chatTitle = chatTitle,
                lastMessage = messageText,
                updatedAt = System.currentTimeMillis()
            )
            val newId = chatDao.insertConversation(newConversation)
            conversation = newConversation.copy(id = newId)
        }

        val conversationId = conversation.id

        // 2. Evaluasi pemicu penghapusan pesan
        if (DeletedMessageClassifier.isDeletedMessageTrigger(messageText)) {
            Log.d(TAG, "Pemicu pesan terhapus terdeteksi untuk chat: $chatTitle")
            val recoveredMessage = chatDao.handleDeletedMessageTrigger(conversationId)
            if (recoveredMessage != null) {
                Log.i(TAG, "BERHASIL MEMULIHKAN PESAN / MEDIA: ${recoveredMessage.messageText}")
                // Cek apakah pesan terhapus ini mengandung kata kunci sensitif
                com.notirecover.app.util.SmartAlertHelper.checkAndTriggerAlert(
                    context = applicationContext,
                    senderTitle = chatTitle,
                    messageText = recoveredMessage.messageText,
                    isDeleted = true
                )
            }
        } else {
            // 3. Simpan sebagai pesan/media baru
            val messageType = if (savedMediaPath != null) MessageEntity.TYPE_IMAGE else MessageEntity.TYPE_TEXT

            val cleanText = if (savedMediaPath != null && (messageText.isBlank() || messageText.lowercase().contains("sent a photo") || messageText.lowercase().contains("mengirim foto"))) {
                "📷 [Foto]"
            } else {
                messageText
            }

            val newMessage = MessageEntity(
                conversationId = conversationId,
                senderName = chatTitle,
                messageText = cleanText,
                mediaUri = savedMediaPath,
                messageType = messageType,
                isDeleted = false,
                receivedAt = System.currentTimeMillis()
            )
            chatDao.insertMessage(newMessage)
            chatDao.updateLastMessage(conversationId, cleanText)

            // Cek peringatan kata kunci penting jika ada pesan baru
            com.notirecover.app.util.SmartAlertHelper.checkAndTriggerAlert(
                context = applicationContext,
                senderTitle = chatTitle,
                messageText = cleanText,
                isDeleted = false
            )
        }
    }

    private fun extractTitle(extras: Bundle): String? {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (!title.isNullOrBlank()) return title

        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        if (!conversationTitle.isNullOrBlank()) return conversationTitle

        return null
    }

    private fun extractMessageText(extras: Bundle): String? {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) return bigText

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text

        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NotificationListenerService Dimatikan")
    }
}
