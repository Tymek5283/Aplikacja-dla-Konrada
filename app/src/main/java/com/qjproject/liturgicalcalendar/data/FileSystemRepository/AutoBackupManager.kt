// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/data/repository/FileSystemRepository/AutoBackupManager.kt
// Opis: Zarządza automatycznym tworzeniem miesięcznych kopii zapasowych danych aplikacji w tle, bez ingerencji użytkownika.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.qjproject.liturgicalcalendar.ui.screens.settings.ExportConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

internal class AutoBackupManager(
    private val context: Context,
    private val importExportManager: ImportExportManager
) {
    companion object {
        private const val TAG = "AutoBackupManager"
        private const val BACKUP_FOLDER_NAME = "Laudate"
        private const val MAX_BACKUP_FILES = 3
        private val BACKUP_FILE_PATTERN = Regex("^\\d{4}-\\d{2}\\.zip$")
    }

    /**
     * Główna funkcja wykonująca automatyczną kopię zapasową.
     * Wywoływana przy każdym uruchomieniu aplikacji.
     */
    suspend fun performAutoBackupIfNeeded() {
        try {
            withContext(Dispatchers.IO) {
                val backupFolder = getOrCreateBackupFolder()
                if (backupFolder == null) {
                    Log.w(TAG, "Nie można utworzyć folderu kopii zapasowych")
                    return@withContext
                }

                val currentMonthFileName = getCurrentMonthFileName()
                val currentMonthFile = File(backupFolder, currentMonthFileName)

                // Sprawdź czy kopia dla bieżącego miesiąca już istnieje
                if (currentMonthFile.exists()) {
                    Log.d(TAG, "Kopia zapasowa dla bieżącego miesiąca już istnieje: $currentMonthFileName")
                    return@withContext
                }

                // Utwórz nową kopię zapasową
                createAutoBackup(backupFolder, currentMonthFileName)

                // Wykonaj rotację - usuń najstarsze kopie jeśli przekroczono limit
                performBackupRotation(backupFolder)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas tworzenia automatycznej kopii zapasowej", e)
        }
    }

    /**
     * Pobiera lub tworzy folder Laudate w katalogu Downloads.
     */
    private fun getOrCreateBackupFolder(): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val backupFolder = File(downloadsDir, BACKUP_FOLDER_NAME)
            if (!backupFolder.exists()) {
                val created = backupFolder.mkdirs()
                if (!created) {
                    Log.e(TAG, "Nie można utworzyć folderu kopii zapasowych: ${backupFolder.absolutePath}")
                    return null
                }
                Log.d(TAG, "Utworzono folder kopii zapasowych: ${backupFolder.absolutePath}")
            }

            backupFolder
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas tworzenia folderu kopii zapasowych", e)
            null
        }
    }

    /**
     * Generuje nazwę pliku dla bieżącego miesiąca w formacie YYYY-MM.zip.
     */
    private fun getCurrentMonthFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return "${dateFormat.format(Date())}.zip"
    }

    /**
     * Tworzy automatyczną kopię zapasową z wszystkimi dostępnymi danymi.
     */
    private suspend fun createAutoBackup(backupFolder: File, fileName: String) {
        try {
            // Konfiguracja z wszystkimi opcjami włączonymi (domyślne wartości w ExportConfiguration)
            val fullExportConfiguration = ExportConfiguration(
                includeSongs = true,
                includeDays = true,
                includeCategories = true,
                includeTags = true,
                includeNeumy = true,
                includeNotes = true,
                includeYears = true
            )

            // Użyj istniejącej logiki eksportu
            val exportResult = importExportManager.exportDataToZip(fullExportConfiguration)
            
            exportResult.fold(
                onSuccess = { tempExportFile ->
                    // Przenieś plik z domyślnej lokalizacji do folderu Laudate z odpowiednią nazwą
                    val targetFile = File(backupFolder, fileName)
                    val moved = tempExportFile.renameTo(targetFile)
                    
                    if (moved) {
                        Log.d(TAG, "Utworzono automatyczną kopię zapasową: ${targetFile.absolutePath}")
                    } else {
                        // Jeśli rename nie zadziałał, spróbuj skopiować
                        tempExportFile.copyTo(targetFile, overwrite = true)
                        tempExportFile.delete()
                        Log.d(TAG, "Skopiowano automatyczną kopię zapasową: ${targetFile.absolutePath}")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Błąd podczas tworzenia kopii zapasowej", exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas tworzenia automatycznej kopii zapasowej", e)
        }
    }

    /**
     * Wykonuje rotację kopii zapasowych - usuwa najstarsze pliki jeśli przekroczono limit.
     */
    private fun performBackupRotation(backupFolder: File) {
        try {
            // Znajdź wszystkie pliki kopii zapasowych (pasujące do wzorca YYYY-MM.zip)
            val backupFiles = backupFolder.listFiles { file ->
                file.isFile && BACKUP_FILE_PATTERN.matches(file.name)
            }?.toList() ?: emptyList()

            if (backupFiles.size <= MAX_BACKUP_FILES) {
                Log.d(TAG, "Liczba kopii zapasowych (${backupFiles.size}) nie przekracza limitu ($MAX_BACKUP_FILES)")
                return
            }

            // Posortuj pliki według nazwy (która odpowiada dacie YYYY-MM)
            val sortedFiles = backupFiles.sortedBy { it.name }
            
            // Usuń najstarsze pliki, pozostawiając tylko MAX_BACKUP_FILES najnowszych
            val filesToDelete = sortedFiles.take(sortedFiles.size - MAX_BACKUP_FILES)
            
            filesToDelete.forEach { file ->
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Usunięto starą kopię zapasową: ${file.name}")
                } else {
                    Log.w(TAG, "Nie można usunąć starej kopii zapasowej: ${file.name}")
                }
            }

            Log.d(TAG, "Rotacja kopii zapasowych zakończona. Usunięto ${filesToDelete.size} plików.")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas rotacji kopii zapasowych", e)
        }
    }
}
