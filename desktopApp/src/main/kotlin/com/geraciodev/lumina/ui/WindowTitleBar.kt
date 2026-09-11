package com.geraciodev.lumina.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope

@Composable
fun WindowScope.WindowTitleBar(
    title: String,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    WindowDraggableArea {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y Título
                Icon(
                    imageVector = Icons.Default.WebAsset,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.width(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
                
                Spacer(Modifier.weight(1f))
                
                // Botones de Control
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WindowControlButton(
                        icon = Icons.Default.Remove,
                        onClick = onMinimize,
                        contentDescription = "Minimizar"
                    )
                    WindowControlButton(
                        icon = if (isMaximized) Icons.Default.WebAsset else Icons.Default.CropSquare,
                        onClick = onMaximize,
                        contentDescription = if (isMaximized) "Restaurar" else "Maximizar"
                    )
                    WindowControlButton(
                        icon = Icons.Default.Close,
                        onClick = onClose,
                        contentDescription = "Cerrar",
                        isClose = true
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    isClose: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isClose) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp)
        )
    }
}
