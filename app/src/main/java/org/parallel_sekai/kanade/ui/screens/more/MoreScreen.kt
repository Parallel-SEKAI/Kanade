package org.parallel_sekai.kanade.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.adaptive.rememberAdaptiveLayoutInfo
import org.parallel_sekai.kanade.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToScripts: () -> Unit,
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_more), fontWeight = FontWeight.Bold) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (adaptiveInfo.isWideScreen) 720.dp else Dp.Unspecified),
                contentPadding =
                    PaddingValues(
                        bottom = Dimens.MiniPlayerBottomPadding,
                        start = Dimens.PaddingMedium,
                        end = Dimens.PaddingMedium,
                        top = Dimens.PaddingSmall,
                    ),
            ) {
                item {
                    MoreItem(
                        icon = Icons.Default.Settings,
                        label = stringResource(R.string.title_settings),
                        onClick = onNavigateToSettings,
                    )
                }
                item {
                    MoreItem(
                        icon = Icons.Default.Extension,
                        label = stringResource(R.string.title_scripts),
                        onClick = onNavigateToScripts,
                    )
                }
                item {
                    MoreItem(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.label_about),
                        onClick = { /* TODO */ },
                    )
                }
            }
        }
    }
}

@Composable
fun MoreItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
