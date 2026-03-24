package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity

@Dao
interface CustomGroupDao {
    @Query("SELECT * FROM custom_groups ORDER BY sortOrder ASC")
    suspend fun getAll(): List<CustomGroupEntity>

    @Query("SELECT * FROM custom_groups WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): CustomGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: CustomGroupEntity)

    @Query("UPDATE custom_groups SET name = :name, iconEmoji = :iconEmoji, sortOrder = :sortOrder WHERE `key` = :key")
    suspend fun update(key: String, name: String, iconEmoji: String, sortOrder: Int)

    @Query("DELETE FROM custom_groups WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
