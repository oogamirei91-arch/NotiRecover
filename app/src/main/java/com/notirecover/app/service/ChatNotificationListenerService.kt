package com.notirecover.app.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.notirecover.app.data.AppDatabase
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.model.MessageEntity
import com.notirecover.app.util.DeletedMessageClassifier
import com.notirecover.app.util.MediaHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class ParsedChatNotification(
    val chatTitle: String,     // Nama grup (jika grup) atau nama kontak (jika chat pribadi)
    val senderName: String,    // Nama pengirim spesifik (misal anggota dalam grup: "Budi")
    val messageText: String,   // Pesan teks bersih
    val isGroup: Boolean       // Menandakan apakah percakapan grup
)

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

        // 2. Ekstrak data chat terstruktur (memisahkan nama grup & nama pengirim asli)
        val parsed = parseNotification(notification, extras) ?: return

        // 3. Abaikan notifikasi sistem internal
        if (DeletedMessageClassifier.isSystemNotification(parsed.chatTitle, parsed.messageText)) {
            Log.d(TAG, "Mengabaikan notifikasi sistem: ${parsed.chatTitle} - ${parsed.messageText}")
            return
        }

        // 4. Ekstrak foto dari notifikasi dengan anti-duplikasi MD5
        val savedMediaPath = MediaHelper.saveNotificationMedia(applicationContext, notification)

        // 5. Abaikan placeholder "sent a photo" / summary jika tidak ada foto baru
        if (DeletedMessageClassifier.isPlaceholderOrSummary(parsed.chatTitle, parsed.messageText, savedMediaPath != null)) {
            if (savedMediaPath == null) {
                Log.d(TAG, "Mengabaikan teks placeholder / counter: '${parsed.messageText}' dari '${parsed.chatTitle}'")
                return
            }
        }

        val rawText = parsed.messageText
        val messageText = if (rawText.isNotBlank()) rawText else if (savedMediaPath != null) "📷 [Foto]" else return

        serviceScope.launch {
            processIncomingNotification(
                packageName = packageName,
                chatTitle = parsed.chatTitle,
                senderName = parsed.senderName,
                isGroup = parsed.isGroup,
                messageText = messageText,
                savedMediaPath = savedMediaPath
            )
        }
    }

    /**
     * Membedah notifikasi WhatsApp/Telegram untuk memisahkan Nama Grup dan Nama Anggota Pengirim.
     */
    private fun parseNotification(notification: Notification, extras: Bundle): ParsedChatNotification? {
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val isGroupExtra = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)

        var chatTitle = ""
        var senderName = ""
        var messageText: String? = null
        var isGroup = false

        if (messagingStyle != null) {
            isGroup = messagingStyle.isGroupConversation || !conversationTitle.isNullOrBlank() || isGroupExtra
            val latestMsg = messagingStyle.messages.lastOrNull()

            if (isGroup) {
                // Dalam chat grup: conversationTitle adalah NAMA GRUP
                chatTitle = conversationTitle
                    ?: messagingStyle.conversationTitle?.toString()?.trim()
                    ?: if (rawTitle.contains(" @ ")) rawTitle.substringAfter(" @ ").trim() else rawTitle

                // senderName adalah orang yang mengirim pesan di dalam grup
                senderName = latestMsg?.person?.name?.toString()?.trim()
                    ?: latestMsg?.senderPerson?.name?.toString()?.trim()
                    ?: if (rawTitle.contains(" @ ")) rawTitle.substringBefore(" @ ").trim() else rawTitle

                messageText = latestMsg?.text?.toString()?.trim()
            } else {
                // Chat pribadi (1-on-1)
                chatTitle = rawTitle.ifBlank { latestMsg?.person?.name?.toString()?.trim().orEmpty() }
                senderName = chatTitle
                messageText = latestMsg?.text?.toString()?.trim()
            }
        }

        // Fallback jika bukan MessagingStyle atau kosong
        if (chatTitle.isBlank()) {
            if (!conversationTitle.isNullOrBlank()) {
                isGroup = true
                chatTitle = conversationTitle
                senderName = rawTitle.ifBlank { conversationTitle }
            } else if (rawTitle.contains(" @ ")) {
                isGroup = true
                senderName = rawTitle.substringBefore(" @ ").trim()
                chatTitle = rawTitle.substringAfter(" @ ").trim()
            } else if (rawTitle.contains(":") && isGroupExtra) {
                isGroup = true
                chatTitle = rawTitle.substringBefore(":").trim()
                senderName = rawTitle.substringAfter(":").trim()
            } else {
                chatTitle = rawTitle
                senderName = rawTitle
            }
        }

        if (messageText == null) {
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            messageText = bigText ?: text
        }

        // Jika notifikasi grup memiliki format teks "Budi: Halo semuanya"
        if (messageText != null && messageText.contains(": ")) {
            val prefix = messageText.substringBefore(": ").trim()
            val body = messageText.substringAfter(": ").trim()
            if (prefix.length in 1..40 && !prefix.contains("\n")) {
                if (senderName.isBlank() || senderName == chatTitle) {
                    senderName = prefix
                    isGroup = true
                }
                messageText = body
            }
        }

        if (chatTitle.isBlank()) return null
        if (senderName.isBlank()) senderName = chatTitle

        return ParsedChatNotification(
            chatTitle = chatTitle,
            senderName = senderName,
            messageText = messageText.orEmpty(),
            isGroup = isGroup
        )
    }

    private suspend fun processIncomingNotification(
        packageName: String,
        chatTitle: String,
        senderName: String,
        isGroup: Boolean,
        messageText: String,
        savedMediaPath: String?
    ) {
        val chatDao = database.chatDao()

        // 1. Cari atau buat percakapan (Grup memiliki 1 entitas chat tersendiri di database)
        var conversation = chatDao.getConversation(packageName, chatTitle)
        if (conversation == null) {
            val newConversation = ConversationEntity(
                packageName = packageName,
                chatTitle = chatTitle,
                lastMessage = if (isGroup && senderName != chatTitle) "$senderName: $messageText" else messageText,
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
                    senderTitle = if (isGroup) "$chatTitle (${recoveredMessage.senderName})" else chatTitle,
                    messageText = recoveredMessage.messageText,
                    isDeleted = true
                )
            }
        } else {
            // 3. Pencegahan Duplikasi Media & Pesan Berulang
            var finalMediaPath = savedMediaPath

            if (finalMediaPath != null) {
                // Cek apakah foto dengan path/hash ini SUDAH PERNAH dicatat dalam percakapan ini
                val alreadyHasMedia = chatDao.countMediaInConversation(conversationId, finalMediaPath) > 0
                if (alreadyHasMedia) {
                    if (messageText.isBlank() || messageText == "📷 [Foto]" ||
                        DeletedMessageClassifier.isPlaceholderOrSummary(chatTitle, messageText, true)
                    ) {
                        Log.d(TAG, "Mengabaikan media duplikat yang sudah tersimpan: $finalMediaPath")
                        return
                    } else {
                        // Notifikasi membawa pesan teks baru, tapi extras masih menyertakan foto lama
                        finalMediaPath = null
                    }
                }
            }

            // Cek duplikasi teks identik dalam rentang 3 detik terakhir untuk menghindari multi-event update WhatsApp
            if (finalMediaPath == null && messageText.isNotBlank()) {
                val recentDuplicate = chatDao.countRecentDuplicateText(
                    conversationId = conversationId,
                    messageText = messageText,
                    sinceTime = System.currentTimeMillis() - 3000L
                ) > 0
                if (recentDuplicate) {
                    Log.d(TAG, "Mengabaikan notifikasi teks duplikat dalam 3 detik: '$messageText'")
                    return
                }
            }

            val messageType = if (finalMediaPath != null) MessageEntity.TYPE_IMAGE else MessageEntity.TYPE_TEXT

            val cleanText = if (finalMediaPath != null && (messageText.isBlank() || messageText.lowercase().contains("sent a photo") || messageText.lowercase().contains("mengirim foto"))) {
                "📷 [Foto]"
            } else {
                messageText
            }

            val newMessage = MessageEntity(
                conversationId = conversationId,
                senderName = senderName, // Nama pengirim spesifik di dalam grup!
                messageText = cleanText,
                mediaUri = finalMediaPath,
                messageType = messageType,
                isDeleted = false,
                receivedAt = System.currentTimeMillis()
            )
            chatDao.insertMessage(newMessage)

            val displayLastMessage = if (isGroup && senderName != chatTitle) "$senderName: $cleanText" else cleanText
            chatDao.updateLastMessage(conversationId, displayLastMessage)

            // Cek peringatan kata kunci penting jika ada pesan baru
            com.notirecover.app.util.SmartAlertHelper.checkAndTriggerAlert(
                context = applicationContext,
                senderTitle = if (isGroup) "$chatTitle ($senderName)" else chatTitle,
                messageText = cleanText,
                isDeleted = false
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NotificationListenerService Dimatikan")
    }
}
