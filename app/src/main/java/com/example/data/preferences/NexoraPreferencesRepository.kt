package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexoraDataStore by preferencesDataStore(name = "nexora_settings")

data class NexoraSettings(
    val darkAmoledMode: Boolean = true,
    val reducedMotion: Boolean = false,
    val smartQualityEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val preferredVideoQuality: String = "1080p",
    val preferredAudioFormat: String = "MP3 320 kbps",
    val customBackendUrl: String = ""
)

class NexoraPreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_AMOLED = booleanPreferencesKey("dark_amoled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val SMART_QUALITY = booleanPreferencesKey("smart_quality")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val PREFERRED_VIDEO_QUALITY = stringPreferencesKey("preferred_video_quality")
        val PREFERRED_AUDIO_FORMAT = stringPreferencesKey("preferred_audio_format")
        val CUSTOM_BACKEND_URL = stringPreferencesKey("custom_backend_url")
    }

    val settingsFlow: Flow<NexoraSettings> = context.nexoraDataStore.data.map { prefs ->
        NexoraSettings(
            darkAmoledMode = prefs[Keys.DARK_AMOLED] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            smartQualityEnabled = prefs[Keys.SMART_QUALITY] ?: true,
            dataSaverEnabled = prefs[Keys.DATA_SAVER] ?: false,
            preferredVideoQuality = prefs[Keys.PREFERRED_VIDEO_QUALITY] ?: "1080p",
            preferredAudioFormat = prefs[Keys.PREFERRED_AUDIO_FORMAT] ?: "MP3 320 kbps",
            customBackendUrl = prefs[Keys.CUSTOM_BACKEND_URL] ?: ""
        )
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setSmartQuality(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.SMART_QUALITY] = enabled }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun setPreferredVideoQuality(quality: String) {
        context.nexoraDataStore.edit { it[Keys.PREFERRED_VIDEO_QUALITY] = quality }
    }

    suspend fun setPreferredAudioFormat(format: String) {
        context.nexoraDataStore.edit { it[Keys.PREFERRED_AUDIO_FORMAT] = format }
    }

    suspend fun setCustomBackendUrl(url: String) {
        context.nexoraDataStore.edit { it[Keys.CUSTOM_BACKEND_URL] = url.trim() }
    }
}
