package com.mosetian.passwordmanager.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val migration1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE entry_details ADD COLUMN customFieldsJson TEXT NOT NULL DEFAULT '[]'")
    }
}

object DatabaseProvider {
    private const val DATABASE_NAME = "password_manager.db"

    @Volatile
    private var instance: PasswordManagerDatabase? = null

    fun get(context: Context): PasswordManagerDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PasswordManagerDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(migration1To2).build().also { instance = it }
        }
    }

    fun reset(context: Context) {
        synchronized(this) {
            instance?.close()
            instance = null
            context.applicationContext.deleteDatabase(DATABASE_NAME)
        }
    }
}
