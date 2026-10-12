package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NexoraDatabase
import com.example.data.local.RecentDownloadEntity
import com.example.data.preferences.NexoraPreferencesRepository
import com.example.data.preferences.NexoraSettings
import com.example.domain.media.WatermarkFreeMediaEngine
import com.example.domain.model.ActiveDownloadJob
import com.example.domain.model.AnalysisStage
import com.example.domain.model.AudioFormatOption
import com.example.domain.model.AuthorizedSamplePreset
import com.example.domain.model.JobStatus
import com.example.domain.model.MediaAnalysisResult
import com.example.domain.model.MediaTab
import com.example.domain.model.VideoFormatOption
import com.example.domain.provider.NexoraAnalysisPipeline
import com.example.domain.security.UrlSecurityValidator
import com.example.domain.security.UrlValidationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

data class NexoraUiState(
    val urlInput: String = "",
    val analysisStage: AnalysisStage = AnalysisStage.IDLE,
    val validationErrorTitle: String? = null,
    val validationErrorExplanation: String? = null,
    val validationErrorSuggestion: String? = null,
    val analysisResult: MediaAnalysisResult? = null,
    val selectedTab: MediaTab = MediaTab.VIDEO,
    val selectedVideoFormatId: String? = null,
    val selectedAudioFormatId: String? = null,
    val activeJob: ActiveDownloadJob? = null,
    val toastMessage: String? = null,
    val isSettingsOpen: Boolean = false,
    val isArchitectureSheetOpen: Boolean = false,
    val engineHealthSummary: String = "LOCAL ENGINE ONLINE • Watermark-Free Pipeline Ready"
) {
    val isAnalyzing: Boolean
        get() = analysisStage in listOf(
            AnalysisStage.VALIDATING_LINK,
            AnalysisStage.IDENTIFYING_SOURCE,
            AnalysisStage.CHECKING_FORMATS,
            AnalysisStage.PREPARING_OPTIONS
        )

    val selectedVideoOption: VideoFormatOption?
        get() = analysisResult?.videoFormats?.firstOrNull { it.id == selectedVideoFormatId }
            ?: analysisResult?.videoFormats?.firstOrNull { it.isSmartRecommended }
            ?: analysisResult?.videoFormats?.firstOrNull()

    val selectedAudioOption: AudioFormatOption?
        get() = analysisResult?.audioFormats?.firstOrNull { it.id == selectedAudioFormatId }
            ?: analysisResult?.audioFormats?.firstOrNull { it.isSmartRecommended }
            ?: analysisResult?.audioFormats?.firstOrNull()
}

class NexoraViewModel(application: Application) : AndroidViewModel(application) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val database = NexoraDatabase.getInstance(application)
    private val recentDownloadDao = database.recentDownloadDao()
    private val preferencesRepository = NexoraPreferencesRepository(application)
    private val analysisPipeline = NexoraAnalysisPipeline(okHttpClient)
    private val mediaEngine = WatermarkFreeMediaEngine(application, okHttpClient)

    private val _uiState = MutableStateFlow(NexoraUiState())
    val uiState: StateFlow<NexoraUiState> = _uiState.asStateFlow()

    val settingsState: StateFlow<NexoraSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NexoraSettings()
        )

    val recentDownloads: StateFlow<List<RecentDownloadEntity>> = recentDownloadDao.observeAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var activeDownloadCoroutineJob: Job? = null
    private var toastDismissJob: Job? = null

    fun onUrlInputChanged(newUrl: String) {
        _uiState.update {
            it.copy(
                urlInput = newUrl,
                validationErrorTitle = null,
                validationErrorExplanation = null,
                validationErrorSuggestion = null
            )
        }
    }

    fun onPasteClipboardText(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            showToast("Clipboard is empty — paste or type a link manually")
            return
        }
        val cleaned = clipboardText.trim()
        _uiState.update {
            it.copy(
                urlInput = cleaned,
                validationErrorTitle = null,
                validationErrorExplanation = null,
                validationErrorSuggestion = null
            )
        }
        showToast("URL pasted from clipboard")
    }

    fun onClearUrl() {
        _uiState.update {
            it.copy(
                urlInput = "",
                analysisStage = AnalysisStage.IDLE,
                validationErrorTitle = null,
                validationErrorExplanation = null,
                validationErrorSuggestion = null,
                analysisResult = null
            )
        }
    }

    fun onSelectPreset(preset: AuthorizedSamplePreset) {
        _uiState.update {
            it.copy(
                urlInput = preset.url,
                validationErrorTitle = null,
                validationErrorExplanation = null,
                validationErrorSuggestion = null
            )
        }
        analyzeCurrentUrl(preset.url)
    }

    fun analyzeCurrentUrl(explicitUrl: String? = null) {
        val targetUrl = (explicitUrl ?: _uiState.value.urlInput).trim()
        val validation = UrlSecurityValidator.validateMediaUrl(targetUrl)
        if (validation is UrlValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    analysisStage = AnalysisStage.ERROR,
                    validationErrorTitle = validation.reasonTitle,
                    validationErrorExplanation = validation.explanation,
                    validationErrorSuggestion = validation.suggestedAction,
                    analysisResult = null
                )
            }
            showToast(validation.reasonTitle)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    validationErrorTitle = null,
                    validationErrorExplanation = null,
                    validationErrorSuggestion = null
                )
            }
            val currentSettings = settingsState.value
            val isMetered = isConnectionMetered()

            val result = analysisPipeline.analyzeUrl(
                rawUrl = targetUrl,
                settings = currentSettings,
                isNetworkMetered = isMetered,
                onStageChange = { stage ->
                    _uiState.update { state -> state.copy(analysisStage = stage) }
                }
            )

            result.fold(
                onSuccess = { analysis ->
                    val defaultTab = if (analysis.videoFormats.isNotEmpty()) {
                        MediaTab.VIDEO
                    } else if (analysis.audioFormats.isNotEmpty()) {
                        MediaTab.AUDIO
                    } else {
                        MediaTab.VIDEO
                    }
                    val defaultVideoId = analysis.videoFormats.firstOrNull { it.isSmartRecommended }?.id
                        ?: analysis.videoFormats.firstOrNull()?.id
                    val defaultAudioId = analysis.audioFormats.firstOrNull { it.isSmartRecommended }?.id
                        ?: analysis.audioFormats.firstOrNull()?.id

                    _uiState.update {
                        it.copy(
                            analysisStage = AnalysisStage.COMPLETED,
                            analysisResult = analysis,
                            selectedTab = defaultTab,
                            selectedVideoFormatId = defaultVideoId,
                            selectedAudioFormatId = defaultAudioId
                        )
                    }
                    if (analysis.hasDownloadableFormats) {
                        showToast("Analysis completed • ${analysis.videoFormats.size} video & ${analysis.audioFormats.size} audio formats")
                    } else {
                        showToast("Provider limitation detected — see official export route")
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            analysisStage = AnalysisStage.ERROR,
                            validationErrorTitle = "Analysis Failed",
                            validationErrorExplanation = err.message ?: "Unable to inspect media link.",
                            validationErrorSuggestion = "Verify the HTTPS URL or select an Authorized Open Media sample."
                        )
                    }
                    showToast("Network or validation problem during analysis")
                }
            )
        }
    }

    fun onAnalyzeLocalUserVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    urlInput = uri.toString(),
                    analysisStage = AnalysisStage.CHECKING_FORMATS,
                    validationErrorTitle = null,
                    validationErrorExplanation = null,
                    validationErrorSuggestion = null
                )
            }
            val result = mediaEngine.analyzeLocalUserVideo(uri)
            result.fold(
                onSuccess = { analysis ->
                    val enriched = analysisPipeline.applySmartQualityRecommendations(
                        analysis = analysis,
                        settings = settingsState.value,
                        isNetworkMetered = isConnectionMetered()
                    )
                    _uiState.update {
                        it.copy(
                            analysisStage = AnalysisStage.COMPLETED,
                            analysisResult = enriched,
                            selectedTab = MediaTab.VIDEO,
                            selectedVideoFormatId = enriched.videoFormats.firstOrNull()?.id,
                            selectedAudioFormatId = enriched.audioFormats.firstOrNull()?.id
                        )
                    }
                    showToast("User-owned video loaded for clean watermark-free export")
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            analysisStage = AnalysisStage.ERROR,
                            validationErrorTitle = "Local Media Inspection Error",
                            validationErrorExplanation = err.message ?: "Could not read local video metadata.",
                            validationErrorSuggestion = "Choose a valid MP4, WebM, or MOV video file from your device."
                        )
                    }
                    showToast("Unsupported local media format")
                }
            )
        }
    }

    fun onSelectTab(tab: MediaTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSelectVideoFormat(formatId: String) {
        _uiState.update { it.copy(selectedVideoFormatId = formatId) }
    }

    fun onSelectAudioFormat(formatId: String) {
        _uiState.update { it.copy(selectedAudioFormatId = formatId) }
    }

    fun onStartDownload() {
        val state = _uiState.value
        val analysis = state.analysisResult ?: return
        val tab = state.selectedTab
        val videoOpt = state.selectedVideoOption
        val audioOpt = state.selectedAudioOption

        if (tab == MediaTab.VIDEO && videoOpt == null) {
            showToast("Unsupported format — no video stream available")
            return
        }
        if (tab == MediaTab.AUDIO && audioOpt == null) {
            showToast("Unsupported format — no audio stream available")
            return
        }

        activeDownloadCoroutineJob?.cancel()

        val jobId = UrlSecurityValidator.generateSecureJobId()
        val formatLabel = if (tab == MediaTab.VIDEO) {
            "${videoOpt!!.containerFormat} • ${videoOpt.qualityLabel}"
        } else {
            audioOpt!!.label
        }
        val containerFormat = if (tab == MediaTab.VIDEO) {
            videoOpt!!.containerFormat
        } else {
            audioOpt!!.containerFormat
        }
        val resOrBitrate = if (tab == MediaTab.VIDEO) {
            videoOpt!!.resolution
        } else {
            audioOpt!!.bitrateDisplay
        }
        val requiresConversion = if (tab == MediaTab.VIDEO) {
            videoOpt!!.requiresConversionOrMuxing
        } else {
            audioOpt!!.requiresConversion
        }
        val totalEstBytes = if (tab == MediaTab.VIDEO) {
            videoOpt!!.estimatedSizeBytes
        } else {
            audioOpt!!.estimatedSizeBytes
        }

        val initialJob = ActiveDownloadJob(
            jobId = jobId,
            mediaTitle = analysis.title,
            sourceUrl = analysis.sourceUrl,
            providerName = analysis.providerType.displayName,
            mediaTab = tab,
            selectedFormatLabel = formatLabel,
            containerFormat = containerFormat,
            resolutionOrBitrate = resOrBitrate,
            requiresConversion = requiresConversion,
            status = JobStatus.QUEUED,
            progressFraction = 0.02f,
            bytesProcessed = 0L,
            totalBytes = totalEstBytes,
            estimatedSecondsRemaining = 3
        )

        _uiState.update { it.copy(activeJob = initialJob) }

        activeDownloadCoroutineJob = viewModelScope.launch {
            try {
                val exportResult = mediaEngine.executeWatermarkFreeDownload(
                    jobId = jobId,
                    analysis = analysis,
                    mediaTab = tab,
                    selectedVideo = videoOpt,
                    selectedAudio = audioOpt,
                    onProgress = { fraction, bytesDone, totalBytes, _ ->
                        val status = when {
                            fraction < 0.70f -> JobStatus.DOWNLOADING
                            fraction < 0.94f -> JobStatus.REMUXING_WATERMARK_FREE
                            else -> JobStatus.VERIFYING_INTEGRITY
                        }
                        val remainingSec = ((1.0f - fraction) * 4f).toInt().coerceAtLeast(1)
                        _uiState.update { current ->
                            current.copy(
                                activeJob = current.activeJob?.copy(
                                    status = status,
                                    progressFraction = fraction,
                                    bytesProcessed = bytesDone,
                                    totalBytes = totalBytes ?: totalEstBytes,
                                    estimatedSecondsRemaining = remainingSec
                                )
                            )
                        }
                    }
                )

                exportResult.fold(
                    onSuccess = { exported ->
                        val completedJob = initialJob.copy(
                            status = JobStatus.READY,
                            progressFraction = 1.0f,
                            bytesProcessed = exported.sizeBytes,
                            totalBytes = exported.sizeBytes,
                            estimatedSecondsRemaining = 0,
                            outputFilePath = exported.file.absolutePath,
                            outputFileName = exported.fileName,
                            sha256Checksum = exported.sha256Hex
                        )
                        _uiState.update { it.copy(activeJob = completedJob) }

                        val now = System.currentTimeMillis()
                        recentDownloadDao.upsertDownload(
                            RecentDownloadEntity(
                                jobId = jobId,
                                title = analysis.title,
                                sourceUrl = analysis.sourceUrl,
                                providerName = analysis.providerType.displayName,
                                mediaType = tab.name,
                                formatLabel = formatLabel,
                                containerFormat = containerFormat,
                                resolutionOrBitrate = resOrBitrate,
                                fileSizeBytes = exported.sizeBytes,
                                localFilePath = exported.file.absolutePath,
                                sha256Checksum = exported.sha256Hex,
                                watermarkFreeVerified = true,
                                status = "COMPLETED",
                                createdAtMillis = now,
                                expiresAtMillis = now + 24L * 60L * 60L * 1000L // 24-hour signed local availability window
                            )
                        )
                        showToast("Download ready • Watermark-free ${exported.fileName}")
                    },
                    onFailure = { err ->
                        val failedJob = initialJob.copy(
                            status = JobStatus.FAILED,
                            errorMessage = err.message ?: "Stream interrupted during media processing",
                            isRecoverable = true
                        )
                        _uiState.update { it.copy(activeJob = failedJob) }
                        showToast("Download failed — tap Retry to resume")
                    }
                )
            } catch (_: CancellationException) {
                val cancelledJob = initialJob.copy(
                    status = JobStatus.CANCELLED,
                    progressFraction = 0f,
                    estimatedSecondsRemaining = null,
                    errorMessage = "Job cancelled by user and temporary files purged."
                )
                _uiState.update { it.copy(activeJob = cancelledJob) }
                showToast("Download cancelled & temporary files cleaned up")
            }
        }
    }

    fun onCancelActiveDownload() {
        val current = _uiState.value.activeJob ?: return
        activeDownloadCoroutineJob?.cancel()
        activeDownloadCoroutineJob = null
        // Clean up any partial file
        current.outputFilePath?.let { path ->
            val f = File(path)
            if (f.exists()) f.delete()
        }
        _uiState.update {
            it.copy(
                activeJob = current.copy(
                    status = JobStatus.CANCELLED,
                    progressFraction = 0f,
                    estimatedSecondsRemaining = null,
                    errorMessage = "Processing job cancelled and temporary buffers cleaned."
                )
            )
        }
        showToast("Job cancelled & temporary files cleaned")
    }

    fun onRetryDownload() {
        onStartDownload()
    }

    fun onDismissActiveJobCard() {
        _uiState.update { it.copy(activeJob = null) }
    }

    fun openOrShareExportedFile(context: Context, filePath: String, containerFormat: String) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            showToast("File has expired or been removed from temporary storage")
            return
        }
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = mediaEngine.resolveMimeType(
                containerFormat,
                if (containerFormat.uppercase() in listOf("MP3", "M4A", "WAV")) MediaTab.AUDIO else MediaTab.VIDEO
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TITLE, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Open or Save Watermark-Free Media (${file.name})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Saved in Downloads/NEXORA/${file.name}")
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            val existing = recentDownloads.value
            existing.forEach { item ->
                item.localFilePath?.let { path ->
                    val f = File(path)
                    if (f.exists()) f.delete()
                }
            }
            recentDownloadDao.clearAllHistory()
            showToast("Recent download history & cached files cleared")
        }
    }

    fun onDeleteSingleHistoryItem(jobId: String, filePath: String?) {
        viewModelScope.launch {
            filePath?.let {
                val f = File(it)
                if (f.exists()) f.delete()
            }
            recentDownloadDao.deleteByJobId(jobId)
            showToast("Removed from recent history")
        }
    }

    // Settings Controls
    fun setSettingsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun setArchitectureSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isArchitectureSheetOpen = open) }
        if (open) {
            runHealthCheck()
        }
    }

    fun runHealthCheck() {
        viewModelScope.launch {
            val status = analysisPipeline.checkEngineHealth(settingsState.value.customBackendUrl)
            _uiState.update { it.copy(engineHealthSummary = status) }
        }
    }

    fun onToggleReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReducedMotion(enabled)
            showToast(if (enabled) "Reduced motion enabled" else "Standard animations active")
        }
    }

    fun onToggleSmartQuality(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmartQuality(enabled)
            reapplyRecommendations()
            showToast(if (enabled) "Smart Quality enabled" else "Smart Quality disabled")
        }
    }

    fun onToggleDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDataSaver(enabled)
            reapplyRecommendations()
            showToast(if (enabled) "Data Saver active — preferring compact streams" else "Data Saver disabled")
        }
    }

    fun onSetPreferredVideoQuality(quality: String) {
        viewModelScope.launch {
            preferencesRepository.setPreferredVideoQuality(quality)
            reapplyRecommendations()
        }
    }

    fun onSetPreferredAudioFormat(format: String) {
        viewModelScope.launch {
            preferencesRepository.setPreferredAudioFormat(format)
            reapplyRecommendations()
        }
    }

    fun onSetCustomBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomBackendUrl(url)
            runHealthCheck()
            showToast("Backend endpoint updated")
        }
    }

    private suspend fun reapplyRecommendations() {
        delay(60)
        val currentAnalysis = _uiState.value.analysisResult ?: return
        val updated = analysisPipeline.applySmartQualityRecommendations(
            analysis = currentAnalysis,
            settings = settingsState.value,
            isNetworkMetered = isConnectionMetered()
        )
        val smartVideo = updated.videoFormats.firstOrNull { it.isSmartRecommended }?.id
            ?: _uiState.value.selectedVideoFormatId
        val smartAudio = updated.audioFormats.firstOrNull { it.isSmartRecommended }?.id
            ?: _uiState.value.selectedAudioFormatId
        _uiState.update {
            it.copy(
                analysisResult = updated,
                selectedVideoFormatId = smartVideo,
                selectedAudioFormatId = smartAudio
            )
        }
    }

    fun showToast(message: String) {
        toastDismissJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastDismissJob = viewModelScope.launch {
            delay(3200)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private fun isConnectionMetered(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.isActiveNetworkMetered ?: false
        } catch (_: Exception) {
            false
        }
    }
}
