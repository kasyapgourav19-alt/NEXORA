package com.example.domain.model

enum class MediaProviderType(val displayName: String, val badgeCode: String) {
    YOUTUBE("YouTube", "YT-API"),
    INSTAGRAM("Instagram", "IG-META"),
    DIRECT_MEDIA("Direct HTTPS Stream", "HTTPS-DIRECT"),
    AUTHORIZED_OPEN_MEDIA("NEXORA Open Media Studio", "OPEN-MASTER"),
    LOCAL_USER_UPLOAD("User-Owned Device Media", "LOCAL-MASTER"),
    UNSUPPORTED("External Web Source", "EXT-WEB")
}

enum class AnalysisStage(val stepIndex: Int, val label: String, val detail: String) {
    IDLE(0, "Ready", "Awaiting authorized HTTPS link or local media"),
    VALIDATING_LINK(1, "Validating link", "Enforcing HTTPS transport & SSRF security policy..."),
    IDENTIFYING_SOURCE(2, "Identifying media source", "Matching modular provider adapter & permissions..."),
    CHECKING_FORMATS(3, "Checking available formats", "Inspecting genuine video & audio stream manifests..."),
    PREPARING_OPTIONS(4, "Preparing download options", "Calculating bitrates, sizes & Smart Quality rank..."),
    COMPLETED(5, "Analysis complete", "Genuine stream options verified"),
    ERROR(-1, "Analysis halted", "Unable to extract downloadable streams")
}

enum class MediaTab {
    VIDEO,
    AUDIO
}

data class VideoFormatOption(
    val id: String,
    val qualityLabel: String, // e.g., "Original Quality", "2160p — 4K", "1080p — Full HD"
    val resolution: String,   // e.g., "3840×2160", "1920×1080"
    val heightPx: Int,
    val containerFormat: String, // "MP4", "WebM"
    val codecInfo: String,
    val estimatedSizeBytes: Long?,
    val hasAudioTrack: Boolean,
    val requiresConversionOrMuxing: Boolean,
    val processingPipelineNote: String,
    val isSmartRecommended: Boolean = false,
    val isDataSaverRecommended: Boolean = false,
    val sourceStreamUrl: String
)

data class AudioFormatOption(
    val id: String,
    val label: String, // e.g., "Original Audio", "MP3 — 320 kbps", "M4A — Original", "WAV"
    val containerFormat: String, // "MP3", "M4A", "WAV"
    val bitrateKbps: Int,
    val bitrateDisplay: String,
    val estimatedSizeBytes: Long?,
    val requiresConversion: Boolean,
    val conversionStatusNote: String,
    val isSmartRecommended: Boolean = false,
    val isDataSaverRecommended: Boolean = false,
    val sourceStreamUrl: String
)

data class MediaAnalysisResult(
    val analysisId: String,
    val sourceUrl: String,
    val sanitizedBaseFilename: String,
    val title: String,
    val authorOrChannel: String,
    val providerType: MediaProviderType,
    val thumbnailUrl: String?,
    val localThumbnailResId: Int?,
    val durationSeconds: Int?,
    val originalResolution: String?,
    val originalFrameRate: String?,
    val mimeType: String?,
    val sourceContainsCreatorWatermark: Boolean,
    val watermarkPolicyStatement: String,
    val isAuthorizedSampleMode: Boolean,
    val providerStatusBanner: String,
    val limitationExplanation: String?,
    val officialCreatorExportUrl: String?,
    val videoFormats: List<VideoFormatOption>,
    val audioFormats: List<AudioFormatOption>
) {
    val hasDownloadableFormats: Boolean
        get() = videoFormats.isNotEmpty() || audioFormats.isNotEmpty()
}

enum class JobStatus(val displayLabel: String) {
    QUEUED("Queued"),
    DOWNLOADING("Fetching Stream"),
    REMUXING_WATERMARK_FREE("Clean Remux / Audio Synthesis (Zero Watermark)"),
    VERIFYING_INTEGRITY("Verifying File & SHA-256"),
    READY("Download Ready"),
    CANCELLED("Cancelled & Cleaned Up"),
    FAILED("Failed"),
    EXPIRED("Link / File Expired")
}

data class ActiveDownloadJob(
    val jobId: String,
    val mediaTitle: String,
    val sourceUrl: String,
    val providerName: String,
    val mediaTab: MediaTab,
    val selectedFormatLabel: String,
    val containerFormat: String,
    val resolutionOrBitrate: String,
    val requiresConversion: Boolean,
    val status: JobStatus,
    val progressFraction: Float,
    val bytesProcessed: Long,
    val totalBytes: Long?,
    val estimatedSecondsRemaining: Int?,
    val outputFilePath: String? = null,
    val outputFileName: String? = null,
    val sha256Checksum: String? = null,
    val errorMessage: String? = null,
    val isRecoverable: Boolean = true
)

data class AuthorizedSamplePreset(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val providerType: MediaProviderType
)
