package org.parallel_sekai.kanade.data.utils

import org.parallel_sekai.kanade.data.model.WordInfo
import kotlin.math.abs

data class SplitResult(
    val line1: String,
    val line2: String,
    val words1: List<WordInfo>?,
    val words2: List<WordInfo>?,
    val splitIndex: Int
)

object LyricSplitter {
    private const val REMOVABLE_PUNCTUATION = "，, 　;；、"
    private const val STICKY_PUNCTUATION = "。？！…!?." 
    private const val FORBIDDEN_START_PUNCTUATION = "）)》」”'"
    private const val FORBIDDEN_END_PUNCTUATION = "（(《「“'"

    fun findBestSplit(content: String, words: List<WordInfo>?): SplitResult? {
        if (content.length < 6) return null // 降低阈值，更短的行如果字号大也可能需要平衡

        val center = content.length / 2
        // 限制在中心 60% 区域寻找切分点
        val rangeStart = (content.length * 0.2).toInt()
        val rangeEnd = (content.length * 0.8).toInt()

        var bestIndex = -1
        var maxScore = Int.MIN_VALUE

        // 识别原子边界
        val atoms = identifyAtoms(content, words)
        
        // 遍历原子之间的空隙作为切分点
        for (i in 1 until atoms.size) {
            val charIndex = atoms[i].startIndex
            if (charIndex !in rangeStart..rangeEnd) continue

            var score = 0
            
            // 1. 距离中心位置分 (0-100)
            val distanceFactor = 1.0f - (abs(charIndex - center).toFloat() / center)
            score += (distanceFactor * 100).toInt()

            // 2. 标点符号分
            val prevChar = content[charIndex - 1]
            val currentChar = content[charIndex]

            when {
                REMOVABLE_PUNCTUATION.contains(prevChar) -> score += 150
                STICKY_PUNCTUATION.contains(prevChar) -> score += 100
                FORBIDDEN_START_PUNCTUATION.contains(currentChar) -> score -= 1000
                FORBIDDEN_END_PUNCTUATION.contains(prevChar) -> score -= 1000
            }

            // 3. 时间停顿分 (仅限逐字模式)
            if (words != null) {
                val prevAtom = atoms[i-1]
                val currentAtom = atoms[i]
                if (prevAtom.wordInfo != null && currentAtom.wordInfo != null) {
                    val gap = currentAtom.wordInfo.startTime - prevAtom.wordInfo.endTime
                    if (gap > 300) score += 200
                    if (gap > 600) score += 300
                    if (gap < 0) score -= 50
                }
            }

            if (score > maxScore) {
                maxScore = score
                bestIndex = charIndex
            }
        }

        if (bestIndex == -1) return null

        // 执行切分
        var line1 = content.substring(0, bestIndex)
        var line2 = content.substring(bestIndex)

        if (line1.isNotEmpty() && REMOVABLE_PUNCTUATION.contains(line1.last())) {
            line1 = line1.substring(0, line1.length - 1)
        }
        if (line2.startsWith(" ") || line2.startsWith("　")) {
            line2 = line2.substring(1)
        }

        val words1: MutableList<WordInfo>? = if (words != null) mutableListOf() else null
        val words2: MutableList<WordInfo>? = if (words != null) mutableListOf() else null

        if (words != null) {
            var currentPos = 0
            words.forEach { word ->
                if (currentPos + word.text.length <= bestIndex) {
                    words1?.add(word)
                } else if (currentPos >= bestIndex) {
                    words2?.add(word)
                } else {
                    words1?.add(word)
                }
                currentPos += word.text.length
            }
        }

        return SplitResult(line1, line2, words1, words2, bestIndex)
    }

    private data class Atom(
        val startIndex: Int,
        val text: String,
        val wordInfo: WordInfo? = null
    )

    private fun identifyAtoms(content: String, words: List<WordInfo>?): List<Atom> {
        val atoms = mutableListOf<Atom>()
        if (words != null && words.isNotEmpty()) {
            var charIdx = 0
            var wordIdx = 0
            while (charIdx < content.length) {
                if (wordIdx < words.size) {
                    val word = words[wordIdx]
                    val foundIdx = content.indexOf(word.text, charIdx)
                    if (foundIdx != -1) {
                        if (foundIdx > charIdx) {
                            val gapText = content.substring(charIdx, foundIdx)
                            gapText.forEachIndexed { i, c ->
                                atoms.add(Atom(charIdx + i, c.toString()))
                            }
                        }
                        atoms.add(Atom(foundIdx, word.text, word))
                        charIdx = foundIdx + word.text.length
                        wordIdx++
                    } else {
                        atoms.add(Atom(charIdx, content[charIdx].toString()))
                        charIdx++
                    }
                } else {
                    atoms.add(Atom(charIdx, content[charIdx].toString()))
                    charIdx++
                }
            }
        } else {
            var i = 0
            while (i < content.length) {
                val char = content[i]
                if (char.isLetterOrDigit() && char.code < 128) {
                    val start = i
                    while (i < content.length && content[i].isLetterOrDigit() && content[i].code < 128) {
                        i++
                    }
                    atoms.add(Atom(start, content.substring(start, i)))
                } else {
                    atoms.add(Atom(i, char.toString()))
                    i++
                }
            }
        }
        return atoms
    }
}
