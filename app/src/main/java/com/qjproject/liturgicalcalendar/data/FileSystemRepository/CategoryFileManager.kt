// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\CategoryFileManager.kt
// Opis: Odpowiada za zarządzanie plikiem `kategorie.json`, w tym odczyt, zapis oraz aktualizację i usuwanie kategorii w powiązanych pieśniach.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.util.Log
import com.qjproject.liturgicalcalendar.data.Category
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class CategoryFileManager(
    private val internalStorageRoot: File,
    private val json: Json,
    private val cacheManager: CacheManager,
    private val songFileManager: SongFileManager
) {
    fun getCategoryList(): List<Category> {
        cacheManager.categoryListCache?.let { return it }
        return try {
            val file = File(internalStorageRoot, "kategorie.json")
            if (!file.exists()) {
                Log.e("CategoryManager", "Krytyczny błąd: Plik 'kategorie.json' nie istnieje.")
                return emptyList()
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            val categories = json.decodeFromString<List<Category>>(jsonString)
            cacheManager.setCategoryCache(categories)
            categories
        } catch (e: Exception) {
            Log.e("CategoryManager", "Błąd podczas wczytywania kategorie.json", e)
            emptyList()
        }
    }

    fun saveCategoryList(categories: List<Category>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "kategorie.json")
            val jsonString = json.encodeToString(ListSerializer(Category.serializer()), categories)
            file.writeText(jsonString)
            cacheManager.setCategoryCache(categories)
            Log.d("CategoryManager", "Zapisano pomyślnie listę kategorii.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CategoryManager", "Błąd podczas zapisywania listy kategorii", e)
            Result.failure(e)
        }
    }

    fun updateCategoryInSongs(oldCategory: Category, newCategory: Category): Result<Unit> {
        return try {
            val updatedSongs = songFileManager.getSongList().map { song ->
                if (song.kategoria.equals(oldCategory.nazwa, ignoreCase = true)) {
                    song.copy(kategoria = newCategory.nazwa, kategoriaSkr = newCategory.skrot)
                } else song
            }
            songFileManager.saveSongList(updatedSongs)
        } catch (e: Exception) {
            Log.e("CategoryManager", "Błąd podczas aktualizacji kategorii w pieśniach", e)
            Result.failure(e)
        }
    }

    fun removeCategoryFromSongs(categoryToRemove: Category): Result<Unit> {
        return try {
            val updatedSongs = songFileManager.getSongList().map { song ->
                if (song.kategoria.equals(categoryToRemove.nazwa, ignoreCase = true)) {
                    song.copy(kategoria = "", kategoriaSkr = "")
                } else song
            }
            songFileManager.saveSongList(updatedSongs)
        } catch (e: Exception) {
            Log.e("CategoryManager", "Błąd podczas usuwania kategorii z pieśni", e)
            Result.failure(e)
        }
    }
}