package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import kotlin.Result

private const val ORDER_FILE_NAME = ".directory_order.json"

@Serializable
private data class DirectoryOrder(val order: List<String>)

private data class FoundImportFiles(
    val dataDir: File,
    val datowaneDir: File,
    val songFile: File
)

private val fileSystemItemNaturalComparator = compareBy<FileSystemItem> { !it.isDirectory }
    .then(Comparator { a, b ->
        val numA = a.name.takeWhile { it.isDigit() }.toIntOrNull()
        val numB = b.name.takeWhile { it.isDigit() }.toIntOrNull()

        if (numA != null && numB != null) {
            val numCompare = numA.compareTo(numB)
            if (numCompare != 0) numCompare else a.name.compareTo(b.name, ignoreCase = true)
        } else if (numA != null) {
            -1 // Elementy z numerami na początku
        } else if (numB != null) {
            1 // Elementy z numerami na początku
        } else {
            // Brak numerów, sortowanie alfabetyczne
            a.name.compareTo(b.name, ignoreCase = true)
        }
    })


class FileSystemRepository(val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val internalStorageRoot = context.filesDir

    private var songListCache: List<Song>? = null
    private var categoryListCache: List<Category>? = null

    // --- POCZĄTEK ZMIANY ---
    fun invalidateSongCache() {
        songListCache = null
    }
    // --- KONIEC ZMIANY ---

    fun getAllDayFilePaths(): List<String> {
        val paths = mutableListOf<String>()
        val dataDir = File(internalStorageRoot, "data")
        val datowaneDir = File(internalStorageRoot, "Datowane")

        fun findJsonFiles(directory: File) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "json" && file.name != "piesni.json" && file.name != "kategorie.json") {
                    paths.add(file.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/"))
                }
            }
        }

        if (dataDir.exists() && dataDir.isDirectory) {
            findJsonFiles(dataDir)
        }
        if (datowaneDir.exists() && datowaneDir.isDirectory) {
            findJsonFiles(datowaneDir)
        }

        return paths
    }


    fun getItems(path: String): List<FileSystemItem> {
        return try {
            val directory = File(internalStorageRoot, path)
            val allFiles = directory.listFiles() ?: return emptyList()

            val itemsOnDisk = allFiles
                .filter { it.name != ORDER_FILE_NAME }
                .map { item ->
                    val itemName = if (item.isDirectory) item.name else item.nameWithoutExtension
                    val itemPath = item.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/")
                    FileSystemItem(name = itemName, isDirectory = item.isDirectory, path = itemPath)
                }.toMutableList()

            val orderFile = File(directory, ORDER_FILE_NAME)
            if (orderFile.exists()) {
                try {
                    val jsonString = orderFile.readText()
                    val storedOrder = json.decodeFromString<DirectoryOrder>(jsonString).order

                    val orderedItems = mutableListOf<FileSystemItem>()
                    val itemsOnDiskMap = itemsOnDisk.associateBy { it.name }
                    val remainingItems = itemsOnDisk.toMutableSet()

                    storedOrder.forEach { name ->
                        itemsOnDiskMap[name]?.let {
                            orderedItems.add(it)
                            remainingItems.remove(it)
                        }
                    }
                    orderedItems.addAll(remainingItems.sortedWith(fileSystemItemNaturalComparator))
                    orderedItems
                } catch (e: Exception) {
                    Log.e("FileSystemRepository", "Błąd odczytu pliku kolejności dla $path. Sortowanie alfabetyczne.", e)
                    itemsOnDisk.sortedWith(fileSystemItemNaturalComparator)
                }
            } else {
                itemsOnDisk.sortedWith(fileSystemItemNaturalComparator)
            }
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas pobierania elementów z $path", e)
            emptyList()
        }
    }

    fun getDayData(path: String): DayData? {
        try {
            val file = File(internalStorageRoot, path)
            if (!file.exists()) {
                Log.e("FileSystemRepository", "Plik nie istnieje: ${file.absolutePath}")
                return null
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            if (jsonString.isBlank()) {
                Log.w("FileSystemRepository", "Pusty plik JSON, pomijanie: $path")
                return null
            }
            return json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            Log.w("FileSystemRepository", "Błąd podczas odczytu lub parsowania pliku: $path. Powód: ${e.message}")
            return null
        }
    }

    fun getSongList(): List<Song> {
        songListCache?.let { return it }
        return try {
            val file = File(internalStorageRoot, "piesni.json")
            if (!file.exists()) {
                Log.e("FileSystemRepository", "Krytyczny błąd: Plik 'piesni.json' nie istnieje w pamięci wewnętrznej.")
                return emptyList()
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            val songs = json.decodeFromString<List<Song>>(jsonString)
            songListCache = songs
            songs
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas wczytywania piesni.json z pamięci wewnętrznej", e)
            emptyList()
        }
    }

    fun saveSongList(songs: List<Song>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "piesni.json")
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(songs)
            file.writeText(jsonString)
            songListCache = songs // Zaktualizuj bufor
            Log.d("FileSystemRepository", "Zapisano pomyślnie listę pieśni do: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas zapisywania listy pieśni", e)
            Result.failure(e)
        }
    }

    fun getCategoryList(): List<Category> {
        categoryListCache?.let { return it }
        return try {
            val file = File(internalStorageRoot, "kategorie.json")
            if (!file.exists()) {
                Log.e("FileSystemRepository", "Krytyczny błąd: Plik 'kategorie.json' nie istnieje w pamięci wewnętrznej.")
                return emptyList()
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            val categories = json.decodeFromString<List<Category>>(jsonString)
            categoryListCache = categories
            categories
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas wczytywania kategorie.json z pamięci wewnętrznej", e)
            emptyList()
        }
    }

    fun saveCategoryList(categories: List<Category>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "kategorie.json")
            val jsonString = json.encodeToString(categories)
            file.writeText(jsonString)
            categoryListCache = categories // Zaktualizuj bufor
            Log.d("FileSystemRepository", "Zapisano pomyślnie listę kategorii do: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas zapisywania listy kategorii", e)
            Result.failure(e)
        }
    }

    fun getSong(title: String, siedlNum: String?, sakNum: String?, dnNum: String?): Song? {
        if (title.isBlank()) return null
        val songList = getSongList()

        val matchingSongs = songList.filter { it.tytul.equals(title, ignoreCase = true) }

        return when {
            matchingSongs.isEmpty() -> null
            matchingSongs.size == 1 -> matchingSongs.first()
            else -> {
                matchingSongs.find { song ->
                    (siedlNum?.isNotBlank() == true && song.numerSiedl.equals(siedlNum, ignoreCase = true)) ||
                            (sakNum?.isNotBlank() == true && song.numerSAK.equals(sakNum, ignoreCase = true)) ||
                            (dnNum?.isNotBlank() == true && song.numerDN.equals(dnNum, ignoreCase = true))
                } ?: matchingSongs.first() // Fallback to first match if no number matches
            }
        }
    }

    fun saveDayData(path: String, dayData: DayData): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, path) // Ścieżka powinna już zawierać .json
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(dayData)
            file.writeText(jsonString)
            Log.d("FileSystemRepository", "Zapisano pomyślnie dane do: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas zapisywania danych do $path", e)
            Result.failure(e)
        }
    }

    fun saveOrder(path: String, orderedNames: List<String>): Result<Unit> {
        return try {
            val directory = File(internalStorageRoot, path)
            if (!directory.exists()) directory.mkdirs()
            val orderFile = File(directory, ORDER_FILE_NAME)
            val directoryOrder = DirectoryOrder(order = orderedNames)
            val jsonString = json.encodeToString(directoryOrder)
            orderFile.writeText(jsonString)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd zapisu kolejności w $path", e)
            Result.failure(e)
        }
    }

    fun createFolder(path: String, folderName: String): Result<Unit> {
        return try {
            val fullPath = File(internalStorageRoot, path)
            val newFolder = File(fullPath, folderName)
            if (newFolder.exists()) {
                return Result.failure(IOException("Folder o tej nazwie już istnieje."))
            }
            if (newFolder.mkdirs()) {
                Log.d("FileSystemRepository", "Utworzono folder: ${newFolder.absolutePath}")
                Result.success(Unit)
            } else {
                Result.failure(IOException("Nie udało się utworzyć folderu."))
            }
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd tworzenia folderu '$folderName' w '$path'", e)
            Result.failure(e)
        }
    }

    fun createDayFile(path: String, fileName: String, url: String?): Result<String> {
        return try {
            val fullPath = File(internalStorageRoot, path)
            fullPath.mkdirs()
            val newFile = File(fullPath, "$fileName.json")

            if (newFile.exists()) {
                return Result.failure(IOException("Plik o tej nazwie już istnieje."))
            }

            val dayData = DayData(
                urlCzytania = url?.ifBlank { null },
                tytulDnia = fileName,
                czyDatowany = false,
                czytania = emptyList(),
                piesniSugerowane = emptyList()
            )

            val jsonString = json.encodeToString(dayData)
            newFile.writeText(jsonString)
            Log.d("FileSystemRepository", "Utworzono plik dnia: ${newFile.absolutePath}")
            Result.success(newFile.name)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd tworzenia pliku '$fileName' w '$path'", e)
            Result.failure(e)
        }
    }

    fun deleteItem(itemPath: String): Result<Unit> {
        return try {
            val fileToDelete = File(internalStorageRoot, itemPath)
            if (!fileToDelete.exists()) return Result.failure(FileNotFoundException("Element nie istnieje: $itemPath"))

            if (fileToDelete.deleteRecursively()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Nie udało się usunąć elementu: $itemPath"))
            }
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas usuwania $itemPath", e)
            Result.failure(e)
        }
    }

    fun deleteSong(songToDelete: Song, deleteOccurrences: Boolean): Result<Unit> {
        return try {
            // 1. Usunięcie pieśni z głównego pliku `piesni.json`
            val currentSongs = getSongList().toMutableList()
            val removed = currentSongs.removeAll { it.numerSiedl == songToDelete.numerSiedl }
            if (!removed) {
                return Result.failure(FileNotFoundException("Nie znaleziono pieśni w głównym spisie."))
            }
            saveSongList(currentSongs).getOrThrow() // Rzuci wyjątek w razie błędu zapisu

            // 2. Opcjonalne usunięcie wystąpień w dniach liturgicznych
            if (deleteOccurrences) {
                val allDayPaths = getAllDayFilePaths()
                for (path in allDayPaths) {
                    val dayData = getDayData(path)
                    if (dayData?.piesniSugerowane?.any { it?.numer == songToDelete.numerSiedl } == true) {
                        val updatedSongs = dayData.piesniSugerowane.filter { it?.numer != songToDelete.numerSiedl }
                        val updatedDayData = dayData.copy(piesniSugerowane = updatedSongs)
                        saveDayData(path, updatedDayData).getOrThrow()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas usuwania pieśni: ${songToDelete.tytul}", e)
            Result.failure(e)
        }
    }

    fun renameItem(itemPath: String, newName: String): Result<String> {
        return try {
            val oldFile = File(internalStorageRoot, itemPath)
            if (!oldFile.exists()) return Result.failure(FileNotFoundException("Element nie istnieje: $itemPath"))

            val parentDir = oldFile.parentFile ?: return Result.failure(IOException("Brak folderu nadrzędnego."))
            val newFileNameWithExt = if (oldFile.isDirectory) newName else "$newName.json"
            val newFile = File(parentDir, newFileNameWithExt)

            if (newFile.exists()) return Result.failure(IOException("Element o nazwie '$newName' już istnieje."))

            if (oldFile.renameTo(newFile)) {
                if (!newFile.isDirectory && newFile.extension == "json") {
                    try {
                        val dayData = json.decodeFromString<DayData>(newFile.readText())
                        val updatedDayData = dayData.copy(tytulDnia = newName)
                        newFile.writeText(json.encodeToString(updatedDayData))
                    } catch (e: Exception) {
                        Log.w("FileSystemRepo", "Nie udało się zaktualizować tytułu w pliku ${newFile.name}", e)
                    }
                }
                Result.success(newFile.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/"))
            } else {
                Result.failure(IOException("Nie udało się zmienić nazwy."))
            }
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd zmiany nazwy dla $itemPath", e)
            Result.failure(e)
        }
    }


    fun getMonthlyFileMap(month: Month): Map<Int, List<String>> {
        val monthName = month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl")) else it.toString() }
        val monthDir = File(internalStorageRoot, "Datowane/$monthName")

        if (!monthDir.exists() || !monthDir.isDirectory) {
            Log.w("FileSystemRepo", "Folder dla miesiąca nie istnieje: ${monthDir.path}")
            return emptyMap()
        }

        val fileMap = mutableMapOf<Int, MutableList<String>>()

        monthDir.listFiles()?.forEach { file ->
            try {
                val nameWithoutExtension = file.nameWithoutExtension
                val dayString = nameWithoutExtension.substringBefore(" ")
                val dayNumber = dayString.toIntOrNull()

                if (dayNumber != null) {
                    val relativePath = "Datowane/$monthName/${file.name}"
                    fileMap.getOrPut(dayNumber) { mutableListOf() }.add(relativePath)
                }
            } catch (e: Exception) {
                Log.w("FileSystemRepo", "Pominięto plik o nieprawidłowej nazwie: ${file.name}", e)
            }
        }
        return fileMap
    }

    fun exportDataToZip(): Result<File> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(downloadsDir, "Laudate_Export_$timestamp.zip")
            val zip = ZipFile(zipFile)

            val dataDir = File(internalStorageRoot, "data")
            if (dataDir.exists()) zip.addFolder(dataDir)

            val datowaneDir = File(internalStorageRoot, "Datowane")
            if (datowaneDir.exists()) zip.addFolder(datowaneDir)

            val songFile = File(internalStorageRoot, "piesni.json")
            if (songFile.exists()) zip.addFile(songFile)

            val categoryFile = File(internalStorageRoot, "kategorie.json")
            if (categoryFile.exists()) zip.addFile(categoryFile)


            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun importDataFromZip(uri: Uri): Result<Unit> {
        val tempUnzipDir = File(context.cacheDir, "import_unzip_temp")
        val tempZipFile = File(context.cacheDir, "import.zip")

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(IOException("Nie można otworzyć strumienia z URI."))

            if (tempUnzipDir.exists()) tempUnzipDir.deleteRecursively()
            tempUnzipDir.mkdirs()
            ZipFile(tempZipFile).extractAll(tempUnzipDir.absolutePath)

            val foundFiles = findRequiredFiles(tempUnzipDir)
                ?: return Result.failure(IllegalStateException("Plik ZIP nie zawiera wymaganych plików/folderów: 'data', 'Datowane' i 'piesni.json'."))

            if (foundFiles.dataDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'data' w pliku ZIP jest pusty."))
            }
            if (foundFiles.datowaneDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'Datowane' w pliku ZIP jest pusty."))
            }

            File(internalStorageRoot, "data").deleteRecursively()
            File(internalStorageRoot, "Datowane").deleteRecursively()
            File(internalStorageRoot, "piesni.json").delete()
            File(internalStorageRoot, "kategorie.json").delete()

            foundFiles.dataDir.copyRecursively(File(internalStorageRoot, "data"), true)
            foundFiles.datowaneDir.copyRecursively(File(internalStorageRoot, "Datowane"), true)
            foundFiles.songFile.copyTo(File(internalStorageRoot, "piesni.json"), true)

            // Skopiuj plik kategorii, jeśli istnieje w zipie
            val categoryFileInZip = findFileRecursively(tempUnzipDir, "kategorie.json")
            categoryFileInZip?.copyTo(File(internalStorageRoot, "kategorie.json"), true)


            return Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            if (tempUnzipDir.exists()) tempUnzipDir.deleteRecursively()
            if (tempZipFile.exists()) tempZipFile.delete()
        }
    }

    private fun findFileRecursively(directory: File, fileName: String): File? {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findFileRecursively(file, fileName)
                if (found != null) return found
            } else if (file.name == fileName) {
                return file
            }
        }
        return null
    }

    private fun findRequiredFiles(startDir: File): FoundImportFiles? {
        val queue: Queue<File> = LinkedList()
        queue.add(startDir)

        while (queue.isNotEmpty()) {
            val currentDir = queue.poll()
            val dataDir = File(currentDir, "data")
            val datowaneDir = File(currentDir, "Datowane")
            val songFile = File(currentDir, "piesni.json")

            if (dataDir.exists() && dataDir.isDirectory &&
                datowaneDir.exists() && datowaneDir.isDirectory &&
                songFile.exists() && songFile.isFile
            ) {
                return FoundImportFiles(dataDir, datowaneDir, songFile)
            }

            currentDir.listFiles { file -> file.isDirectory }?.forEach { queue.add(it) }
        }
        return null
    }
}