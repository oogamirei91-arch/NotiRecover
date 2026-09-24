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
        val extras = notification.extras ?: return

        val title = extractTitle(extras) ?: return
        val messageText = extractMessageText(extras) ?: "📷 [Foto/Media]"

        if (DeletedMessageClassifier.isSystemNotification(title, messageText)) {
            return
        }

        // Coba ekstrak foto dari notifikasi (jika pengirim mengirim gambar)
        val savedMediaPath = MediaHelper.saveNotificationMedia(applicationContext, notification)

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
            }
        } else {
            // 3. Simpan sebagai pesan/media baru
            val messageType = if (savedMediaPath != null) MessageEntity.TYPE_IMAGE else MessageEntity.TYPE_TEXT

            val newMessage = MessageEntity(
                conversationId = conversationId,
                senderName = chatTitle,
                messageText = messageText,
                mediaUri = savedMediaPath,
                messageType = messageType,
                isDeleted = false,
                receivedAt = System.currentTimeMillis()
            )
            chatDao.insertMessage(newMessage)
            chatDao.updateLastMessage(conversationId, messageText)
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
