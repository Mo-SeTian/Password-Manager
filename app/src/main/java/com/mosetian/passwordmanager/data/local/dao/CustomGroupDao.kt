package com.mosetian.passwordmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity

@Dao
interface CustomGroupDao {
    @Query("SELECT * FROM custom_groups")
    suspend fun getAll(): List<CustomGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: CustomGroupEntity)
}
