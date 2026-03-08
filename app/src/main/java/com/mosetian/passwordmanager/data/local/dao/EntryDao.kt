package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.EntryEntity

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries")
    suspend fun getAll(): List<EntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntryEntity)
}
