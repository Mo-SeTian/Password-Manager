package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryEntity

@Dao
interface DeletedEntryDao {
    @Query("SELECT * FROM deleted_entries")
    suspend fun getAll(): List<DeletedEntryEntity>

    @Query("SELECT * FROM deleted_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeletedEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DeletedEntryEntity)

    @Query("DELETE FROM deleted_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deleted_entries")
    suspend fun deleteAll()

    @Query("DELETE FROM deleted_entries WHERE deletedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
