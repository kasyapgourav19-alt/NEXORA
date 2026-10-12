package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_downloads")
data class RecentDownloadEntity(
    @PrimaryKey val jobId: String,
    val title: String,
    val sourceUrl: String,
    val providerName: String,
    val mediaType: String, // "VIDEO" or "AUDIO"
    val formatLabel: String, // e.g., "MP4 • 1080p Full HD" or "WAV • Lossless PCM"
    val containerFormat: String, // "MP4", "WEBM", "MP3", "M4A", "WAV"
    val resolutionOrBitrate: String,
    val fileSizeBytes: Long,
    val localFilePath: String?,
    val sha256Checksum: String?,
    val watermarkFreeVerified: Boolean,
    val status: String, // "COMPLETED", "CANCELLED", "FAILED", "EXPIRED"
    val createdAtMillis: Long,
    val expiresAtMillis: Long
) {
    fun isFileStillAvailable(): Boolean {
        if (status != "COMPLETED" || localFilePath.isNullOrBlank()) return false
        if (System.currentTimeMillis() > expiresAtMillis) return false
        val file = java.io.File(localFilePath)
        return file.exists() && file.length() > 0L
    }
}
