package org.parallel_sekai.kanade.ui.screens.settings

import android.app.Application // 导入 Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings
import org.parallel_sekai.kanade.data.repository.SettingsRepository // 导入 SettingsRepository
import org.parallel_sekai.kanade.ui.theme.KanadeTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistParsingSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.artistParsingSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Parsing", style = MaterialTheme.typography.titleLarge) },
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
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                item {
                    SettingsSectionHeader(title = "Artist Separators (Priority Order)")
                    Text(
                        text = "Artists will be split using the first matching separator from this list. Drag to reorder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                itemsIndexed(settings.separators, key = { _, separator -> separator }) { index, separator ->
                    var text by remember { mutableStateOf(separator) }
                    LaunchedEffect(separator) { text = separator }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                val newList = settings.separators.toMutableList()
                                newList[index] = it
                                viewModel.updateArtistSeparators(newList)
                            },
                            label = { Text("Separator") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                        )
                        IconButton(onClick = {
                            viewModel.updateArtistSeparators(settings.separators.filterIndexed { i, _ -> i != index })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Separator")
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.updateArtistSeparators(settings.separators + "") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Add New Separator")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader(title = "Artist Whitelist")
                    Text(
                        text = "Artist names in this list will not be split, even if they contain separators.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                itemsIndexed(settings.whitelist, key = { _, item -> item }) { index, item ->
                    var text by remember { mutableStateOf(item) }
                    LaunchedEffect(item) { text = item }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                val newList = settings.whitelist.toMutableList()
                                newList[index] = it
                                viewModel.updateArtistWhitelist(newList)
                            },
                            label = { Text("Whitelisted Artist") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                        IconButton(onClick = {
                            viewModel.updateArtistWhitelist(settings.whitelist.filterIndexed { i, _ -> i != index })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Whitelist Entry")
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.updateArtistWhitelist(settings.whitelist + "") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Add New Whitelist Entry")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader(title = "Display Join String")
                    Text(
                        text = "Used to combine multiple artists for display (e.g., 'Artist A & Artist B').",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    var text by remember { mutableStateOf(settings.joinString) }
                    LaunchedEffect(settings.joinString) { text = settings.joinString }

                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            viewModel.updateArtistJoinString(it)
                        },
                        label = { Text("Join String") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Added spacer
                }
            }
        }
    }
}

// 预览函数
@Preview(showBackground = true)
@Composable
fun PreviewArtistParsingSettingsScreen() {
    KanadeTheme {
        val applicationContext = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application // 移到 remember 外部
        ArtistParsingSettingsScreen(
            viewModel = remember { FakeSettingsViewModel(applicationContext) },
            onNavigateBack = {}
        )
    }
}
