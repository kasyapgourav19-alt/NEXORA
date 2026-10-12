package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDownloadDao {
    @Query("SELECT * FROM recent_downloads ORDER BY createdAtMillis DESC")
    fun observeAllDownloads(): Flow<List<RecentDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(entity: RecentDownloadEntity)

    @Query("DELETE FROM recent_downloads WHERE jobId = :jobId")
    suspend fun deleteByJobId(jobId: String)

    @Query("DELETE FROM recent_downloads")
    suspend fun clearAllHistory()
}
