// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\ImportExportManager.kt
// Opis: Zarządza procesami importu i eksportu danych aplikacji. Odpowiada za pakowanie danych do archiwum ZIP oraz ich rozpakowywanie i weryfikację.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.net.Uri
import android.os.Environment
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

internal class ImportExportManager(
    private val context: Context,
    private val internalStorageRoot: File
) {
    fun exportDataToZip(configuration: com.qjproject.liturgicalcalendar.ui.screens.settings.ExportConfiguration): Result<File> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(downloadsDir, "Laudate_Export_$timestamp.zip")
            val zip = ZipFile(zipFile)

            // Eksportuj dane zgodnie z konfiguracją użytkownika
            if (configuration.includeDays) {
                File(internalStorageRoot, "data").takeIf { it.exists() }?.let { zip.addFolder(it) }
                File(internalStorageRoot, "Datowane").takeIf { it.exists() }?.let { zip.addFolder(it) }
            }
            
            if (configuration.includeSongs) {
                File(internalStorageRoot, "piesni.json").takeIf { it.exists() }?.let { zip.addFile(it) }
            }
            
            if (configuration.includeCategories) {
                File(internalStorageRoot, "kategorie.json").takeIf { it.exists() }?.let { zip.addFile(it) }
            }
            
            if (configuration.includeTags) {
                File(internalStorageRoot, "tagi.json").takeIf { it.exists() }?.let { zip.addFile(it) }
            }
            
            if (configuration.includeNeumy) {
                File(internalStorageRoot, "neumy").takeIf { it.exists() }?.let { zip.addFolder(it) }
            }
            
            if (configuration.includeNotes) {
                File(internalStorageRoot, "notatki").takeIf { it.exists() }?.let { zip.addFolder(it) }
            }
            
            if (configuration.includeYears) {
                File(internalStorageRoot, "calendar_data").takeIf { it.exists() }?.let { zip.addFolder(it) }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun analyzeImportData(uri: Uri): Result<com.qjproject.liturgicalcalendar.ui.screens.settings.AvailableImportData> {
        val tempUnzipDir = File(context.cacheDir, "import_analyze_temp")
        val tempZipFile = File(context.cacheDir, "analyze.zip")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZipFile).use { output -> input.copyTo(output) }
            } ?: return Result.failure(IOException("Nie można otworzyć strumienia z URI."))

            tempUnzipDir.deleteRecursively()
            tempUnzipDir.mkdirs()
            ZipFile(tempZipFile).extractAll(tempUnzipDir.absolutePath)

            val availableData = com.qjproject.liturgicalcalendar.ui.screens.settings.AvailableImportData(
                hasDays = findDirectoryRecursively(tempUnzipDir, "data") != null && findDirectoryRecursively(tempUnzipDir, "Datowane") != null,
                hasSongs = findFileRecursively(tempUnzipDir, "piesni.json") != null,
                hasCategories = findFileRecursively(tempUnzipDir, "kategorie.json") != null,
                hasTags = findFileRecursively(tempUnzipDir, "tagi.json") != null,
                hasNeumy = findDirectoryRecursively(tempUnzipDir, "neumy") != null,
                hasNotes = findDirectoryRecursively(tempUnzipDir, "notatki") != null,
                hasYears = findDirectoryRecursively(tempUnzipDir, "calendar_data") != null
            )

            return Result.success(availableData)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            tempUnzipDir.deleteRecursively()
            tempZipFile.delete()
        }
    }

    fun importDataFromZip(uri: Uri, configuration: com.qjproject.liturgicalcalendar.ui.screens.settings.ImportConfiguration): Result<Unit> {
        val tempUnzipDir = File(context.cacheDir, "import_unzip_temp")
        val tempZipFile = File(context.cacheDir, "import.zip")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZipFile).use { output -> input.copyTo(output) }
            } ?: return Result.failure(IOException("Nie można otworzyć strumienia z URI."))

            tempUnzipDir.deleteRecursively()
            tempUnzipDir.mkdirs()
            ZipFile(tempZipFile).extractAll(tempUnzipDir.absolutePath)

            // Selektywny import - tylko wybrane dane
            if (configuration.includeDays) {
                val dataDir = findDirectoryRecursively(tempUnzipDir, "data")
                val datowaneDir = findDirectoryRecursively(tempUnzipDir, "Datowane")
                
                if (dataDir != null && datowaneDir != null) {
                    File(internalStorageRoot, "data").deleteRecursively()
                    File(internalStorageRoot, "Datowane").deleteRecursively()
                    dataDir.copyRecursively(File(internalStorageRoot, "data"), true)
                    datowaneDir.copyRecursively(File(internalStorageRoot, "Datowane"), true)
                }
            }
            
            if (configuration.includeSongs) {
                findFileRecursively(tempUnzipDir, "piesni.json")?.let { songFile ->
                    File(internalStorageRoot, "piesni.json").delete()
                    songFile.copyTo(File(internalStorageRoot, "piesni.json"), true)
                }
            }
            
            if (configuration.includeCategories) {
                findFileRecursively(tempUnzipDir, "kategorie.json")?.let { categoriesFile ->
                    File(internalStorageRoot, "kategorie.json").delete()
                    categoriesFile.copyTo(File(internalStorageRoot, "kategorie.json"), true)
                }
            }
            
            if (configuration.includeTags) {
                findFileRecursively(tempUnzipDir, "tagi.json")?.let { tagsFile ->
                    File(internalStorageRoot, "tagi.json").delete()
                    tagsFile.copyTo(File(internalStorageRoot, "tagi.json"), true)
                }
            }
            
            if (configuration.includeNeumy) {
                findDirectoryRecursively(tempUnzipDir, "neumy")?.let { neumyDir ->
                    File(internalStorageRoot, "neumy").deleteRecursively()
                    neumyDir.copyRecursively(File(internalStorageRoot, "neumy"), true)
                }
            }
            
            if (configuration.includeNotes) {
                findDirectoryRecursively(tempUnzipDir, "notatki")?.let { notesDir ->
                    val targetNotesDir = File(internalStorageRoot, "notatki")
                    // Dla notatek: dopisywanie, nie nadpisywanie
                    if (!targetNotesDir.exists()) {
                        targetNotesDir.mkdirs()
                    }
                    // Kopiuj tylko pliki, które jeszcze nie istnieją
                    notesDir.walkTopDown().filter { it.isFile }.forEach { sourceFile ->
                        val relativePath = sourceFile.relativeTo(notesDir).path
                        val targetFile = File(targetNotesDir, relativePath)
                        if (!targetFile.exists()) {
                            targetFile.parentFile?.mkdirs()
                            sourceFile.copyTo(targetFile, false)
                        }
                    }
                }
            }
            
            if (configuration.includeYears) {
                findDirectoryRecursively(tempUnzipDir, "calendar_data")?.let { calendarDir ->
                    File(internalStorageRoot, "calendar_data").deleteRecursively()
                    calendarDir.copyRecursively(File(internalStorageRoot, "calendar_data"), true)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            tempUnzipDir.deleteRecursively()
            tempZipFile.delete()
        }
    }

    private fun findFileRecursively(directory: File, fileName: String): File? {
        return directory.walkTopDown().find { it.isFile && it.name == fileName }
    }

    private fun findDirectoryRecursively(directory: File, dirName: String): File? {
        return directory.walkTopDown().find { it.isDirectory && it.name == dirName }
    }
}