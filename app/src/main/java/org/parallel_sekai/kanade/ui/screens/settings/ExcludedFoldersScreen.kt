package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.data.source.FolderModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedFoldersScreen(
    viewModel: SettingsViewModel,
    allFolders: List<FolderModel>,
    onNavigateBack: () -> Unit
) {
    val excludedFolders by viewModel.excludedFolders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluded Folders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (excludedFolders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No folders excluded.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Added bottom padding
                ) {
                    item {
                        Text(
                            text = "Music in these folders will be hidden from your library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(excludedFolders.toList()) { path ->
                        ListItem(
                            headlineContent = { Text(path.split("/").last()) },
                            supportingContent = { Text(path) },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeExcludedFolder(path) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddFolderDialog(
                allFolders = allFolders,
                excludedFolders = excludedFolders,
                onDismiss = { showAddDialog = false },
                onConfirm = { path ->
                    viewModel.addExcludedFolder(path)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddFolderDialog(
    allFolders: List<FolderModel>,
    excludedFolders: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val availableFolders = allFolders.filter { it.path !in excludedFolders }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exclude Folder") },
        text = {
            if (availableFolders.isEmpty()) {
                Text("No more folders available in library.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(availableFolders) { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            supportingContent = { Text(folder.path) },
                            modifier = Modifier.clickable { onConfirm(folder.path) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
