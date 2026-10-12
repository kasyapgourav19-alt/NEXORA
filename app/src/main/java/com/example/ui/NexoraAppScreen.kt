package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NexoraArchitectureBottomSheet
import com.example.ui.components.NexoraCreatorFooter
import com.example.ui.components.NexoraDownloadCenterSection
import com.example.ui.components.NexoraHeroWorkspaceSection
import com.example.ui.components.NexoraMediaPlayerDialog
import com.example.ui.components.NexoraRecentDownloadsSection
import com.example.ui.components.NexoraSettingsBottomSheet
import com.example.ui.components.NexoraTopHeader
import com.example.ui.theme.NexoraAmoledBlack
import com.example.ui.theme.NexoraElectricCyan
import com.example.ui.theme.NexoraPrimaryPanel
import com.example.ui.theme.NexoraSoftWhite

@Composable
fun NexoraAppScreen(
    viewModel: NexoraViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val recentDownloads by viewModel.recentDownloads.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Zero-Permission Android Photo Picker for User-Owned Video Clean Remux
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onAnalyzeLocalUserVideo(uri)
        }
    }

    Scaffold(
        containerColor = NexoraAmoledBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NexoraAmoledBlack)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("nexora_main_scroll"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Compact Responsive Futuristic Header
                item {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        NexoraTopHeader(
                            reducedMotion = settings.reducedMotion,
                            onOpenArchitectureSheet = { viewModel.setArchitectureSheetOpen(true) },
                            onOpenSettingsSheet = { viewModel.setSettingsSheetOpen(true) }
                        )
                    }
                }

                // 2. Hero & Central Glassmorphic URL Input Panel + Presets
                item {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        NexoraHeroWorkspaceSection(
                            urlInput = uiState.urlInput,
                            isAnalyzing = uiState.isAnalyzing,
                            analysisStage = uiState.analysisStage,
                            validationErrorTitle = uiState.validationErrorTitle,
                            validationErrorExplanation = uiState.validationErrorExplanation,
                            validationErrorSuggestion = uiState.validationErrorSuggestion,
                            reducedMotion = settings.reducedMotion,
                            onUrlChanged = viewModel::onUrlInputChanged,
                            onPasteClicked = {
                                val clipText = clipboardManager.getText()?.text
                                viewModel.onPasteClipboardText(clipText)
                            },
                            onClearClicked = viewModel::onClearUrl,
                            onAnalyzeClicked = {
                                focusManager.clearFocus()
                                viewModel.analyzeCurrentUrl()
                            },
                            onSelectPreset = { preset ->
                                focusManager.clearFocus()
                                viewModel.onSelectPreset(preset)
                            },
                            onPickLocalVideoClicked = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        )
                    }
                }

                // 3. Video & Audio Download Center (Rendered after analysis)
                uiState.analysisResult?.let { analysis ->
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            NexoraDownloadCenterSection(
                                analysis = analysis,
                                selectedTab = uiState.selectedTab,
                                selectedVideoOption = uiState.selectedVideoOption,
                                selectedAudioOption = uiState.selectedAudioOption,
                                settings = settings,
                                activeJob = uiState.activeJob,
                                onSelectTab = viewModel::onSelectTab,
                                onSelectVideoFormat = viewModel::onSelectVideoFormat,
                                onSelectAudioFormat = viewModel::onSelectAudioFormat,
                                onToggleSmartQuality = viewModel::onToggleSmartQuality,
                                onToggleDataSaver = viewModel::onToggleDataSaver,
                                onStartDownload = viewModel::onStartDownload,
                                onCancelDownload = viewModel::onCancelActiveDownload,
                                onRetryDownload = viewModel::onRetryDownload,
                                onOpenDownloadedFile = { path, fmt ->
                                    viewModel.openOrShareExportedFile(context, path, fmt)
                                }
                            )
                        }
                    }
                }

                // 4. Recent Activity Section
                item {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        NexoraRecentDownloadsSection(
                            recentDownloads = recentDownloads,
                            onOpenDownloadedFile = { path, fmt ->
                                viewModel.openOrShareExportedFile(context, path, fmt)
                            },
                            onDeleteSingleItem = viewModel::onDeleteSingleHistoryItem,
                            onClearHistory = viewModel::onClearHistory
                        )
                    }
                }

                // 5. Creator Branding Footer ("Designed & Built by Gaurav" + @avkaxofficial & @gxr.avka)
                item {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        NexoraCreatorFooter(
                            onHandleCopied = viewModel::showToast
                        )
                    }
                }
            }

            // Subtle Floating Toast Notification Banner
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Surface(
                    color = NexoraPrimaryPanel.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, NexoraElectricCyan),
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("nexora_toast_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = NexoraElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.toastMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoraSoftWhite
                        )
                    }
                }
            }
        }
    }

    // Settings Modal Sheet
    if (uiState.isSettingsOpen) {
        NexoraSettingsBottomSheet(
            settings = settings,
            onDismiss = { viewModel.setSettingsSheetOpen(false) },
            onToggleReducedMotion = viewModel::onToggleReducedMotion,
            onToggleSmartQuality = viewModel::onToggleSmartQuality,
            onToggleDataSaver = viewModel::onToggleDataSaver,
            onSelectVideoQuality = viewModel::onSetPreferredVideoQuality,
            onSelectAudioFormat = viewModel::onSetPreferredAudioFormat,
            onClearHistory = viewModel::onClearHistory
        )
    }

    // Backend & REST API Architecture Sheet
    if (uiState.isArchitectureSheetOpen) {
        NexoraArchitectureBottomSheet(
            customBackendUrl = settings.customBackendUrl,
            engineHealthSummary = uiState.engineHealthSummary,
            onUpdateBackendUrl = viewModel::onSetCustomBackendUrl,
            onRunHealthCheck = viewModel::runHealthCheck,
            onDismiss = { viewModel.setArchitectureSheetOpen(false) }
        )
    }

    // Built-In Watermark-Free Media Player & File Inspector Dialog
    uiState.inspectedMediaFile?.let { inspected ->
        NexoraMediaPlayerDialog(
            mediaFile = inspected,
            onShareExternal = {
                viewModel.shareFileExternally(context, inspected.filePath, inspected.containerFormat)
            },
            onDismiss = viewModel::onDismissMediaInspector
        )
    }
}
