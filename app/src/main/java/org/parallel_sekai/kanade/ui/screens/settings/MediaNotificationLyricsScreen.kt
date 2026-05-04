package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaNotificationLyricsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val mediaNotificationLyricsSettings by viewModel.mediaNotificationLyricsSettings.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.header_media_notification_lyrics),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
        ) {
            // 1. 启用媒体通知歌词总开关
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_media_notification_lyrics)) },
                supportingContent = { Text(stringResource(R.string.desc_media_notification_lyrics)) },
                trailingContent = {
                    Switch(
                        checked = mediaNotificationLyricsSettings.enabled,
                        onCheckedChange = { viewModel.updateMediaNotificationLyricsEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 2. 显示模式选择
            ListItem(
                headlineContent = { Text(stringResource(R.string.label_display_mode)) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
                        ) {
                            FilterChip(
                                selected = mediaNotificationLyricsSettings.mode == 0,
                                onClick = { viewModel.updateMediaNotificationLyricsMode(0) },
                                label = { Text(stringResource(R.string.mode_original_only)) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = mediaNotificationLyricsSettings.mode == 1,
                                onClick = { viewModel.updateMediaNotificationLyricsMode(1) },
                                label = { Text(stringResource(R.string.mode_original_translation)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text =
                                if (mediaNotificationLyricsSettings.mode == 0) {
                                    stringResource(R.string.desc_mode_original_only)
                                } else {
                                    stringResource(R.string.desc_mode_original_translation)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 3. 按时长滚动截取
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_scrolling_truncate)) },
                supportingContent = { Text(stringResource(R.string.desc_scrolling_truncate)) },
                trailingContent = {
                    Switch(
                        checked = mediaNotificationLyricsSettings.scrollingTruncateEnabled,
                        onCheckedChange = { viewModel.updateMediaNotificationLyricsScrollingTruncateEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 4. 最大展示单位 Slider
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            R.string.label_max_display_units,
                            mediaNotificationLyricsSettings.maxDisplayUnits,
                        ),
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            stringResource(R.string.desc_max_display_units),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                        Slider(
                            value = mediaNotificationLyricsSettings.maxDisplayUnits.toFloat(),
                            onValueChange = { viewModel.updateMediaNotificationLyricsMaxDisplayUnits(it.roundToInt()) },
                            valueRange = 3f..120f,
                            steps = 116, // 120 - 3 - 1
                        )
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 5. 翻译最大展示单位 Slider (仅在原文/翻译模式下显示)
            if (mediaNotificationLyricsSettings.mode == 1) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.label_translation_max_display_units,
                                mediaNotificationLyricsSettings.translationMaxDisplayUnits,
                            ),
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                stringResource(R.string.desc_translation_max_display_units),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                            Slider(
                                value = mediaNotificationLyricsSettings.translationMaxDisplayUnits.toFloat(),
                                onValueChange = {
                                    viewModel.updateMediaNotificationLyricsTranslationMaxDisplayUnits(
                                        it.roundToInt(),
                                    )
                                },
                                valueRange = 3f..120f,
                                steps = 116, // 120 - 3 - 1
                            )
                        }
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))
            }

            // 6. 智能单位
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_smart_units)) },
                supportingContent = { Text(stringResource(R.string.desc_smart_units)) },
                trailingContent = {
                    Switch(
                        checked = mediaNotificationLyricsSettings.smartUnitsEnabled,
                        onCheckedChange = { viewModel.updateMediaNotificationLyricsSmartUnitsEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 7. 显示时刻
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_show_timestamp)) },
                supportingContent = { Text(stringResource(R.string.desc_show_timestamp)) },
                trailingContent = {
                    Switch(
                        checked = mediaNotificationLyricsSettings.showTimestamp,
                        onCheckedChange = { viewModel.updateMediaNotificationLyricsShowTimestamp(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 8. 展示设备状态多选
            ListItem(
                headlineContent = { Text(stringResource(R.string.label_display_states)) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                        Text(
                            stringResource(R.string.desc_display_states),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // 使用 Checkbox + ListItem 组合
                        val displayStates = mediaNotificationLyricsSettings.displayStates

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = displayStates.contains(0),
                                onCheckedChange = { checked ->
                                    val newStates =
                                        if (checked) {
                                            displayStates + 0
                                        } else {
                                            // 防止取消最后一个
                                            if (displayStates.size > 1) displayStates - 0 else displayStates
                                        }
                                    viewModel.updateMediaNotificationLyricsDisplayStates(newStates)
                                },
                            )
                            Text(
                                stringResource(R.string.state_unlocked),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = displayStates.contains(1),
                                onCheckedChange = { checked ->
                                    val newStates =
                                        if (checked) {
                                            displayStates + 1
                                        } else {
                                            if (displayStates.size > 1) displayStates - 1 else displayStates
                                        }
                                    viewModel.updateMediaNotificationLyricsDisplayStates(newStates)
                                },
                            )
                            Text(
                                stringResource(R.string.state_locked_screen_on),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = displayStates.contains(2),
                                onCheckedChange = { checked ->
                                    val newStates =
                                        if (checked) {
                                            displayStates + 2
                                        } else {
                                            if (displayStates.size > 1) displayStates - 2 else displayStates
                                        }
                                    viewModel.updateMediaNotificationLyricsDisplayStates(newStates)
                                },
                            )
                            Text(
                                stringResource(R.string.state_screen_off),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 9. 暂停时还原
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_restore_on_pause)) },
                supportingContent = { Text(stringResource(R.string.desc_restore_on_pause)) },
                trailingContent = {
                    Switch(
                        checked = mediaNotificationLyricsSettings.restoreOnPause,
                        onCheckedChange = { viewModel.updateMediaNotificationLyricsRestoreOnPause(it) },
                    )
                },
            )

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}
