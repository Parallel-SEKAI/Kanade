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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens

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
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
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
            SettingsSectionHeader(title = stringResource(R.string.header_interface))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_lyrics_settings)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToLyricsSettings)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_artist_parsing)) }, // 新增入口
                supportingContent = { Text(stringResource(R.string.desc_artist_parsing)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToArtistParsingSettings)
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
            SettingsSectionHeader(title = stringResource(R.string.header_general))

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_search_as_playlist)) },
                supportingContent = { Text(stringResource(R.string.desc_search_as_playlist)) },
                trailingContent = {
                    Switch(
                        checked = searchAsPlaylist.value,
                        onCheckedChange = { viewModel.updateSearchResultAsPlaylist(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_excluded_folders)) },
                supportingContent = { Text(stringResource(R.string.desc_excluded_folders_pref)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToExcludedFolders)
            )

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall)
    )
}
