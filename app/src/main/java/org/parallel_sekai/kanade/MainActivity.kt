package org.parallel_sekai.kanade

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.parallel_sekai.kanade.data.repository.PlaybackRepository
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.ui.screens.library.LibraryScreen
import org.parallel_sekai.kanade.ui.screens.artist.ArtistListScreen
import org.parallel_sekai.kanade.ui.screens.artist.ArtistDetailScreen
import org.parallel_sekai.kanade.ui.screens.library.AlbumListScreen
import org.parallel_sekai.kanade.ui.screens.library.AlbumDetailScreen
import org.parallel_sekai.kanade.ui.screens.library.PlaylistListScreen
import org.parallel_sekai.kanade.ui.screens.library.PlaylistDetailScreen
import org.parallel_sekai.kanade.ui.screens.library.FolderListScreen
import org.parallel_sekai.kanade.ui.screens.library.FolderDetailScreen
import org.parallel_sekai.kanade.ui.screens.more.MoreScreen
import org.parallel_sekai.kanade.ui.screens.player.DetailType
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.parallel_sekai.kanade.ui.screens.search.SearchScreen
import org.parallel_sekai.kanade.ui.screens.search.SearchViewModel
import org.parallel_sekai.kanade.ui.screens.settings.SettingsScreen
import org.parallel_sekai.kanade.ui.screens.settings.LyricsSettingsScreen
import org.parallel_sekai.kanade.ui.screens.settings.ExcludedFoldersScreen
import org.parallel_sekai.kanade.ui.screens.settings.ArtistParsingSettingsScreen // New
import org.parallel_sekai.kanade.ui.screens.settings.SettingsViewModel
import org.parallel_sekai.kanade.ui.screens.player.KanadePlayerContainer
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerViewModel
import org.parallel_sekai.kanade.ui.theme.KanadeTheme
import androidx.compose.ui.res.stringResource
import org.parallel_sekai.kanade.ui.theme.Dimens

sealed class Screen(val route: String, val labelResId: Int, val icon: ImageVector?) {
    object Library : Screen("library", R.string.title_library, Icons.Filled.Home)
    object Search : Screen("search", R.string.label_search, Icons.Filled.Search)
    object More : Screen("more", R.string.title_more, Icons.Filled.Info)
    object Settings : Screen("settings", R.string.title_settings, null)
    object LyricsSettings : Screen("lyrics_settings", R.string.title_lyrics_settings, null)
    object ExcludedFolders : Screen("excluded_folders", R.string.title_excluded_folders, null)
    object ArtistParsingSettings : Screen("artist_parsing_settings", R.string.title_artist_parsing, null)

    // Library sub-screens
    object Artists : Screen("artists", R.string.label_artists, null)
    object Albums : Screen("albums", R.string.label_albums, null)
    object Playlists : Screen("playlists", R.string.label_playlists, null)
    object Folders : Screen("folders", R.string.label_folders, null)

    // Detail screens
    object ArtistDetail : Screen("artist_detail/{name}", R.string.title_artists, null) {
        fun createRoute(name: String) = "artist_detail/$name"
    }
    object AlbumDetail : Screen("album_detail/{id}/{title}", R.string.label_albums, null) {
        fun createRoute(id: String, title: String) = "album_detail/$id/$title"
    }
    object FolderDetail : Screen("folder_detail/{path}", R.string.label_folders, null) {
        fun createRoute(path: String) = "folder_detail/${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object PlaylistDetail : Screen("playlist_detail/{id}/{title}", R.string.label_playlists, null) {
        fun createRoute(id: String, title: String) = "playlist_detail/$id/$title"
    }
}

val items = listOf(
    Screen.Library,
    Screen.Search,
    Screen.More,
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(applicationContext)
        val playbackRepository = PlaybackRepository(applicationContext, settingsRepository)
        val viewModel = PlayerViewModel(playbackRepository, settingsRepository, applicationContext)
        val settingsViewModel = SettingsViewModel(settingsRepository)
        val searchViewModel = SearchViewModel(playbackRepository, settingsRepository)

        setContent {
            KanadeTheme {
                val state by viewModel.state.collectAsState()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 记录底部内边距以便播放器定位
                var bottomPadding by remember { mutableStateOf<Dp>(0.dp) }

                // 权限处理
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionState = rememberPermissionState(permission)

                LaunchedEffect(permissionState.status.isGranted) {
                    if (permissionState.status.isGranted) {
                        viewModel.handleIntent(PlayerIntent.RefreshList)
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            // 底部导航栏
                            NavigationBar {
                                items.forEach { screen ->
                                    val label = stringResource(screen.labelResId)
                                    NavigationBarItem(
                                        icon = { screen.icon?.let { Icon(it, contentDescription = label) } },
                                        label = { Text(label) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        bottomPadding = innerPadding.calculateBottomPadding()
                        // NavHost 始终填充全屏
                        NavHost(
                            navController,
                            startDestination = Screen.Library.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = bottomPadding)
                        ) {
                            composable(Screen.Library.route) {
                                if (permissionState.status.isGranted) {
                                    LibraryScreen(
                                        state = state,
                                        onSongClick = { song -> viewModel.handleIntent(PlayerIntent.SelectSong(song)) },
                                        onNavigateToArtists = { navController.navigate(Screen.Artists.route) },
                                        onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                                        onNavigateToFolders = { navController.navigate(Screen.Folders.route) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = stringResource(R.string.msg_grant_permission))
                                    }
                                }
                            }
                            composable(Screen.Artists.route) {
                                ArtistListScreen(
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onArtistClick = { name ->
                                        viewModel.handleIntent(PlayerIntent.FetchDetailList(DetailType.ARTIST, name))
                                        navController.navigate(Screen.ArtistDetail.createRoute(name))
                                    }
                                )
                            }
                            composable(Screen.Albums.route) {
                                AlbumListScreen(
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onAlbumClick = { id, title ->
                                        viewModel.handleIntent(PlayerIntent.FetchDetailList(DetailType.ALBUM, id))
                                        navController.navigate(Screen.AlbumDetail.createRoute(id, title))
                                    }
                                )
                            }
                            composable(Screen.Playlists.route) {
                                PlaylistListScreen(
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onPlaylistClick = { id, title ->
                                        viewModel.handleIntent(PlayerIntent.FetchDetailList(DetailType.PLAYLIST, id))
                                        navController.navigate(Screen.PlaylistDetail.createRoute(id, title))
                                    }
                                )
                            }
                            composable(Screen.Folders.route) {
                                FolderListScreen(
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onFolderClick = { path ->
                                        viewModel.handleIntent(PlayerIntent.FetchDetailList(DetailType.FOLDER, path))
                                        navController.navigate(Screen.FolderDetail.createRoute(path))
                                    }
                                )
                            }
                            composable(Screen.ArtistDetail.route) { backStackEntry ->
                                val name = backStackEntry.arguments?.getString("name") ?: ""
                                ArtistDetailScreen(
                                    name = name,
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onSongClick = { song, list -> viewModel.handleIntent(PlayerIntent.SelectSong(song, list)) }
                                )
                            }
                            composable(Screen.AlbumDetail.route) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id") ?: ""
                                val title = backStackEntry.arguments?.getString("title") ?: ""
                                AlbumDetailScreen(
                                    id = id,
                                    title = title,
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onSongClick = { song, list -> viewModel.handleIntent(PlayerIntent.SelectSong(song, list)) }
                                )
                            }
                            composable(Screen.PlaylistDetail.route) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id") ?: ""
                                val title = backStackEntry.arguments?.getString("title") ?: ""
                                PlaylistDetailScreen(
                                    id = id,
                                    title = title,
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onSongClick = { song, list -> viewModel.handleIntent(PlayerIntent.SelectSong(song, list)) }
                                )
                            }
                            composable(Screen.FolderDetail.route) { backStackEntry ->
                                val path = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", "UTF-8")
                                FolderDetailScreen(
                                    path = path,
                                    state = state,
                                    onBackClick = { navController.popBackStack() },
                                    onSongClick = { song, list -> viewModel.handleIntent(PlayerIntent.SelectSong(song, list)) }
                                )
                            }
                            composable(Screen.Search.route) {
                                SearchScreen(
                                    viewModel = searchViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.More.route) {
                                MoreScreen(
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToLyricsSettings = { navController.navigate(Screen.LyricsSettings.route) },
                                    onNavigateToExcludedFolders = { navController.navigate(Screen.ExcludedFolders.route) },
                                    onNavigateToArtistParsingSettings = { navController.navigate(Screen.ArtistParsingSettings.route) } // New
                                )
                            }
                            composable(Screen.LyricsSettings.route) {
                                LyricsSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.ExcludedFolders.route) {
                                ExcludedFoldersScreen(
                                    viewModel = settingsViewModel,
                                    allFolders = state.folderList,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.ArtistParsingSettings.route) { // New
                                ArtistParsingSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // 统一的播放器容器，置于顶层（Scaffold 之上）
                    KanadePlayerContainer(
                        state = state,
                        onIntent = { viewModel.handleIntent(it) },
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}