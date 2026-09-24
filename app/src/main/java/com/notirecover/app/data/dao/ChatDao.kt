package com.notirecover.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.notirecover.app.data.model.ConversationEntity
import com.notirecover.app.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ==========================================
    // Percakapan (Conversations)
    // ==========================================

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE packageName = :packageName ORDER BY updatedAt DESC")
    fun getConversationsByPackage(packageName: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE packageName = :packageName AND chatTitle = :chatTitle LIMIT 1")
    suspend fun getConversation(packageName: String, chatTitle: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET deletedCount = deletedCount + 1, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun incrementDeletedCount(conversationId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET lastMessage = :lastMessage, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: Long, lastMessage: String, updatedAt: Long = System.currentTimeMillis())

    // ==========================================
    // Pesan & Media (Messages & Media)
    // ==========================================

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY receivedAt ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE mediaUri IS NOT NULL ORDER BY receivedAt DESC")
    fun getAllMediaMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
          AND isDeleted = 0 
        ORDER BY receivedAt DESC 
        LIMIT 1
    """)
    suspend fun getLastActiveMessage(conversationId: Long): MessageEntity?

    @Query("""
        UPDATE messages 
        SET isDeleted = 1, deletedAt = :deletedAt 
        WHERE id = :messageId
    """)
    suspend fun markMessageAsDeleted(messageId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationOnly(conversationId: Long)

    @Transaction
    suspend fun deleteConversation(conversationId: Long) {
        deleteMessagesForConversation(conversationId)
        deleteConversationOnly(conversationId)
    }

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Transaction
    suspend fun deleteAllData() {
        deleteAllMessages()
        deleteAllConversations()
    }

    @Transaction
    suspend fun handleDeletedMessageTrigger(conversationId: Long): MessageEntity? {
        val lastMessage = getLastActiveMessage(conversationId)
        if (lastMessage != null) {
            val now = System.currentTimeMillis()
            markMessageAsDeleted(lastMessage.id, now)
            incrementDeletedCount(conversationId, now)
            return lastMessage
        }
        return null
    }
}
