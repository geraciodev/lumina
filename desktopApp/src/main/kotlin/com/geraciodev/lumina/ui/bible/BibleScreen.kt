package com.geraciodev.lumina.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geraciodev.lumina.ui.MainViewModel

@Composable
fun BibleScreen(viewModel: MainViewModel) {
    val bible = viewModel.bibleVersion
    val selectedBook = viewModel.selectedBibleBook
    val selectedChapter = viewModel.selectedBibleChapter
    val selectedVerses = viewModel.selectedVerses

    Row(modifier = Modifier.fillMaxSize()) {
        // Selector de Libros/Capítulos (Panel Izquierdo)
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 24.dp)
        ) {
            Text(
                text = "BIBLIA - ${bible?.local_title ?: "CARGANDO..."}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TextField(
                value = viewModel.bibleSearchQuery,
                onValueChange = { viewModel.onBibleSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar palabra o frase...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                trailingIcon = {
                    if (viewModel.isBibleSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            if (viewModel.bibleSearchQuery.length >= 3) {
                // Resultados de Búsqueda
                Text(
                    text = "RESULTADOS (${viewModel.bibleSearchResults.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.bibleSearchResults) { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToSearchResult(result) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${result.bookName} ${result.chapterReference}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = result.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.weight(1f)) {
                    // Lista de Libros
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val filteredBooks = bible?.books ?: emptyList()

                        items(filteredBooks) { book ->
                            val isSelected = selectedBook == book
                            ListItem(
                                headlineContent = {
                                    Text(
                                        book.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier
                                    .clickable { viewModel.selectBibleBook(book) }
                                    .padding(vertical = 2.dp),
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                                )
                            )
                        }
                    }

                    // Lista de Capítulos
                    if (selectedBook != null) {
                        LazyColumn(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .padding(start = 8.dp)
                        ) {
                            items(selectedBook.chapters) { chapter ->
                                val isSelected = selectedChapter == chapter
                                val chapterNum = chapter.current.human.split(" ").last()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.selectBibleChapter(chapter) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chapterNum,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Visor de Texto (Panel Derecho)
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(32.dp)
            ) {
                if (selectedChapter != null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = selectedChapter.current.human,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                        
                        items(selectedChapter.items) { item ->
                            if (item.type == "verse") {
                                val verseNum = item.verse_numbers.firstOrNull() ?: 0
                                val isSelected = selectedVerses.contains(verseNum)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleVerseSelection(verseNum) }
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "$verseNum ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(28.dp).padding(top = 4.dp)
                                    )
                                    Text(
                                        text = item.lines.joinToString(" "),
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 28.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else if (item.type.startsWith("heading")) {
                                Text(
                                    text = item.lines.joinToString(" "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Selecciona un libro y capítulo", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Controles de proyección para la Biblia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedVerses.isNotEmpty()) {
                    Text(
                        text = "${selectedVerses.size} versículos seleccionados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row {
                    if (viewModel.projectingBibleVerses != null) {
                        TextButton(
                            onClick = { viewModel.stopProjection() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("DETENER", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.projectSelectedVerses() },
                        enabled = selectedVerses.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "PROYECTAR SELECCIÓN",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
