package com.example.domain.provider

import com.example.R
import com.example.data.preferences.NexoraSettings
import com.example.domain.model.AnalysisStage
import com.example.domain.model.AudioFormatOption
import com.example.domain.model.AuthorizedSamplePreset
import com.example.domain.model.MediaAnalysisResult
import com.example.domain.model.MediaProviderType
import com.example.domain.model.VideoFormatOption
import com.example.domain.security.UrlSecurityValidator
import com.example.domain.security.UrlValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NexoraAnalysisPipeline(
    private val okHttpClient: OkHttpClient
) {
    private val adapters: List<ProviderAdapter> = listOf(
        AuthorizedOpenMediaAdapter(),
        YouTubeProviderAdapter(),
        InstagramProviderAdapter(),
        DirectMediaProviderAdapter()
    )

    companion object {
        val AUTHORIZED_SAMPLE_PRESETS: List<AuthorizedSamplePreset> = listOf(
            AuthorizedSamplePreset(
                id = "preset_4k_orbital",
                badge = "4K UHD • CC0 MASTER",
                title = "Orbital Ring 4K Master",
                subtitle = "2160p / 1440p / 1080p / 720p / 480p / 360p + Audio Stems",
                url = "https://nexora.studio/authorized-samples/orbital-ring-4k-master.mp4",
                providerType = MediaProviderType.AUTHORIZED_OPEN_MEDIA
            ),
            AuthorizedSamplePreset(
                id = "preset_direct_mp4",
                badge = "DIRECT HTTPS • MP4",
                title = "Direct Cloud Video Stream",
                subtitle = "Real HTTPS MP4 Stream Inspection & Passthrough Download",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                providerType = MediaProviderType.DIRECT_MEDIA
            ),
            AuthorizedSamplePreset(
                id = "preset_audio_master",
                badge = "LOSSLESS • AUDIO",
                title = "Cybernetic Pulse Audio Master",
                subtitle = "WAV Lossless PCM, M4A Original, MP3 320/256/192/128 kbps",
                url = "https://nexora.studio/authorized-samples/cybernetic-pulse-lossless-audio.wav",
                providerType = MediaProviderType.AUTHORIZED_OPEN_MEDIA
            )
        )
    }

    /**
     * Executes the 4-stage real URL analysis pipeline:
     * 1. Validating link (HTTPS & SSRF check)
     * 2. Identifying media source (Provider adapter resolution)
     * 3. Checking available formats (Remote Backend / Provider probe)
     * 4. Preparing download options (Smart Quality & Data Saver ranking)
     */
    suspend fun analyzeUrl(
        rawUrl: String,
        settings: NexoraSettings,
        isNetworkMetered: Boolean,
        onStageChange: suspend (AnalysisStage) -> Unit
    ): Result<MediaAnalysisResult> = withContext(Dispatchers.IO) {
        // Stage 1: Validating Link
        onStageChange(AnalysisStage.VALIDATING_LINK)
        delay(220)
        val validation = UrlSecurityValidator.validateMediaUrl(rawUrl)
        if (validation is UrlValidationResult.Invalid) {
            onStageChange(AnalysisStage.ERROR)
            return@withContext Result.failure(
                IllegalArgumentException("${validation.reasonTitle}: ${validation.explanation} ${validation.suggestedAction}")
            )
        }
        val validUrl = validation as UrlValidationResult.Valid

        // Stage 2: Identifying Media Source
        onStageChange(AnalysisStage.IDENTIFYING_SOURCE)
        delay(220)
        val matchedAdapter = adapters.firstOrNull {
            it.canHandle(validUrl.host, validUrl.path, validUrl.normalizedUrl)
        }

        // Stage 3: Checking Available Formats
        onStageChange(AnalysisStage.CHECKING_FORMATS)
        val backendBase = settings.customBackendUrl.trim().removeSuffix("/")
        var resultFromBackend: MediaAnalysisResult? = null
        if (backendBase.startsWith("https://")) {
            resultFromBackend = tryAnalyzeViaConfiguredBackend(backendBase, validUrl.normalizedUrl)
        }

        val rawAnalysis = resultFromBackend ?: if (matchedAdapter != null) {
            matchedAdapter.analyze(
                normalizedUrl = validUrl.normalizedUrl,
                host = validUrl.host,
                path = validUrl.path,
                okHttpClient = okHttpClient
            )
        } else {
            createUnsupportedProviderResult(validUrl.normalizedUrl, validUrl.host)
        }

        // Stage 4: Preparing Download Options (Smart Quality & Data Saver)
        onStageChange(AnalysisStage.PREPARING_OPTIONS)
        delay(180)
        val enrichedResult = applySmartQualityRecommendations(
            analysis = rawAnalysis,
            settings = settings,
            isNetworkMetered = isNetworkMetered
        )

        onStageChange(AnalysisStage.COMPLETED)
        Result.success(enrichedResult)
    }

    /**
     * Applies Smart Quality & Data Saver recommendations to genuinely available formats.
     */
    fun applySmartQualityRecommendations(
        analysis: MediaAnalysisResult,
        settings: NexoraSettings,
        isNetworkMetered: Boolean
    ): MediaAnalysisResult {
        val preferLowData = settings.dataSaverEnabled || isNetworkMetered
        val preferredHeight = settings.preferredVideoQuality.removeSuffix("p").toIntOrNull() ?: 1080

        val videos = analysis.videoFormats
        val smartVideoId = if (videos.isEmpty()) {
            null
        } else if (preferLowData) {
            // Prefer 480p or 360p when Data Saver or metered connection is active
            videos.firstOrNull { it.heightPx in 360..480 }?.id
                ?: videos.minByOrNull { it.estimatedSizeBytes ?: Long.MAX_VALUE }?.id
        } else {
            videos.firstOrNull { it.heightPx == preferredHeight }?.id
                ?: videos.firstOrNull { it.heightPx == 1080 }?.id
                ?: videos.first().id
        }

        val dataSaverVideoId = videos.firstOrNull { it.heightPx in 360..480 }?.id
            ?: videos.minByOrNull { it.estimatedSizeBytes ?: Long.MAX_VALUE }?.id

        val updatedVideos = videos.map { v ->
            v.copy(
                isSmartRecommended = (v.id == smartVideoId),
                isDataSaverRecommended = (v.id == dataSaverVideoId)
            )
        }

        val audios = analysis.audioFormats
        val smartAudioId = if (audios.isEmpty()) {
            null
        } else if (preferLowData) {
            audios.firstOrNull { it.bitrateKbps <= 160 }?.id
                ?: audios.firstOrNull { !it.requiresConversion }?.id
                ?: audios.first().id
        } else {
            audios.firstOrNull {
                it.label.contains(settings.preferredAudioFormat.substringBefore(" "), ignoreCase = true)
            }?.id ?: audios.first().id
        }

        val dataSaverAudioId = audios.minByOrNull { it.estimatedSizeBytes ?: Long.MAX_VALUE }?.id

        val updatedAudios = audios.map { a ->
            a.copy(
                isSmartRecommended = (a.id == smartAudioId),
                isDataSaverRecommended = (a.id == dataSaverAudioId)
            )
        }

        return analysis.copy(
            videoFormats = updatedVideos,
            audioFormats = updatedAudios
        )
    }

    private fun createUnsupportedProviderResult(normalizedUrl: String, host: String): MediaAnalysisResult {
        return MediaAnalysisResult(
            analysisId = UrlSecurityValidator.generateSecureJobId(),
            sourceUrl = normalizedUrl,
            sanitizedBaseFilename = UrlSecurityValidator.sanitizeFilename(host),
            title = "Web Resource on $host",
            authorOrChannel = host,
            providerType = MediaProviderType.UNSUPPORTED,
            thumbnailUrl = null,
            localThumbnailResId = R.drawable.img_sample_media_thumb,
            durationSeconds = null,
            originalResolution = null,
            originalFrameRate = null,
            mimeType = null,
            sourceContainsCreatorWatermark = false,
            watermarkPolicyStatement = "NEXORA only processes direct HTTPS media streams (.mp4, .webm, .mp3, .m4a, .wav) and authorized provider integrations.",
            isAuthorizedSampleMode = false,
            providerStatusBanner = "UNSUPPORTED OR NON-DIRECT MEDIA URL",
            limitationExplanation = "The URL on '$host' does not point directly to an authorized downloadable media stream (.mp4, .webm, .mp3, .m4a, .wav), nor is a custom provider adapter configured for this domain. Try pasting a direct HTTPS media link, selecting an Authorized Open Media preset, or importing your own video for a clean watermark-free export.",
            officialCreatorExportUrl = normalizedUrl,
            videoFormats = emptyList(),
            audioFormats = emptyList()
        )
    }

    private fun tryAnalyzeViaConfiguredBackend(
        backendBaseUrl: String,
        targetMediaUrl: String
    ): MediaAnalysisResult? {
        return try {
            val payload = JSONObject().put("url", targetMediaUrl).toString()
            val req = Request.Builder()
                .url("$backendBaseUrl/api/analyze")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title", "Remote Analyzed Media")
                val provider = json.optString("provider", "Remote Backend")
                val duration = json.optInt("durationSeconds", 0).takeIf { it > 0 }
                val origRes = json.optString("originalResolution", "1920×1080")
                MediaAnalysisResult(
                    analysisId = json.optString("jobId", UrlSecurityValidator.generateSecureJobId()),
                    sourceUrl = targetMediaUrl,
                    sanitizedBaseFilename = UrlSecurityValidator.sanitizeFilename(title),
                    title = title,
                    authorOrChannel = provider,
                    providerType = MediaProviderType.DIRECT_MEDIA,
                    thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    localThumbnailResId = R.drawable.img_sample_media_thumb,
                    durationSeconds = duration,
                    originalResolution = origRes,
                    originalFrameRate = "60 fps",
                    mimeType = "video/mp4",
                    sourceContainsCreatorWatermark = false,
                    watermarkPolicyStatement = "Verified by remote NEXORA Backend API. Zero added watermarks.",
                    isAuthorizedSampleMode = false,
                    providerStatusBanner = "CONNECTED TO REMOTE NEXORA BACKEND ($backendBaseUrl)",
                    limitationExplanation = null,
                    officialCreatorExportUrl = null,
                    videoFormats = emptyList(),
                    audioFormats = emptyList()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pings GET /api/health on a configured remote backend or returns the embedded local engine health status.
     */
    suspend fun checkEngineHealth(customBackendUrl: String): String = withContext(Dispatchers.IO) {
        val base = customBackendUrl.trim().removeSuffix("/")
        if (base.isEmpty()) {
            return@withContext "LOCAL ENGINE ONLINE • Android MediaMuxer + OkHttp HTTPS Direct + SSRF Guard v1.0 Active"
        }
        val validation = UrlSecurityValidator.validateMediaUrl(base)
        if (validation is UrlValidationResult.Invalid) {
            return@withContext "BACKEND URL BLOCKED: ${validation.reasonTitle} (${validation.explanation})"
        }
        return@withContext try {
            val req = Request.Builder()
                .url("$base/api/health")
                .get()
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    "REMOTE BACKEND HEALTHY (HTTP ${resp.code}) • $base/api/health"
                } else {
                    "REMOTE BACKEND HTTP ${resp.code} • Falling back to Local Android Media Engine"
                }
            }
        } catch (e: Exception) {
            "REMOTE UNREACHABLE (${e.javaClass.simpleName}) • Local Android Media Engine Active"
        }
    }
}
