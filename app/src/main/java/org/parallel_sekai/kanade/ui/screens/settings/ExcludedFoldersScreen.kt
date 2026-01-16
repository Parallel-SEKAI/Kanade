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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.FolderModel
import org.parallel_sekai.kanade.ui.theme.Dimens

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
                title = { Text(stringResource(R.string.title_excluded_folders), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(bottom = Dimens.MiniPlayerBottomPadding)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add))
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
                        text = stringResource(R.string.msg_no_folders_excluded),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding) // Added bottom padding
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.desc_excluded_folders_screen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Dimens.PaddingMedium)
                        )
                    }
                    items(excludedFolders.toList()) { path ->
                        ListItem(
                            headlineContent = { Text(path.split("/").last()) },
                            supportingContent = { Text(path) },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeExcludedFolder(path) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.desc_remove))
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
        title = { Text(stringResource(R.string.title_exclude_folder_dialog)) },
        text = {
            if (availableFolders.isEmpty()) {
                Text(stringResource(R.string.msg_no_more_folders))
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
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
