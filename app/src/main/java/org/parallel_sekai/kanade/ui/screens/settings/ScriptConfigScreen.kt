package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptConfigScreen(
    scriptId: String,
    state: PlayerState,
    settingsRepository: SettingsRepository,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val manifest = state.scriptManifests.find { it.id == scriptId } ?: return
    val configs = manifest.configs ?: return

    val currentConfigsJson by settingsRepository.scriptConfigsFlow.collectAsState(initial = null)
    val currentValues = remember(currentConfigsJson) {
        currentConfigsJson?.let {
            try {
                Json.decodeFromString<Map<String, Map<String, String>>>(it)[scriptId]
            } catch (e: Exception) {
                null
            }
        } ?: emptyMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(manifest.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            items(configs) { item ->
                val value = currentValues[item.key] ?: item.default
                
                ListItem(
                    headlineContent = { Text(item.label) },
                    supportingContent = {
                        when (item.type) {
                            "string", "number" -> {
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { onIntent(PlayerIntent.UpdateScriptConfig(scriptId, item.key, it)) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    singleLine = true
                                )
                            }
                            "boolean" -> {
                                Switch(
                                    checked = value.lowercase() == "true",
                                    onCheckedChange = { onIntent(PlayerIntent.UpdateScriptConfig(scriptId, item.key, it.toString())) }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
