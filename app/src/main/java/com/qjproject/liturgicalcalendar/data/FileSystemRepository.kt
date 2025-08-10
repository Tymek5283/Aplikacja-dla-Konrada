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
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import kotlin.Result

private const val ORDER_FILE_NAME = ".directory_order.json"

@Serializable
private data class DirectoryOrder(val order: List<String>)

class FileSystemRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val internalStorageRoot = context.filesDir

    private var songListCache: List<Song>? = null

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
                    orderedItems.addAll(remainingItems.sortedWith(compareBy({ !it.isDirectory }, { it.name })))
                    orderedItems
                } catch (e: Exception) {
                    Log.e("FileSystemRepository", "Błąd odczytu pliku kolejności dla $path. Sortowanie alfabetyczne.", e)
                    itemsOnDisk.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                }
            } else {
                itemsOnDisk.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            }
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas pobierania elementów z $path", e)
            emptyList()
        }
    }

    fun getDayData(path: String): DayData? {
        return try {
            val file = File(internalStorageRoot, path) // Ścieżka powinna już zawierać .json
            if (!file.exists()) {
                Log.e("FileSystemRepository", "Plik nie istnieje: ${file.absolutePath}")
                return null
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSongList(): List<Song> {
        songListCache?.let { return it }
        return try {
            val file = File(internalStorageRoot, "data/piesni.json")
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
            val file = File(internalStorageRoot, "data/piesni.json")
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

    fun getSongByNumber(number: String): Song? {
        return getSongList().find { it.numer.equals(number, ignoreCase = true) }
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

    fun renameItem(itemPath: String, newName: String): Result<String> {
        return try {
            val oldFile = File(internalStorageRoot, itemPath)
            if (!oldFile.exists()) return Result.failure(FileNotFoundException("Element nie istnieje: $itemPath"))

            val parentDir = oldFile.parentFile ?: return Result.failure(IOException("Brak folderu nadrzędnego."))
            val newFileNameWithExt = if (oldFile.isDirectory) newName else "$newName.json"
            val newFile = File(parentDir, newFileNameWithExt)

            if (newFile.exists()) return Result.failure(IOException("Element o nazwie '$newName' już istnieje."))

            if (oldFile.renameTo(newFile)) {
                // Jeśli to plik JSON, zaktualizuj jego wewnętrzny tytuł
                if (!newFile.isDirectory && newFile.extension == "json") {
                    try {
                        val dayData = json.decodeFromString<DayData>(newFile.readText())
                        val updatedDayData = dayData.copy(tytulDnia = newName)
                        newFile.writeText(json.encodeToString(updatedDayData))
                    } catch (e: Exception) {
                        Log.w("FileSystemRepo", "Nie udało się zaktualizować tytułu w pliku ${newFile.name}", e)
                        // Kontynuuj, bo zmiana nazwy pliku się udała
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

            val (dataDir, datowaneDir) = findRequiredFolders(tempUnzipDir)
                ?: return Result.failure(IllegalStateException("Plik ZIP nie zawiera wymaganych folderów 'data' i 'Datowane' na tym samym poziomie."))

            if (dataDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'data' w pliku ZIP jest pusty."))
            }
            if (datowaneDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'Datowane' w pliku ZIP jest pusty."))
            }

            File(internalStorageRoot, "data").deleteRecursively()
            File(internalStorageRoot, "Datowane").deleteRecursively()

            dataDir.copyRecursively(File(internalStorageRoot, "data"), true)
            datowaneDir.copyRecursively(File(internalStorageRoot, "Datowane"), true)

            return Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            if (tempUnzipDir.exists()) tempUnzipDir.deleteRecursively()
            if (tempZipFile.exists()) tempZipFile.delete()
        }
    }

    private fun findRequiredFolders(startDir: File): Pair<File, File>? {
        val queue: Queue<File> = LinkedList()
        queue.add(startDir)

        while (queue.isNotEmpty()) {
            val currentDir = queue.poll()
            val dataDir = File(currentDir, "data")
            val datowaneDir = File(currentDir, "Datowane")

            if (dataDir.exists() && dataDir.isDirectory && datowaneDir.exists() && datowaneDir.isDirectory) {
                return Pair(dataDir, datowaneDir)
            }

            currentDir.listFiles { file -> file.isDirectory }?.forEach { queue.add(it) }
        }
        return null
    }
}