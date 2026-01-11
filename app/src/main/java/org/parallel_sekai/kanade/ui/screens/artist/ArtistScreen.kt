package org.parallel_sekai.kanade.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.data.source.MusicModel
import org.parallel_sekai.kanade.ui.screens.library.MusicListDetailScreen
import org.parallel_sekai.kanade.ui.screens.player.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    state: PlayerState,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artists") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    modifier = Modifier.clickable { onArtistClick(artist.name) }
                )
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    name: String,
    state: PlayerState,
    onBackClick: () -> Unit,
    onSongClick: (MusicModel, List<MusicModel>) -> Unit
) {
    MusicListDetailScreen(
        title = name,
        subtitle = "${state.detailMusicList.size} songs",
        songs = state.detailMusicList,
        currentSong = state.currentSong,
        artistJoinString = state.artistJoinString,
        onBackClick = onBackClick,
        onSongClick = onSongClick
    )
}
