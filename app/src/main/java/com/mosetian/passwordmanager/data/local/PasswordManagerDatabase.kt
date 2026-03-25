package com.mosetian.passwordmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mosetian.passwordmanager.data.local.dao.CustomGroupDao
import com.mosetian.passwordmanager.data.local.dao.DeletedEntryDao
import com.mosetian.passwordmanager.data.local.dao.DeletedEntryDetailDao
import com.mosetian.passwordmanager.data.local.dao.EntryDao
import com.mosetian.passwordmanager.data.local.dao.EntryDetailDao
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryEntity
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.EntryEntity

@Database(
    entities = [
        EntryEntity::class,
        EntryDetailEntity::class,
        CustomGroupEntity::class,
        DeletedEntryEntity::class,
        DeletedEntryDetailEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PasswordManagerDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun entryDetailDao(): EntryDetailDao
    abstract fun customGroupDao(): CustomGroupDao
    abstract fun deletedEntryDao(): DeletedEntryDao
    abstract fun deletedEntryDetailDao(): DeletedEntryDetailDao
}
