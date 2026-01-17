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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.ui.screens.library.MusicListDetailScreen
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    state: PlayerState,
    onIntent: (org.parallel_sekai.kanade.ui.screens.player.PlayerIntent) -> Unit,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onIntent(org.parallel_sekai.kanade.ui.screens.player.PlayerIntent.RefreshArtists)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_artists)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding)
        ) {
            items(state.artistList) { artist ->
                ListItem(
                    headlineContent = { Text(artist.name) },
                    supportingContent = { Text(stringResource(R.string.fmt_albums_songs, artist.albumCount, artist.songCount)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSizeHuge).clip(RoundedCornerShape(Dimens.CornerRadiusExtraLarge)).background(MaterialTheme.colorScheme.surfaceVariant).padding(Dimens.PaddingSmall)
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
        subtitle = stringResource(R.string.fmt_songs_count, state.detailMusicList.size),
        songs = state.detailMusicList,
        currentSong = state.currentSong,
        artistJoinString = state.artistJoinString,
        onBackClick = onBackClick,
        onSongClick = onSongClick
    )
}
