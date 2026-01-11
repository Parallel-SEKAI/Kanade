package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.lyricsSettings.collectAsState()
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
                title = { Text("歌词界面", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Preview Section
            Text(
                text = "预览",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = previewAlignment
                ) {
                    Text(
                        text = "这是一段歌词预览",
                        fontSize = settings.fontSize.sp,
                        fontWeight = FontWeight(settings.fontWeight),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = previewTextAlign
                    )
                    if (settings.showTranslation) {
                        Text(
                            text = "This is a lyric preview",
                            fontSize = (settings.fontSize * 0.8f).sp,
                            fontWeight = FontWeight(settings.fontWeight),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = previewTextAlign
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionHeader(title = "显示")
            
            ListItem(
                headlineContent = { Text("显示翻译") },
                trailingContent = {
                    Switch(
                        checked = settings.showTranslation,
                        onCheckedChange = { viewModel.updateShowTranslation(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("非活跃行模糊") },
                trailingContent = {
                    Switch(
                        checked = settings.blurEnabled,
                        onCheckedChange = { viewModel.updateBlurEnabled(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("平衡长行") },
                supportingContent = { Text("自动切分过长的歌词以使排版更整齐") },
                trailingContent = {
                    Switch(
                        checked = settings.balanceLines,
                        onCheckedChange = { viewModel.updateBalanceLines(it) }
                    )
                }
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "字体大小 (${settings.fontSize.toInt()} sp)", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = settings.fontSize,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 12f..32f,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            SettingsSectionHeader(title = "样式")
            
            Column(modifier = Modifier.padding(16.dp)) {
                val weightLabel = when (settings.fontWeight) {
                    in 100..299 -> "极细"
                    in 300..399 -> "细"
                    in 400..499 -> "常规"
                    in 500..599 -> "中等"
                    in 600..699 -> "半粗"
                    in 700..799 -> "加粗"
                    else -> "极粗"
                }
                Text(text = "字体粗细 ($weightLabel - ${settings.fontWeight})", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = settings.fontWeight.toFloat(),
                    onValueChange = { viewModel.updateFontWeight(it.toInt()) },
                    valueRange = 100f..900f,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "对齐方式", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                
                val alignments = listOf("居左", "居中", "居右")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    alignments.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = alignments.size),
                            onClick = { viewModel.updateAlignment(index) },
                            selected = settings.alignment == index
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

