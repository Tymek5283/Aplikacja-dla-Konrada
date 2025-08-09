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

    fun getDatedFilesForMonth(month: java.time.Month): List<String> {
        val monthName = month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl")) else it.toString() }

        return try {
            val path = "Datowane/$monthName"
            val dir = File(internalStorageRoot, path)
            dir.list()?.toList() ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
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
            // Krok 1: Kopiowanie strumienia do pliku tymczasowego
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(IOException("Nie można otworzyć strumienia z URI."))

            // Krok 2: Rozpakowanie do folderu tymczasowego
            if (tempUnzipDir.exists()) tempUnzipDir.deleteRecursively()
            tempUnzipDir.mkdirs()
            ZipFile(tempZipFile).extractAll(tempUnzipDir.absolutePath)

            // Krok 3: Walidacja zawartości
            val (dataDir, datowaneDir) = findRequiredFolders(tempUnzipDir)
                ?: return Result.failure(IllegalStateException("Plik ZIP nie zawiera wymaganych folderów 'data' i 'Datowane' na tym samym poziomie."))

            if (dataDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'data' w pliku ZIP jest pusty."))
            }
            if (datowaneDir.listFiles().isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Folder 'Datowane' w pliku ZIP jest pusty."))
            }

            // Krok 4: Usunięcie starych danych
            File(internalStorageRoot, "data").deleteRecursively()
            File(internalStorageRoot, "Datowane").deleteRecursively()

            // Krok 5: Skopiowanie nowych danych
            dataDir.copyRecursively(File(internalStorageRoot, "data"), true)
            datowaneDir.copyRecursively(File(internalStorageRoot, "Datowane"), true)

            return Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            // Krok 6: Sprzątanie
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