package com.notirecover.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notirecover.app.data.model.DirectContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectContactDao {

    @Query("SELECT * FROM direct_contacts ORDER BY createdAt DESC")
    fun getAllDirectContacts(): Flow<List<DirectContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: DirectContactEntity): Long

    @Update
    suspend fun updateContact(contact: DirectContactEntity)

    @Query("DELETE FROM direct_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Delete
    suspend fun deleteContact(contact: DirectContactEntity)
}
