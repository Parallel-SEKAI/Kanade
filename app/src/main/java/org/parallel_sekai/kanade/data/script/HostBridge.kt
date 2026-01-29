package org.parallel_sekai.kanade.data.script

import android.util.Log
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import org.parallel_sekai.kanade.data.source.MusicUtils

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

interface CryptoBridge {
    fun md5(text: String): String
    fun aesEncrypt(text: String, key: String, mode: String, padding: String): String
    fun aesDecrypt(hex: String, key: String, mode: String, padding: String): String
}

class HostBridge(private val client: OkHttpClient) :
    HttpBridge,
    LogBridge,
    CryptoBridge {

    private val json = Json { ignoreUnknownKeys = true }
    private val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override fun md5(text: String): String = try {
        val md = java.security.MessageDigest.getInstance("MD5")
        val bytes = md.digest(text.toByteArray())
        MusicUtils.bytesToHex(bytes)
    } catch (e: Exception) {
        Log.e("HostBridge", "MD5 failed", e)
        ""
    }

    override fun aesEncrypt(text: String, key: String, mode: String, padding: String): String {
        val transformation = "AES/$mode/$padding"
        return try {
            val cipher = javax.crypto.Cipher.getInstance(transformation)
            val skey = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, skey)
            val encrypted = cipher.doFinal(text.toByteArray())
            MusicUtils.bytesToHex(encrypted)
        } catch (e: Exception) {
            Log.e("HostBridge", "AES Encrypt failed: $transformation", e)
            ""
        }
    }

    override fun aesDecrypt(hex: String, key: String, mode: String, padding: String): String {
        val transformation = "AES/$mode/$padding"
        return try {
            val cipher = javax.crypto.Cipher.getInstance(transformation)
            val skey = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, skey)

            val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val decrypted = cipher.doFinal(bytes)
            String(decrypted)
        } catch (e: Exception) {
            Log.e("HostBridge", "AES Decrypt failed: $transformation", e)
            ""
        }
    }

    override fun get(url: String, options: String?): String {
        val startTime = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url)
            builder.addHeader("User-Agent", DEFAULT_UA) // Set default UA
            
            var responseType = "text"

            options?.let { parseOptions(it) }?.let { opt ->
                opt["headers"]?.jsonObject?.forEach { (k, v) ->
                    val value = v.jsonPrimitive.content
                    builder.header(k, value) // Use header() to override default UA if provided
                }
                opt["responseType"]?.jsonPrimitive?.content?.let {
                    responseType = it
                }
            }

            val request = builder.build()
            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    Log.e("HostBridge", "GET failed (${response.code}) in ${duration}ms: $url")
                } else {
                    Log.d("HostBridge", "GET success (${response.code}) in ${duration}ms: $url")
                }

                if (responseType == "hex") {
                    response.body?.bytes()?.let { MusicUtils.bytesToHex(it) } ?: ""
                } else {
                    val body = response.body?.string() ?: ""
                    sanitizeResponse(body)
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("HostBridge", "HTTP GET error after ${duration}ms: $url", e)
            ""
        }
    }

    override fun post(url: String, body: String, options: String?): String {
        val startTime = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url)
            builder.addHeader("User-Agent", DEFAULT_UA)
            
            var contentType = "text/plain"
            var responseType = "text"

            options?.let { parseOptions(it) }?.let { opt ->
                opt["headers"]?.jsonObject?.forEach { (k, v) ->
                    val value = v.jsonPrimitive.content
                    builder.header(k, value)
                    if (k.equals("Content-Type", ignoreCase = true)) {
                        contentType = value
                    }
                }
                opt["contentType"]?.jsonPrimitive?.content?.let { ct ->
                    contentType = ct
                }
                opt["responseType"]?.jsonPrimitive?.content?.let {
                    responseType = it
                }
            }

            val requestBody = body.toRequestBody(contentType.toMediaTypeOrNull())
            val request = builder.post(requestBody).build()
            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    Log.e("HostBridge", "POST failed (${response.code}) in ${duration}ms: $url")
                } else {
                    Log.d("HostBridge", "POST success (${response.code}) in ${duration}ms: $url")
                }

                if (responseType == "hex") {
                    response.body?.bytes()?.let { MusicUtils.bytesToHex(it) } ?: ""
                } else {
                    val responseBody = response.body?.string() ?: ""
                    sanitizeResponse(responseBody)
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("HostBridge", "HTTP POST error after ${duration}ms: $url", e)
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
