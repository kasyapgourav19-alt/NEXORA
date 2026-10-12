package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.RecentDownloadEntity
import com.example.data.preferences.NexoraSettings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NexoraAmberWarning
import com.example.ui.theme.NexoraAmoledBlack
import com.example.ui.theme.NexoraCoralError
import com.example.ui.theme.NexoraElectricCyan
import com.example.ui.theme.NexoraElectricCyanBorder
import com.example.ui.theme.NexoraEmeraldReady
import com.example.ui.theme.NexoraIceBlue
import com.example.ui.theme.NexoraMutedBlueGray
import com.example.ui.theme.NexoraPrimaryPanel
import com.example.ui.theme.NexoraSecondaryPanel
import com.example.ui.theme.NexoraSoftWhite
import com.example.ui.theme.NexoraSubtleDivider
import com.example.ui.theme.OrbitronFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NexoraRecentDownloadsSection(
    recentDownloads: List<RecentDownloadEntity>,
    onOpenDownloadedFile: (filePath: String, containerFormat: String) -> Unit,
    onDeleteSingleItem: (jobId: String, filePath: String?) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NexoraPrimaryPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NexoraSubtleDivider),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_downloads_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NexoraElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "RECENT ACTIVITY",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NexoraSoftWhite
                    )
                }

                if (recentDownloads.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NexoraCoralError.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = NexoraCoralError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear History",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoraCoralError
                        )
                    }
                }
            }

            if (recentDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexoraSecondaryPanel.copy(alpha = 0.6f))
                        .padding(vertical = 22.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No recent downloads stored locally",
                            style = MaterialTheme.typography.titleMedium,
                            color = NexoraSoftWhite
                        )
                        Text(
                            text = "Analyze an HTTPS link or select an Authorized Open Media preset above to export watermark-free video or audio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoraMutedBlueGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentDownloads.forEach { item ->
                        val available = item.isFileStillAvailable()
                        Surface(
                            color = NexoraSecondaryPanel,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NexoraSubtleDivider),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_item_${item.jobId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = NexoraSoftWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.formatLabel} • ${formatBytes(item.fileSizeBytes)} • ${dateFormat.format(Date(item.createdAtMillis))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NexoraIceBlue
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSingleItem(item.jobId, item.localFilePath) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove item",
                                            tint = NexoraMutedBlueGray
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (available) NexoraEmeraldReady else NexoraAmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (available) {
                                                "WATERMARK-FREE • READY ON DEVICE"
                                            } else {
                                                "TEMPORARY FILE EXPIRED / REMOVED"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (available) NexoraEmeraldReady else NexoraAmberWarning
                                        )
                                    }

                                    if (available && item.localFilePath != null) {
                                        Button(
                                            onClick = {
                                                onOpenDownloadedFile(item.localFilePath, item.containerFormat)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NexoraElectricCyan,
                                                contentColor = NexoraAmoledBlack
                                            ),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Open / Share",
                                                fontWeight = FontWeight.Bold
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
    }
}

@Composable
fun NexoraCreatorFooter(
    onHandleCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 28.dp)
            .testTag("nexora_footer"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = NexoraElectricCyan.copy(alpha = 0.28f),
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.creator_branding),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            color = NexoraSoftWhite,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )

        // Contact Instagram Handles requested by user: @avkaxofficial and @gxr.avka
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstagramContactPill(
                handle = stringResource(R.string.creator_ig_1),
                onClick = {
                    clipboard.setText(AnnotatedString("@avkaxofficial"))
                    onHandleCopied("Instagram handle @avkaxofficial copied")
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/avkaxofficial/"))
                        )
                    } catch (_: Exception) {
                    }
                }
            )

            InstagramContactPill(
                handle = stringResource(R.string.creator_ig_2),
                onClick = {
                    clipboard.setText(AnnotatedString("@gxr.avka"))
                    onHandleCopied("Instagram handle @gxr.avka copied")
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/gxr.avka/"))
                        )
                    } catch (_: Exception) {
                    }
                }
            )
        }

        Text(
            text = "NEXORA MEDIA WORKSTATION • WATERMARK-FREE OUTPUT ENGINE",
            style = MaterialTheme.typography.labelSmall,
            color = NexoraMutedBlueGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstagramContactPill(
    handle: String,
    onClick: () -> Unit
) {
    Surface(
        color = NexoraPrimaryPanel,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.45f)),
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag("ig_pill_${handle.removePrefix("@")}")
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IG: $handle",
                style = MaterialTheme.typography.labelLarge,
                color = NexoraElectricCyan
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraSettingsBottomSheet(
    settings: NexoraSettings,
    onDismiss: () -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleSmartQuality: (Boolean) -> Unit,
    onToggleDataSaver: (Boolean) -> Unit,
    onSelectVideoQuality: (String) -> Unit,
    onSelectAudioFormat: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val videoQualities = listOf("2160p", "1440p", "1080p", "720p", "480p", "360p")
    val audioFormats = listOf("MP3 320 kbps", "MP3 256 kbps", "M4A Original", "WAV Lossless")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexoraPrimaryPanel,
        contentColor = NexoraSoftWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NEXORA WORKSTATION SETTINGS",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NexoraElectricCyan
            )

            // Dark AMOLED Appearance Indicator
            SettingToggleRow(
                title = "AMOLED Deep Black Appearance (#05070B)",
                subtitle = "Default JARVIS high-contrast dark theme optimized for OLED panels",
                checked = settings.darkAmoledMode,
                onCheckedChange = {}
            )

            SettingToggleRow(
                title = "Reduced Motion",
                subtitle = "Minimize ambient pulse animations for accessibility & battery savings",
                checked = settings.reducedMotion,
                onCheckedChange = onToggleReducedMotion
            )

            SettingToggleRow(
                title = "Smart Quality Recommender",
                subtitle = "Automatically select optimal stream based on source & preference",
                checked = settings.smartQualityEnabled,
                onCheckedChange = onToggleSmartQuality
            )

            SettingToggleRow(
                title = "Data Saver Mode",
                subtitle = "Prefer compact 360p / 480p video and 128 kbps audio streams",
                checked = settings.dataSaverEnabled,
                onCheckedChange = onToggleDataSaver
            )

            // Preferred Default Video Quality
            Text(
                text = "Preferred Default Video Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NexoraSoftWhite
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                videoQualities.forEach { q ->
                    FilterChip(
                        selected = settings.preferredVideoQuality == q,
                        onClick = { onSelectVideoQuality(q) },
                        label = { Text(q) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraElectricCyan,
                            selectedLabelColor = NexoraAmoledBlack
                        ),
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            // Preferred Default Audio Format
            Text(
                text = "Preferred Default Audio Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NexoraSoftWhite
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                audioFormats.forEach { fmt ->
                    FilterChip(
                        selected = settings.preferredAudioFormat == fmt,
                        onClick = { onSelectAudioFormat(fmt) },
                        label = { Text(fmt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraIceBlue,
                            selectedLabelColor = NexoraAmoledBlack
                        ),
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            // Privacy & Watermark Policy Box
            Surface(
                color = NexoraSecondaryPanel,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NexoraSubtleDivider)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NexoraElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Privacy & Security: All history is stored locally on your device only. SSRF protection blocks localhost, RFC1918 private IPs, and cloud metadata endpoints. Downloaded files contain zero NEXORA watermarks or tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraMutedBlueGray
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    onClearHistory()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NexoraCoralError),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Clear Recent History & Temporary Files", color = NexoraCoralError)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NexoraSoftWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraMutedBlueGray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NexoraAmoledBlack,
                checkedTrackColor = NexoraElectricCyan
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraArchitectureBottomSheet(
    customBackendUrl: String,
    engineHealthSummary: String,
    onUpdateBackendUrl: (String) -> Unit,
    onRunHealthCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlDraft by remember(customBackendUrl) { mutableStateOf(customBackendUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexoraPrimaryPanel,
        contentColor = NexoraSoftWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = NexoraElectricCyan
                )
                Text(
                    text = "BACKEND & REST API ARCHITECTURE",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoraElectricCyan
                )
            }

            Surface(
                color = NexoraSecondaryPanel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NexoraElectricCyanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LIVE ENGINE HEALTH STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraIceBlue
                    )
                    Text(
                        text = engineHealthSummary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = NexoraEmeraldReady
                    )
                }
            }

            Text(
                text = "Optional Remote NEXORA Node.js + FFmpeg Backend URL (HTTPS):",
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraSoftWhite
            )

            OutlinedTextField(
                value = urlDraft,
                onValueChange = { urlDraft = it },
                placeholder = { Text("https://your-nexora-backend.run.app (leave blank for Local Engine)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexoraElectricCyan,
                    unfocusedBorderColor = NexoraElectricCyanBorder,
                    focusedTextColor = NexoraSoftWhite,
                    unfocusedTextColor = NexoraSoftWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onUpdateBackendUrl(urlDraft) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoraElectricCyan,
                        contentColor = NexoraAmoledBlack
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Save Endpoint", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRunHealthCheck,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NexoraElectricCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ping /api/health", color = NexoraElectricCyan)
                }
            }

            Surface(
                color = NexoraAmoledBlack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NexoraSubtleDivider)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "IMPLEMENTED REST ROUTES (/backend/server.ts & Local Engine)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraElectricCyan
                    )
                    Text(
                        text = "• POST /api/analyze — SSRF-guarded URL & provider format inspection\n" +
                            "• POST /api/download — Starts watermark-free stream copy or FFmpeg mux\n" +
                            "• GET  /api/download/status/:jobId — Real-time job telemetry & SHA-256\n" +
                            "• DELETE /api/download/:jobId — Cancels job & purges temp files\n" +
                            "• GET  /api/health — Backend & FFmpeg readiness check",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = NexoraSoftWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
