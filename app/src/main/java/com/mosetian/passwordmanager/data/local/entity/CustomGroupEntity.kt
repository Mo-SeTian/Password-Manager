package com.mosetian.passwordmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_groups")
data class CustomGroupEntity(
    @PrimaryKey val key: String,
    val name: String,
    val iconEmoji: String = "📁",
    val sortOrder: Int = 0
)
