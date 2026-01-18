package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val settings by viewModel.lyricsSettings.collectAsState()
    val scrollState = rememberScrollState()
    val previewTextAlign = when (settings.alignment) {
        0 -> TextAlign.Start
        1 -> TextAlign.Center
        else -> TextAlign.End
    }
    val previewAlignment = when (settings.alignment) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_lyrics_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            // Preview Section
            Text(
                text = stringResource(R.string.label_preview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.PaddingMedium)
                    .clip(RoundedCornerShape(Dimens.CornerRadiusLarge)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.PaddingMedium),
                    horizontalAlignment = previewAlignment,
                ) {
                    Text(
                        text = stringResource(R.string.text_lyric_preview),
                        fontSize = settings.fontSize.sp,
                        fontWeight = FontWeight(settings.fontWeight),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = previewTextAlign,
                    )
                    if (settings.showTranslation) {
                        Text(
                            text = "This is a lyric preview",
                            fontSize = (settings.fontSize * 0.8f).sp,
                            fontWeight = FontWeight(settings.fontWeight),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.PaddingExtraSmall),
                            textAlign = previewTextAlign,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
            SettingsSectionHeader(title = stringResource(R.string.header_display))

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_show_translation)) },
                trailingContent = {
                    Switch(
                        checked = settings.showTranslation,
                        onCheckedChange = { viewModel.updateShowTranslation(it) },
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_blur_inactive)) },
                trailingContent = {
                    Switch(
                        checked = settings.blurEnabled,
                        onCheckedChange = { viewModel.updateBlurEnabled(it) },
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_balance_lines)) },
                supportingContent = { Text(stringResource(R.string.desc_balance_lines)) },
                trailingContent = {
                    Switch(
                        checked = settings.balanceLines,
                        onCheckedChange = { viewModel.updateBalanceLines(it) },
                    )
                },
            )

            Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                Text(text = stringResource(R.string.label_font_size, settings.fontSize.toInt()), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = settings.fontSize,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 12f..32f,
                    modifier = Modifier.padding(top = Dimens.PaddingSmall),
                )
            }

            SettingsSectionHeader(title = stringResource(R.string.header_style))

            Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                val weightLabel = when (settings.fontWeight) {
                    in 100..299 -> stringResource(R.string.weight_thin)
                    in 300..399 -> stringResource(R.string.weight_light)
                    in 400..499 -> stringResource(R.string.weight_normal)
                    in 500..599 -> stringResource(R.string.weight_medium)
                    in 600..699 -> stringResource(R.string.weight_semi_bold)
                    in 700..799 -> stringResource(R.string.weight_bold)
                    else -> stringResource(R.string.weight_extra_bold)
                }
                Text(text = stringResource(R.string.label_font_weight, weightLabel, settings.fontWeight), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = settings.fontWeight.toFloat(),
                    onValueChange = { viewModel.updateFontWeight(it.toInt()) },
                    valueRange = 100f..900f,
                    modifier = Modifier.padding(top = Dimens.PaddingSmall),
                )
            }

            Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                Text(text = stringResource(R.string.label_alignment), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = Dimens.PaddingSmall))

                val alignments = listOf(stringResource(R.string.align_left), stringResource(R.string.align_center), stringResource(R.string.align_right))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    alignments.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = alignments.size),
                            onClick = { viewModel.updateAlignment(index) },
                            selected = settings.alignment == index,
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            SettingsSectionHeader(title = stringResource(R.string.header_general))

            // ListItem(
            //     headlineContent = { Text(stringResource(R.string.pref_lyric_sharing)) },
            //     supportingContent = { Text(stringResource(R.string.desc_lyric_sharing)) },
            //     trailingContent = {
            //         Switch(
            //             checked = settings.isSharingEnabled,
            //             onCheckedChange = { viewModel.updateLyricSharingEnabled(it) },
            //         )
            //     },
            // )

            var showQualityDialog by remember { mutableStateOf(false) }
            val currentQualityLabel = when (settings.shareQuality) {
                1.0f -> stringResource(R.string.quality_standard)
                1.5f -> stringResource(R.string.quality_high)
                2.0f -> stringResource(R.string.quality_ultra)
                else -> stringResource(R.string.quality_standard)
            }

            Surface(
                onClick = { showQualityDialog = true },
                color = Color.Transparent,
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pref_share_quality)) },
                    supportingContent = { Text(currentQualityLabel) },
                )
            }

            if (showQualityDialog) {
                AlertDialog(
                    onDismissRequest = { showQualityDialog = false },
                    title = { Text(stringResource(R.string.pref_share_quality)) },
                    text = {
                        Column {
                            QualityOption(
                                label = stringResource(R.string.quality_standard),
                                selected = settings.shareQuality == 1.0f,
                                onClick = {
                                    viewModel.updateLyricShareQuality(1.0f)
                                    showQualityDialog = false
                                },
                            )
                            QualityOption(
                                label = stringResource(R.string.quality_high),
                                selected = settings.shareQuality == 1.5f,
                                onClick = {
                                    viewModel.updateLyricShareQuality(1.5f)
                                    showQualityDialog = false
                                },
                            )
                            QualityOption(
                                label = stringResource(R.string.quality_ultra),
                                selected = settings.shareQuality == 2.0f,
                                onClick = {
                                    viewModel.updateLyricShareQuality(2.0f)
                                    showQualityDialog = false
                                },
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQualityDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}

@Composable
private fun QualityOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Dimens.PaddingSmall),
        )
    }
}
