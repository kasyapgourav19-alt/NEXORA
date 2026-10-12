package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentDownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun recentDownloadDao(): RecentDownloadDao

    companion object {
        @Volatile
        private var INSTANCE: NexoraDatabase? = null

        fun getInstance(context: Context): NexoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexoraDatabase::class.java,
                    "nexora_workstation.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
