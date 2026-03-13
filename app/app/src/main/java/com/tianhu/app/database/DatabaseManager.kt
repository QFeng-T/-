package com.tianhu.app.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseManager {
    private var database: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE recognition_record ADD COLUMN freshness_score REAL")
            database.execSQL("ALTER TABLE recognition_record ADD COLUMN is_fresh INTEGER")
        }
    }

    fun getInstance(context: Context): AppDatabase {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "fresh_id_db"
                    ).addMigrations(MIGRATION_1_2)
                    .build()
                }
            }
        }
        return database!!
    }

    fun clearInstance() {
        database = null
    }
}