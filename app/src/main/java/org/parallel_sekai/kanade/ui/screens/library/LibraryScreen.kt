package org.parallel_sekai.kanade.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.parallel_sekai.kanade.data.source.MusicModel
import org.parallel_sekai.kanade.ui.screens.player.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: PlayerState,
    onSongClick: (MusicModel) -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToFolders: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = 80.dp // Space for MiniPlayer
        )
    ) {
        item {
            SectionHeader("Your Library")
        }

        item {
            LibraryGrid(
                onNavigateToArtists = onNavigateToArtists,
                onNavigateToAlbums = onNavigateToAlbums,
                onNavigateToFolders = onNavigateToFolders
            )
        }

        item {
            SectionHeader("All Music")
        }

        items(state.allMusicList) { song ->
            SongListItem(
                song = song,
                isSelected = state.currentSong?.id == song.id,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
fun LibraryGrid(
    onNavigateToArtists: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToFolders: () -> Unit
) {
    val items = listOf(
        LibraryGridItem("Artists", Icons.Default.Person, onNavigateToArtists),
        LibraryGridItem("Albums", Icons.Default.Album, onNavigateToAlbums),
        LibraryGridItem("Playlists", Icons.Default.PlaylistPlay, {}),
        LibraryGridItem("Folders", Icons.Default.Folder, onNavigateToFolders)
    )

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
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

data class LibraryGridItem(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun LibraryCard(item: LibraryGridItem, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = item.onClick,
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun RecentSongCard(song: MusicModel) {
    Column(modifier = Modifier.width(120.dp)) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SongListItem(
    song: MusicModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Text(text = song.title, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
            Text(text = song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMusicScreen(
    state: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Music") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(state.allMusicList) { song ->
                SongListItem(
                    song = song,
                    isSelected = state.currentSong?.id == song.id,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    state: PlayerState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artists") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(state.artistList) { artist ->
                ListItem(
                    headlineContent = { Text(artist.name) },
                    supportingContent = { Text("${artist.albumCount} albums • ${artist.songCount} songs") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
                        )
                    },
                    modifier = Modifier.clickable { /* TODO: Navigate to artist detail */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    state: PlayerState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(state.albumList) { album ->
                ListItem(
                    headlineContent = { Text(album.title) },
                    supportingContent = { Text("${album.artist} • ${album.songCount} songs") },
                    leadingContent = {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    },
                    modifier = Modifier.clickable { /* TODO: Navigate to album detail */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    state: PlayerState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(state.folderList) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text("${folder.path} • ${folder.songCount} songs") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp)
                        )
                    },
                    modifier = Modifier.clickable { /* TODO: Navigate to folder content */ }
                )
            }
        }
    }
}
