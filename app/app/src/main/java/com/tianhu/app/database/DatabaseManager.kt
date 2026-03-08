package com.tianhu.app.database

import android.content.Context
import androidx.room.Room

object DatabaseManager {
    private var database: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "fresh_id_db"
                    ).build()
                }
            }
        }
        return database!!
    }

    fun clearInstance() {
        database = null
    }
}