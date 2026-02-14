package org.parallel_sekai.kanade.data.parser

import android.util.Xml
import org.parallel_sekai.kanade.data.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object LyricUtils {
    /**
     * 解析时间戳，支持 [mm:ss.xx]、[mm:ss:xx] 或 mm:ss.xxx 格式，兼容逗号分隔符
     */
    fun parseTimestamp(timeStr: String): Long {
        return try {
            val cleanTime =
                timeStr
                    .trim()
                    .removeSurrounding("[", "]")
                    .removeSurrounding("<", ">")
                    .replace(",", ".") // 兼容某些格式的逗号

            // 针对 [mm:ss:xx] 这种乱格式，如果存在两个冒号，将最后一个冒号替换为点
            val lastColonIndex = cleanTime.lastIndexOf(':')
            val firstColonIndex = cleanTime.indexOf(':')
            val normalizedTime =
                if (lastColonIndex > firstColonIndex && lastColonIndex != -1) {
                    cleanTime.substring(0, lastColonIndex) + "." + cleanTime.substring(lastColonIndex + 1)
                } else {
                    cleanTime
                }

            val parts = normalizedTime.split(":")
            if (parts.size < 2) return 0L

            val minutes = parts[0].toLong()
            val seconds = parts[1].toDouble()
            (minutes * 60 * 1000 + seconds * 1000).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

interface LyricParser {
    fun parse(content: String): LyricData
}

/**
 * LRC 解析器：支持标准、翻译（同时间戳行）和逐字（Enhanced LRC）
 */
class LrcParser : LyricParser {
    // Matches one or more [mm:ss.xx] or [mm:ss:xx] tags at the beginning of a line
    private val timeTagsRegex = Regex("""^((?:\[\d{1,3}:\d{1,2}(?:[:.]\d+)?\])+)(.*)""")

    // Individual time tag extractor, supports both . and : for milliseconds
    private val singleTagRegex = Regex("""\[(\d{1,3}:\d{1,2}(?:[:.]\d+)?)\]""")

    // Enhanced LRC word tags
    private val wordRegex = Regex("""<(\d{1,3}:\d{1,2}(?:[:.]\d+)?)>([^<]*)""")

    // Metadata tags: [key:value]
    private val tagRegex = Regex("""^\[([a-zA-Z]+):(.+)\]$""")

    override fun parse(content: String): LyricData {
        val allLyricEntries = mutableListOf<RawLyricEntry>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null

        val rawLines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        for (rawLine in rawLines) {
            // 1. Check for time-tagged lyric lines
            val timeMatch = timeTagsRegex.find(rawLine)
            if (timeMatch != null) {
                val tagsPart = timeMatch.groupValues[1]
                val textPart = timeMatch.groupValues[2].trim()

                // Parse word-by-word info if present
                val words = mutableListOf<WordInfo>()
                val wordMatches = wordRegex.findAll(textPart).toList()
                wordMatches.forEachIndexed { index, wMatch ->
                    val start = LyricUtils.parseTimestamp(wMatch.groupValues[1])
                    val nextStart =
                        if (index < wordMatches.size - 1) {
                            LyricUtils.parseTimestamp(wordMatches[index + 1].groupValues[1])
                        } else {
                            start + 500
                        }
                    words.add(WordInfo(wMatch.groupValues[2], start, nextStart))
                }
                val cleanText = if (words.isNotEmpty()) words.joinToString("") { it.text } else textPart

                // Extract all timestamps from the tags part
                singleTagRegex.findAll(tagsPart).forEach { tag ->
                    val timestamp = LyricUtils.parseTimestamp(tag.groupValues[1])
                    allLyricEntries.add(RawLyricEntry(timestamp, cleanText, words))
                }
                continue
            }

            // 2. Check for metadata tags
            val tagMatch = tagRegex.find(rawLine)
            if (tagMatch != null) {
                val key = tagMatch.groupValues[1].lowercase()
                val value = tagMatch.groupValues[2].trim()
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                }
            }
        }

        // 3. Sort by timestamp and merge entries with identical timestamps
        val sortedEntries = allLyricEntries.sortedBy { it.timestamp }
        val mergedLines = mutableListOf<LyricLine>()

        var current: RawLyricEntry? = null
        for (entry in sortedEntries) {
            if (current == null) {
                current = entry
            } else if (current.timestamp == entry.timestamp) {
                // Merge as translation if text is different
                if (current.text != entry.text) {
                    current =
                        if (current.translation == null) {
                            current.copy(translation = entry.text)
                        } else if (!current.translation.contains(entry.text)) {
                            current.copy(translation = current.translation + "\n" + entry.text)
                        } else {
                            current
                        }
                }
            } else {
                mergedLines.add(current.toLyricLine())
                current = entry
            }
        }
        current?.let { mergedLines.add(it.toLyricLine()) }

        // 4. Calculate endTimes
        val processedLines =
            mergedLines.mapIndexed { index, line ->
                val endTime =
                    if (index < mergedLines.size - 1) {
                        mergedLines[index + 1].startTime
                    } else {
                        line.startTime + 5000
                    }
                line.copy(endTime = endTime)
            }

        return LyricData(title, artist, album, processedLines)
    }

    private data class RawLyricEntry(
        val timestamp: Long,
        val text: String,
        val words: List<WordInfo> = emptyList(),
        val translation: String? = null,
    ) {
        fun toLyricLine() =
            LyricLine(
                startTime = timestamp,
                endTime = 0, // Placeholder
                content = text,
                translation = translation,
                words = words,
            )
    }
}

/**
 * TTML 解析器：支持 Apple 风格逐字和翻译
 */
class TtmlParser : LyricParser {
    override fun parse(content: String): LyricData {
        val lines = mutableListOf<LyricLine>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(content))

        var eventType = parser.eventType
        var currentLine: LyricLine? = null
        val currentWords = mutableListOf<WordInfo>()
        var lineContent = ""
        var translation: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "p") {
                        val begin = LyricUtils.parseTimestamp(parser.getAttributeValue(null, "begin") ?: "0:0.0")
                        val end = LyricUtils.parseTimestamp(parser.getAttributeValue(null, "end") ?: "0:0.0")
                        currentLine = LyricLine(startTime = begin, endTime = end, content = "")
                        currentWords.clear()
                        lineContent = ""
                        translation = null
                    } else if (tagName == "span") {
                        val role = parser.getAttributeValue(null, "ttm:role")
                        val begin = parser.getAttributeValue(null, "begin")
                        val end = parser.getAttributeValue(null, "end")

                        if (role == "x-translation") {
                            translation =
                                try {
                                    parser.nextText()
                                } catch (e: Exception) {
                                    null
                                }
                        } else if (begin != null) {
                            val startTime = LyricUtils.parseTimestamp(begin)
                            val endTime = LyricUtils.parseTimestamp(end ?: begin)
                            val text =
                                try {
                                    parser.nextText()
                                } catch (e: Exception) {
                                    ""
                                }
                            currentWords.add(WordInfo(text, startTime, endTime))
                            lineContent += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "p" && currentLine != null) {
                        lines.add(
                            currentLine.copy(
                                content = lineContent.ifBlank { "..." },
                                translation = translation,
                                words = currentWords.toList(),
                            ),
                        )
                        currentLine = null
                    }
                }
            }
            eventType = parser.next()
        }
        return LyricData(lines = lines)
    }
}

object LyricParserFactory {
    fun getParser(content: String): LyricParser {
        val trimmed = content.trim()
        return if (trimmed.startsWith("<?xml") || trimmed.startsWith("<tt") || trimmed.contains("<body")) {
            TtmlParser()
        } else {
            LrcParser()
        }
    }
}
