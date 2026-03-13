package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryDetailEntity

@Dao
interface DeletedEntryDetailDao {
    @Query("SELECT * FROM deleted_entry_details")
    suspend fun getAll(): List<DeletedEntryDetailEntity>

    @Query("SELECT * FROM deleted_entry_details WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeletedEntryDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(detail: DeletedEntryDetailEntity)

    @Query("DELETE FROM deleted_entry_details WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deleted_entry_details")
    suspend fun deleteAll()

    @Query("DELETE FROM deleted_entry_details WHERE deletedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
