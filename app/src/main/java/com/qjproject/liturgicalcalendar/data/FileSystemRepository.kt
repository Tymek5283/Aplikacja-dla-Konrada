package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class FileSystemRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val internalStorageRoot = context.filesDir

    fun getItems(path: String): List<FileSystemItem> {
        return try {
            val file = File(internalStorageRoot, path)
            file.listFiles()?.map { item ->
                FileSystemItem(name = item.name.removeSuffix(".json"), isDirectory = item.isDirectory)
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FileSystemRepository", "Błąd podczas pobierania elementów z $path", e)
            emptyList()
        }
    }

    fun getDayData(path: String): DayData? {
        return try {
            val file = File(internalStorageRoot, "$path.json")
            val jsonString = file.bufferedReader().use { it.readText() }
            json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMonthlyFileMap(month: Month): Map<Int, List<String>> {
        // --- POCZĄTEK OSTATECZNEJ POPRAWKI: Użycie poprawnego formatu nazwy miesiąca ---
        // TextStyle.FULL_STANDALONE zwraca nazwę w mianowniku (np. "Styczeń"),
        // a nie w dopełniaczu ("stycznia"), co jest zgodne z nazwami folderów.
        val monthName = month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl")) else it.toString() }
        // --- KONIEC OSTATECZNEJ POPRAWKI ---
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
                    val relativePath = "Datowane/$monthName/${file.name}".removeSuffix(".json")
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
            val zipFile = File(downloadsDir, "LiturgicalCalendar_Export_$timestamp.zip")
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