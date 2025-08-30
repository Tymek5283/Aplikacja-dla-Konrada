// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\index.kt
// Opis: Główny plik repozytorium, który integruje i zarządza wszystkimi operacjami na plikach, delegując zadania do wyspecjalizowanych managerów. Stanowi centralny punkt dostępu do danych aplikacji.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.net.Uri
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.screens.settings.ExportConfiguration
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Month
import kotlin.Result

class FileSystemRepository(val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val internalStorageRoot: File = context.filesDir

    private val cacheManager = CacheManager()
    private val dayFileManager = DayFileManager(context, internalStorageRoot, json)
    private val songFileManager = SongFileManager(context, internalStorageRoot, json, cacheManager, dayFileManager)
    private val categoryFileManager = CategoryFileManager(internalStorageRoot, json, cacheManager, songFileManager)
    private val tagFileManager = TagFileManager(context, internalStorageRoot, json, cacheManager)
    private val directoryManager = DirectoryManager(internalStorageRoot, json)
    private val importExportManager = ImportExportManager(context, internalStorageRoot)
    private val autoBackupManager = AutoBackupManager(context, importExportManager)

    // Cache Management
    fun invalidateSongCache() = cacheManager.invalidateSongCache()
    fun invalidateCategoryCache() = cacheManager.invalidateCategoryCache()
    fun invalidateTagCache() = cacheManager.invalidateTagCache()
    fun invalidateAllCaches() {
        cacheManager.invalidateSongCache()
        cacheManager.invalidateCategoryCache()
        cacheManager.invalidateTagCache()
    }

    // DayFile Operations
    fun getAllDayFilePaths(): List<String> = dayFileManager.getAllDayFilePaths()
    fun getDayData(path: String): DayData? = dayFileManager.getDayData(path)
    fun saveDayData(path: String, dayData: DayData): Result<Unit> = dayFileManager.saveDayData(path, dayData)
    fun getMonthlyFileMap(month: Month): Map<Int, List<String>> = dayFileManager.getMonthlyFileMap(month)

    // SongFile Operations
    fun getSongList(): List<Song> = songFileManager.getSongList()
    fun saveSongList(songs: List<Song>): Result<Unit> = songFileManager.saveSongList(songs)
    fun getSong(title: String, siedlNum: String?, sakNum: String?, dnNum: String?, sak2020Num: String? = null): Song? = songFileManager.getSong(title, siedlNum, sakNum, dnNum, sak2020Num)
    fun updateSongOccurrencesInDayFiles(originalSong: Song, updatedSong: Song): Result<Unit> = songFileManager.updateSongOccurrencesInDayFiles(originalSong, updatedSong)
    fun deleteSong(songToDelete: Song, deleteOccurrences: Boolean): Result<Unit> = songFileManager.deleteSong(songToDelete, deleteOccurrences)

    // CategoryFile Operations
    fun getCategoryList(): List<Category> = categoryFileManager.getCategoryList()
    fun saveCategoryList(categories: List<Category>): Result<Unit> = categoryFileManager.saveCategoryList(categories)
    fun updateCategoryInSongs(oldCategory: Category, newCategory: Category): Result<Unit> = categoryFileManager.updateCategoryInSongs(oldCategory, newCategory)
    fun removeCategoryFromSongs(categoryToRemove: Category): Result<Unit> = categoryFileManager.removeCategoryFromSongs(categoryToRemove)

    // TagFile Operations
    fun getTagList(): List<String> = tagFileManager.getTagList()
    fun saveTagList(tags: List<String>): Result<Unit> = tagFileManager.saveTagList(tags)
    fun addTag(tag: String): Result<Unit> = tagFileManager.addTag(tag)
    fun updateTag(oldTag: String, newTag: String): Result<Unit> = tagFileManager.updateTag(oldTag, newTag, songFileManager)
    fun removeTag(tag: String): Result<Unit> = tagFileManager.removeTag(tag, songFileManager)

    // Directory Operations
    fun getItems(path: String): List<FileSystemItem> = directoryManager.getItems(path)
    fun saveOrder(path: String, orderedNames: List<String>): Result<Unit> = directoryManager.saveOrder(path, orderedNames)
    fun createFolder(path: String, folderName: String): Result<Unit> = directoryManager.createFolder(path, folderName)
    fun createDayFile(path: String, fileName: String, url: String?): Result<String> = directoryManager.createDayFile(path, fileName, url)
    fun deleteItem(itemPath: String): Result<Unit> = directoryManager.deleteItem(itemPath)
    fun renameItem(itemPath: String, newName: String): Result<String> = directoryManager.renameItem(itemPath, newName)

    // Import/Export Operations
    fun exportDataToZip(configuration: ExportConfiguration): Result<File> = importExportManager.exportDataToZip(configuration)
    fun analyzeImportData(uri: Uri): Result<com.qjproject.liturgicalcalendar.ui.screens.settings.AvailableImportData> {
        return importExportManager.analyzeImportData(uri)
    }

    fun importDataFromZip(uri: Uri, configuration: com.qjproject.liturgicalcalendar.ui.screens.settings.ImportConfiguration): Result<Unit> {
        return importExportManager.importDataFromZip(uri, configuration)
    }

    // Auto Backup Operations
    suspend fun performAutoBackupIfNeeded() = autoBackupManager.performAutoBackupIfNeeded()
}