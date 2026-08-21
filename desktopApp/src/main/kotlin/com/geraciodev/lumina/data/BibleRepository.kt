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

    /**
     * Resuelve la ruta del archivo de la Biblia buscando en:
     * 1. El directorio de appResources de la distribución nativa
     *    (propiedad del sistema "compose.application.resources.dir")
     * 2. El working directory (útil en desarrollo con `./gradlew run`)
     * 3. El directorio del JAR ejecutable (fallback)
     */
    private fun resolveFile(fileName: String): File {
        // Ruta en distribución nativa (Compose Desktop packaged app)
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val file = File(resourcesDir, fileName)
            if (file.exists()) return file
        }

        // Ruta relativa al working directory (desarrollo con `./gradlew run`)
        val cwdFile = File(fileName)
        if (cwdFile.exists()) return cwdFile

        // Ruta relativa al directorio del JAR (fallback)
        val jarLocation = BibleRepository::class.java.protectionDomain?.codeSource?.location
        if (jarLocation != null) {
            val jarDir = File(jarLocation.toURI()).parentFile
            val jarRelativeFile = File(jarDir, fileName)
            if (jarRelativeFile.exists()) return jarRelativeFile
        }

        return cwdFile // devuelve aunque no exista, para log correcto
    }

    fun loadBible(fileName: String): BibleVersion? {
        return try {
            val file = resolveFile(fileName)
            if (file.exists()) {
                println("Loading Bible from: ${file.absolutePath}")
                val content = file.readText()
                json.decodeFromString<BibleVersion>(content)
            } else {
                println("Bible file not found. Searched in:")
                println("  - compose.application.resources.dir: ${System.getProperty("compose.application.resources.dir")}")
                println("  - Working directory: ${File(".").absolutePath}")
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
