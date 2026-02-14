@file:Suppress("KtlintStandardMaxLineLength")

package org.parallel_sekai.kanade.ui.screens.player

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.ui.theme.Dimens
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoScreen(
    state: PlayerState,
    onBackClick: () -> Unit,
) {
    val song = state.currentSong ?: return
    val context = LocalContext.current

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val metadata = remember(song) {
        val list = mutableListOf<Pair<Int, String>>()
        list.add(R.string.label_title to song.title)
        list.add(R.string.label_artist to song.artists.joinToString(state.artistJoinString))
        list.add(R.string.label_album to song.album)
        list.add(R.string.label_duration to formatDuration(song.duration))
        list.add(R.string.label_source to song.sourceId)

        if (song.sourceId == "local_storage") {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(song.mediaUri)
                retriever.setDataSource(context, uri)

                val bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)

                if (mimeType != null) list.add(R.string.label_mime_type to mimeType)
                if (bitRate != null) {
                    val kbps = bitRate.toInt() / 1000
                    list.add(R.string.label_bitrate to "$kbps kbps")
                }
                if (sampleRate != null) {
                    val khz = sampleRate.toFloat() / 1000
                    list.add(R.string.label_sample_rate to "$khz kHz")
                }

                // Get file path and size if it's a content URI or file URI
                if (uri.scheme == "content") {
                    val projection = arrayOf(
                        android.provider.MediaStore.Audio.Media.DATA,
                        android.provider.MediaStore.Audio.Media.SIZE,
                    )
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val path = cursor.getString(0)
                            val size = cursor.getLong(1)
                            list.add(R.string.label_file_path to path)
                            list.add(R.string.label_file_size to formatSize(size))
                        }
                    }
                } else if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        list.add(R.string.label_file_path to file.absolutePath)
                        list.add(R.string.label_file_size to formatSize(file.length()))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            list.add(R.string.label_metadata to song.mediaUri)
        }

        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_song_info)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(metadata) { (labelRes, value) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingSmall),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.pointerInput(value) {
                            detectTapGestures(
                                onLongPress = {
                                    clipboardManager.setText(AnnotatedString(value))
                                    Toast.makeText(context, R.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimens.PaddingLarge),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            // Lyrics Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.pref_lyrics_settings),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!state.lyrics.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = state.lyrics,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                ),
                                modifier = Modifier
                                    .padding(Dimens.PaddingMedium)
                                    .pointerInput(state.lyrics) {
                                        detectTapGestures(
                                            onLongPress = {
                                                clipboardManager.setText(AnnotatedString(state.lyrics))
                                                @Suppress("LocalContextGetResourceValueCall")
                                                val msg = context.getString(R.string.msg_copied_to_clipboard)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },
                                        )
                                    },
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.msg_no_lyrics),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.2f KB", kb)
    }
}
