package com.geraciodev.lumina.data

import com.geraciodev.lumina.util.normalize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger

class VideoSearchRepository {
    private val mediaExtensions = setOf(
        // Videos
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
        // Audio
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
    )
    private val cachedIndex = mutableSetOf<File>()
    private val indexMutex = Mutex()
    private var isIndexing = false
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Iniciar indexación en segundo plano al instanciar el repositorio
        repositoryScope.launch {
            buildIndex()
        }
    }

    private var userFolders = emptyList<File>()

    fun updateScanFolders(folders: List<String>) {
        userFolders = folders.map { File(it) }.filter { it.exists() && it.isDirectory }
        repositoryScope.launch {
            buildIndex()
        }
    }

    private suspend fun buildIndex() = coroutineScope {
        if (isIndexing) return@coroutineScope
        isIndexing = true
        
        // Limpiar índice antes de reconstruir si es necesario, 
        // o simplemente añadir. Para simplificar, reconstruimos.
        indexMutex.withLock {
            cachedIndex.clear()
        }

        val roots = if (userFolders.isNotEmpty()) userFolders else getSearchRoots()
        roots.forEach { root ->
            launch {
                try {
                    Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val fileName = file.fileName.toString().lowercase()
                            if (mediaExtensions.any { fileName.endsWith(".$it") }) {
                                repositoryScope.launch {
                                    indexMutex.withLock {
                                        cachedIndex.add(file.toFile())
                                    }
                                }
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult {
                            return FileVisitResult.SKIP_SUBTREE
                        }

                        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val name = dir.fileName?.toString() ?: ""
                            if (shouldSkipDirectory(name, dir.toAbsolutePath().toString(), root.absolutePath)) {
                                return FileVisitResult.SKIP_SUBTREE
                            }
                            return FileVisitResult.CONTINUE
                        }
                    })
                } catch (e: Exception) {
                    // Error accediendo a raíz
                }
            }
        }
    }

    private fun shouldSkipDirectory(name: String, pathString: String, rootPath: String): Boolean {
        if (rootPath == "/" || rootPath == "C:\\") {
            if (listOf("/home", "/media", "/mnt", "/Volumes", "/run").any { pathString == it }) {
                return true
            }
        }
        return name.startsWith(".") || 
            name.equals("System Volume Information", ignoreCase = true) || 
            name.equals("\$Recycle.Bin", ignoreCase = true) ||
            name.equals("node_modules", ignoreCase = true) ||
            listOf("proc", "sys", "dev", "run", "var", "tmp", "boot", "etc", "usr", "bin", "sbin", "lib", "lib64", "AppData", "Library")
                .any { name.equals(it, ignoreCase = true) }
    }

    fun searchMediaFlow(query: String): Flow<List<File>> = callbackFlow {
        if (query.length < 2) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val normalizedQuery = query.normalize()
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }

        // Primero enviamos resultados rápidos desde el cache
        val instantResults = indexMutex.withLock {
            cachedIndex.filter { file ->
                val normalizedFileName = file.name.normalize()
                queryWords.all { normalizedFileName.contains(it) }
            }
        }
        trySend(instantResults)

        if (!isIndexing) {
            close()
        }
        
        awaitClose { }
    }.flowOn(Dispatchers.Default)

    private fun getSearchRoots(): List<File> {
        val roots = mutableSetOf<File>()
        
        // 1. Raíces lógicas reportadas por el SO (C:\, D:\, /)
        File.listRoots()?.forEach { roots.add(it.absoluteFile) }
        
        // 2. Home del usuario (Donde suelen estar los videos)
        System.getProperty("user.home")?.let { roots.add(File(it).absoluteFile) }
        
        // 3. Puntos de montaje comunes para discos extraíbles y otras particiones
        // En Linux moderno (Ubuntu/Fedora/etc), los discos suelen estar en /run/media/USUARIO/ o /media/USUARIO/
        val user = System.getProperty("user.name")
        val mountPoints = listOf(
            "/media", 
            "/media/$user",
            "/mnt", 
            "/Volumes", 
            "/run/media",
            "/run/media/$user"
        )
        
        mountPoints.forEach { mp ->
            val dir = File(mp)
            if (dir.exists() && dir.isDirectory) {
                // Añadimos las subcarpetas de estos puntos (que son los discos reales)
                dir.listFiles()?.forEach { sub ->
                    if (sub.isDirectory && !sub.isHidden) {
                        roots.add(sub.absoluteFile)
                    }
                }
            }
        }

        // Limpieza: eliminar duplicados y asegurar que las rutas existen y son accesibles
        return roots.filter { it.exists() && it.canRead() }
            .map { it.absoluteFile }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.absolutePath.length } // Priorizar rutas específicas sobre "/"
    }
}
