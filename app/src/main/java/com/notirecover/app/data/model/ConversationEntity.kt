package com.notirecover.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["packageName", "chatTitle"], unique = true)
    ]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,       // com.whatsapp, com.instagram.android, dll.
    val chatTitle: String,         // Nama kontak atau grup
    val lastMessage: String,       // Cuplikan pesan terakhir
    val deletedCount: Int = 0,     // Total pesan yang terhapus di chat ini
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
