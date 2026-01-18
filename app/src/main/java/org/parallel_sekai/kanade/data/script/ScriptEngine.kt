package org.parallel_sekai.kanade.data.script

import android.util.Log
import app.cash.quickjs.QuickJs
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.Executors

class ScriptEngine : Closeable {
    private val TAG = "ScriptEngine"
    
    // Create a single thread with a larger stack size (2MB) for QuickJS
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(null, runnable, "quickjs-worker", 2 * 1024 * 1024)
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(executor + SupervisorJob())
    private var quickJs: QuickJs? = null
    private val callbacks = mutableMapOf<String, CompletableDeferred<String>>()
    private val engineReady = CompletableDeferred<Unit>()

    interface PromiseBridge {
        fun resolve(id: String, result: String?)
        fun reject(id: String, error: String?)
    }

    init {
        scope.launch {
            try {
                val js = QuickJs.create()
                quickJs = js
                
                js.set("__kanade_bridge", PromiseBridge::class.java, object : PromiseBridge {
                    override fun resolve(id: String, result: String?) {
                        Log.d(TAG, "Bridge resolved: $id")
                        callbacks[id]?.complete(result ?: "null")
                    }

                    override fun reject(id: String, error: String?) {
                        Log.e(TAG, "Bridge rejected: $id, error: $error")
                        callbacks[id]?.completeExceptionally(Exception(error ?: "Unknown JS Error"))
                    }
                })

                // Use a more compatible way to find the global object and handle undefined results
                js.evaluate("""
                    (function() {
                        const global = (typeof globalThis !== 'undefined') ? globalThis : 
                                     (typeof window !== 'undefined') ? window : 
                                     (typeof self !== 'undefined') ? self : this;
                        
                        // Wrapper for http bridge to handle object to JSON conversion
                        global.http = {
                            get: function(url, options) {
                                return __kanade_http_bridge.get(url, options ? JSON.stringify(options) : null);
                            },
                            post: function(url, body, options) {
                                return __kanade_http_bridge.post(url, body, options ? JSON.stringify(options) : null);
                            }
                        };

                        global.crypto = {
                            md5: function(text) {
                                return __kanade_crypto_bridge.md5(text);
                            },
                            aesEncrypt: function(text, key, mode, padding) {
                                return __kanade_crypto_bridge.aesEncrypt(text, key, mode, padding);
                            },
                            aesDecrypt: function(hex, key, mode, padding) {
                                return __kanade_crypto_bridge.aesDecrypt(hex, key, mode, padding);
                            }
                        };
                        
                        // Wrapper for console bridge
                        global.console = {
                            log: function(...args) {
                                const msg = args.map(arg => {
                                    if (typeof arg === 'object') {
                                        try { return JSON.stringify(arg); } catch (e) { return String(arg); }
                                    }
                                    return String(arg);
                                }).join(' ');
                                __kanade_log_bridge.log(msg);
                            },
                            info: function(...args) { this.log(...args); },
                            debug: function(...args) { this.log(...args); },
                            error: function(...args) {
                                const msg = args.map(arg => {
                                    if (typeof arg === 'object') {
                                        try { return JSON.stringify(arg); } catch (e) { return String(arg); }
                                    }
                                    return String(arg);
                                }).join(' ');
                                __kanade_log_bridge.error(msg);
                            },
                            warn: function(...args) {
                                const msg = args.map(arg => {
                                    if (typeof arg === 'object') {
                                        try { return JSON.stringify(arg); } catch (e) { return String(arg); }
                                    }
                                    return String(arg);
                                }).join(' ');
                                __kanade_log_bridge.warn(msg);
                            }
                        };

                        global.__kanade_call_async = function(objectName, methodName, args, callbackId) {
                            try {
                                const target = objectName ? global[objectName] : global;
                                const func = target[methodName];
                                if (typeof func !== 'function') {
                                    __kanade_bridge.reject(callbackId, methodName + ' is not a function');
                                    return;
                                }
                                
                                const result = func.apply(target, args);
                                
                                if (result instanceof Promise) {
                                    result.then(res => {
                                        const stringified = JSON.stringify(res);
                                        __kanade_bridge.resolve(callbackId, typeof stringified === 'string' ? stringified : "null");
                                    }).catch(err => {
                                        __kanade_bridge.reject(callbackId, err ? (err.stack || err.toString()) : 'Unknown async error');
                                    });
                                } else {
                                    const stringified = JSON.stringify(result);
                                    __kanade_bridge.resolve(callbackId, typeof stringified === 'string' ? stringified : "null");
                                }
                            } catch (e) {
                                __kanade_bridge.reject(callbackId, e.stack || e.toString());
                            }
                        };
                    })();
                """.trimIndent())
                
                engineReady.complete(Unit)
                Log.d(TAG, "QuickJS Engine initialized on dedicated thread")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize QuickJS Engine", e)
                engineReady.completeExceptionally(e)
            }
        }
    }

    suspend fun <T : Any> registerInterface(name: String, type: Class<T>, instance: T) {
        engineReady.await()
        withContext(executor) {
            quickJs?.set(name, type, instance)
        }
    }

    suspend fun evaluate(script: String, fileName: String = "script.js") : Any? {
        engineReady.await()
        return withContext(executor) {
            quickJs?.evaluate(script, fileName)
        }
    }

    suspend fun callInit(config: Map<String, String>) {
        // Pass the map directly, callAsync will use toJson to convert it to a JS object
        callAsync(null, "init", config)
    }

    suspend fun callAsync(objectName: String?, methodName: String, vararg args: Any?): String {
        engineReady.await()
        return withContext(executor) {
            val callbackId = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<String>()
            callbacks[callbackId] = deferred

            val argsJson = args.joinToString(", ") { toJson(it) }
            val callScript = "__kanade_call_async(${if (objectName != null) "'$objectName'" else "null"}, '$methodName', [$argsJson], '$callbackId')"
            
            try {
                quickJs?.evaluate(callScript)
            } catch (e: Exception) {
                callbacks.remove(callbackId)
                throw e
            }

            try {
                withTimeout(15000) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                callbacks.remove(callbackId)
                throw Exception("Script call timed out: $methodName")
            } finally {
                callbacks.remove(callbackId)
            }
        }
    }

    private fun toJson(arg: Any?): String {
        return when (arg) {
            null -> "null"
            is String -> Json.encodeToString(arg)
            is Number, is Boolean -> arg.toString()
            is Map<*, *> -> {
                try {
                    // Convert Map to JSON object string
                    val entries = arg.entries.joinToString(", ") { entry ->
                        val k = entry.key.toString()
                        val v = entry.value
                        "\"$k\": ${toJson(v)}"
                    }
                    "{$entries}"
                } catch (e: Exception) {
                    "{}"
                }
            }
            is Iterable<*> -> {
                val items = arg.joinToString(", ") { toJson(it) }
                "[$items]"
            }
            is Array<*> -> {
                val items = arg.joinToString(", ") { toJson(it) }
                "[$items]"
            }
            else -> try {
                Json.encodeToString(arg.toString())
            } catch (e: Exception) {
                "null"
            }
        }
    }

    override fun close() {
        scope.cancel()
        Executors.newSingleThreadExecutor().execute {
            try {
                quickJs?.close()
                quickJs = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
