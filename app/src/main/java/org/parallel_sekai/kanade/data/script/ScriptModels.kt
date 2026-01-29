package org.parallel_sekai.kanade.data.script

import kotlinx.serialization.Serializable

@Serializable
data class ScriptMusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val cover: String? = null,
    val duration: Long? = null,
)

@Serializable
data class ScriptMusicListResponse(
    val items: List<ScriptMusicItem>,
    val total: Int? = null,
)

@Serializable
data class ScriptStreamInfo(
    val url: String,
    val headers: Map<String, String>? = null,
    val format: String,
)

@Serializable
data class ScriptManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String? = null,
    val configs: List<ScriptConfigItem>? = null,
)

@Serializable
data class ScriptConfigItem(
    val key: String,
    val label: String,
    val type: String, // "string", "number", "boolean", "select"
    val default: String, // Store as string, parse later
    val options: List<String>? = null,
)
