package org.parallel_sekai.kanade.data.source

import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings

object MusicUtils {
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun parseArtists(
        artistString: String?,
        settings: ArtistParsingSettings = ArtistParsingSettings(), // 提供默认值
    ): List<String> {
        if (artistString.isNullOrBlank()) return listOf("Unknown Artist")

        // 1. Placeholder replacement for whitelist items
        var tempString: String = artistString
        val placeholderMap = mutableMapOf<String, String>()
        settings.whitelist.forEachIndexed { index, name ->
            // 使用 settings.whitelist
            val placeholder = "__WHITELIST_${index}__"
            val regex = Regex(Regex.escape(name), RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(tempString)) {
                placeholderMap[placeholder] = name
                tempString = tempString.replace(regex, placeholder)
            }
        }

        // 2. Find the highest priority matching separator
        val usedSeparator =
            settings.separators.firstOrNull {
                tempString.contains(it, ignoreCase = true)
            } // 使用 settings.separators

        // 3. Execute splitting
        val parts =
            if (usedSeparator != null) {
                tempString.split(Regex(Regex.escape(usedSeparator), RegexOption.IGNORE_CASE))
            } else {
                listOf(tempString)
            }

        // 4. Restore whitelist items and clean up
        return parts
            .map { part ->
                var restored = part.trim()
                placeholderMap.forEach { (token, original) ->
                    restored = restored.replace(token, original)
                }
                restored
            }.filter { it.isNotBlank() && it.lowercase() != "unknown" }
            .distinct()
            .ifEmpty { listOf("Unknown Artist") }
    }
}
