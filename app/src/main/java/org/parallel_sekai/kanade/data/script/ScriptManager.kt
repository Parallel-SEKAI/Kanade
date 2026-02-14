package org.parallel_sekai.kanade.data.script

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import java.io.File
import java.io.FileOutputStream

class ScriptManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val TAG = "ScriptManager"
    private val scope = MainScope()
    private val scriptDir: File by lazy {
        context.getExternalFilesDir("scripts") ?: File(context.filesDir, "scripts")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
    private val engineTasks = mutableMapOf<String, Deferred<ScriptEngine?>>()
    private val manifests = mutableMapOf<String, ScriptManifest>()
    private val scriptFiles = mutableMapOf<String, File>()

    init {
        if (!scriptDir.exists()) {
            scriptDir.mkdirs()
        }
    }

    suspend fun scanScripts(): List<ScriptManifest> =
        withContext(Dispatchers.IO) {
            engineTasks.clear() // Clear existing engines on new scan
            val files = scriptDir.listFiles { file -> file.extension == "js" } ?: emptyArray()
            val results = mutableListOf<ScriptManifest>()

            files.forEach { file ->
                try {
                    val content = file.readText()
                    val manifest = parseManifest(content)
                    if (manifest != null) {
                        manifests[manifest.id] = manifest
                        scriptFiles[manifest.id] = file
                        results.add(manifest)
                    } else {
                        Log.w(TAG, "Failed to parse manifest for: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading script: ${file.absolutePath}. Permission issue?", e)
                }
            }
            results
        }

    private fun parseManifest(content: String): ScriptManifest? {
        val tagName = "@" + "kanade_script"
        // Allow optional '*' and whitespace before the tag to support standard JSDoc multiline comments
        val regex = Regex("""/\*\*?.*?[*\s]+${Regex.escape(tagName)}\s*(.*?)\*/""", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(content)

        return match?.groupValues?.get(1)?.let { rawJson ->
            // Clean up possible leading '*' and whitespace from multi-line comments
            val cleanedJson =
                rawJson
                    .lines()
                    .map { it.trim().removePrefix("*").trim() }
                    .joinToString("\n")
                    .trim()

            try {
                json.decodeFromString<ScriptManifest>(cleanedJson)
            } catch (e: Exception) {
                Log.e(TAG, "JSON decoding failed for manifest: $cleanedJson", e)
                null
            }
        } ?: run {
            Log.w(TAG, "No $tagName block found in script")
            null
        }
    }

    suspend fun getEngine(scriptId: String): ScriptEngine? {
        val manifest = manifests[scriptId] ?: return null
        return engineTasks
            .getOrPut(scriptId) {
                scope.async {
                    try {
                        val engine = ScriptEngine()
                        val bridge = HostBridge(client)

                        engine.registerInterface("__kanade_http_bridge", HttpBridge::class.java, bridge)
                        engine.registerInterface("__kanade_log_bridge", LogBridge::class.java, bridge)
                        engine.registerInterface("__kanade_crypto_bridge", CryptoBridge::class.java, bridge)

                        val scriptFile = scriptFiles[scriptId]
                        if (scriptFile != null && scriptFile.exists()) {
                            engine.evaluate(scriptFile.readText(), scriptFile.name)

                            // Pass configuration
                            val configsJson = settingsRepository.scriptConfigsFlow.first()
                            val allConfigs =
                                configsJson?.let {
                                    try {
                                        Json.decodeFromString<Map<String, Map<String, String>>>(it)
                                    } catch (e: Exception) {
                                        emptyMap()
                                    }
                                } ?: emptyMap()

                            val scriptConfig = allConfigs[scriptId] ?: emptyMap()
                            engine.callInit(scriptConfig)
                        }
                        Log.d(TAG, "Engine for $scriptId is fully ready with config")
                        engine
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize engine for $scriptId", e)
                        null
                    }
                }
            }.await()
    }

    suspend fun importScript(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = getFileName(uri) ?: "imported_script_${System.currentTimeMillis()}.js"
                if (!fileName.endsWith(".js")) return@withContext Result.failure(Exception("Not a .js file"))

                val destFile = File(scriptDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Imported script to: ${destFile.absolutePath}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import script", e)
                Result.failure(e)
            }
        }

    suspend fun deleteScript(scriptId: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = scriptFiles[scriptId] ?: return@withContext false
            try {
                if (file.exists() && file.delete()) {
                    manifests.remove(scriptId)
                    scriptFiles.remove(scriptId)
                    engineTasks.remove(scriptId)
                    Log.d(TAG, "Deleted script: $scriptId")
                    true
                } else {
                    Log.e(TAG, "Failed to delete script file: ${file.absolutePath}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting script", e)
                false
            }
        }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name ?: uri.path?.split("/")?.last()
    }
}
