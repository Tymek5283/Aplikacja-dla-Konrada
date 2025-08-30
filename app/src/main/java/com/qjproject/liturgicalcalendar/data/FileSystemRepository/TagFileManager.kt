// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\TagFileManager.kt
// Opis: Odpowiada za zarządzanie plikiem `tagi.json`, w tym odczyt, zapis oraz operacje CRUD na tagach.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

internal class TagFileManager(
    private val context: Context,
    private val internalStorageRoot: File,
    private val json: Json,
    private val cacheManager: CacheManager
) {
    fun getTagList(): List<String> {
        cacheManager.tagListCache?.let { return it }
        return try {
            // Najpierw próbuj odczytać z pamięci wewnętrznej
            val internalFile = File(internalStorageRoot, "tagi.json")
            val jsonString = if (internalFile.exists()) {
                internalFile.bufferedReader().use { it.readText() }
            } else {
                // Jeśli nie ma w pamięci wewnętrznej, odczytaj z assets
                try {
                    context.assets.open("tagi.json").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("TagManager", "Nie można odczytać pliku tagi.json z assets", e)
                    return emptyList()
                }
            }
            
            val tags = json.decodeFromString<List<String>>(jsonString)
            cacheManager.setTagCache(tags)
            tags
        } catch (e: Exception) {
            Log.e("TagManager", "Błąd podczas wczytywania tagi.json", e)
            emptyList()
        }
    }

    fun saveTagList(tags: List<String>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "tagi.json")
            val sortedTags = tags.sorted() // Sortuj alfabetycznie
            val jsonString = json.encodeToString(ListSerializer(String.serializer()), sortedTags)
            file.writeText(jsonString)
            cacheManager.setTagCache(sortedTags)
            Log.d("TagManager", "Zapisano pomyślnie listę tagów.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TagManager", "Błąd podczas zapisywania listy tagów", e)
            Result.failure(e)
        }
    }

    fun addTag(tag: String): Result<Unit> {
        val trimmedTag = tag.trim()
        if (trimmedTag.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tag nie może być pusty"))
        }
        
        val currentTags = getTagList()
        if (currentTags.any { it.equals(trimmedTag, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Tag już istnieje"))
        }
        
        return saveTagList(currentTags + trimmedTag)
    }

    fun updateTag(oldTag: String, newTag: String, songFileManager: SongFileManager): Result<Unit> {
        val trimmedNewTag = newTag.trim()
        if (trimmedNewTag.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tag nie może być pusty"))
        }
        
        val currentTags = getTagList()
        if (!currentTags.any { it.equals(oldTag, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Stary tag nie istnieje"))
        }
        
        if (currentTags.any { it.equals(trimmedNewTag, ignoreCase = true) && !it.equals(oldTag, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Tag o tej nazwie już istnieje"))
        }
        
        // Najpierw aktualizuj tagi w pieśniach
        val updateSongsResult = updateTagInAllSongs(oldTag, trimmedNewTag, songFileManager)
        if (updateSongsResult.isFailure) {
            return updateSongsResult
        }
        
        // Następnie aktualizuj listę tagów
        val updatedTags = currentTags.map { 
            if (it.equals(oldTag, ignoreCase = true)) trimmedNewTag else it 
        }
        
        val result = saveTagList(updatedTags)
        
        // Invaliduj cache po pomyślnej aktualizacji
        if (result.isSuccess) {
            cacheManager.invalidateSongCache() // Invaliduj cache pieśni bo tagi się zmieniły
        }
        
        return result
    }

    private fun updateTagInAllSongs(oldTag: String, newTag: String, songFileManager: SongFileManager): Result<Unit> {
        return try {
            val songs = songFileManager.getSongList()
            var hasChanges = false
            
            val updatedSongs = songs.map { song ->
                if (song.tagi.any { it.equals(oldTag, ignoreCase = true) }) {
                    hasChanges = true
                    val updatedTags = song.tagi.map { tag ->
                        if (tag.equals(oldTag, ignoreCase = true)) newTag else tag
                    }
                    song.copy(tagi = updatedTags)
                } else {
                    song
                }
            }
            
            if (hasChanges) {
                songFileManager.saveSongList(updatedSongs).getOrThrow()
                Log.d("TagManager", "Zaktualizowano tagi w ${updatedSongs.count { it.tagi.contains(newTag) }} pieśniach")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TagManager", "Błąd podczas aktualizacji tagów w pieśniach", e)
            Result.failure(e)
        }
    }

    fun removeTag(tag: String, songFileManager: SongFileManager): Result<Unit> {
        val currentTags = getTagList()
        val updatedTags = currentTags.filter { !it.equals(tag, ignoreCase = true) }
        
        if (updatedTags.size == currentTags.size) {
            return Result.failure(IllegalArgumentException("Tag nie istnieje"))
        }
        
        // Najpierw usuń tag z wszystkich pieśni
        val removeSongsResult = removeTagFromAllSongs(tag, songFileManager)
        if (removeSongsResult.isFailure) {
            return removeSongsResult
        }
        
        // Następnie usuń tag z listy tagów
        val result = saveTagList(updatedTags)
        
        // Invaliduj cache po pomyślnym usunięciu
        if (result.isSuccess) {
            cacheManager.invalidateSongCache() // Invaliduj cache pieśni bo tagi się zmieniły
        }
        
        return result
    }

    private fun removeTagFromAllSongs(tagToRemove: String, songFileManager: SongFileManager): Result<Unit> {
        return try {
            val songs = songFileManager.getSongList()
            var hasChanges = false
            
            val updatedSongs = songs.map { song ->
                if (song.tagi.any { it.equals(tagToRemove, ignoreCase = true) }) {
                    hasChanges = true
                    val updatedTags = song.tagi.filter { !it.equals(tagToRemove, ignoreCase = true) }
                    song.copy(tagi = updatedTags)
                } else {
                    song
                }
            }
            
            if (hasChanges) {
                songFileManager.saveSongList(updatedSongs).getOrThrow()
                Log.d("TagManager", "Usunięto tag '$tagToRemove' z ${songs.count { it.tagi.any { tag -> tag.equals(tagToRemove, ignoreCase = true) } }} pieśni")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TagManager", "Błąd podczas usuwania tagu z pieśni", e)
            Result.failure(e)
        }
    }
}
