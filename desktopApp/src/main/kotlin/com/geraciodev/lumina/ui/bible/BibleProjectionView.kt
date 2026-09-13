package com.geraciodev.lumina.ui.bible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geraciodev.lumina.data.model.bible.BibleItem
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun BibleProjectionView(
    verses: List<BibleItem>,
    reference: String,
    fontSize: Int = 48,
    fontFamily: String = "Inter",
    fontColor: Long = 0xFFFFFFFFL,
    backgroundImagePath: String? = null
) {
    val backgroundBitmap = remember(backgroundImagePath) {
        loadBackgroundBitmap(backgroundImagePath)
    }

    // Intentamos cargar la fuente de forma robusta para Skia
    val customFontFamily = remember(fontFamily) {
        try {
            val file = File(fontFamily)
            if (file.exists() && file.isFile) {
                // Carga desde archivo local (.ttf, .otf)
                FontFamily(Font(file))
            } else if (fontFamily.isBlank() || fontFamily.equals("Inter", ignoreCase = true) || fontFamily.equals("Default", ignoreCase = true)) {
                FontFamily.Default
            } else {
                // Carga desde el sistema por nombre
                FontFamily(fontFamily)
            }
        } catch (e: Exception) {
            println("Error cargando fuente $fontFamily: ${e.message}")
            FontFamily.Default
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Velo oscuro para que el texto siga siendo legible sobre cualquier imagen.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                items(verses) { item ->
                    val text = item.lines.joinToString(" ")
                    val verseNums = if (item.verse_numbers.isNotEmpty()) {
                        "${item.verse_numbers.joinToString("-")} "
                    } else ""
                    
                    Text(
                        text = verseNums + text,
                        style = TextStyle(
                            fontSize = fontSize.sp,
                            fontFamily = customFontFamily,
                            color = Color(fontColor),
                            textAlign = TextAlign.Center,
                            lineHeight = (fontSize * 1.4).sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(60.dp))

            // Referencia con estilo minimalista y primario
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = reference.uppercase(),
                    style = TextStyle(
                        fontSize = (fontSize * 0.5).sp,
                        fontFamily = customFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

private fun loadBackgroundBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return try {
        val file = File(path)
        if (!file.exists()) return null
        ImageIO.read(file)?.toComposeImageBitmap()
    } catch (e: Exception) {
        println("Error cargando imagen de fondo $path: ${e.message}")
        null
    }
}
