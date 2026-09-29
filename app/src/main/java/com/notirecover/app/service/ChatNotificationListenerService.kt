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

        /**
         * Membersihkan dan menggabungkan percakapan bertumpuk (seperti "suka cucur (2messages)")
         * ke percakapan utama ("suka cucur") agar riwayat chat tidak terpisah menjadi dua entitas.
         */
        suspend fun cleanupDuplicateStackedConversations(chatDao: com.notirecover.app.data.dao.ChatDao) {
            try {
                val conversations = chatDao.getAllConversationsList()
                for (conv in conversations) {
                    val cleanTitle = DeletedMessageClassifier.cleanChatTitle(conv.chatTitle)
                    if (conv.chatTitle != cleanTitle) {
                        val mainConv = conversations.find {
                            it.packageName == conv.packageName &&
                            it.id != conv.id &&
                            (it.chatTitle.trim().equals(cleanTitle, ignoreCase = true) ||
                             DeletedMessageClassifier.cleanChatTitle(it.chatTitle).equals(cleanTitle, ignoreCase = true))
                        }

                        if (mainConv != null && mainConv.id != conv.id) {
                            chatDao.mergeConversations(
                                sourceId = conv.id,
                                targetId = mainConv.id,
                                addDeleted = conv.deletedCount
                            )
                            Log.i(TAG, "Berhasil menggabungkan chat bertumpuk '${conv.chatTitle}' ke '${mainConv.chatTitle}'")
                        } else {
                            chatDao.updateChatTitle(conv.id, cleanTitle)
                            Log.i(TAG, "Membersihkan judul chat bertumpuk '${conv.chatTitle}' menjadi '$cleanTitle'")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membersihkan percakapan bertumpuk", e)
            }
        }
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

        // Abaikan notifikasi progress transfer file yang sedang berjalan (progress bar aktif)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        if (progressMax > 0 && progressCurrent < progressMax) {
            Log.d(TAG, "Mengabaikan notifikasi progress transfer file dari $packageName")
            return
        }

        // 2. Ekstrak data chat terstruktur (memisahkan nama grup & nama pengirim asli)
        val parsed = parseNotification(notification, extras) ?: return

        // 3. Abaikan notifikasi sistem internal & status transfer file
        if (DeletedMessageClassifier.isSystemNotification(parsed.chatTitle, parsed.messageText)) {
            Log.d(TAG, "Mengabaikan notifikasi sistem: ${parsed.chatTitle} - ${parsed.messageText}")
            return
        }

        // 5. Ekstrak foto dari notifikasi dengan anti-duplikasi MD5
        val savedMediaPath = MediaHelper.saveNotificationMedia(applicationContext, notification)

        // 6. Abaikan placeholder "sent a photo" / summary jika tidak ada foto baru
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
        val rawConvTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val conversationTitle = if (!rawConvTitle.isNullOrBlank()) DeletedMessageClassifier.cleanChatTitle(rawConvTitle) else null
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
                val rawGroupFromTitle = if (rawTitle.contains(" @ ")) {
                    rawTitle.substringAfter(" @ ").trim()
                } else {
                    rawTitle
                }

                chatTitle = conversationTitle
                    ?: messagingStyle.conversationTitle?.toString()?.trim()?.let { DeletedMessageClassifier.cleanChatTitle(it) }
                    ?: DeletedMessageClassifier.cleanChatTitle(rawGroupFromTitle)

                // senderName adalah orang yang mengirim pesan di dalam grup
                senderName = latestMsg?.person?.name?.toString()?.trim()
                    ?: latestMsg?.sender?.toString()?.trim()
                    ?: if (rawTitle.contains(" @ ")) rawTitle.substringBefore(" @ ").trim() else ""

                messageText = latestMsg?.text?.toString()?.trim()
            } else {
                // Chat pribadi (1-on-1)
                val raw1on1 = rawTitle.ifBlank { latestMsg?.person?.name?.toString()?.trim().orEmpty() }
                chatTitle = DeletedMessageClassifier.cleanChatTitle(raw1on1)
                senderName = chatTitle
                messageText = latestMsg?.text?.toString()?.trim()
            }
        }

        // Fallback jika bukan MessagingStyle atau kosong
        if (chatTitle.isBlank()) {
            if (!conversationTitle.isNullOrBlank()) {
                isGroup = true
                chatTitle = DeletedMessageClassifier.cleanChatTitle(conversationTitle)
                senderName = if (rawTitle.isNotBlank() && rawTitle != conversationTitle) {
                    DeletedMessageClassifier.cleanChatTitle(rawTitle)
                } else {
                    chatTitle
                }
            } else if (rawTitle.contains(" @ ")) {
                isGroup = true
                senderName = rawTitle.substringBefore(" @ ").trim()
                chatTitle = DeletedMessageClassifier.cleanChatTitle(rawTitle.substringAfter(" @ ").trim())
            } else if (rawTitle.contains(":") && isGroupExtra) {
                isGroup = true
                chatTitle = DeletedMessageClassifier.cleanChatTitle(rawTitle.substringBefore(":").trim())
                senderName = rawTitle.substringAfter(":").trim()
            } else {
                chatTitle = DeletedMessageClassifier.cleanChatTitle(rawTitle)
                senderName = chatTitle
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

        chatTitle = DeletedMessageClassifier.cleanChatTitle(chatTitle)
        if (chatTitle.isBlank()) return null
        if (senderName.isBlank() || senderName == chatTitle) {
            senderName = chatTitle
        } else {
            senderName = DeletedMessageClassifier.cleanChatTitle(senderName)
        }

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
        val cleanTitle = DeletedMessageClassifier.cleanChatTitle(chatTitle)

        // 1. Cari atau buat percakapan (Grup memiliki 1 entitas chat tersendiri di database)
        var conversation = chatDao.getConversation(packageName, cleanTitle)
        if (conversation == null) {
            // Cek apakah ada percakapan lama yang namanya belum dibersihkan (cth: "suka cucur (2messages)")
            val allConvs = chatDao.getAllConversationsList()
            val existingDirty = allConvs.find {
                it.packageName == packageName &&
                DeletedMessageClassifier.cleanChatTitle(it.chatTitle).equals(cleanTitle, ignoreCase = true)
            }
            if (existingDirty != null) {
                chatDao.updateChatTitle(existingDirty.id, cleanTitle)
                conversation = existingDirty.copy(chatTitle = cleanTitle)
            } else {
                val newConversation = ConversationEntity(
                    packageName = packageName,
                    chatTitle = cleanTitle,
                    lastMessage = if (isGroup && senderName != cleanTitle) "$senderName: $messageText" else messageText,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = chatDao.insertConversation(newConversation)
                conversation = newConversation.copy(id = newId)
            }
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
            // 3. Pencegahan Duplikasi Media & Pesan Berulang (Link Preview TikTok/YouTube, Multi-Event WhatsApp, dsb)
            var finalMediaPath = savedMediaPath

            val cleanText = if (finalMediaPath != null && (messageText.isBlank() || messageText.lowercase().contains("sent a photo") || messageText.lowercase().contains("mengirim foto"))) {
                "📷 [Foto]"
            } else {
                messageText
            }

            // Cek apakah pesan dengan teks & pengirim yang sama persis sudah masuk dalam 6 detik terakhir (menghindari duplikasi multi-event link preview WhatsApp)
            if (cleanText.isNotBlank()) {
                val recentMatch = chatDao.getRecentMatchingMessage(
                    conversationId = conversationId,
                    senderName = senderName,
                    messageText = cleanText,
                    sinceTime = System.currentTimeMillis() - 6000L
                )

                if (recentMatch != null) {
                    // Kasus Link Preview (misal link TikTok / YouTube):
                    // Pesan teks pertama masuk tanpa gambar preview, lalu beberapa detik kemudian WhatsApp
                    // mengupdate notifikasi dengan membawa gambar thumbnail link preview.
                    if (finalMediaPath != null && recentMatch.mediaUri == null) {
                        chatDao.updateMessageMedia(recentMatch.id, finalMediaPath, MessageEntity.TYPE_IMAGE)
                        Log.i(TAG, "Menyematkan media thumbnail link preview ke pesan sebelumnya (skip duplikasi baris)")
                    } else {
                        Log.d(TAG, "Mengabaikan update notifikasi pesan duplikat identik dalam 6 detik: '$cleanText'")
                    }
                    return
                }
            }

            if (finalMediaPath != null) {
                // Cek apakah foto dengan path/hash ini SUDAH PERNAH dicatat dalam percakapan ini
                val alreadyHasMedia = chatDao.countMediaInConversation(conversationId, finalMediaPath) > 0
                if (alreadyHasMedia) {
                    if (cleanText.isBlank() || cleanText == "📷 [Foto]" ||
                        DeletedMessageClassifier.isPlaceholderOrSummary(chatTitle, cleanText, true)
                    ) {
                        Log.d(TAG, "Mengabaikan media duplikat yang sudah tersimpan: $finalMediaPath")
                        return
                    } else {
                        // Notifikasi membawa pesan teks baru, tapi extras masih menyertakan foto lama
                        finalMediaPath = null
                    }
                }
            }

            val messageType = if (finalMediaPath != null) MessageEntity.TYPE_IMAGE else MessageEntity.TYPE_TEXT

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
