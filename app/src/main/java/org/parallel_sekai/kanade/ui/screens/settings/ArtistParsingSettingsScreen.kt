@file:Suppress("KtlintStandardMaxLineLength")

package org.parallel_sekai.kanade.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.preview.FakeSettingsViewModel
import org.parallel_sekai.kanade.ui.theme.Dimens
import org.parallel_sekai.kanade.ui.theme.KanadeTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistParsingSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val settings by viewModel.artistParsingSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_artist_parsing), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.PaddingMedium),
                contentPadding = PaddingValues(top = Dimens.PaddingSmall),
            ) {
                item {
                    SettingsSectionHeader(title = stringResource(R.string.header_artist_separators))
                    Text(
                        text = stringResource(R.string.desc_artist_separators),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Dimens.PaddingSmall),
                    )
                }
                itemsIndexed(settings.separators, key = { _, separator -> separator }) { index, separator ->
                    var text by remember { mutableStateOf(separator) }
                    LaunchedEffect(separator) { text = separator }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.PaddingExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.desc_reorder),
                            modifier = Modifier.padding(end = Dimens.PaddingSmall),
                        )
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                val newList = settings.separators.toMutableList()
                                newList[index] = it
                                viewModel.updateArtistSeparators(newList)
                            },
                            label = { Text(stringResource(R.string.label_separator)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        )
                        IconButton(onClick = {
                            viewModel.updateArtistSeparators(settings.separators.filterIndexed { i, _ -> i != index })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.desc_delete_separator))
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.updateArtistSeparators(settings.separators + "") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingSmall),
                    ) {
                        Text(stringResource(R.string.action_add_separator))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                    SettingsSectionHeader(title = stringResource(R.string.header_artist_whitelist))
                    Text(
                        text = stringResource(R.string.desc_artist_whitelist),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Dimens.PaddingSmall),
                    )
                }
                itemsIndexed(settings.whitelist, key = { _, item -> item }) { index, item ->
                    var text by remember { mutableStateOf(item) }
                    LaunchedEffect(item) { text = item }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.PaddingExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                val newList = settings.whitelist.toMutableList()
                                newList[index] = it
                                viewModel.updateArtistWhitelist(newList)
                            },
                            label = { Text(stringResource(R.string.label_whitelisted_artist)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        )
                        IconButton(onClick = {
                            viewModel.updateArtistWhitelist(settings.whitelist.filterIndexed { i, _ -> i != index })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.desc_delete_whitelist))
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.updateArtistWhitelist(settings.whitelist + "") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingSmall),
                    ) {
                        Text(stringResource(R.string.action_add_whitelist))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                    SettingsSectionHeader(title = stringResource(R.string.header_display_join_string))
                    Text(
                        text = stringResource(R.string.desc_display_join_string),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Dimens.PaddingSmall),
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
                        label = { Text(stringResource(R.string.label_join_string)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingExtraSmall),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding)) // Added spacer
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
            onNavigateBack = {},
        )
    }
}
