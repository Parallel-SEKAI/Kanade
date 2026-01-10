package org.parallel_sekai.kanade.ui.screens.player

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object LyricUtils {
    /**
     * 解析时间戳，支持 [mm:ss.xx] 或 mm:ss.xxx 格式，兼容逗号分隔符
     */
    fun parseTimestamp(timeStr: String): Long {
        return try {
            val cleanTime = timeStr.trim()
                .removeSurrounding("[", "]")
                .removeSurrounding("<", ">")
                .replace(",", ".") // 兼容某些格式的逗号
            val parts = cleanTime.split(":")
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
    // 歌词行正则：必须以 [时间戳] 开头
    private val lineRegex = Regex("""^\[(\d{1,3}:\d{1,2}(?:\.\d+)?)\](.*)""")
    // 逐字标签正则
    private val wordRegex = Regex("""<(\d{1,3}:\d{1,2}(?:\.\d+)?)>([^<]*)""")
    // 标签正则：严格匹配整个括号内的内容，且 Key 必须是字母
    private val tagRegex = Regex("""^\[([a-zA-Z]+):(.+)\]$""")

    override fun parse(content: String): LyricData {
        val lines = mutableListOf<LyricLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null

        val rawLines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        android.util.Log.d("LyricDebug", "LrcParser starting with ${rawLines.size} lines")
        
        var i = 0
        while (i < rawLines.size) {
            val rawLine = rawLines[i]
            
            // 优先检查是否为歌词行 (lineRegex)
            val lineMatch = lineRegex.find(rawLine)
            if (lineMatch != null) {
                val timestampStr = lineMatch.groupValues[1]
                val timestamp = LyricUtils.parseTimestamp(timestampStr)
                val contentText = lineMatch.groupValues[2].trim()
                
                // 处理逐字 (Enhanced LRC)
                val words = mutableListOf<WordInfo>()
                val wordMatches = wordRegex.findAll(contentText).toList()
                wordMatches.forEachIndexed { index, wMatch ->
                    val start = LyricUtils.parseTimestamp(wMatch.groupValues[1])
                    val nextStart = if (index < wordMatches.size - 1) {
                        LyricUtils.parseTimestamp(wordMatches[index + 1].groupValues[1])
                    } else start + 500
                    words.add(WordInfo(wMatch.groupValues[2], start, nextStart))
                }

                val cleanText = if (words.isNotEmpty()) words.joinToString("") { it.text } else contentText
                
                // 检查翻译 (如果下一行时间戳完全一致)
                var translation: String? = null
                if (i + 1 < rawLines.size) {
                    val nextLineMatch = lineRegex.find(rawLines[i + 1])
                    if (nextLineMatch != null) {
                        val nextTimestamp = LyricUtils.parseTimestamp(nextLineMatch.groupValues[1])
                        if (nextTimestamp == timestamp) {
                            translation = nextLineMatch.groupValues[2].trim()
                            i++ // 消费掉翻译行
                        }
                    }
                }
                
                lines.add(LyricLine(startTime = timestamp, content = cleanText, translation = translation, words = words))
                i++
                continue
            }

            // 其次检查是否为标签 (tagRegex)
            val tagMatch = tagRegex.find(rawLine)
            if (tagMatch != null) {
                val key = tagMatch.groupValues[1].lowercase()
                val value = tagMatch.groupValues[2].trim()
                android.util.Log.d("LyricDebug", "Matched tag: $key -> $value")
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                }
                i++
                continue
            }

            android.util.Log.w("LyricDebug", "Line skipped (no match): $rawLine")
            i++
        }
        
        // 自动计算 endTime
        val processedLines = lines.mapIndexed { index, line ->
            val endTime = if (index < lines.size - 1) lines[index + 1].startTime else line.startTime + 5000
            line.copy(endTime = endTime)
        }

        android.util.Log.d("LyricDebug", "LrcParser finished. Total lines added: ${processedLines.size}")
        return LyricData(title, artist, album, processedLines)
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
                            translation = try { parser.nextText() } catch(e: Exception) { null }
                        } else if (begin != null) {
                            val startTime = LyricUtils.parseTimestamp(begin)
                            val endTime = LyricUtils.parseTimestamp(end ?: begin)
                            val text = try { parser.nextText() } catch(e: Exception) { "" }
                            currentWords.add(WordInfo(text, startTime, endTime))
                            lineContent += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "p" && currentLine != null) {
                        lines.add(currentLine.copy(
                            content = lineContent.ifBlank { "..." },
                            translation = translation,
                            words = currentWords.toList()
                        ))
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