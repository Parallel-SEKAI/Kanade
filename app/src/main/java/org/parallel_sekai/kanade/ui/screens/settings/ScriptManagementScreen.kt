package org.parallel_sekai.kanade.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptManagementScreen(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateToScriptConfig: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var scriptToDelete by remember { mutableStateOf<String?>(null) }
    var scriptNameToDelete by remember { mutableStateOf<String?>(null) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { pickedUri ->
            pickedUri?.let { uri -> onIntent(PlayerIntent.ImportScript(uri)) }
        }

    if (scriptToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                scriptToDelete = null
                scriptNameToDelete = null
            },
            title = { Text(stringResource(R.string.title_delete_script)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_script, scriptNameToDelete ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scriptToDelete?.let { id -> onIntent(PlayerIntent.DeleteScript(id)) }
                        scriptToDelete = null
                        scriptNameToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scriptToDelete = null
                        scriptNameToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_scripts)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { launcher.launch("application/javascript") }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_import_script))
                    }
                    IconButton(onClick = { onIntent(PlayerIntent.ReloadScripts) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.btn_reload_scripts))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.scriptManifests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.msg_no_scripts), color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = { launcher.launch("application/javascript") }) {
                            Text(stringResource(R.string.btn_import_script))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { onIntent(PlayerIntent.ReloadScripts) }) {
                            Text(stringResource(R.string.btn_reload_scripts))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = Dimens.MiniPlayerBottomPadding),
            ) {
                items(state.scriptManifests) { manifest ->
                    val isActive = state.activeScriptId == manifest.id
                    ListItem(
                        headlineContent = { Text(manifest.name) },
                        supportingContent = {
                            Text(
                                "${manifest.version} by ${manifest.author}\n${manifest.description ?: ""}",
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Default.Extension, contentDescription = null)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    scriptToDelete = manifest.id
                                    scriptNameToDelete = manifest.name
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.desc_delete),
                                    )
                                }
                                if (manifest.configs?.isNotEmpty() == true) {
                                    IconButton(onClick = { onNavigateToScriptConfig(manifest.id) }) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.title_settings),
                                        )
                                    }
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { checked ->
                                        onIntent(PlayerIntent.ToggleActiveScript(if (checked) manifest.id else null))
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
