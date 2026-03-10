package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity

@Dao
interface EntryDetailDao {
    @Query("SELECT * FROM entry_details")
    suspend fun getAll(): List<EntryDetailEntity>

    @Query("SELECT * FROM entry_details WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EntryDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(detail: EntryDetailEntity)
}
