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

private val migration2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_entries (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                iconEmoji TEXT NOT NULL,
                groupKey TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                isWeak INTEGER NOT NULL,
                isRecent INTEGER NOT NULL,
                deletedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_entry_details (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                iconEmoji TEXT NOT NULL,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                website TEXT,
                note TEXT,
                customFieldsJson TEXT NOT NULL,
                deletedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val migration3To4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE custom_groups ADD COLUMN iconEmoji TEXT NOT NULL DEFAULT '📁'")
        database.execSQL("ALTER TABLE custom_groups ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
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
            ).addMigrations(migration1To2, migration2To3, migration3To4).build().also { instance = it }
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
