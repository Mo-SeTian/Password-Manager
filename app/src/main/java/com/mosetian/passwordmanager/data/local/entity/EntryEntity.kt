package com.mosetian.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val groupKey: String,
    val isFavorite: Boolean,
    val isWeak: Boolean,
    val isRecent: Boolean
)
