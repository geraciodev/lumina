package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.BibleRepository
import com.geraciodev.lumina.data.model.bible.BibleBook
import com.geraciodev.lumina.data.model.bible.BibleChapter
import com.geraciodev.lumina.data.model.bible.BibleItem
import com.geraciodev.lumina.data.model.bible.BibleVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Navegación, búsqueda y proyección de versículos bíblicos.
 *
 * [onProjectionStarted] se invoca cada vez que se empieza a proyectar un versículo, para que
 * quien componga este ViewModel (ver [MainViewModel]) pueda detener una proyección de video
 * activa en [PlayerViewModel] sin que ambas clases se conozcan entre sí.
 */
class BibleViewModel(
    private val bibleRepository: BibleRepository,
    private val scope: CoroutineScope,
    private val onProjectionStarted: () -> Unit = {}
) {
    var bibleVersion by mutableStateOf<BibleVersion?>(null)
        private set
    var selectedBibleBook by mutableStateOf<BibleBook?>(null)
        private set
    var selectedBibleChapter by mutableStateOf<BibleChapter?>(null)
        private set
    val selectedVerses = mutableStateListOf<Int>()
    var bibleSearchQuery by mutableStateOf("")
        private set
    var bibleSearchResults by mutableStateOf(emptyList<BibleRepository.SearchResult>())
        private set
    var isBibleSearching by mutableStateOf(false)
        private set
    var scrollToVerseIndex by mutableStateOf<Int?>(null)
    var projectingBibleVerses by mutableStateOf<List<BibleItem>?>(null)
        private set
    var projectingBibleRef by mutableStateOf("")
        private set

    private var bibleSearchJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
            val bible = bibleRepository.loadBible("RVR1960_vid_149.json")
            withContext(Dispatchers.Main) {
                bibleVersion = bible
                selectedBibleBook = bible?.books?.firstOrNull()
                selectedBibleChapter = selectedBibleBook?.chapters?.firstOrNull()
            }
        }
    }

    fun selectBibleBook(book: BibleBook) {
        selectedBibleBook = book
        selectedBibleChapter = book.chapters.firstOrNull()
        selectedVerses.clear()
    }

    fun selectBibleChapter(chapter: BibleChapter) {
        selectedBibleChapter = chapter
        selectedVerses.clear()
    }

    fun nextBibleChapter() {
        val book = selectedBibleBook ?: return
        val currentChapter = selectedBibleChapter ?: return
        val currentIndex = book.chapters.indexOf(currentChapter)

        if (currentIndex < book.chapters.size - 1) {
            selectBibleChapter(book.chapters[currentIndex + 1])
        } else {
            // Ir al siguiente libro
            val books = bibleVersion?.books ?: return
            val bookIndex = books.indexOf(book)
            if (bookIndex < books.size - 1) {
                selectBibleBook(books[bookIndex + 1])
            }
        }
    }

    fun previousBibleChapter() {
        val book = selectedBibleBook ?: return
        val currentChapter = selectedBibleChapter ?: return
        val currentIndex = book.chapters.indexOf(currentChapter)

        if (currentIndex > 0) {
            selectBibleChapter(book.chapters[currentIndex - 1])
        } else {
            // Ir al libro anterior
            val books = bibleVersion?.books ?: return
            val bookIndex = books.indexOf(book)
            if (bookIndex > 0) {
                val prevBook = books[bookIndex - 1]
                selectBibleBook(prevBook)
                // Ir al último capítulo del libro anterior
                selectBibleChapter(prevBook.chapters.last())
            }
        }
    }

    fun nextBibleBook() {
        val books = bibleVersion?.books ?: return
        val currentBook = selectedBibleBook ?: return
        val currentIndex = books.indexOf(currentBook)

        if (currentIndex < books.size - 1) {
            selectBibleBook(books[currentIndex + 1])
        }
    }

    fun previousBibleBook() {
        val books = bibleVersion?.books ?: return
        val currentBook = selectedBibleBook ?: return
        val currentIndex = books.indexOf(currentBook)

        if (currentIndex > 0) {
            selectBibleBook(books[currentIndex - 1])
        }
    }

    fun toggleVerseSelection(verseNumber: Int) {
        if (selectedVerses.contains(verseNumber)) {
            selectedVerses.remove(verseNumber)
        } else {
            selectedVerses.add(verseNumber)
            selectedVerses.sort()
        }
    }

    fun projectSelectedVerses() {
        val chapter = selectedBibleChapter ?: return
        if (selectedVerses.isEmpty()) return

        val versesToProject = chapter.items.filter { item ->
            item.type == "verse" && item.verse_numbers.any { it in selectedVerses }
        }

        projectingBibleVerses = versesToProject
        val chapterNum = chapter.current.human.split(" ").last()
        projectingBibleRef = "${selectedBibleBook?.name} $chapterNum:${formatVerseRange(selectedVerses)}"
        onProjectionStarted()
    }

    private fun formatVerseRange(verses: List<Int>): String {
        if (verses.isEmpty()) return ""
        val sorted = verses.sorted()
        val groups = mutableListOf<Pair<Int, Int>>()

        var start = sorted[0]
        var end = sorted[0]

        for (i in 1 until sorted.size) {
            if (sorted[i] == end + 1) {
                end = sorted[i]
            } else {
                groups.add(start to end)
                start = sorted[i]
                end = sorted[i]
            }
        }
        groups.add(start to end)

        return groups.joinToString(", ") { (s, e) ->
            if (s == e) "$s" else "$s-$e"
        }
    }

    fun stopBibleProjection() {
        projectingBibleVerses = null
        projectingBibleRef = ""
    }

    fun onBibleSearchQueryChanged(query: String) {
        bibleSearchQuery = query
        bibleSearchJob?.cancel()

        if (query.length < 3) {
            bibleSearchResults = emptyList()
            isBibleSearching = false
            return
        }

        bibleSearchJob = scope.launch(Dispatchers.Default) {
            delay(300)
            isBibleSearching = true
            val version = bibleVersion
            if (version != null) {
                val results = bibleRepository.search(version, query)
                withContext(Dispatchers.Main) {
                    bibleSearchResults = results
                }
            }
            isBibleSearching = false
        }
    }

    fun navigateToSearchResult(result: BibleRepository.SearchResult) {
        val book = bibleVersion?.books?.find { it.book_usfm == result.bookUsfm }
        if (book != null) {
            selectedBibleBook = book
            val chapter = book.chapters.find { it.chapter_usfm == result.chapterUsfm }
            if (chapter != null) {
                selectedBibleChapter = chapter
                selectedVerses.clear()
                selectedVerses.addAll(result.verseNumbers)
                syncScrollToSelected()
            }
        }
        // Limpiar búsqueda al navegar
        bibleSearchQuery = ""
        bibleSearchResults = emptyList()
    }

    private fun syncScrollToSelected() {
        val chapter = selectedBibleChapter ?: return
        val firstVerse = selectedVerses.firstOrNull() ?: return
        val index = chapter.items.indexOfFirst {
            it.type == "verse" && it.verse_numbers.contains(firstVerse)
        }
        if (index != -1) {
            scrollToVerseIndex = index + 1 // +1 por el header del capítulo
        }
    }

    fun projectNextVerse() {
        val chapter = selectedBibleChapter ?: return
        val currentVerses = selectedVerses.toList()
        if (currentVerses.isEmpty()) return

        val lastVerse = currentVerses.last()
        val allVerses = chapter.items.filter { it.type == "verse" }
        val currentIndex = allVerses.indexOfFirst { it.verse_numbers.contains(lastVerse) }

        if (currentIndex != -1 && currentIndex < allVerses.size - 1) {
            // Siguiente versículo en el mismo capítulo
            val nextVerseItem = allVerses[currentIndex + 1]
            selectedVerses.clear()
            selectedVerses.addAll(nextVerseItem.verse_numbers)
            projectSelectedVerses()
            syncScrollToSelected()
        } else {
            // Siguiente capítulo
            nextBibleChapter()
            val newChapter = selectedBibleChapter ?: return
            val firstVerseItem = newChapter.items.firstOrNull { it.type == "verse" }
            if (firstVerseItem != null) {
                selectedVerses.clear()
                selectedVerses.addAll(firstVerseItem.verse_numbers)
                projectSelectedVerses()
                syncScrollToSelected()
            }
        }
    }

    fun projectPreviousVerse() {
        val chapter = selectedBibleChapter ?: return
        val currentVerses = selectedVerses.toList()
        if (currentVerses.isEmpty()) return

        val firstVerse = currentVerses.first()
        val allVerses = chapter.items.filter { it.type == "verse" }
        val currentIndex = allVerses.indexOfFirst { it.verse_numbers.contains(firstVerse) }

        if (currentIndex > 0) {
            // Versículo anterior en el mismo capítulo
            val prevVerseItem = allVerses[currentIndex - 1]
            selectedVerses.clear()
            selectedVerses.addAll(prevVerseItem.verse_numbers)
            projectSelectedVerses()
            syncScrollToSelected()
        } else {
            // Capítulo anterior
            previousBibleChapter()
            val newChapter = selectedBibleChapter ?: return
            val lastVerseItem = newChapter.items.lastOrNull { it.type == "verse" }
            if (lastVerseItem != null) {
                selectedVerses.clear()
                selectedVerses.addAll(lastVerseItem.verse_numbers)
                projectSelectedVerses()
                syncScrollToSelected()
            }
        }
    }
}
