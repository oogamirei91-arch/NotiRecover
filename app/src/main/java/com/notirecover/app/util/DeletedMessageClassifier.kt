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
        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()
        val ignoreList = listOf(
            "checking for new messages",
            "memeriksa pesan baru",
            "whatsapp web is currently active",
            "whatsapp web sedang aktif",
            "backup in progress",
            "cadangan sedang berjalan"
        )
        return ignoreList.any { combined.contains(it) }
    }
}
