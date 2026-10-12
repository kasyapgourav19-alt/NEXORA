package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.preferences.NexoraSettings
import com.example.domain.model.ActiveDownloadJob
import com.example.domain.model.AudioFormatOption
import com.example.domain.model.JobStatus
import com.example.domain.model.MediaAnalysisResult
import com.example.domain.model.MediaTab
import com.example.domain.model.VideoFormatOption
import com.example.ui.theme.NexoraAmberWarning
import com.example.ui.theme.NexoraAmoledBlack
import com.example.ui.theme.NexoraCoralError
import com.example.ui.theme.NexoraElectricCyan
import com.example.ui.theme.NexoraEmeraldReady
import com.example.ui.theme.NexoraIceBlue
import com.example.ui.theme.NexoraMutedBlueGray
import com.example.ui.theme.NexoraPrimaryPanel
import com.example.ui.theme.NexoraSecondaryPanel
import com.example.ui.theme.NexoraSoftWhite
import com.example.ui.theme.NexoraSubtleDivider
import com.example.ui.theme.NexoraVioletAccent
import com.example.ui.theme.OrbitronFontFamily
import java.util.Locale

@Composable
fun NexoraDownloadCenterSection(
    analysis: MediaAnalysisResult,
    selectedTab: MediaTab,
    selectedVideoOption: VideoFormatOption?,
    selectedAudioOption: AudioFormatOption?,
    settings: NexoraSettings,
    activeJob: ActiveDownloadJob?,
    onSelectTab: (MediaTab) -> Unit,
    onSelectVideoFormat: (String) -> Unit,
    onSelectAudioFormat: (String) -> Unit,
    onToggleSmartQuality: (Boolean) -> Unit,
    onToggleDataSaver: (Boolean) -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onOpenDownloadedFile: (filePath: String, containerFormat: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_center_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Media Preview & Metadata Card
        MediaMetadataPreviewCard(analysis = analysis)

        // 2. If Provider has a limitation and zero direct formats, show Official Creator Export Card
        if (!analysis.hasDownloadableFormats && analysis.limitationExplanation != null) {
            ProviderLimitationCard(
                explanation = analysis.limitationExplanation,
                officialExportUrl = analysis.officialCreatorExportUrl,
                onOpenOfficialRoute = { url ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                    }
                }
            )
        }

        // 3. When formats are available: Smart Quality Bar + VIDEO/AUDIO Tabs + Format Selection Grid + Confirmation Card
        if (analysis.hasDownloadableFormats) {
            SmartQualityAndDataSaverBar(
                smartQualityEnabled = settings.smartQualityEnabled,
                dataSaverEnabled = settings.dataSaverEnabled,
                onToggleSmartQuality = onToggleSmartQuality,
                onToggleDataSaver = onToggleDataSaver
            )

            // VIDEO & AUDIO Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoraPrimaryPanel)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaTabButton(
                    title = "${stringResource(R.string.tab_video)} (${analysis.videoFormats.size})",
                    isSelected = selectedTab == MediaTab.VIDEO,
                    icon = Icons.Default.VideoFile,
                    onClick = { onSelectTab(MediaTab.VIDEO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_video")
                )
                MediaTabButton(
                    title = "${stringResource(R.string.tab_audio)} (${analysis.audioFormats.size})",
                    isSelected = selectedTab == MediaTab.AUDIO,
                    icon = Icons.Default.AudioFile,
                    onClick = { onSelectTab(MediaTab.AUDIO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_audio")
                )
            }

            // Tab Content
            if (selectedTab == MediaTab.VIDEO) {
                VideoFormatSelectionList(
                    formats = analysis.videoFormats,
                    selectedId = selectedVideoOption?.id,
                    onSelectFormat = onSelectVideoFormat
                )
            } else {
                AudioFormatSelectionList(
                    formats = analysis.audioFormats,
                    selectedId = selectedAudioOption?.id,
                    onSelectFormat = onSelectAudioFormat
                )
            }

            // Final Confirmation & Download Execution Card
            FinalDownloadConfirmationCard(
                mediaTab = selectedTab,
                selectedVideo = selectedVideoOption,
                selectedAudio = selectedAudioOption,
                activeJob = activeJob,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onRetryDownload = onRetryDownload,
                onOpenDownloadedFile = onOpenDownloadedFile
            )
        }
    }
}

@Composable
private fun MediaMetadataPreviewCard(analysis: MediaAnalysisResult) {
    Surface(
        color = NexoraPrimaryPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media_preview_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Status Banner (Clearly shows Authorized Demo Mode or Live Provider Adapter)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (analysis.isAuthorizedSampleMode) {
                            NexoraVioletAccent.copy(alpha = 0.22f)
                        } else {
                            NexoraElectricCyan.copy(alpha = 0.14f)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (analysis.isAuthorizedSampleMode) NexoraIceBlue else NexoraElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = analysis.providerStatusBanner,
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraSoftWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail Preview
                Box(
                    modifier = Modifier
                        .size(width = 116.dp, height = 74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraSecondaryPanel)
                ) {
                    if (!analysis.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = analysis.thumbnailUrl,
                            contentDescription = analysis.title,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.img_sample_media_thumb),
                            error = painterResource(R.drawable.img_sample_media_thumb),
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(
                                analysis.localThumbnailResId ?: R.drawable.img_sample_media_thumb
                            ),
                            contentDescription = analysis.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    analysis.durationSeconds?.let { sec ->
                        Surface(
                            color = NexoraAmoledBlack.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = formatDuration(sec),
                                style = MaterialTheme.typography.labelSmall,
                                color = NexoraElectricCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Title & Provider Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = analysis.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NexoraSoftWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("analyzed_media_title")
                    )
                    Text(
                        text = "${analysis.providerType.displayName} • ${analysis.authorOrChannel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraIceBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    analysis.originalResolution?.let { res ->
                        Text(
                            text = "Source: $res" + (analysis.originalFrameRate?.let { " @ $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraMutedBlueGray
                        )
                    }
                }
            }

            // Watermark-Free Guarantee Strip
            Surface(
                color = NexoraSecondaryPanel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NexoraEmeraldReady,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = analysis.watermarkPolicyStatement,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = NexoraMutedBlueGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderLimitationCard(
    explanation: String,
    officialExportUrl: String?,
    onOpenOfficialRoute: (String) -> Unit
) {
    Surface(
        color = NexoraSecondaryPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NexoraAmberWarning.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_limitation_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NexoraAmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Provider Permission & Compliance Notice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NexoraSoftWhite
                )
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraSoftWhite.copy(alpha = 0.9f)
            )
            if (!officialExportUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { onOpenOfficialRoute(officialExportUrl) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = NexoraElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Official Creator Export Route",
                        color = NexoraElectricCyan,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartQualityAndDataSaverBar(
    smartQualityEnabled: Boolean,
    dataSaverEnabled: Boolean,
    onToggleSmartQuality: (Boolean) -> Unit,
    onToggleDataSaver: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = smartQualityEnabled,
            onClick = { onToggleSmartQuality(!smartQualityEnabled) },
            label = {
                Text(
                    text = "Smart Quality",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NexoraElectricCyan.copy(alpha = 0.18f),
                selectedLabelColor = NexoraElectricCyan,
                selectedLeadingIconColor = NexoraElectricCyan,
                containerColor = NexoraPrimaryPanel,
                labelColor = NexoraMutedBlueGray
            ),
            modifier = Modifier
                .height(48.dp)
                .testTag("smart_quality_chip")
        )

        FilterChip(
            selected = dataSaverEnabled,
            onClick = { onToggleDataSaver(!dataSaverEnabled) },
            label = {
                Text(
                    text = "Data Saver (360p/480p)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DataSaverOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NexoraIceBlue.copy(alpha = 0.22f),
                selectedLabelColor = NexoraIceBlue,
                selectedLeadingIconColor = NexoraIceBlue,
                containerColor = NexoraPrimaryPanel,
                labelColor = NexoraMutedBlueGray
            ),
            modifier = Modifier
                .height(48.dp)
                .testTag("data_saver_chip")
        )
    }
}

@Composable
private fun MediaTabButton(
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) NexoraElectricCyan else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NexoraAmoledBlack else NexoraMutedBlueGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) NexoraAmoledBlack else NexoraSoftWhite
            )
        }
    }
}

@Composable
private fun VideoFormatSelectionList(
    formats: List<VideoFormatOption>,
    selectedId: String?,
    onSelectFormat: (String) -> Unit
) {
    if (formats.isEmpty()) {
        Surface(
            color = NexoraPrimaryPanel,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "This source contains audio streams only. Switch to the AUDIO tab to download.",
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraMutedBlueGray,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        formats.forEach { option ->
            val isSelected = option.id == selectedId
            Surface(
                color = if (isSelected) NexoraSecondaryPanel else NexoraPrimaryPanel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) NexoraElectricCyan else NexoraSubtleDivider
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectFormat(option.id) }
                    .testTag("video_format_${option.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = if (isSelected) NexoraElectricCyan else NexoraIceBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = option.qualityLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NexoraSoftWhite
                            )
                            if (option.isSmartRecommended) {
                                BadgePill("SMART PICK", NexoraElectricCyan)
                            } else if (option.isDataSaverRecommended) {
                                BadgePill("LOW DATA", NexoraIceBlue)
                            }
                        }

                        Text(
                            text = "${option.containerFormat} • ${formatBytes(option.estimatedSizeBytes)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NexoraElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${option.resolution} • ${option.codecInfo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraMutedBlueGray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (option.hasAudioTrack) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (option.hasAudioTrack) NexoraEmeraldReady else NexoraAmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (option.hasAudioTrack) "Audio Included" else "Video Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (option.hasAudioTrack) NexoraEmeraldReady else NexoraAmberWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (option.requiresConversionOrMuxing) {
                            "Conversion/Mux Required: ${option.processingPipelineNote}"
                        } else {
                            "Direct Stream: ${option.processingPipelineNote}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = NexoraMutedBlueGray
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioFormatSelectionList(
    formats: List<AudioFormatOption>,
    selectedId: String?,
    onSelectFormat: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Transparency Note about Audio Bitrate Conversion (Requirement 5)
        Surface(
            color = NexoraSecondaryPanel.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NexoraIceBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NexoraIceBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.audio_bitrate_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = NexoraMutedBlueGray
                )
            }
        }

        formats.forEach { option ->
            val isSelected = option.id == selectedId
            Surface(
                color = if (isSelected) NexoraSecondaryPanel else NexoraPrimaryPanel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) NexoraElectricCyan else NexoraSubtleDivider
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectFormat(option.id) }
                    .testTag("audio_format_${option.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = if (isSelected) NexoraElectricCyan else NexoraIceBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NexoraSoftWhite
                            )
                            if (option.isSmartRecommended) {
                                BadgePill("SMART PICK", NexoraElectricCyan)
                            }
                        }

                        Text(
                            text = "${option.containerFormat} • ${formatBytes(option.estimatedSizeBytes)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NexoraElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${option.bitrateDisplay} • ${if (option.requiresConversion) "Conversion Required" else "Direct Demux (Zero Re-encode)"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraIceBlue
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = option.conversionStatusNote,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = NexoraMutedBlueGray
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalDownloadConfirmationCard(
    mediaTab: MediaTab,
    selectedVideo: VideoFormatOption?,
    selectedAudio: AudioFormatOption?,
    activeJob: ActiveDownloadJob?,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onOpenDownloadedFile: (filePath: String, containerFormat: String) -> Unit
) {
    val selectedFormatText = if (mediaTab == MediaTab.VIDEO) {
        selectedVideo?.let { "${it.containerFormat} (${it.qualityLabel})" } ?: "None"
    } else {
        selectedAudio?.label ?: "None"
    }
    val selectedSpecText = if (mediaTab == MediaTab.VIDEO) {
        selectedVideo?.resolution ?: "—"
    } else {
        selectedAudio?.bitrateDisplay ?: "—"
    }
    val selectedSizeText = if (mediaTab == MediaTab.VIDEO) {
        formatBytes(selectedVideo?.estimatedSizeBytes)
    } else {
        formatBytes(selectedAudio?.estimatedSizeBytes)
    }

    val isJobRunning = activeJob?.status in listOf(
        JobStatus.QUEUED,
        JobStatus.DOWNLOADING,
        JobStatus.REMUXING_WATERMARK_FREE,
        JobStatus.VERIFYING_INTEGRITY
    )

    Surface(
        color = NexoraPrimaryPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, NexoraElectricCyan.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_confirmation_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPORT TELEMETRY & CONFIRMATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = NexoraElectricCyan
                )
                BadgePill("NO WATERMARK", NexoraEmeraldReady)
            }

            // Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryColumn("FORMAT", selectedFormatText)
                TelemetryColumn("RESOLUTION / BITRATE", selectedSpecText)
                TelemetryColumn("EST. SIZE", selectedSizeText)
            }

            // Primary Action Button: "Download Video" or "Download Audio"
            Button(
                onClick = onStartDownload,
                enabled = !isJobRunning,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraElectricCyan,
                    contentColor = NexoraAmoledBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(if (mediaTab == MediaTab.VIDEO) "download_video_button" else "download_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mediaTab == MediaTab.VIDEO) {
                        stringResource(R.string.btn_download_video)
                    } else {
                        stringResource(R.string.btn_download_audio)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Active Job Real-Time Progress & Controls
            AnimatedVisibility(visible = activeJob != null) {
                activeJob?.let { job ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoraAmoledBlack)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${job.status.displayLabel} (${(job.progressFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (job.status) {
                                    JobStatus.READY -> NexoraEmeraldReady
                                    JobStatus.FAILED, JobStatus.CANCELLED -> NexoraCoralError
                                    else -> NexoraElectricCyan
                                }
                            )
                            job.estimatedSecondsRemaining?.let { sec ->
                                if (sec > 0 && isJobRunning) {
                                    Text(
                                        text = "~${sec}s remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NexoraMutedBlueGray
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { job.progressFraction },
                            color = if (job.status == JobStatus.READY) NexoraEmeraldReady else NexoraElectricCyan,
                            trackColor = NexoraSecondaryPanel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = "${formatBytes(job.bytesProcessed)} / ${formatBytes(job.totalBytes)} • GET /api/download/status/${job.jobId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraMutedBlueGray
                        )

                        if (job.sha256Checksum != null) {
                            Text(
                                text = "SHA-256: ${job.sha256Checksum.take(24)}... (Watermark-Free Verified)",
                                style = MaterialTheme.typography.labelSmall,
                                color = NexoraEmeraldReady
                            )
                        }

                        if (job.errorMessage != null) {
                            Text(
                                text = job.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NexoraCoralError
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isJobRunning) {
                                OutlinedButton(
                                    onClick = onCancelDownload,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NexoraCoralError),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("cancel_download_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = NexoraCoralError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel & Clean Up", color = NexoraCoralError)
                                }
                            }

                            if (job.status == JobStatus.FAILED || job.status == JobStatus.CANCELLED) {
                                Button(
                                    onClick = onRetryDownload,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexoraIceBlue,
                                        contentColor = NexoraAmoledBlack
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("retry_download_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Download", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (job.status == JobStatus.READY && job.outputFilePath != null) {
                                Button(
                                    onClick = { onOpenDownloadedFile(job.outputFilePath, job.containerFormat) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexoraEmeraldReady,
                                        contentColor = NexoraAmoledBlack
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("open_downloaded_file_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open / Save ${job.outputFileName ?: "Media File"}",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NexoraMutedBlueGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = NexoraSoftWhite
        )
    }
}

@Composable
private fun BadgePill(text: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "Unknown"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.2f MB", mb)
    } else {
        val kb = bytes.toDouble() / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
