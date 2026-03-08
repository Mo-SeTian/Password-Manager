package com.mosetian.passwordmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mosetian.passwordmanager.data.local.dao.CustomGroupDao
import com.mosetian.passwordmanager.data.local.dao.EntryDao
import com.mosetian.passwordmanager.data.local.dao.EntryDetailDao
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.EntryEntity

@Database(
    entities = [EntryEntity::class, EntryDetailEntity::class, CustomGroupEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PasswordManagerDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun entryDetailDao(): EntryDetailDao
    abstract fun customGroupDao(): CustomGroupDao
}
