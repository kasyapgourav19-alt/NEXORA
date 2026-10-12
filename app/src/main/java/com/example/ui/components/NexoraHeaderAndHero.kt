package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AnalysisStage
import com.example.domain.model.AuthorizedSamplePreset
import com.example.domain.provider.NexoraAnalysisPipeline
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
import com.example.ui.theme.NexoraVioletAccent
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NexoraTopHeader(
    reducedMotion: Boolean,
    onOpenArchitectureSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "media_engine_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = if (reducedMotion) 0.55f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = NexoraPrimaryPanel.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.24f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nexora_header_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Custom Minimal NEXORA Symbol + Wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NexoraHexSymbol(
                    modifier = Modifier
                        .size(34.dp)
                        .semantics { contentDescription = "NEXORA Emblem" }
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 2.2.sp,
                        color = NexoraSoftWhite
                    )
                    // Glowing MEDIA ENGINE status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NexoraElectricCyan.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = stringResource(R.string.status_media_engine),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            color = NexoraElectricCyan
                        )
                    }
                }
            }

            // Right: Backend API Inspector & Settings Icon Buttons (min 48dp touch targets)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onOpenArchitectureSheet,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("backend_api_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Backend API and Architecture",
                        tint = NexoraIceBlue
                    )
                }

                IconButton(
                    onClick = onOpenSettingsSheet,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open NEXORA Settings",
                        tint = NexoraElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
fun NexoraHexSymbol(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.44f

        val hexPath = Path()
        for (i in 0..5) {
            val angleRad = Math.toRadians((60.0 * i - 30.0))
            val x = cx + (radius * cos(angleRad)).toFloat()
            val y = cy + (radius * sin(angleRad)).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()

        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                colors = listOf(NexoraElectricCyan.copy(alpha = 0.25f), NexoraVioletAccent.copy(alpha = 0.15f))
            )
        )
        drawPath(
            path = hexPath,
            color = NexoraElectricCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner energy arrow / core
        drawLine(
            color = NexoraElectricCyan,
            start = Offset(cx, cy - radius * 0.45f),
            end = Offset(cx, cy + radius * 0.42f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = NexoraIceBlue,
            start = Offset(cx - radius * 0.34f, cy + radius * 0.08f),
            end = Offset(cx, cy + radius * 0.44f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = NexoraIceBlue,
            start = Offset(cx + radius * 0.34f, cy + radius * 0.08f),
            end = Offset(cx, cy + radius * 0.44f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun NexoraHeroWorkspaceSection(
    urlInput: String,
    isAnalyzing: Boolean,
    analysisStage: AnalysisStage,
    validationErrorTitle: String?,
    validationErrorExplanation: String?,
    validationErrorSuggestion: String?,
    reducedMotion: Boolean,
    onUrlChanged: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onAnalyzeClicked: () -> Unit,
    onSelectPreset: (AuthorizedSamplePreset) -> Unit,
    onPickLocalVideoClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtle Watermark-Free HUD Badge
        Surface(
            color = NexoraSecondaryPanel,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = NexoraElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.watermark_free_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = NexoraElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Heading: "Your Media. Your Quality."
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.displayMedium,
            color = NexoraSoftWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("hero_main_heading")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Supporting Text
        Text(
            text = stringResource(R.string.hero_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = NexoraMutedBlueGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Central Glassmorphism URL Input Panel with Soft Ambient Cyan Lighting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexoraElectricCyan.copy(alpha = 0.16f),
                                NexoraIceBlue.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                }
        ) {
            Surface(
                color = NexoraPrimaryPanel.copy(alpha = 0.94f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            NexoraElectricCyan.copy(alpha = 0.65f),
                            NexoraIceBlue.copy(alpha = 0.35f),
                            NexoraElectricCyan.copy(alpha = 0.65f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_panel")
            ) {
                Box {
                    // Subtle ambient hero art overlay at 12% opacity
                    Image(
                        painter = painterResource(id = R.drawable.img_nexora_hero_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.12f,
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = onUrlChanged,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.input_placeholder),
                                    color = NexoraMutedBlueGray.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "HTTPS Media Link",
                                    tint = NexoraElectricCyan
                                )
                            },
                            trailingIcon = {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClearClicked,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = NexoraMutedBlueGray
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { if (!isAnalyzing) onAnalyzeClicked() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraElectricCyan,
                                unfocusedBorderColor = NexoraElectricCyanBorder,
                                focusedContainerColor = NexoraAmoledBlack.copy(alpha = 0.85f),
                                unfocusedContainerColor = NexoraAmoledBlack.copy(alpha = 0.72f),
                                focusedTextColor = NexoraSoftWhite,
                                unfocusedTextColor = NexoraSoftWhite,
                                cursorColor = NexoraElectricCyan
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Row: Paste Button + Clear Button + Primary Glowing Analyze Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPasteClicked,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.35f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = NexoraSecondaryPanel,
                                    contentColor = NexoraSoftWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("paste_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = NexoraElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.btn_paste),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = onAnalyzeClicked,
                                enabled = !isAnalyzing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexoraElectricCyan,
                                    contentColor = NexoraAmoledBlack,
                                    disabledContainerColor = NexoraElectricCyan.copy(alpha = 0.45f),
                                    disabledContentColor = NexoraAmoledBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("analyze_url_button")
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        color = NexoraAmoledBlack,
                                        strokeWidth = 2.2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Analyzing...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_analyze),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Animated 4-Stage Progress Stepper during URL Analysis
                        AnimatedVisibility(visible = isAnalyzing) {
                            AnalysisProgressStagesCard(
                                currentStage = analysisStage,
                                modifier = Modifier.padding(top = 14.dp)
                            )
                        }

                        // Validation / SSRF Error Card
                        AnimatedVisibility(visible = validationErrorTitle != null) {
                            ValidationErrorBanner(
                                title = validationErrorTitle ?: "",
                                explanation = validationErrorExplanation ?: "",
                                suggestion = validationErrorSuggestion ?: "",
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .testTag("validation_error_banner")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Sources & Authorized Sample Quick-Test Strip
        SupportedSourcesAndPresetsPanel(
            onSelectPreset = onSelectPreset,
            onPickLocalVideoClicked = onPickLocalVideoClicked
        )
    }
}

@Composable
private fun AnalysisProgressStagesCard(
    currentStage: AnalysisStage,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        AnalysisStage.VALIDATING_LINK,
        AnalysisStage.IDENTIFYING_SOURCE,
        AnalysisStage.CHECKING_FORMATS,
        AnalysisStage.PREPARING_OPTIONS
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexoraAmoledBlack.copy(alpha = 0.85f))
            .border(1.dp, NexoraElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stages.forEach { stage ->
            val isDone = currentStage.stepIndex > stage.stepIndex
            val isActive = currentStage == stage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    isDone -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = NexoraEmeraldReady,
                        modifier = Modifier.size(16.dp)
                    )
                    isActive -> CircularProgressIndicator(
                        color = NexoraElectricCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, NexoraMutedBlueGray.copy(alpha = 0.4f), CircleShape)
                    )
                }
                Column {
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> NexoraElectricCyan
                            isDone -> NexoraSoftWhite
                            else -> NexoraMutedBlueGray
                        }
                    )
                    if (isActive) {
                        Text(
                            text = stage.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraMutedBlueGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationErrorBanner(
    title: String,
    explanation: String,
    suggestion: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NexoraCoralError.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NexoraCoralError.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Validation Error",
                tint = NexoraCoralError,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NexoraSoftWhite
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraSoftWhite.copy(alpha = 0.9f)
                )
                if (suggestion.isNotBlank()) {
                    Text(
                        text = "Next step: $suggestion",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraAmberWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedSourcesAndPresetsPanel(
    onSelectPreset: (AuthorizedSamplePreset) -> Unit,
    onPickLocalVideoClicked: () -> Unit
) {
    Surface(
        color = NexoraPrimaryPanel,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NexoraSubtleDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Supported Platform Indicators Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProviderIndicatorChip("YouTube", "oEmbed + Studio API")
                ProviderIndicatorChip("Instagram", "Permalink + Creator Export")
                ProviderIndicatorChip("Direct Video URLs", "HTTPS MP4 / WebM")
                ProviderIndicatorChip("Open Media Studio", "4K & Lossless CC0")
            }

            Text(
                text = stringResource(R.string.provider_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraMutedBlueGray,
                fontSize = 12.sp
            )

            // Quick-Test Authorized Sample Presets + Local Device Video Clean Remux
            Text(
                text = "AUTHORIZED INSTANT TEST STREAMS & USER-OWNED CLEAN EXPORT",
                style = MaterialTheme.typography.labelSmall,
                color = NexoraElectricCyan
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NexoraAnalysisPipeline.AUTHORIZED_SAMPLE_PRESETS.forEach { preset ->
                    Surface(
                        color = NexoraSecondaryPanel,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NexoraElectricCyan.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset) }
                            .testTag("preset_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = NexoraElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraSoftWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexoraIceBlue
                                )
                            }
                        }
                    }
                }

                // User-Owned Local Video Clean Export Button
                Surface(
                    color = NexoraVioletAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NexoraVioletAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickLocalVideoClicked() }
                        .testTag("import_local_video_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Import Own Video",
                            tint = NexoraVioletAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Import Own Video",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NexoraSoftWhite
                            )
                            Text(
                                text = "CLEAN WATERMARK-FREE REMUX",
                                style = MaterialTheme.typography.labelSmall,
                                color = NexoraIceBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderIndicatorChip(
    name: String,
    capability: String
) {
    Surface(
        color = NexoraAmoledBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NexoraSubtleDivider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = NexoraIceBlue,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = NexoraSoftWhite
            )
            Text(
                text = "• $capability",
                style = MaterialTheme.typography.labelSmall,
                color = NexoraMutedBlueGray
            )
        }
    }
}
