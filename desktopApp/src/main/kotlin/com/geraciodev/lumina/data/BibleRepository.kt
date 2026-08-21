package com.geraciodev.lumina.data

import com.geraciodev.lumina.data.model.bible.BibleVersion
import com.geraciodev.lumina.util.normalize
import kotlinx.serialization.json.Json
import java.io.File

class BibleRepository {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    fun loadBible(filePath: String): BibleVersion? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val content = file.readText()
                json.decodeFromString<BibleVersion>(content)
            } else {
                println("Bible file not found at: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            println("Error loading Bible: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun search(bible: BibleVersion, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.length < 3) return results

        val normalizedQuery = query.normalize()

        for (book in bible.books) {
            for (chapter in book.chapters) {
                for (item in chapter.items) {
                    if (item.type == "verse") {
                        val text = item.lines.joinToString(" ")
                        val normalizedText = text.normalize()
                        
                        if (normalizedText.contains(normalizedQuery)) {
                            results.add(
                                SearchResult(
                                    bookName = book.name,
                                    chapterReference = chapter.current.human,
                                    verseNumbers = item.verse_numbers,
                                    text = text,
                                    bookUsfm = book.book_usfm,
                                    chapterUsfm = chapter.chapter_usfm
                                )
                            )
                        }
                    }
                }
            }
        }
        return results
    }

    data class SearchResult(
        val bookName: String,
        val chapterReference: String,
        val verseNumbers: List<Int>,
        val text: String,
        val bookUsfm: String,
        val chapterUsfm: String
    )
}
