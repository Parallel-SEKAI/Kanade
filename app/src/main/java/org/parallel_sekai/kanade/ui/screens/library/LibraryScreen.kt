package org.parallel_sekai.kanade.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: PlayerState,
    onIntent: (org.parallel_sekai.kanade.ui.screens.player.PlayerIntent) -> Unit,
    onSongClick: (MusicModel, List<MusicModel>?) -> Unit,
    onScriptClick: (String?) -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFolders: () -> Unit,
) {
    val activeManifest = state.scriptManifests.find { it.id == state.activeScriptId }
    val isScriptActive = activeManifest != null

    androidx.compose.runtime.LaunchedEffect(state.activeScriptId) {
        if (isScriptActive) {
            onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.RefreshHome)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = Dimens.MiniPlayerBottomPadding, // Space for MiniPlayer
            ),
    ) {
        item {
            SectionHeader(stringResource(R.string.header_your_library))
        }

        item {
            LibraryGrid(
                onNavigateToArtists = onNavigateToArtists,
                onNavigateToAlbums = onNavigateToAlbums,
                onNavigateToPlaylists = onNavigateToPlaylists,
                onNavigateToFolders = onNavigateToFolders,
            )
        }

        // 外部音源 Tab 切换
        if (state.scriptManifests.isNotEmpty()) {
            item {
                val tabs =
                    androidx.compose.runtime.remember(state.scriptManifests) {
                        listOf(null) + state.scriptManifests.map { it.id }
                    }
                val selectedIndex =
                    androidx.compose.runtime.remember(tabs, state.activeScriptId) {
                        tabs.indexOf(state.activeScriptId).coerceAtLeast(0)
                    }
                val scriptNamesMap =
                    androidx.compose.runtime.remember(state.scriptManifests) {
                        state.scriptManifests.associate { it.id to it.name }
                    }

                SecondaryScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingSmall),
                    edgePadding = Dimens.PaddingMedium,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, scriptId ->
                        val title =
                            if (scriptId == null) {
                                stringResource(R.string.label_local)
                            } else {
                                scriptNamesMap[scriptId] ?: ""
                            }
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { onScriptClick(scriptId) },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
        }

        // 主列表部分：根据音源状态切换
        item {
            val title =
                if (isScriptActive) {
                    activeManifest.name
                } else {
                    stringResource(R.string.header_all_music)
                }
            val icon = if (isScriptActive) Icons.Default.AutoAwesome else null
            SectionHeader(title = title, icon = icon)
        }

        val displayList = if (isScriptActive) state.homeMusicList else state.allMusicList

        if (isScriptActive && state.isHomeLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingExtraLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (isScriptActive && displayList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingExtraLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.no_content), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            itemsIndexed(displayList) { index, song ->
                // 预加载逻辑：距离末尾还有 20 个项目时触发加载
                if (isScriptActive && state.canLoadMoreHome && !state.isHomeLoadingMore &&
                    index >= displayList.size - 20
                ) {
                    onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.LoadMoreHome)
                }

                SongListItem(
                    song = song,
                    isSelected = state.currentSong?.id == song.id,
                    onClick = {
                        // 如果是脚本首页，不传入 displayList，让 ViewModel 触发完整列表获取逻辑
                        onSongClick(song, if (isScriptActive) null else displayList)
                    },
                    artistJoinString = state.artistJoinString,
                )
            }

            // 底部加载状态指示器
            if (isScriptActive && state.canLoadMoreHome && displayList.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingMedium),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isHomeLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryGrid(
    onNavigateToArtists: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFolders: () -> Unit,
) {
    val items =
        listOf(
            LibraryGridItem(stringResource(R.string.label_artists), Icons.Default.Person, onNavigateToArtists),
            LibraryGridItem(stringResource(R.string.label_albums), Icons.Default.Album, onNavigateToAlbums),
            LibraryGridItem(
                stringResource(R.string.label_playlists),
                Icons.AutoMirrored.Filled.PlaylistPlay,
                onNavigateToPlaylists,
            ),
            LibraryGridItem(stringResource(R.string.label_folders), Icons.Default.Folder, onNavigateToFolders),
        )

    Column(modifier = Modifier.padding(horizontal = Dimens.PaddingSmall)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LibraryCard(items[0], Modifier.weight(1f))
            LibraryCard(items[1], Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            LibraryCard(items[2], Modifier.weight(1f))
            LibraryCard(items[3], Modifier.weight(1f))
        }
    }
}

data class LibraryGridItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun LibraryCard(
    item: LibraryGridItem,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = item.onClick,
        modifier = modifier.padding(Dimens.PaddingSmall),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(Dimens.PaddingMedium)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier.padding(Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SongListItem(
    song: MusicModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    artistJoinString: String,
    showCover: Boolean = true,
    showArtist: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCover) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(Dimens.AlbumCoverSizeListItem)
                        .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier =
                Modifier
                    .padding(start = if (showCover) Dimens.PaddingMedium else 0.dp)
                    .weight(1f),
        ) {
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
            if (showArtist) {
                Text(
                    text = song.artists.joinToString(artistJoinString),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    state: PlayerState,
    onIntent: (org.parallel_sekai.kanade.ui.screens.player.PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    onAlbumClick: (String, String) -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.RefreshAlbums)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_albums)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
        ) {
            items(state.albumList) { album ->
                ListItem(
                    headlineContent = { Text(album.title) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.fmt_item_info_songs,
                                album.artists.joinToString(state.artistJoinString),
                                album.songCount,
                            ),
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(
                                        Dimens.AlbumCoverSizeListItem,
                                    ).clip(
                                        RoundedCornerShape(Dimens.CornerRadiusMedium),
                                    ).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                    },
                    modifier = Modifier.clickable { onAlbumClick(album.id, album.title) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    state: PlayerState,
    onIntent: (org.parallel_sekai.kanade.ui.screens.player.PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    onPlaylistClick: (String, String) -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.RefreshPlaylists)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_playlists)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
        ) {
            items(state.playlistList) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text(stringResource(R.string.fmt_songs_count, playlist.songCount)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(
                                        Dimens.IconSizeHuge,
                                    ).background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(Dimens.CornerRadiusMedium),
                                    ).padding(Dimens.PaddingSmall),
                        )
                    },
                    modifier = Modifier.clickable { onPlaylistClick(playlist.id, playlist.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    state: PlayerState,
    onIntent: (org.parallel_sekai.kanade.ui.screens.player.PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    onFolderClick: (String) -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.RefreshFolders)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_folders)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
        ) {
            items(state.folderList) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = {
                        Text(
                            stringResource(R.string.fmt_folder_info, folder.path, folder.songCount),
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(
                                        Dimens.IconSizeHuge,
                                    ).background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(Dimens.CornerRadiusMedium),
                                    ).padding(Dimens.PaddingSmall),
                        )
                    },
                    modifier = Modifier.clickable { onFolderClick(folder.path) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListDetailScreen(
    title: String,
    subtitle: String? = null,
    coverUrl: String? = null,
    songs: List<MusicModel>,
    currentSong: MusicModel?,
    artistJoinString: String,
    showSongCover: Boolean = true,
    showSongArtist: Boolean = true,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel, List<MusicModel>?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
        ) {
            if (coverUrl != null) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(Dimens.AlbumCoverSizeFullScreenPlayer),
                    ) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                                        ),
                                    ),
                        )
                        Column(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(Dimens.PaddingMedium),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else if (subtitle != null) {
                item {
                    Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(songs) { song ->
                SongListItem(
                    song = song,
                    isSelected = currentSong?.id == song.id,
                    onClick = { onSongClick(song, songs) },
                    artistJoinString = artistJoinString,
                    showCover = showSongCover,
                    showArtist = showSongArtist,
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    id: String,
    title: String,
    state: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel, List<MusicModel>?) -> Unit,
) {
    val coverUrl = state.detailMusicList.firstOrNull()?.coverUrl

    // Calculate intersection of artists for all songs in the album
    val allArtistsList = state.detailMusicList.map { it.artists.toSet() }
    val commonArtists =
        if (allArtistsList.isNotEmpty()) {
            allArtistsList.reduce { acc, set -> acc.intersect(set) }
        } else {
            emptySet()
        }

    val albumArtist =
        if (commonArtists.isNotEmpty()) {
            commonArtists.joinToString(state.artistJoinString)
        } else {
            ""
        }

    // Determine if we show artist for each song
    // Hide artist if all songs share the exact same set of artists (equal to the intersection)
    val allSongsHaveSameArtists = state.detailMusicList.all { it.artists.toSet() == commonArtists }

    MusicListDetailScreen(
        title = title,
        subtitle = albumArtist.ifEmpty { null },
        coverUrl = coverUrl,
        songs = state.detailMusicList,
        currentSong = state.currentSong,
        artistJoinString = state.artistJoinString,
        showSongCover = false,
        showSongArtist = !allSongsHaveSameArtists,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
    )
}

@Composable
fun FolderDetailScreen(
    path: String,
    state: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel, List<MusicModel>?) -> Unit,
) {
    MusicListDetailScreen(
        title = path.split("/").last(),
        subtitle = path,
        songs = state.detailMusicList,
        currentSong = state.currentSong,
        artistJoinString = state.artistJoinString,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
    )
}

@Composable
fun PlaylistDetailScreen(
    id: String,
    title: String,
    state: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel, List<MusicModel>?) -> Unit,
) {
    MusicListDetailScreen(
        title = title,
        songs = state.detailMusicList,
        currentSong = state.currentSong,
        artistJoinString = state.artistJoinString,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
    )
}
