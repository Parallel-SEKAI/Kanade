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
import org.parallel_sekai.kanade.ui.screens.more.MoreScreen
import org.parallel_sekai.kanade.ui.screens.settings.SettingsScreen
import org.parallel_sekai.kanade.ui.screens.settings.LyricsSettingsScreen
import org.parallel_sekai.kanade.ui.screens.settings.SettingsViewModel
import org.parallel_sekai.kanade.ui.screens.player.KanadePlayerContainer
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerViewModel
import org.parallel_sekai.kanade.ui.theme.KanadeTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    object Library : Screen("library", "Library", Icons.Filled.Home)
    object Search : Screen("search", "Search", Icons.Filled.Search)
    object More : Screen("more", "More", Icons.Filled.Info)
    object Settings : Screen("settings", "Settings", null)
    object LyricsSettings : Screen("lyrics_settings", "Lyrics Settings", null)
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

        val playbackRepository = PlaybackRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val viewModel = PlayerViewModel(playbackRepository, settingsRepository, applicationContext)
        val settingsViewModel = SettingsViewModel(settingsRepository)

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
                                    NavigationBarItem(
                                        icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                                        label = { Text(screen.label) },
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
                                        musicList = state.musicList,
                                        onSongClick = { song -> viewModel.handleIntent(PlayerIntent.SelectSong(song)) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Please grant storage permission to scan music.")
                                    }
                                }
                            }
                            composable(Screen.Search.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Search Screen")
                                }
                            }
                            composable(Screen.More.route) {
                                MoreScreen(
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToLyricsSettings = { navController.navigate(Screen.LyricsSettings.route) }
                                )
                            }
                            composable(Screen.LyricsSettings.route) {
                                LyricsSettingsScreen(
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