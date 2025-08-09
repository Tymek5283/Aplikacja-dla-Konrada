package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.os.Environment
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Month
import java.util.*

class FileSystemRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getItems(path: String): List<FileSystemItem> {
        return try {
            context.assets.list(path)?.map { itemName ->
                val isDirectory = try {
                    !itemName.contains(".") && (context.assets.list("$path/$itemName")?.isNotEmpty() == true)
                } catch (e: IOException) {
                    false
                }
                FileSystemItem(name = itemName.removeSuffix(".json"), isDirectory = isDirectory)
            } ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun getDayData(path: String): DayData? {
        return try {
            val inputStream: InputStream = context.assets.open("$path.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDatedFilesForMonth(month: Month): List<String> {
        // Poprawna nazwa folderu miesiąca z dużej litery, zgodna z Twoją strukturą
        val monthName = month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl")) else it.toString() }

        return try {
            val path = "Datowane/$monthName"
            context.assets.list(path)?.toList() ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun exportAssetsToZip(assetRootPath: String): Result<File> {
        return try {
            val tempDir = File(context.cacheDir, "export_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            // --- POCZĄTEK ZMIANY: Poprawna iteracja po głównym folderze 'assets' ---
            // Pobieramy listę wszystkich plików i folderów z podanej ścieżki (teraz to będzie "")
            val rootAssets = context.assets.list(assetRootPath) ?: arrayOf()

            // Dla każdego elementu na najwyższym poziomie (np. "data", "Datowane") uruchamiamy kopiowanie.
            for (assetName in rootAssets) {
                // Budujemy pełną ścieżkę dla funkcji rekursywnej
                val fullPath = if (assetRootPath.isEmpty()) assetName else "$assetRootPath/$assetName"
                copyAssetsRecursively(fullPath, tempDir)
            }
            // --- KONIEC ZMIANY ---

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(downloadsDir, "LiturgicalCalendar_Export_$timestamp.zip")

            ZipFile(zipFile).addFolder(tempDir)
            tempDir.deleteRecursively()

            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun copyAssetsRecursively(path: String, destDir: File) {
        try {
            val assets = context.assets.list(path)
            if (assets.isNullOrEmpty()) {
                copyAssetFile(path, destDir)
            } else {
                val dir = File(destDir, path.substringAfterLast('/'))
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                assets.forEach { assetName ->
                    copyAssetsRecursively("$path/$assetName", dir)
                }
            }
        } catch (e: IOException) {
            copyAssetFile(path, destDir.parentFile ?: destDir)
        }
    }

    private fun copyAssetFile(assetPath: String, destDir: File) {
        val destFile = File(destDir, assetPath.substringAfterLast('/'))
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}