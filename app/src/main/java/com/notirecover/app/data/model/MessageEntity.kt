package com.notirecover.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["receivedAt"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderName: String,        // Nama pengirim spesifik (penting untuk chat grup)
    val messageText: String,       // Isi teks pesan asli
    val mediaUri: String? = null,  // Path foto/voice note jika ada
    val messageType: String = TYPE_TEXT,
    val isDeleted: Boolean = false, // True jika pesan ditarik/dihapus pengirim
    val receivedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    companion object {
        const val TYPE_TEXT = "TEXT"
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_VOICE_NOTE = "VOICE_NOTE"
        const val TYPE_DOCUMENT = "DOCUMENT"
    }
}
