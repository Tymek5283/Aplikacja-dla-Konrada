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

    fun updateTag(oldTag: String, newTag: String): Result<Unit> {
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
        
        val updatedTags = currentTags.map { 
            if (it.equals(oldTag, ignoreCase = true)) trimmedNewTag else it 
        }
        
        return saveTagList(updatedTags)
    }

    fun removeTag(tag: String): Result<Unit> {
        val currentTags = getTagList()
        val updatedTags = currentTags.filter { !it.equals(tag, ignoreCase = true) }
        
        if (updatedTags.size == currentTags.size) {
            return Result.failure(IllegalArgumentException("Tag nie istnieje"))
        }
        
        return saveTagList(updatedTags)
    }
}
