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
fun LyricsGetterApiScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val lyricGetterApiSettings by viewModel.lyricGetterApiSettings.collectAsState()
    val isLyricGetterActivated = viewModel.isLyricsGetterActivated
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_lyric_getter_api), fontWeight = FontWeight.Bold) },
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
            SettingsSectionHeader(title = stringResource(R.string.label_activation_status))

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_lyric_getter_api)) },
                trailingContent = {
                    Text(
                        text =
                            if (isLyricGetterActivated) {
                                stringResource(R.string.status_activated)
                            } else {
                                stringResource(R.string.status_not_activated)
                            },
                        color =
                            if (isLyricGetterActivated) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        fontWeight = FontWeight.Bold,
                    )
                },
            )

            Text(
                text = stringResource(R.string.desc_lyric_getter_api_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.PaddingMedium),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            SettingsSectionHeader(title = stringResource(R.string.header_lyric_getter_settings))

            // 启用 LyricGetter API 歌词发送
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_enable_lyric_getter_sending)) },
                supportingContent = { Text(stringResource(R.string.desc_enable_lyric_getter_sending)) },
                trailingContent = {
                    Switch(
                        checked = lyricGetterApiSettings.enabled,
                        onCheckedChange = { viewModel.updateLyricGetterApiEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 自动滚动
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_scrolling_truncate)) },
                supportingContent = { Text(stringResource(R.string.desc_scrolling_truncate)) },
                trailingContent = {
                    Switch(
                        checked = lyricGetterApiSettings.scrollingTruncateEnabled,
                        onCheckedChange = { viewModel.updateLyricGetterApiScrollingTruncateEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 最大展示单位
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            R.string.label_max_display_units,
                            lyricGetterApiSettings.maxDisplayUnits,
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
                            value = lyricGetterApiSettings.maxDisplayUnits.toFloat(),
                            onValueChange = { viewModel.updateLyricGetterApiMaxDisplayUnits(it.roundToInt()) },
                            valueRange = 3f..120f,
                            steps = 116,
                        )
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 智能单位
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_smart_units)) },
                supportingContent = { Text(stringResource(R.string.desc_smart_units)) },
                trailingContent = {
                    Switch(
                        checked = lyricGetterApiSettings.smartUnitsEnabled,
                        onCheckedChange = { viewModel.updateLyricGetterApiSmartUnitsEnabled(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 显示时刻
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_show_timestamp)) },
                supportingContent = { Text(stringResource(R.string.desc_show_timestamp)) },
                trailingContent = {
                    Switch(
                        checked = lyricGetterApiSettings.showTimestamp,
                        onCheckedChange = { viewModel.updateLyricGetterApiShowTimestamp(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            // 展示设备状态多选
            ListItem(
                headlineContent = { Text(stringResource(R.string.label_display_states)) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                        Text(
                            stringResource(R.string.desc_display_states),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val displayStates = lyricGetterApiSettings.displayStates

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
                                            if (displayStates.size > 1) displayStates - 0 else displayStates
                                        }
                                    viewModel.updateLyricGetterApiDisplayStates(newStates)
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
                                    viewModel.updateLyricGetterApiDisplayStates(newStates)
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
                                    viewModel.updateLyricGetterApiDisplayStates(newStates)
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

            // 暂停时自动清除歌词
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_clear_on_pause)) },
                supportingContent = { Text(stringResource(R.string.desc_clear_on_pause)) },
                trailingContent = {
                    Switch(
                        checked = lyricGetterApiSettings.clearOnPause,
                        onCheckedChange = { viewModel.updateLyricGetterApiClearOnPause(it) },
                    )
                },
            )

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}
