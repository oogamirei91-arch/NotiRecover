package com.notirecover.app.util

object DeletedMessageClassifier {

    // Daftar kata kunci penghapusan pesan lintas bahasa dan platform
    private val DELETED_PATTERNS = listOf(
        // Bahasa Indonesia
        "pesan ini telah dihapus",
        "pesan ini telah ditarik",
        "pesan telah dihapus",
        "anda telah menghapus pesan ini",

        // English
        "this message was deleted",
        "this message has been deleted",
        "message was deleted",
        "you deleted this message",

        // Spanish / Portuguese
        "este mensaje fue eliminado",
        "esta mensagem foi apagada",
        "mensaje eliminado",

        // French / German
        "ce message a été supprimé",
        "diese nachricht wurde gelöscht",

        // Arabic
        "تم حذف هذه الرسالة"
    )

    /**
     * Memeriksa apakah teks notifikasi merupakan indikasi pesan yang ditarik/dihapus pengirim.
     */
    fun isDeletedMessageTrigger(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val cleanText = text.trim().lowercase()
        return DELETED_PATTERNS.any { pattern -> cleanText.contains(pattern) }
    }

    /**
     * Memeriksa apakah notifikasi adalah notifikasi sistem internal medsos yang perlu diabaikan
     * (misal: "Memeriksa pesan baru...", "WhatsApp Web aktif", backup status).
     */
    fun isSystemNotification(title: String?, text: String?): Boolean {
        val cleanTitle = title.orEmpty().trim().lowercase()
        val cleanText = text.orEmpty().trim().lowercase()
        val combined = "$cleanTitle $cleanText"

        val ignoreList = listOf(
            "checking for new messages",
            "memeriksa pesan baru",
            "whatsapp web is currently active",
            "whatsapp web sedang aktif",
            "backup in progress",
            "cadangan sedang berjalan",
            "backup finished",
            "cadangan selesai",
            "tap to chat",
            "ketuk untuk mengobrol",
            "searching for incoming messages",
            "mencari pesan masuk"
        )
        return ignoreList.any { combined.contains(it) }
    }

    /**
     * Memeriksa apakah pesan hanya teks placeholder/summary (seperti "sent a photo", "3 new messages", "pesan baru")
     * yang tidak membawa isi teks asli dan tidak boleh dicatat sebagai pesan biasa.
     */
    fun isPlaceholderOrSummary(title: String?, text: String?, hasSavedMedia: Boolean): Boolean {
        if (text.isNullOrBlank()) return true

        // Jika ini adalah trigger pesan yang dihapus pengirim, JANGAN diabaikan!
        if (isDeletedMessageTrigger(text)) return false

        val cleanText = text.trim().lowercase()
        val cleanTitle = title.orEmpty().trim().lowercase()

        // 1. Abaikan notifikasi ringkasan / counter pesan (misal: "3 new messages", "2 pesan baru")
        val summaryRegexes = listOf(
            Regex("""^\d+\s+(new messages?|pesan baru|unread messages?|pesan belum dibaca|messages?|pesan)$""", RegexOption.IGNORE_CASE),
            Regex("""^(new messages?|pesan baru|unread messages?|pesan belum dibaca)$""", RegexOption.IGNORE_CASE),
            Regex("""^\d+\s+(pesan dari|messages from)\s+\d+\s+(obrolan|chats?)$""", RegexOption.IGNORE_CASE)
        )
        if (summaryRegexes.any { it.matches(cleanText) }) {
            return true
        }

        // 2. Notifikasi placeholder media tanpa teks asli (misal: "sent a photo", "mengirim foto")
        val mediaPlaceholderPatterns = listOf(
            "sent a photo",
            "mengirim foto",
            "mengirimkan foto",
            "sent a video",
            "mengirim video",
            "mengirimkan video",
            "sent a voice message",
            "mengirim pesan suara",
            "sent a voice note",
            "mengirim vn",
            "sent an audio",
            "mengirim audio",
            "sent a sticker",
            "mengirim stiker",
            "sent a gif",
            "mengirim gif",
            "sent a reel",
            "mengirim reel",
            "sent a story",
            "mengirim cerita",
            "sent an attachment",
            "mengirim lampiran",
            "sent a file",
            "mengirim berkas",
            "mengirim file",
            "sent a document",
            "mengirim dokumen",
            "sent a contact",
            "mengirim kontak",
            "sent a location",
            "mengirim lokasi"
        )

        val isMediaPlaceholder = mediaPlaceholderPatterns.any { cleanText == it || cleanText.startsWith(it) }

        // Jika murni placeholder dan TIDAK ADA file media fisik yang berhasil diekstrak, jangan catat ke database
        if (isMediaPlaceholder && !hasSavedMedia) {
            return true
        }

        // 3. Jika judulnya hanya nama aplikasi (WhatsApp / Instagram) dan pesannya ringkasan
        if ((cleanTitle == "whatsapp" || cleanTitle == "instagram" || cleanTitle == "telegram" || cleanTitle == "messenger") &&
            (cleanText.contains("pesan baru") || cleanText.contains("new message") || cleanText.contains("new messages"))
        ) {
            return true
        }

        return false
    }
}
