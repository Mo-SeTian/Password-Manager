package com.mosetian.passwordmanager.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: PasswordManagerDatabase? = null

    fun get(context: Context): PasswordManagerDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PasswordManagerDatabase::class.java,
                "password_manager.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
