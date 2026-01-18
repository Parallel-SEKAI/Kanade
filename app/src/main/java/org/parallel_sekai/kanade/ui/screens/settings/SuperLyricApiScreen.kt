package org.parallel_sekai.kanade.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperLyricApiScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val settings by viewModel.lyricsSettings.collectAsState()
    val isSuperLyricActivated = viewModel.isSuperLyricActivated
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_super_lyric_api), fontWeight = FontWeight.Bold) },
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
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            SettingsSectionHeader(title = stringResource(R.string.label_activation_status))

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_super_lyric_api)) },
                trailingContent = {
                    Text(
                        text = if (isSuperLyricActivated) stringResource(R.string.status_activated) else stringResource(R.string.status_not_activated),
                        color = if (isSuperLyricActivated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )

            Text(
                text = stringResource(R.string.desc_super_lyric_api_info), // Reuse info desc or create specific one
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.PaddingMedium),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

            SettingsSectionHeader(title = stringResource(R.string.header_general))

            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_lyric_sharing)) },
                supportingContent = { Text(stringResource(R.string.desc_lyric_sharing)) },
                trailingContent = {
                    Switch(
                        checked = settings.isSharingEnabled,
                        onCheckedChange = { viewModel.updateLyricSharingEnabled(it) },
                    )
                },
            )

            // 为底部的 MiniPlayer 留出空间
            Spacer(modifier = Modifier.height(Dimens.MiniPlayerBottomPadding))
        }
    }
}
