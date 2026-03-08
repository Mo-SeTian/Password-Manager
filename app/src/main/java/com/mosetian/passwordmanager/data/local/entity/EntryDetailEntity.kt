package com.mosetian.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entry_details")
data class EntryDetailEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val username: String,
    val password: String,
    val website: String?,
    val note: String?,
    val customFieldsJson: String = "[]"
)
