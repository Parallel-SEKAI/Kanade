package org.parallel_sekai.kanade.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.ShowError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(effect.messageResId, *effect.formatArgs.toTypedArray()),
                            actionLabel = null,
                            withDismissAction = true,
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding),
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.handleIntent(SearchIntent.UpdateQuery(it)) },
                        onSearch = { viewModel.handleIntent(SearchIntent.PerformSearch(it)) },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.hint_search_music)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.handleIntent(SearchIntent.UpdateQuery("")) }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.desc_clear))
                                }
                            }
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.PaddingMedium),
            ) {}

            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

            SourceSelector(
                availableSources = state.availableSources,
                selectedSourceIds = state.selectedSourceIds,
                onToggleSource = { viewModel.handleIntent(SearchIntent.ToggleSource(it)) },
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
            ) {
                if (state.searchQuery.isEmpty()) {
                    if (state.searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.PaddingMedium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.title_recent_searches),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                TextButton(onClick = { viewModel.handleIntent(SearchIntent.ClearHistory) }) {
                                    Text(stringResource(R.string.action_clear))
                                }
                            }
                        }

                        items(state.searchHistory) { query ->
                            HistoryItem(
                                query = query,
                                onClick = { viewModel.handleIntent(SearchIntent.PerformSearch(query)) },
                                onRemove = { viewModel.handleIntent(SearchIntent.RemoveHistoryItem(query)) },
                            )
                        }
                    }
                } else {
                    if (state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.PaddingExtraLarge),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (state.searchResults.isEmpty() && state.isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.PaddingExtraLarge),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.msg_no_results), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        items(state.searchResults) { song ->
                            SearchSongListItem(
                                song = song,
                                onClick = { viewModel.handleIntent(SearchIntent.PlayMusic(song, state.searchResults)) },
                                artistJoinString = state.artistJoinString,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelector(
    availableSources: List<SourceInfo>,
    selectedSourceIds: Set<String>,
    onToggleSource: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.PaddingMedium),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(availableSources) { source ->
            val isSelected = selectedSourceIds.contains(source.id)
            FilterChip(
                selected = isSelected,
                onClick = { onToggleSource(source.id) },
                label = { Text(source.name) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
fun HistoryItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.CornerRadiusLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = query,
            modifier = Modifier
                .padding(start = Dimens.PaddingMedium)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.desc_remove),
                modifier = Modifier.size(Dimens.IconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SearchSongListItem(
    song: MusicModel,
    onClick: () -> Unit,
    artistJoinString: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(Dimens.AlbumCoverSizeListItem)
                .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.padding(start = Dimens.PaddingMedium).weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${song.artists.joinToString(artistJoinString)} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
