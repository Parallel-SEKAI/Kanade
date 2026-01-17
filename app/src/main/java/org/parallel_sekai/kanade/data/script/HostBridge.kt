package org.parallel_sekai.kanade.data.script

import android.util.Log
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface HttpBridge {
    fun get(url: String, options: String?): String
    fun post(url: String, body: String, options: String?): String
}

interface CacheBridge {
    fun get(key: String): String?
    fun set(key: String, value: String, ttl: Int)
}

interface LogBridge {
    fun log(message: String?)
    fun error(message: String?)
    fun warn(message: String?)
}

class HostBridge(private val client: OkHttpClient) : HttpBridge, LogBridge {

    private val json = Json { ignoreUnknownKeys = true }

    override fun get(url: String, options: String?): String {
        Log.d("HostBridge", "GET: $url, options: $options")
        return try {
            val builder = Request.Builder().url(url)
            
            options?.let { parseOptions(it) }?.let { opt ->
                opt["headers"]?.jsonObject?.forEach { (k, v) ->
                    val value = v.jsonPrimitive.content
                    builder.addHeader(k, value)
                }
            }
            
            val request = builder.build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (!response.isSuccessful) {
                    Log.e("HostBridge", "GET failed with code ${response.code}: $url")
                }
                // Clean potential BOM or invisible characters
                sanitizeResponse(body)
            }
        } catch (e: Exception) {
            Log.e("HostBridge", "HTTP GET failed: $url", e)
            ""
        }
    }

    override fun post(url: String, body: String, options: String?): String {
        Log.d("HostBridge", "POST: $url, body length: ${body.length}, options: $options")
        return try {
            val builder = Request.Builder().url(url)
            var contentType = "text/plain"
            
            options?.let { parseOptions(it) }?.let { opt ->
                opt["headers"]?.jsonObject?.forEach { (k, v) ->
                    val value = v.jsonPrimitive.content
                    builder.addHeader(k, value)
                    if (k.equals("Content-Type", ignoreCase = true)) {
                        contentType = value
                    }
                }
                opt["contentType"]?.jsonPrimitive?.content?.let { ct ->
                    contentType = ct
                }
            }

            val requestBody = body.toRequestBody(contentType.toMediaTypeOrNull())
            val request = builder.post(requestBody).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.trim() ?: ""
                if (!response.isSuccessful) {
                    Log.e("HostBridge", "POST failed with code ${response.code}: $url")
                }
                sanitizeResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e("HostBridge", "HTTP POST failed: $url", e)
            ""
        }
    }

    private fun sanitizeResponse(content: String): String {
        if (content.isEmpty()) return ""
        // Remove UTF-8 BOM if present and trim whitespace/control characters
        return content.replace("\uFEFF", "").trim()
    }

    private fun parseOptions(options: String): JsonObject? {
        if (options.isBlank() || options == "[object Object]") {
            return null
        }
        return try {
            json.parseToJsonElement(options).jsonObject
        } catch (e: Exception) {
            Log.w("HostBridge", "Failed to parse options JSON: $options")
            null
        }
    }

    override fun log(message: String?) {
        Log.i("KanadeScript", message ?: "null")
    }

    override fun error(message: String?) {
        Log.e("KanadeScript", message ?: "null")
    }

    override fun warn(message: String?) {
        Log.w("KanadeScript", message ?: "null")
    }
}