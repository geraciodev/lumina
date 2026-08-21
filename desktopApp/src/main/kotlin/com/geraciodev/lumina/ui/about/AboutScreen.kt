package com.geraciodev.lumina.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.style.TextAlign
import java.util.Properties

@Composable
fun AboutScreen() {
    val version = remember {
        try {
            val properties = Properties()
            val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                properties.getProperty("version", "unknown")
            } else {
                "1.0.0" // Fallback
            }
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LUMINA",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "v$version",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(Modifier.height(48.dp))
        
        Text(
            text = "Un reproductor multimedia moderno y minimalista\ndiseñado para la simplicidad y el rendimiento.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(48.dp))
        
        Text(
            text = "DESARROLLADO POR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "GERACIODEV",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "POTENCIADO POR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "VLCJ & COMPOSE MULTIPLATFORM",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
