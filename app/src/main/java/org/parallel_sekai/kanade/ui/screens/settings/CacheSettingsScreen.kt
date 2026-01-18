package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val maxCacheSize by viewModel.maxCacheSize.collectAsState()
    val currentCacheSize by viewModel.currentCacheSize.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize(context)
    }

    // 非线性映射函数：将 0.0-1.0 映射到 0.5GB-64GB
    // 使用 2^(7x - 1) 映射，因为 2^-1 = 0.5, 2^6 = 64
    fun normalizedToGb(n: Float): Float {
        val gb = Math.pow(2.0, (7 * n - 1).toDouble()).toFloat()
        return when {
            gb < 2f -> (Math.round(gb * 10f) / 10f) // 2GB以下保留一位小数
            gb < 10f -> (Math.round(gb * 2f) / 2f) // 10GB以下按0.5步进
            else -> Math.round(gb).toFloat() // 10GB以上按1GB步进
        }
    }

    fun gbToNormalized(gb: Float): Float {
        val logValue = Math.log(gb.toDouble()) / Math.log(2.0)
        return ((logValue + 1) / 7).toFloat().coerceIn(0f, 1f)
    }

    var sliderPosition by remember(maxCacheSize) {
        mutableFloatStateOf(gbToNormalized(maxCacheSize.toFloat() / (1024 * 1024 * 1024)))
    }
    val displayGb = normalizedToGb(sliderPosition)

    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.action_clear_cache)) },
            text = { Text(stringResource(R.string.msg_confirm_clear_cache)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache(context)
                        showClearConfirm = false
                    },
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_cache_settings), fontWeight = FontWeight.Bold) },
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
            SettingsSectionHeader(title = stringResource(R.string.header_cache_usage))

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_current_cache_size)) },
                supportingContent = { Text(formatFileSize(currentCacheSize)) },
                trailingContent = {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text(stringResource(R.string.action_clear))
                    }
                },
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // 缓存上限部分，应用水平内边距以对齐 ListItem 的内容
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.PaddingMedium),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_cache_limit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.fmt_cache_limit_gb,
                            if (displayGb < 10f && displayGb % 1f != 0f) {
                                String.format(Locale.US, "%.1f", displayGb)
                            } else {
                                displayGb.toInt().toString()
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        val finalGb = normalizedToGb(sliderPosition)
                        viewModel.updateMaxCacheSize((finalGb * 1024 * 1024 * 1024).toLong())
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(top = Dimens.PaddingSmall),
                )
            }

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
