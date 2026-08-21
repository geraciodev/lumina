package com.geraciodev.lumina.data.model.bible

import kotlinx.serialization.Serializable

@Serializable
data class BibleVersion(
    val version_id: Int,
    val local_abbreviation: String,
    val local_title: String,
    val books: List<BibleBook>
)

@Serializable
data class BibleBook(
    val book_usfm: String,
    val name: String,
    val chapters: List<BibleChapter>
)

@Serializable
data class BibleChapter(
    val chapter_usfm: String,
    val current: ChapterReference,
    val items: List<BibleItem>
)

@Serializable
data class ChapterReference(
    val usfm: String,
    val human: String
)

@Serializable
data class BibleItem(
    val type: String,
    val verse_numbers: List<Int> = emptyList(),
    val lines: List<String> = emptyList()
)
