package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.adaptive.rememberAdaptiveLayoutInfo
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLyricsSettings: () -> Unit,
    onNavigateToExcludedFolders: () -> Unit,
    onNavigateToArtistParsingSettings: () -> Unit,
    onNavigateToCacheSettings: () -> Unit,
    onNavigateToLyricsGetterApi: () -> Unit,
    onNavigateToSuperLyricApi: () -> Unit,
    onNavigateToLyriconApi: () -> Unit,
    onNavigateToMediaNotificationLyrics: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val searchAsPlaylist = viewModel.searchResultAsPlaylist.collectAsState()
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold) },
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
        if (adaptiveInfo.isWideScreen) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1000.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    ) {
                        InterfaceSettingsSection(
                            onNavigateToLyricsSettings = onNavigateToLyricsSettings,
                            onNavigateToArtistParsingSettings = onNavigateToArtistParsingSettings,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    ) {
                        GeneralSettingsSection(
                            searchAsPlaylist = searchAsPlaylist.value,
                            onUpdateSearchAsPlaylist = { viewModel.updateSearchResultAsPlaylist(it) },
                            onNavigateToLyricsGetterApi = onNavigateToLyricsGetterApi,
                            onNavigateToSuperLyricApi = onNavigateToSuperLyricApi,
                            onNavigateToLyriconApi = onNavigateToLyriconApi,
                            onNavigateToMediaNotificationLyrics = onNavigateToMediaNotificationLyrics,
                            onNavigateToExcludedFolders = onNavigateToExcludedFolders,
                            onNavigateToCacheSettings = onNavigateToCacheSettings,
                        )
                        Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState),
            ) {
                InterfaceSettingsSection(
                    onNavigateToLyricsSettings = onNavigateToLyricsSettings,
                    onNavigateToArtistParsingSettings = onNavigateToArtistParsingSettings,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                GeneralSettingsSection(
                    searchAsPlaylist = searchAsPlaylist.value,
                    onUpdateSearchAsPlaylist = { viewModel.updateSearchResultAsPlaylist(it) },
                    onNavigateToLyricsGetterApi = onNavigateToLyricsGetterApi,
                    onNavigateToSuperLyricApi = onNavigateToSuperLyricApi,
                    onNavigateToLyriconApi = onNavigateToLyriconApi,
                    onNavigateToMediaNotificationLyrics = onNavigateToMediaNotificationLyrics,
                    onNavigateToExcludedFolders = onNavigateToExcludedFolders,
                    onNavigateToCacheSettings = onNavigateToCacheSettings,
                )

                Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
            }
        }
    }
}

@Composable
private fun InterfaceSettingsSection(
    onNavigateToLyricsSettings: () -> Unit,
    onNavigateToArtistParsingSettings: () -> Unit,
) {
    SettingsSectionHeader(title = stringResource(R.string.header_interface))

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_lyrics_settings)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToLyricsSettings),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_artist_parsing)) },
        supportingContent = { Text(stringResource(R.string.desc_artist_parsing)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToArtistParsingSettings),
    )
}

@Composable
private fun GeneralSettingsSection(
    searchAsPlaylist: Boolean,
    onUpdateSearchAsPlaylist: (Boolean) -> Unit,
    onNavigateToLyricsGetterApi: () -> Unit,
    onNavigateToSuperLyricApi: () -> Unit,
    onNavigateToLyriconApi: () -> Unit,
    onNavigateToMediaNotificationLyrics: () -> Unit,
    onNavigateToExcludedFolders: () -> Unit,
    onNavigateToCacheSettings: () -> Unit,
) {
    SettingsSectionHeader(title = stringResource(R.string.header_general))

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_lyrics_getter)) },
        supportingContent = { Text(stringResource(R.string.desc_lyrics_getter)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToLyricsGetterApi),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_super_lyric)) },
        supportingContent = { Text(stringResource(R.string.desc_super_lyric)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToSuperLyricApi),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_lyricon)) },
        supportingContent = { Text(stringResource(R.string.desc_lyricon)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToLyriconApi),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_media_notification_lyrics_entry)) },
        supportingContent = { Text(stringResource(R.string.desc_media_notification_lyrics_entry)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToMediaNotificationLyrics),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_search_as_playlist)) },
        supportingContent = { Text(stringResource(R.string.desc_search_as_playlist)) },
        trailingContent = {
            Switch(
                checked = searchAsPlaylist,
                onCheckedChange = onUpdateSearchAsPlaylist,
            )
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_excluded_folders)) },
        supportingContent = { Text(stringResource(R.string.desc_excluded_folders_pref)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToExcludedFolders),
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_cache_settings)) },
        supportingContent = { Text(stringResource(R.string.desc_cache_settings)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onNavigateToCacheSettings),
    )
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
    )
}
