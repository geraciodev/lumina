package com.geraciodev.lumina.data

import com.geraciodev.lumina.util.normalize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class VideoSearchRepository {
    private val mediaExtensions = setOf(
        // Videos
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg",
        // Audio
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
    )
    private val cachedIndex = MutableStateFlow<Set<File>>(emptySet())
    private val indexMutex = Mutex()
    private val indexingMutex = Mutex()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var userFolders = emptyList<File>()

    fun updateScanFolders(folders: List<String>) {
        userFolders = folders.map { File(it) }.filter { it.exists() && it.isDirectory }
        repositoryScope.launch {
            buildIndex()
        }
    }

    private suspend fun buildIndex() = indexingMutex.withLock {
        val indexedFiles = mutableSetOf<File>()
        val roots = if (userFolders.isNotEmpty()) userFolders else getSearchRoots()

        roots.forEach { root ->
            try {
                Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (file.fileName.toString().hasMediaExtension()) {
                            indexedFiles.add(file.toFile())
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult =
                        FileVisitResult.SKIP_SUBTREE

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val name = dir.fileName?.toString() ?: ""
                        return if (shouldSkipDirectory(name, dir.toAbsolutePath().toString(), root.absolutePath)) {
                            FileVisitResult.SKIP_SUBTREE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }
                })
            } catch (_: Exception) {
                // Una carpeta inaccesible no debe interrumpir la indexación restante.
            }
        }

        indexMutex.withLock {
            cachedIndex.value = indexedFiles
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

    fun searchMediaFlow(query: String): Flow<List<File>> {
        if (query.length < 2) return flowOf(emptyList())

        val normalizedQuery = query.normalize()
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }

        return cachedIndex.map { files ->
            files.filter { file ->
                file.name.matchesQuery(queryWords)
            }
        }.flowOn(Dispatchers.Default)
    }

    internal fun String.matchesQuery(queryWords: List<String>): Boolean {
        val normalizedFileName = normalize()
        return queryWords.all { normalizedFileName.contains(it) }
    }

    private fun String.hasMediaExtension(): Boolean =
        mediaExtensions.any { endsWith(".$it", ignoreCase = true) }

    private fun getSearchRoots(): List<File> {
        val roots = mutableSetOf<File>()
        
        // Directorio personal del usuario (donde normalmente se almacenan medios).
        System.getProperty("user.home")?.let { roots.add(File(it).absoluteFile) }

        // Puntos de montaje comunes para discos extraíbles y otras particiones.
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
