package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLyricsSettings: () -> Unit,
    onNavigateToExcludedFolders: () -> Unit,
    onNavigateToArtistParsingSettings: () -> Unit // 新增：导航到艺术家解析设置
) {
    val scrollState = rememberScrollState()
    val searchAsPlaylist = viewModel.searchResultAsPlaylist.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
        ) {
            SettingsSectionHeader(title = "界面")
            
            ListItem(
                headlineContent = { Text("歌词界面") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToLyricsSettings)
            )

            ListItem(
                headlineContent = { Text("艺术家解析") }, // 新增入口
                supportingContent = { Text("自定义艺术家分隔符和显示方式") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToArtistParsingSettings)
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionHeader(title = "常规")

            ListItem(
                headlineContent = { Text("搜索结果作为播放列表") },
                supportingContent = { Text("播放搜索结果中的歌曲时，将整个列表设为当前队列") },
                trailingContent = {
                    Switch(
                        checked = searchAsPlaylist.value,
                        onCheckedChange = { viewModel.updateSearchResultAsPlaylist(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("排除文件夹") },
                supportingContent = { Text("隐藏特定文件夹中的音乐") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToExcludedFolders)
            )

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("System Default") }
            )
            ListItem(
                headlineContent = { Text("Audio Quality") },
                supportingContent = { Text("High") }
            )

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
