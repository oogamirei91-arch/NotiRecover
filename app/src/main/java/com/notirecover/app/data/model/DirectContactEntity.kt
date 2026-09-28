package com.notirecover.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "direct_contacts")
data class DirectContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val countryCode: String = "62",
    val createdAt: Long = System.currentTimeMillis()
)
