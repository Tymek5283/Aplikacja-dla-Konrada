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
import java.util.*

class FileSystemRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // Funkcje getItems i getDayData pozostają bez zmian
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

    // --- POCZĄTEK POPRAWKI: W pełni przepisana logika eksportu ---
    fun exportAssetsToZip(assetRootPath: String): Result<File> {
        return try {
            // 1. Stwórz tymczasowy folder w pamięci podręcznej aplikacji
            val tempDir = File(context.cacheDir, "export_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            // 2. Rekursywnie skopiuj całą zawartość folderu 'assets/data' do folderu tymczasowego
            copyAssetsRecursively(assetRootPath, tempDir)

            // 3. Przygotuj plik docelowy w folderze Pobrane
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(downloadsDir, "LiturgicalCalendar_Export_$timestamp.zip")

            // 4. Spakuj zawartość folderu tymczasowego do pliku ZIP
            ZipFile(zipFile).addFolder(tempDir)

            // 5. Posprzątaj po sobie, usuwając folder tymczasowy
            tempDir.deleteRecursively()

            // 6. Zwróć sukces z informacją o utworzonym pliku
            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Rekursywnie kopiuje folder z zasobów 'assets' do podanej lokalizacji.
     */
    private fun copyAssetsRecursively(path: String, destDir: File) {
        try {
            val assets = context.assets.list(path)
            if (assets.isNullOrEmpty()) {
                // To jest plik
                copyAssetFile(path, destDir)
            } else {
                // To jest folder
                val dir = File(destDir, path.substringAfterLast('/'))
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                assets.forEach { assetName ->
                    copyAssetsRecursively("$path/$assetName", dir)
                }
            }
        } catch (e: IOException) {
            // Jeśli list() rzuci wyjątek, to znaczy, że 'path' jest plikiem.
            copyAssetFile(path, destDir.parentFile ?: destDir)
        }
    }

    /**
     * Kopiuje pojedynczy plik z 'assets' do podanej lokalizacji.
     */
    private fun copyAssetFile(assetPath: String, destDir: File) {
        val destFile = File(destDir, assetPath.substringAfterLast('/'))
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    // --- KONIEC POPRAWKI ---
}