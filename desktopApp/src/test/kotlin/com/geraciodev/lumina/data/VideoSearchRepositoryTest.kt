package com.geraciodev.lumina.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSearchRepositoryTest {
    @Test
    fun queryMatchesAllNormalizedWordsInFileName() {
        val repository = VideoSearchRepository()

        assertTrue(repository.run { "Sermón de Juan.mp4".matchesQuery(listOf("sermon", "juan")) })
        assertFalse(repository.run { "Sermón de Juan.mp4".matchesQuery(listOf("sermon", "mateo")) })
    }
}
